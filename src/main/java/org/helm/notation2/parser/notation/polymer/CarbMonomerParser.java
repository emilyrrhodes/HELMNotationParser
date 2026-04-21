package org.helm.notation2.parser.notation.polymer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helm.notation2.parser.exceptionparser.NotationException;

/**
 * Parses carbohydrate monomer identifiers into their constituent parts.
 *
 * Supported formats:
 *   β-D-Gal      → anomericity=β, stereochemistry=D, baseName=Gal
 *   α-D-GalNAc   → anomericity=α, stereochemistry=D, baseName=GalNAc
 *   Gal          → anomericity=null, stereochemistry=null, baseName=Gal
 *
 * The R{@literal <n>} linkage-position prefix (e.g. "R4" in "R4[β-D-GlcNAc]") must be
 * stripped by the caller before passing the ID to this parser.
 */
public class CarbMonomerParser {

  // Optional "α-D-" or "β-L-" prefix, followed by the base monomer name
  private static final Pattern PATTERN =
      Pattern.compile("^([αβ])-([DL])-(.+)$");

  private CarbMonomerParser() {}

  /**
   * Parses a carbohydrate monomer ID into its components.
   *
   * @param id the monomer identifier, e.g. "β-D-Gal" or "Gal"
   * @return parsed CarbMonomerNotation
   * @throws NotationException if the string does not match any recognised format
   */
  public static CarbMonomerNotation parse(String id) throws NotationException {
    if (id == null || id.isEmpty()) {
      throw new NotationException("CARB monomer ID must not be empty");
    }

    Matcher m = PATTERN.matcher(id);
    if (m.matches()) {
      return new CarbMonomerNotation(m.group(1), m.group(2), m.group(3));
    }

    // Accept unqualified names like "Gal" directly
    if (id.matches("[A-Za-z][A-Za-z0-9]*")) {
      return new CarbMonomerNotation(null, null, id);
    }

    throw new NotationException("Cannot parse CARB monomer ID: " + id);
  }

}
