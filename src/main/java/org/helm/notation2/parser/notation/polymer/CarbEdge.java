package org.helm.notation2.parser.notation.polymer;

/**
 * One resolved intra-polymer bond within a CARB polymer: monomer
 * {@code sourcePosition}'s {@code sourceRGroup} attachment point bonds to
 * monomer {@code targetPosition}'s {@code targetRGroup} attachment point.
 * Monomer positions match {@link PolymerNotation#getMonomerNotation(int)}'s
 * numbering (left-to-right, depth-first, including branch-nested monomers).
 *
 * Unlike {@link PolymerNotation#getMapIntraConnection()}, which only records
 * that a given (position, R-group) slot is consumed by an intra-polymer bond
 * (matching the convention already used for RNA/PEPTIDE), this class records
 * the actual pairing - which is what structure-building needs to know which
 * two attachment points to merge.
 */
public class CarbEdge {

  private final int sourcePosition;

  private final String sourceRGroup;

  private final int targetPosition;

  private final String targetRGroup;

  public CarbEdge(int sourcePosition, String sourceRGroup, int targetPosition, String targetRGroup) {
    this.sourcePosition = sourcePosition;
    this.sourceRGroup = sourceRGroup;
    this.targetPosition = targetPosition;
    this.targetRGroup = targetRGroup;
  }

  public int getSourcePosition() {
    return sourcePosition;
  }

  public String getSourceRGroup() {
    return sourceRGroup;
  }

  public int getTargetPosition() {
    return targetPosition;
  }

  public String getTargetRGroup() {
    return targetRGroup;
  }

}
