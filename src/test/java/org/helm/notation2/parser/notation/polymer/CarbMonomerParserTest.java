package org.helm.notation2.parser.notation.polymer;

import org.helm.notation2.parser.exceptionparser.NotationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Direct unit tests for {@link CarbMonomerParser}.
 */
public class CarbMonomerParserTest {

  @Test
  public void testQualifiedResidue() throws NotationException {
    CarbMonomerNotation mon = CarbMonomerParser.parse("a-D-Glcp");
    Assert.assertEquals(mon.getAnomer(), "a");
    Assert.assertEquals(mon.getAbsoluteConfiguration(), "D");
    Assert.assertEquals(mon.getBaseName(), "Glcp");
  }

  @Test
  public void testAchiralAnomerOnlyResidue() throws NotationException {
    CarbMonomerNotation mon = CarbMonomerParser.parse("a-Quip");
    Assert.assertEquals(mon.getAnomer(), "a");
    Assert.assertNull(mon.getAbsoluteConfiguration());
    Assert.assertEquals(mon.getBaseName(), "Quip");
  }

  @Test
  public void testUnqualifiedSingleLetterPlaceholder() throws NotationException {
    CarbMonomerNotation mon = CarbMonomerParser.parse("A");
    Assert.assertNull(mon.getAnomer());
    Assert.assertNull(mon.getAbsoluteConfiguration());
    Assert.assertEquals(mon.getBaseName(), "A");
  }

  @Test
  public void testSubstituentSuffixBaseName() throws NotationException {
    CarbMonomerNotation mon = CarbMonomerParser.parse("a-L-4-en-4-deoxy-thrHexpA");
    Assert.assertEquals(mon.getAnomer(), "a");
    Assert.assertEquals(mon.getAbsoluteConfiguration(), "L");
    Assert.assertEquals(mon.getBaseName(), "4-en-4-deoxy-thrHexpA");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testEmptyResidueIsRejected() throws NotationException {
    CarbMonomerParser.parse("");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testMalformedResidueIsRejected() throws NotationException {
    CarbMonomerParser.parse("???");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testTrailingHyphenBaseNameIsRejected() throws NotationException {
    // hyphens/commas are internal separators only - a trailing "-" is malformed
    CarbMonomerParser.parse("Gal-");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testTrailingCommaBaseNameIsRejected() throws NotationException {
    CarbMonomerParser.parse("a-D-Gal,");
  }

}
