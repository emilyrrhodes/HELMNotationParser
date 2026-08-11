package org.helm.notation2.parser.notation.polymer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helm.notation2.parser.exceptionparser.HELM1ConverterException;

/**
 * A single CARB (carbohydrate) monomer within a polymer chain, together with
 * the R-group it is attached to (the R-group on this monomer that receives
 * the bond from the previous monomer's anomeric carbon), an optional override
 * of its own anomeric position (needed for ketoses, whose anomeric carbon is
 * not R1), and zero or more branches converging onto it.
 *
 * Unlike {@link MonomerNotationUnit}, which other polymer types use for a
 * bare, unstructured monomer token, CARB monomers carry this extra linkage
 * information so that it survives round-trip serialization and can drive
 * chemical structure generation - see the "R{@literal <n>}:" grammar
 * described in {@link CarbMonomerNotationParser}.
 */
public class CarbMonomerNotationUnit extends MonomerNotation {

  /** R-group on this monomer that the previous monomer's anomeric carbon bonds into; null for the very first monomer in a chain or the first monomer of a branch. */
  private final String incomingRGroup;

  /** This monomer's own anomeric (donor) R-group; "R1" unless overridden for a ketose. */
  private final String anomericRGroup;

  private final List<CarbBranch> branches = new ArrayList<CarbBranch>();

  /**
   * @param residue the bracketed residue notation, e.g. "[a-D-Glcp]"
   * @param type polymer type, always "CARB"
   * @param incomingRGroup R-group receiving the bond from the previous monomer, or null if none
   * @param anomericRGroup this monomer's own anomeric R-group; defaults to "R1" if null
   */
  public CarbMonomerNotationUnit(String residue, String type, String incomingRGroup, String anomericRGroup) {
    super(residue, type);
    this.incomingRGroup = incomingRGroup;
    this.anomericRGroup = (anomericRGroup == null) ? "R1" : anomericRGroup;
  }

  public String getIncomingRGroup() {
    return incomingRGroup;
  }

  public String getAnomericRGroup() {
    return anomericRGroup;
  }

  public void addBranch(CarbBranch branch) {
    branches.add(branch);
  }

  public List<CarbBranch> getBranches() {
    return Collections.unmodifiableList(branches);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toHELM2() {
    StringBuilder sb = new StringBuilder();
    if (incomingRGroup != null) {
      sb.append(incomingRGroup).append(':');
    }
    sb.append(unit);
    if (!"R1".equalsIgnoreCase(anomericRGroup)) {
      sb.append(':').append(anomericRGroup);
    }
    for (CarbBranch branch : branches) {
      sb.append(branch.toHELM2());
    }
    if (isAnnotationTrue()) {
      sb.append('"').append(getAnnotation()).append('"');
    }
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @throws HELM1ConverterException always - CARB is a HELM2-only polymer type
   *           with no HELM1 equivalent
   */
  @Override
  public String toHELM() throws HELM1ConverterException {
    throw new HELM1ConverterException("CARB polymer type is a HELM2-only feature and cannot be downgraded to HELM1");
  }

}
