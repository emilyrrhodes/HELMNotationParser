package org.helm.notation2.parser.notation.polymer;

import org.helm.notation2.parser.exceptionparser.NotationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Direct unit tests for {@link CarbMonomerNotationParser}, isolated from the
 * character-level {@code StateMachineParser} FSM so grammar edge cases can be
 * exercised precisely.
 */
public class CarbMonomerNotationParserTest {

  @Test
  public void testFirstMonomerHasNoIncomingRGroup() throws NotationException {
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken("[a-D-Glcp]", "CARB");
    Assert.assertNull(unit.getIncomingRGroup());
    Assert.assertEquals(unit.getAnomericRGroup(), "R1");
    Assert.assertTrue(unit.getBranches().isEmpty());
    Assert.assertEquals(unit.toHELM2(), "[a-D-Glcp]");
  }

  @Test
  public void testLeadingRGroup() throws NotationException {
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken("R4:[a-D-Glcp]", "CARB");
    Assert.assertEquals(unit.getIncomingRGroup(), "R4");
    Assert.assertEquals(unit.getUnit(), "[a-D-Glcp]");
    Assert.assertEquals(unit.toHELM2(), "R4:[a-D-Glcp]");
  }

  @Test
  public void testUnknownLeadingRGroup() throws NotationException {
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken("R?:[a-D-Glcp]", "CARB");
    Assert.assertEquals(unit.getIncomingRGroup(), "R?");
    Assert.assertEquals(unit.toHELM2(), "R?:[a-D-Glcp]");
  }

  @Test
  public void testKetoseAnomericOverride() throws NotationException {
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken("R4:[a-D-Fruf]:R2", "CARB");
    Assert.assertEquals(unit.getIncomingRGroup(), "R4");
    Assert.assertEquals(unit.getAnomericRGroup(), "R2");
    Assert.assertEquals(unit.toHELM2(), "R4:[a-D-Fruf]:R2");
  }

  @Test
  public void testSingleBranch() throws NotationException {
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken("R3:[a-D-Glcp]([a-D-Glcp].R2)", "CARB");
    Assert.assertEquals(unit.getIncomingRGroup(), "R3");
    Assert.assertEquals(unit.getBranches().size(), 1);

    CarbBranch branch = unit.getBranches().get(0);
    Assert.assertEquals(branch.getConvergenceRGroup(), "R2");
    Assert.assertEquals(branch.getChain().size(), 1);
    Assert.assertNull(branch.getChain().get(0).getIncomingRGroup());

    Assert.assertEquals(unit.toHELM2(), "R3:[a-D-Glcp]([a-D-Glcp].R2)");
  }

  @Test
  public void testNestedBranch() throws NotationException {
    String token = "R2:[A]([B].R2:[B].R2:[B]([C].R2:[C].R2:[C].R6).R2:[B].R6)";
    CarbMonomerNotationUnit unit = CarbMonomerNotationParser.parseToken(token, "CARB");

    Assert.assertEquals(unit.getBranches().size(), 1);
    CarbBranch outer = unit.getBranches().get(0);
    Assert.assertEquals(outer.getConvergenceRGroup(), "R6");
    Assert.assertEquals(outer.getChain().size(), 4);

    CarbMonomerNotationUnit thirdB = outer.getChain().get(2);
    Assert.assertEquals(thirdB.getBranches().size(), 1);
    CarbBranch nested = thirdB.getBranches().get(0);
    Assert.assertEquals(nested.getConvergenceRGroup(), "R6");
    Assert.assertEquals(nested.getChain().size(), 3);

    Assert.assertEquals(unit.toHELM2(), token);
  }

  @Test(expectedExceptions = NotationException.class)
  public void testMissingColonIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("R4[a-D-Glcp]", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testMissingBracketsIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("a-D-Glcp", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testUnbalancedBranchIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("R3:[a-D-Glcp]([a-D-Glcp].R2", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testBranchMissingConvergenceRGroupIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("R3:[a-D-Glcp]([a-D-Glcp])", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testGroupSyntaxIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("(R1:[a-D-Glcp]+R1:[b-D-Glcp])", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testEmptyResidueIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("[]", "CARB");
  }

  @Test(expectedExceptions = NotationException.class)
  public void testMalformedResidueContentIsRejected() throws NotationException {
    CarbMonomerNotationParser.parseToken("[???]", "CARB");
  }

}
