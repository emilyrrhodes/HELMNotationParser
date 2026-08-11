package org.helm.notation2.parser.notation.polymer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helm.notation2.parser.exceptionparser.NotationException;

/**
 * Parses carbohydrate monomer identifiers into their constituent parts.
 *
 * CarbBank/IUPAC-style names carry the anomeric configuration and absolute
 * configuration as a prefix, followed by the base monosaccharide name, which
 * may itself include a ring-size letter (p/f) and substituent suffixes, e.g.:
 *   b-D-Gal              → anomer=b, absoluteConfiguration=D, baseName=Gal
 *   a-D-GalpNAc           → anomer=a, absoluteConfiguration=D, baseName=GalpNAc
 *   b-D-Galp3Me           → anomer=b, absoluteConfiguration=D, baseName=Galp3Me
 *   a-L-4-en-4-deoxy-thrHexpA → anomer=a, absoluteConfiguration=L, baseName=4-en-4-deoxy-thrHexpA
 *   a-Quip                → anomer=a, absoluteConfiguration=null, baseName=Quip (achiral sugar, no D/L)
 *   Gal                   → anomer=null, absoluteConfiguration=null, baseName=Gal
 *
 * The R{@literal <n>}: linkage-position prefix/suffix (e.g. "R4:" in
 * "R4:[b-D-GlcNAc]") is grammar handled by {@link CarbMonomerNotationParser},
 * not by this class - by the time a residue name reaches here, it has already
 * been stripped of its enclosing brackets and any R-group notation.
 */
public class CarbMonomerParser {

  // baseName may contain letters, digits, and the punctuation CarbBank names use
  // for substituent/ring-size suffixes and locant lists (e.g. "4-en-4-deoxy-thrHexpA", "2,6-deoxy-ribHexp").
  // Hyphens and commas are only ever internal separators, so the name must both
  // start and end with an alphanumeric character - a trailing "-"/"," (e.g. "Gal-")
  // is malformed and must be rejected, not silently accepted.
  private static final String BASE_NAME = "[A-Za-z0-9](?:[A-Za-z0-9,\\-]*[A-Za-z0-9])?";

  // Fully qualified: anomer-configuration-baseName, e.g. "b-D-Gal"
  private static final Pattern QUALIFIED_PATTERN =
      Pattern.compile("^([ab])-([DL])-(" + BASE_NAME + ")$");

  // Anomer only, no absolute configuration - achiral sugars, e.g. "a-Quip"
  private static final Pattern ANOMER_ONLY_PATTERN =
      Pattern.compile("^([ab])-(" + BASE_NAME + ")$");

  // Unqualified base name only, e.g. "Gal"
  private static final Pattern UNQUALIFIED_PATTERN =
      Pattern.compile("^(" + BASE_NAME + ")$");

  private CarbMonomerParser() {}

  /**
   * Parses a carbohydrate monomer ID into its components.
   *
   * @param id the monomer identifier, e.g. "b-D-Gal" or "Gal"
   * @return parsed CarbMonomerNotation
   * @throws NotationException if the string does not match any recognised format
   */
  public static CarbMonomerNotation parse(String id) throws NotationException {
    if (id == null || id.isEmpty()) {
      throw new NotationException("CARB monomer ID must not be empty");
    }

    Matcher m = QUALIFIED_PATTERN.matcher(id);
    if (m.matches()) {
      return new CarbMonomerNotation(m.group(1), m.group(2), m.group(3));
    }

    m = ANOMER_ONLY_PATTERN.matcher(id);
    if (m.matches()) {
      return new CarbMonomerNotation(m.group(1), null, m.group(2));
    }

    m = UNQUALIFIED_PATTERN.matcher(id);
    if (m.matches()) {
      return new CarbMonomerNotation(null, null, m.group(1));
    }

    throw new NotationException("Cannot parse CARB monomer ID: " + id);
  }

}
