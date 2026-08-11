/**
 * *****************************************************************************
 * Copyright C 2015, The Pistoia Alliance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *****************************************************************************
 */
package org.helm.notation2.parser.notation.polymer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helm.notation2.parser.exceptionparser.HELM1ConverterException;
import org.helm.notation2.parser.exceptionparser.NotationException;
import org.helm.notation2.parser.notation.ValidationMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * PolymerNotation class to represent a polymer with its content
 *
 * @author hecht
 */
public final class PolymerNotation {

  private PolymerEntity polymerID;

  private String annotation;

  @JsonIgnore
  private boolean annotationHere = false;

  private PolymerElements polymerElements = null;

  @JsonIgnore
  private Map<Integer, MonomerNotation> mapOfMonomers = new HashMap<Integer, MonomerNotation>();

  @JsonIgnore
  private Map<String, String> mapIntraConnection = new HashMap<String, String>();;

  @JsonIgnore
  private List<CarbEdge> carbEdges = new ArrayList<CarbEdge>();

  public PolymerNotation() {

  }

  /**
   * Constructs with the given String
   *
   * @param str polymer ID
   * @throws NotationException if notation is not valid
   */
  public PolymerNotation(String str) throws NotationException {
    polymerID = (PolymerEntity) ValidationMethod.decideWhichEntity(str);
    setPolymerElements();
  }

  /**
   * Constructs with a given PolymerEnttiy, PolymerElements
   *
   * @param poly PolymerEntity
   * @param ele PolymerElements
   * @throws NotationException if notation is not valid
   */
  public PolymerNotation(PolymerEntity poly, PolymerElements ele) throws NotationException {
    this.polymerID = poly;
    this.polymerElements = ele;
  }

  /**
   * Constructs with a given PolymerEnttiy, PolymerElements and an annotation
   *
   * @param poly PolymerEntity
   * @param ele PolymerElements
   * @param anno new annotation
   */
  public PolymerNotation(PolymerEntity poly, PolymerElements ele, String anno) {
    this.polymerID = poly;
    this.polymerElements = ele;
    if (anno != null) {
      setAnnotation(anno);
    }
  }

  /**
   * method to generate the right PolymerElements, in the case of Chem and Blob
   * only one Monomer is allowed
   */
  private void setPolymerElements() {
    if (polymerID instanceof RNAEntity || polymerID instanceof PeptideEntity || polymerID instanceof CarbEntity) {
      this.polymerElements = new PolymerListElements(polymerID);
    } else {
      this.polymerElements = new PolymerSingleElements(polymerID);
    }

  }

  /**
   * method to add/set the annotation
   *
   * @param str new annotation
   */
  private void setAnnotation(String str) {
    this.annotation = str;
    if (str != null) {
      this.annotationHere = true;
    }
  }

  /**
   * method to get the polymer entity
   *
   * @return polymer ID
   */
  public PolymerEntity getPolymerID() {
    return this.polymerID;
  }

  /**
   * method to get the PolymerElements
   *
   * @return PolymerElements
   */
  public PolymerElements getPolymerElements() {
    return this.polymerElements;
  }

  /**
   * method to get the annotation of the simple polymer
   *
   * @return annotation
   */
  public String getAnnotation() {
    return this.annotation;
  }

  /**
   * method to check if an annotation is there
   *
   * @return true if the annotation is there, false otherwise
   */
  public boolean isAnnotationHere() {
    return this.annotationHere;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    if (isAnnotationHere()) {
      return "PolymerID: " + polymerID + "\nElements: " + polymerElements.toString() + "Annotation: " + annotation;
    } else {
      return "PolymerID: " + polymerID + "\nElements: " + polymerElements.toString();
    }

  }

  /**
   * method to generate a valid HELM2 notation for this object
   *
   * @return valid HELM2 notation
   */
  public String toHELM2() {
    return this.polymerElements.toHELM2();
  }

  /**
   * method to generate a valid HELM notation for this object
   *
   * @return HELM notation in string format
   * @throws HELM1ConverterException if object can not downgraded to HELM1-Format
   */
  public String toHELM() throws HELM1ConverterException {
    if (polymerID instanceof BlobEntity) {
      throw new HELM1ConverterException("Can't be downgraded to HELM1-Format");
    }
    return this.polymerElements.toHELM();
  }

  @JsonIgnore
  public MonomerNotation getMonomerNotation(int count) {
    initializeMapOfMonomersAndMapOfIntraConnection();

    if (mapOfMonomers.containsKey(count)) {
      return mapOfMonomers.get(count);
    }
    return null;
  }

  public void initializeMapOfMonomersAndMapOfIntraConnection() {
    mapOfMonomers.clear();
    mapIntraConnection.clear();
    if (polymerID instanceof CarbEntity) {
      carbEdges.clear();
      List<CarbMonomerNotationUnit> chain = new ArrayList<CarbMonomerNotationUnit>();
      for (MonomerNotation element : polymerElements.getListOfElements()) {
        if (!(element instanceof CarbMonomerNotationUnit)) {
          throw new IllegalStateException(
              "CARB polymer element is not a CarbMonomerNotationUnit (found "
                  + element.getClass().getSimpleName() + "); CARB does not support "
                  + "group/mixture/list monomer syntax: " + element);
        }
        chain.add((CarbMonomerNotationUnit) element);
      }
      numberCarbChain(chain, new int[] {0}, 0);
      return;
    }
    int multiply = 1;
    int value = 0;
    int lastValue = -1;
    for (MonomerNotation element : polymerElements.getListOfElements()) {
      try {
        // multiply = Integer.parseInt(element.getCount());
        multiply = 1;
        if (multiply < 1) {
          multiply = 1;
        }
      } catch (NumberFormatException ex) {
        multiply = 1;
      }

// if (element instanceof MonomerNotationList) {
// for (int z = 0; z < multiply; z++) {
// for (MonomerNotation monomerNotationUnit : ((MonomerNotationList)
// element).getListofMonomerUnits()) {
// value++;
// lastValue++;
// mapOfMonomers.put(value, monomerNotationUnit);
// if (lastValue != 0) {
// mapIntraConnection.put(lastValue + "$R2", "");
// mapIntraConnection.put(value + "$R1", "");
// }
// }
// }
// }
      if (element instanceof MonomerNotationUnitRNA) {
        for (int z = 0; z < multiply; z++) {
          lastValue = value;
          for (MonomerNotationUnit monomerNotationUnit : ((MonomerNotationUnitRNA) element).getContents()) {
            value++;
            mapOfMonomers.put(value, monomerNotationUnit);
          }

          /* Intra nucleotide Connections will be not scanned */
          if (value >= 4) {
            mapIntraConnection.put(lastValue + "$R2", "");
            int val = lastValue + 1;
            mapIntraConnection.put(val + "$R1", "");
          }
        }

      } else {
        for (int z = 0; z < multiply; z++) {
          value++;
          lastValue++;
          mapOfMonomers.put(value, element);
          if (lastValue != 0) {
            mapIntraConnection.put(lastValue + "$R2", "");
            mapIntraConnection.put(value + "$R1", "");
          }
        }

      }
    }
  }

  /**
   * Numbers one sequential CARB chain - either the polymer's top-level list,
   * or a branch's own chain - depth-first in text order, and records every
   * intra-polymer R-group edge (main-chain and branch-convergence) into
   * {@link #mapIntraConnection}. Descends into each monomer's own branches
   * immediately after numbering it and before moving on to the next chain
   * element, matching how branches are written directly after the trunk
   * monomer they attach to (see {@link CarbMonomerNotationParser}).
   *
   * @param chain the chain to number
   * @param counter single-element mutable holder for the next position to
   *          assign, shared across the whole recursive walk
   * @param precedingPosition position of the monomer whose own anomeric
   *          carbon bonds into the first element of {@code chain}, or 0 if
   *          there is none (the very first monomer of the polymer, or the
   *          first monomer of a branch)
   * @return the position assigned to the last element of {@code chain}
   */
  private int numberCarbChain(List<CarbMonomerNotationUnit> chain, int[] counter, int precedingPosition) {
    int lastPosition = precedingPosition;
    for (CarbMonomerNotationUnit unit : chain) {
      counter[0]++;
      int position = counter[0];
      mapOfMonomers.put(position, unit);

      if (lastPosition != 0) {
        CarbMonomerNotationUnit previous = (CarbMonomerNotationUnit) mapOfMonomers.get(lastPosition);
        String outgoing = previous.getAnomericRGroup();
        String incoming = unit.getIncomingRGroup() == null ? "R1" : unit.getIncomingRGroup();
        mapIntraConnection.put(lastPosition + "$" + outgoing, "");
        mapIntraConnection.put(position + "$" + incoming, "");
        carbEdges.add(new CarbEdge(lastPosition, outgoing, position, incoming));
      }

      for (CarbBranch branch : unit.getBranches()) {
        int branchLastPosition = numberCarbChain(branch.getChain(), counter, 0);
        CarbMonomerNotationUnit branchLastUnit = (CarbMonomerNotationUnit) mapOfMonomers.get(branchLastPosition);
        mapIntraConnection.put(branchLastPosition + "$" + branchLastUnit.getAnomericRGroup(), "");
        mapIntraConnection.put(position + "$" + branch.getConvergenceRGroup(), "");
        carbEdges.add(new CarbEdge(branchLastPosition, branchLastUnit.getAnomericRGroup(), position, branch.getConvergenceRGroup()));
      }

      lastPosition = position;
    }
    return lastPosition;
  }

  @JsonIgnore
  public Map<String, String> getMapIntraConnection() {
    return mapIntraConnection;
  }

  /**
   * method to get the resolved intra-polymer bond graph for a CARB polymer -
   * empty for every other polymer type.
   *
   * @return list of intra-polymer edges, in the order they were parsed
   */
  @JsonIgnore
  public List<CarbEdge> getCarbEdges() {
    initializeMapOfMonomersAndMapOfIntraConnection();
    return carbEdges;
  }

  @JsonIgnore
  public List<MonomerNotation> getListMonomers() {
    List<MonomerNotation> listMonomerNotation = new ArrayList<MonomerNotation>();
    for (MonomerNotation monomerNotation : polymerElements.getListOfElements()) {
      if (monomerNotation instanceof MonomerNotationUnit) {
        listMonomerNotation.add(monomerNotation);
      } else if (monomerNotation instanceof CarbMonomerNotationUnit) {
        collectCarbMonomers((CarbMonomerNotationUnit) monomerNotation, listMonomerNotation);
      } else {
        if (monomerNotation instanceof MonomerNotationGroup) {
          for (MonomerNotationGroupElement groupElement : ((MonomerNotationGroup) monomerNotation).getListOfElements()) {
            listMonomerNotation.add(groupElement.getMonomerNotation());
          }
        }
        if (monomerNotation instanceof MonomerNotationList) {
          listMonomerNotation.addAll(((MonomerNotationList) monomerNotation).getListofMonomerUnits());
        }
      }
    }
    return listMonomerNotation;
  }

  /**
   * Recursively collects one CARB monomer and every monomer nested in its
   * branches (and their nested branches), depth-first in text order, so that
   * callers that need every monomer of a polymer - e.g. for monomer
   * validation - see branch-nested CARB monomers too, not just the top-level
   * chain.
   */
  private void collectCarbMonomers(CarbMonomerNotationUnit unit, List<MonomerNotation> collected) {
    collected.add(unit);
    for (CarbBranch branch : unit.getBranches()) {
      for (CarbMonomerNotationUnit branchUnit : branch.getChain()) {
        collectCarbMonomers(branchUnit, collected);
      }
    }
  }

}
