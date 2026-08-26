package org.helm.notation2.parser.notation.polymer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.helm.notation2.parser.exceptionparser.HELM1ConverterException;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

  private static final Pattern INTEGER_COUNT = Pattern.compile("\\d+");

  /** R-group on this monomer that the previous monomer's anomeric carbon bonds into; null for the very first monomer in a chain or the first monomer of a branch. */
  private final String incomingRGroup;

  /** This monomer's own anomeric (donor) R-group; "R1" unless overridden for a ketose; null for an unknown monomer, which carries no connection points. */
  private final String anomericRGroup;

  /** True for a fully-unknown monomer ("X", "*", or "?"), which per the proposal has no connection points and so participates in no bonds. */
  private final boolean unknown;

  private final List<CarbBranch> branches = new ArrayList<CarbBranch>();

  /**
   * @param residue the bracketed residue notation, e.g. "[a-D-Glcp]"
   * @param type polymer type, always "CARB"
   * @param incomingRGroup R-group receiving the bond from the previous monomer, or null if none
   * @param anomericRGroup this monomer's own anomeric R-group; defaults to "R1" if null
   */
  public CarbMonomerNotationUnit(String residue, String type, String incomingRGroup, String anomericRGroup) {
    this(residue, type, incomingRGroup, anomericRGroup, false);
  }

  /**
   * @param residue the residue notation - bracketed for a known monomer, or a bare
   *          "X"/"*"/"?" for an unknown one
   * @param type polymer type, always "CARB"
   * @param incomingRGroup R-group receiving the bond from the previous monomer, or null if none
   * @param anomericRGroup this monomer's own anomeric R-group; defaults to "R1" if null,
   *          unless {@code unknown} is set, in which case it is left null (no connection points)
   * @param unknown true for a fully-unknown monomer, which carries no connection points
   */
  public CarbMonomerNotationUnit(String residue, String type, String incomingRGroup, String anomericRGroup, boolean unknown) {
    super(residue, type);
    this.unknown = unknown;
    this.incomingRGroup = incomingRGroup;
    this.anomericRGroup = unknown ? anomericRGroup : ((anomericRGroup == null) ? "R1" : anomericRGroup);
  }

  /**
   * @return true if this is a fully-unknown monomer ("X", "*", or "?"), which
   *         carries no connection points and participates in no bonds
   */
  public boolean isUnknown() {
    return unknown;
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
   * @return true when this known CARB monomer has a concrete positive integer
   *         count that can be expanded into a molecule
   */
  @JsonIgnore
  public boolean hasIntegerCount() {
    if (unknown) {
      return false;
    }
    if (!INTEGER_COUNT.matcher(getCount().trim()).matches()) {
      return false;
    }
    try {
      return Integer.parseInt(getCount().trim()) >= 1;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toHELM2() {
    StringBuilder sb = new StringBuilder();
    if (unknown) {
      // A fully-unknown monomer is just its bare token ("X"/"*"/"?") - no
      // attachment prefix, no anomeric override, no branches.
      sb.append(unit);
      if (isAnnotationTrue()) {
        sb.append('"').append(getAnnotation()).append('"');
      }
      return sb.toString();
    }
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
    if (!isDefault) {
      sb.append('\'').append(count).append('\'');
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
