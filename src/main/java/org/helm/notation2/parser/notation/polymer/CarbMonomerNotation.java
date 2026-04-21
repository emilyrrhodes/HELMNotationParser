package org.helm.notation2.parser.notation.polymer;

/**
 * Parsed representation of a carbohydrate monomer identifier.
 *
 * A CARB monomer ID like "β-D-Gal" is composed of three parts:
 *   anomericity    – α or β (configuration at the anomeric C1 carbon)
 *   stereochemistry – D or L (absolute configuration of the sugar)
 *   baseName       – the core monosaccharide abbreviation (e.g. Gal, GlcNAc)
 *
 * Identifiers without qualifiers (e.g. "Gal") are also accepted; in that
 * case anomericity and stereochemistry are null.
 */
public class CarbMonomerNotation {

  private final String anomericity;
  private final String stereochemistry;
  private final String baseName;

  public CarbMonomerNotation(String anomericity, String stereochemistry, String baseName) {
    this.anomericity = anomericity;
    this.stereochemistry = stereochemistry;
    this.baseName = baseName;
  }

  /** α or β, or null if not specified. */
  public String getAnomericity() {
    return anomericity;
  }

  /** D or L, or null if not specified. */
  public String getStereochemistry() {
    return stereochemistry;
  }

  /** Core monosaccharide name, e.g. "Gal", "GlcNAc", "GalNAc". */
  public String getBaseName() {
    return baseName;
  }

  @Override
  public String toString() {
    if (anomericity != null && stereochemistry != null) {
      return anomericity + "-" + stereochemistry + "-" + baseName;
    }
    return baseName;
  }

}
