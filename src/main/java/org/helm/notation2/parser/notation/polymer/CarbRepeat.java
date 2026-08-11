package org.helm.notation2.parser.notation.polymer;

import java.util.List;

import org.helm.notation2.parser.exceptionparser.HELM1ConverterException;
import org.helm.notation2.parser.exceptionparser.NotationException;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A CARB repeating group: a whole CARB sub-chain that is repeated a number of
 * times, written {@code (<sub-chain>)'count'} - e.g.
 * {@code (R4:[a-D-Glcp].R3:[a-D-Glcp])'3'}. The repeated unit is a plain chain
 * of {@link CarbMonomerNotationUnit}s (which may themselves carry branches); it
 * has no trailing convergence R-group, which is what distinguishes it from a
 * branch.
 *
 * <p>Following how the other HELM polymer types already behave, an
 * <em>integer</em> count is expanded into concrete structure during numbering
 * (see {@link PolymerNotation}); a <em>non-integer</em> count (a range, or the
 * open-ended {@code n}) parses, validates and round-trips, but is a build-time
 * dead end - {@link #hasIntegerCount()} lets the molecule builder reject it with
 * a clear error rather than mis-build it.
 *
 * <p>The {@code count} itself is captured by the existing tokenizer via
 * {@link MonomerNotation#setCount(String)} after this object is created, exactly
 * as for a repeated peptide/RNA monomer.
 */
public class CarbRepeat extends MonomerNotation {

  private final List<CarbMonomerNotationUnit> chain;

  /**
   * @param content the sub-chain text between the enclosing parentheses (not
   *          including them), e.g. "R4:[a-D-Glcp].R3:[a-D-Glcp]"
   * @param type polymer type, always "CARB"
   * @throws NotationException if the sub-chain is empty or malformed
   */
  public CarbRepeat(String content, String type) throws NotationException {
    super(content, type);
    this.chain = CarbMonomerNotationParser.parseChain(content, type);
  }

  /**
   * @return the ordered monomers of the repeated sub-chain (one repetition)
   */
  public List<CarbMonomerNotationUnit> getChain() {
    return chain;
  }

  /**
   * @return true if this repeat's count is a concrete positive integer (so it
   *         can be expanded and built), false for a range / {@code n} / other
   *         non-integer count
   */
  @JsonIgnore
  public boolean hasIntegerCount() {
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
    sb.append('(');
    for (int i = 0; i < chain.size(); i++) {
      if (i > 0) {
        sb.append('.');
      }
      sb.append(chain.get(i).toHELM2());
    }
    sb.append(')');
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
