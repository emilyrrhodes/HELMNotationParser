package org.helm.notation2.parser.notation.polymer;

import java.util.Collections;
import java.util.List;

/**
 * A single branch attached to a CARB trunk monomer.
 *
 * Per the CARB notation grammar, branches converge toward the reducing end
 * rather than diverging like SMILES branches: the branch's own chain is read
 * left to right starting from its non-reducing end, and the branch's last
 * monomer uses its own anomeric carbon (R1, or its stated anomeric override)
 * to bond into {@link #getConvergenceRGroup()} of the trunk monomer this
 * branch is attached to.
 */
public class CarbBranch {

  private final List<CarbMonomerNotationUnit> chain;

  private final String convergenceRGroup;

  /**
   * @param chain the branch's own monomer chain, in left-to-right (non-reducing to reducing) order
   * @param convergenceRGroup the R-group (e.g. "R4", or "R?" if unknown) of the trunk monomer that
   *          the last monomer in {@code chain} attaches to via its own anomeric carbon
   */
  public CarbBranch(List<CarbMonomerNotationUnit> chain, String convergenceRGroup) {
    this.chain = chain;
    this.convergenceRGroup = convergenceRGroup;
  }

  public List<CarbMonomerNotationUnit> getChain() {
    return Collections.unmodifiableList(chain);
  }

  public String getConvergenceRGroup() {
    return convergenceRGroup;
  }

  /**
   * method to generate a valid HELM2 substring for this branch, including the
   * enclosing parentheses and trailing convergence R-group
   *
   * @return valid HELM2 branch notation, e.g. "([a-D-Glcp].R2)"
   */
  public String toHELM2() {
    StringBuilder sb = new StringBuilder("(");
    for (int i = 0; i < chain.size(); i++) {
      if (i > 0) {
        sb.append('.');
      }
      sb.append(chain.get(i).toHELM2());
    }
    sb.append('.').append(convergenceRGroup).append(')');
    return sb.toString();
  }

}
