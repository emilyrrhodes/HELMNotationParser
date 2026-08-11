package org.helm.notation2.parser.notation.polymer;

/**
 * Parsed representation of a carbohydrate monomer identifier.
 *
 * A CARB monomer ID like "b-D-Gal" is composed of three parts:
 *   anomer                – a or b (configuration at the anomeric C1 carbon)
 *   absoluteConfiguration – D or L (absolute configuration of the sugar)
 *   baseName              – the core monosaccharide abbreviation (e.g. Gal, GlcNAc)
 *
 * Identifiers without qualifiers (e.g. "Gal") are also accepted; in that
 * case anomer and absoluteConfiguration are null.
 */
public class CarbMonomerNotation {

  private final String anomer;
  private final String absoluteConfiguration;
  private final String baseName;

  public CarbMonomerNotation(String anomer, String absoluteConfiguration, String baseName) {
    this.anomer = anomer;
    this.absoluteConfiguration = absoluteConfiguration;
    this.baseName = baseName;
  }

  /** a or b, or null if not specified. */
  public String getAnomer() {
    return anomer;
  }

  /** D or L, or null if not specified. */
  public String getAbsoluteConfiguration() {
    return absoluteConfiguration;
  }

  /** Core monosaccharide name, e.g. "Gal", "GlcNAc", "GalNAc". */
  public String getBaseName() {
    return baseName;
  }

  @Override
  public String toString() {
    if (anomer != null && absoluteConfiguration != null) {
      return anomer + "-" + absoluteConfiguration + "-" + baseName;
    }
    if (anomer != null) {
      return anomer + "-" + baseName;
    }
    return baseName;
  }

}
