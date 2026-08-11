package org.helm.notation2.parser.notation;

import org.helm.notation2.parser.exceptionparser.NotationException;
import org.helm.notation2.parser.notation.polymer.MonomerNotation;
import org.helm.notation2.parser.notation.polymer.MonomerNotationUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Direct unit tests for {@link ValidationMethod}.
 */
public class ValidationMethodTest {

  /**
   * Regression test for a String reference-equality bug: the BLOB check used
   * to be {@code type != "BLOB"}, which only worked by accident because every
   * real call site passes an interned string literal. A deliberately
   * non-interned "BLOB" string must still be routed correctly.
   */
  @Test
  public void testBlobTypeCheckUsesValueEquality() throws NotationException {
    String type = new String("BLOB");
    MonomerNotation mon = ValidationMethod.decideWhichMonomerNotation("XY", type);
    Assert.assertTrue(mon instanceof MonomerNotationUnit);
  }

  /**
   * Same regression, for the RNA checks (both occurrences in
   * decideWhichMonomerNotation) which used {@code type == "RNA"}.
   */
  @Test
  public void testRnaTypeCheckUsesValueEquality() throws NotationException {
    String type = new String("RNA");
    MonomerNotation mon = ValidationMethod.decideWhichMonomerNotation("A", type);
    Assert.assertEquals(mon.getClass().getSimpleName(), "MonomerNotationUnitRNA");
  }

  @Test
  public void testRnaTypeCheckUsesValueEqualityInGroupBranch() throws NotationException {
    String type = new String("RNA");
    MonomerNotation mon = ValidationMethod.decideWhichMonomerNotation("(A)", type);
    Assert.assertEquals(mon.getClass().getSimpleName(), "MonomerNotationUnitRNA");
  }

}
