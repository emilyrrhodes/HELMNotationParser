package org.helm.notation2.parser.notation.polymer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helm.notation2.parser.exceptionparser.NotationException;

/**
 * Recursive-descent parser for a single CARB monomer token, as delimited by
 * the existing bracket/paren-depth-aware "." tokenizer in
 * {@code SimplePolymersNotationParser} (which requires no changes for CARB -
 * it already produces one balanced-paren token per "." separated main-chain
 * element, and a trunk monomer's attached branch(es) are written directly
 * adjacent to it with no separator, so they arrive as part of the same
 * token).
 *
 * Grammar for one token:
 * <pre>
 *   token   := (leadingR ':')? '[' residue ']' (':' anomericR)? branch*
 *   branch  := '(' chain '.' convergenceR ')'
 *   chain   := token ('.' token)*
 *   leadingR, anomericR, convergenceR := 'R' (digits | '?')
 * </pre>
 *
 * A branch's own chain follows the same {@code token} grammar recursively
 * (including further nested branches); its first element has no leading
 * R-group, since it isn't attached to anything - only its own trailing
 * anomeric carbon is used, via the branch's convergence R-group.
 */
public final class CarbMonomerNotationParser {

  private static final Pattern LEADING_R = Pattern.compile("^([Rr](?:\\d+|\\?)):");

  private static final Pattern TRAILING_R = Pattern.compile("^:([Rr](?:\\d+|\\?))");

  private static final Pattern BARE_R = Pattern.compile("^[Rr](?:\\d+|\\?)$");

  /** A fully-unknown monomer: "X" (a single unknown residue), "*" (0..n), or "?" (unknown). Per the proposal these carry no connection points. */
  private static final Pattern UNKNOWN_MONOMER = Pattern.compile("^[Xx*?]$");

  private CarbMonomerNotationParser() {
  }

  /**
   * Parses one compound CARB monomer token into a monomer node, including any
   * branches attached to it.
   *
   * @param token e.g. "R4:[a-D-Glcp]", "[a-D-Glcp]", or "R3:[a-D-Glcp]([a-D-Glcp].R2)"
   * @param type polymer type, always "CARB"
   * @return the parsed monomer node
   * @throws NotationException if the token does not follow CARB grammar
   */
  public static CarbMonomerNotationUnit parseToken(String token, String type) throws NotationException {
    // A fully-unknown monomer ("X"/"*"/"?") stands alone: no leading "R<n>:"
    // attachment, no brackets, no anomeric override, no branches. It carries no
    // connection points, so it participates in no bonds (see numbering).
    if (UNKNOWN_MONOMER.matcher(token).matches()) {
      return new CarbMonomerNotationUnit(token, type, null, null, true);
    }

    String remainder = token;

    String incomingRGroup = null;
    Matcher leading = LEADING_R.matcher(remainder);
    if (leading.find()) {
      incomingRGroup = leading.group(1).toUpperCase();
      remainder = remainder.substring(leading.end());
    }

    if (remainder.isEmpty() || remainder.charAt(0) != '[') {
      throw new NotationException("CARB monomer has to be in brackets: " + token);
    }
    int closeBracket = remainder.indexOf(']');
    if (closeBracket < 0) {
      throw new NotationException("CARB monomer is missing a closing bracket: " + token);
    }
    String residue = remainder.substring(0, closeBracket + 1);
    remainder = remainder.substring(closeBracket + 1);

    // Fails fast on malformed residue content (e.g. "[]", "[???]") that the
    // bracket-only check above does not catch. The parsed anomer/absolute-
    // configuration/base-name decomposition is intentionally transient - it is
    // used only to validate the residue's shape here; downstream code re-derives
    // the base name from the raw residue when it needs it (see the toolkit's
    // Validation.resolveCarbMonomer), so nothing is stored on the unit.
    CarbMonomerParser.parse(residue.substring(1, residue.length() - 1));

    String anomericRGroup = null;
    Matcher trailing = TRAILING_R.matcher(remainder);
    if (trailing.find()) {
      anomericRGroup = trailing.group(1).toUpperCase();
      remainder = remainder.substring(trailing.end());
    }

    CarbMonomerNotationUnit unit = new CarbMonomerNotationUnit(residue, type, incomingRGroup, anomericRGroup);

    while (!remainder.isEmpty() && remainder.charAt(0) == '(') {
      int close = findMatchingParen(remainder, 0);
      if (close < 0) {
        throw new NotationException("CARB branch is missing a closing parenthesis: " + token);
      }
      String branchContent = remainder.substring(1, close);
      unit.addBranch(parseBranch(branchContent, type));
      remainder = remainder.substring(close + 1);
    }

    if (!remainder.isEmpty()) {
      throw new NotationException("Unexpected trailing content in CARB monomer: " + token);
    }

    return unit;
  }

  /**
   * Parses a bare CARB chain - a "." separated sequence of monomer tokens with
   * NO trailing convergence R-group (unlike a branch) - into its ordered list of
   * monomer nodes. Used for a repeating group's repeated sub-chain, whose content
   * is a plain chain (see {@link CarbRepeat}).
   *
   * @param content the chain text, e.g. "R4:[a-D-Glcp].R3:[a-D-Glcp]"
   * @param type polymer type, always "CARB"
   * @return the ordered monomer nodes of the chain
   * @throws NotationException if the chain is empty or any token is malformed
   */
  public static List<CarbMonomerNotationUnit> parseChain(String content, String type) throws NotationException {
    List<String> segments = splitTopLevel(content);
    List<CarbMonomerNotationUnit> chain = new ArrayList<CarbMonomerNotationUnit>();
    for (String segment : segments) {
      if (segment.isEmpty()) {
        throw new NotationException("CARB chain has an empty monomer token: (" + content + ")");
      }
      chain.add(parseToken(segment, type));
    }
    if (chain.isEmpty()) {
      throw new NotationException("CARB chain has no monomers: (" + content + ")");
    }
    return chain;
  }

  /**
   * Parses the content of a branch - the text between its enclosing
   * parentheses, not including them - into its own monomer chain plus the
   * trailing R-group it uses to converge onto the enclosing scope's current
   * (trunk) monomer.
   */
  private static CarbBranch parseBranch(String content, String type) throws NotationException {
    List<String> segments = splitTopLevel(content);
    String last = segments.get(segments.size() - 1);
    if (!BARE_R.matcher(last).matches()) {
      throw new NotationException("CARB branch is missing its convergence R-group: (" + content + ")");
    }
    String convergenceRGroup = last.toUpperCase();

    List<CarbMonomerNotationUnit> chain = new ArrayList<CarbMonomerNotationUnit>();
    for (int i = 0; i < segments.size() - 1; i++) {
      chain.add(parseToken(segments.get(i), type));
    }
    if (chain.isEmpty()) {
      throw new NotationException("CARB branch has no monomers: (" + content + ")");
    }
    return new CarbBranch(chain, convergenceRGroup);
  }

  /**
   * Splits a chain string on '.' at bracket/paren depth 0, following the same
   * nesting-aware scanning convention already used elsewhere in this codebase
   * (e.g. {@code MonomerNotationList.parseMonomer(String)} and
   * {@code MonomerNotationUnitRNA.extractContents(String)}).
   */
  private static List<String> splitTopLevel(String str) {
    List<String> parts = new ArrayList<String>();
    StringBuilder current = new StringBuilder();
    int bracketDepth = 0;
    int parenDepth = 0;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '[') {
        bracketDepth++;
      } else if (c == ']') {
        bracketDepth--;
      } else if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        parenDepth--;
      }
      if (c == '.' && bracketDepth == 0 && parenDepth == 0) {
        parts.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    parts.add(current.toString());
    return parts;
  }

  /**
   * @param str string to scan
   * @param openIndex index of the opening '(' character
   * @return index of the matching ')' character, or -1 if unbalanced
   */
  private static int findMatchingParen(String str, int openIndex) {
    int depth = 0;
    for (int i = openIndex; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

}
