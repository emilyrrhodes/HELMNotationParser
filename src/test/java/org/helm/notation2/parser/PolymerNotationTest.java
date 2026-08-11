/**
 * *****************************************************************************
 * Copyright C 2015, The Pistoia Alliance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *****************************************************************************
 */
package org.helm.notation2.parser;

import java.io.IOException;

import org.helm.notation2.parser.exceptionparser.ExceptionState;
import org.jdom2.JDOMException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * PolymerNotationTest
 * 
 * @author
 */
public class PolymerNotationTest {

  StateMachineParser parser;

  @Test
  public void testCARBPolymer() throws ExceptionState, org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // An Rn: prefix names the hydroxyl on the monomer it PRECEDES (the following
    // one), into which the anomeric carbon (R1 by default) of the PRECEDING monomer
    // bonds. So in "[a-D-Glcp].R4:[a-D-Glcp]", the R4 hydroxyl belongs to the second
    // Glcp and the bond is (first Glcp)$R1 -> (second Glcp)$R4.
    String test = "CARB1{[a-D-Glcp].R4:[a-D-Glcp].R6:[a-L-Galp].R3:[a-D-Glcp]}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    Assert.assertEquals(parser.notationContainer.getListOfPolymers().size(), 1);
    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.getPolymerElements().getListOfElements().size(), 4);

    // Lock the glycosidic bond DIRECTION, not just the edge count: a mass/formula
    // check cannot catch a swapped source/target attachment point. Each edge must
    // run from the preceding monomer's anomeric carbon (R1) into the following
    // monomer's named hydroxyl (R4, then R6, then R3).
    java.util.List<org.helm.notation2.parser.notation.polymer.CarbEdge> edges = polymer.getCarbEdges();
    Assert.assertEquals(edges.size(), 3);
    assertEdge(edges.get(0), 1, "R1", 2, "R4");
    assertEdge(edges.get(1), 2, "R1", 3, "R6");
    assertEdge(edges.get(2), 3, "R1", 4, "R3");
  }

  private static void assertEdge(org.helm.notation2.parser.notation.polymer.CarbEdge edge,
      int sourcePosition, String sourceRGroup, int targetPosition, String targetRGroup) {
    Assert.assertEquals(edge.getSourcePosition(), sourcePosition, "source position");
    Assert.assertEquals(edge.getSourceRGroup(), sourceRGroup, "source R-group");
    Assert.assertEquals(edge.getTargetPosition(), targetPosition, "target position");
    Assert.assertEquals(edge.getTargetRGroup(), targetRGroup, "target R-group");
  }

  @Test
  public void testCARBPolymerRoundTrip() throws ExceptionState {
    parser = new StateMachineParser();
    String inner = "[a-D-Glcp].R4:[a-D-Glcp].R6:[a-L-Galp].R3:[a-D-Glcp]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    Assert.assertEquals(parser.notationContainer.getListOfPolymers().get(0).toHELM2(), inner);
  }

  @Test
  public void testCARBPolymerKetoseAnomericOverride() throws ExceptionState,
      org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // the middle monomer's own anomeric carbon is R2 (a ketose), not the default R1
    String inner = "[a-D-Glcp].R4:[a-D-Fruf]:R2.R2:[a-L-Galp]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), inner);

    // The ketose override must change the SOURCE anomeric R-group of the bond
    // leaving the Fruf (position 2): the first bond enters Fruf's R4 hydroxyl from
    // Glcp's default R1 anomeric; the second bond LEAVES Fruf via its R2 anomeric
    // (not the default R1) into Galp's R2 hydroxyl.
    java.util.List<org.helm.notation2.parser.notation.polymer.CarbEdge> edges = polymer.getCarbEdges();
    Assert.assertEquals(edges.size(), 2);
    assertEdge(edges.get(0), 1, "R1", 2, "R4");
    assertEdge(edges.get(1), 2, "R2", 3, "R2");
  }

  @Test
  public void testCARBPolymerBranchConvergenceEdge() throws ExceptionState,
      org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // A trunk of two monomers joined (1->4), with a third monomer branching onto
    // the second: "([a-D-Glcp].R2)" means the branch monomer's anomeric carbon (R1)
    // converges onto the trunk monomer's R2 hydroxyl.
    String inner = "[a-D-Glcp].R4:[a-D-Glcp]([a-D-Glcp].R2)";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), inner);

    java.util.List<org.helm.notation2.parser.notation.polymer.CarbEdge> edges = polymer.getCarbEdges();
    Assert.assertEquals(edges.size(), 2);
    // main-chain bond: trunk monomer 1's anomeric R1 into trunk monomer 2's R4
    assertEdge(edges.get(0), 1, "R1", 2, "R4");
    // branch-convergence bond: branch monomer 3's anomeric R1 into trunk monomer 2's R2
    assertEdge(edges.get(1), 3, "R1", 2, "R2");
  }

  /*
   * Re-derives the spec's 15-monomer worked branching example: a trunk of 5
   * A's, a branch of 3 B's containing a nested branch of 3 C's, then 3 more
   * trunk A's after the branch closes. Verifies total monomer count (via the
   * numbering map, which counts branch-nested monomers too) and round-trip.
   */
  @Test
  public void testCARBPolymerBranching() throws ExceptionState, org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // the CARB spec's own worked example: a trunk of 5 A's, a branch of 3 B's
    // (attached to the 5th A) containing its own nested branch of 3 C's
    // (attached to the 3rd B), then 3 more trunk A's after the branch closes
    String inner =
        "[A].R3:[A].R2:[A].R2:[A].R2:[A]"
            + "([B].R2:[B].R2:[B]"
            + "([C].R2:[C].R2:[C].R6)"
            + ".R2:[B].R6)"
            + ".R3:[A].R4:[A].R4:[A]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    // 8 top-level tokens: the branch attached to the 5th one does not add a
    // 9th top-level list entry, since it is nested inside that token
    Assert.assertEquals(polymer.getPolymerElements().getListOfElements().size(), 8);

    polymer.initializeMapOfMonomersAndMapOfIntraConnection();
    // 15 total monomers once branch-nested ones are counted
    for (int position = 1; position <= 15; position++) {
      Assert.assertNotNull(polymer.getMonomerNotation(position), "Missing monomer at position " + position);
    }
    Assert.assertNull(polymer.getMonomerNotation(16));

    Assert.assertEquals(polymer.toHELM2(), inner);
  }

  @Test
  public void testCARBRepeatIntegerExpansion() throws ExceptionState,
      org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // one lead monomer, then a 2-unit sub-chain repeated twice; the repeat's
    // first monomer carries its own connecting group (R4) so each repetition
    // chains onto whatever precedes it
    String inner = "[a-D-Glcp].(R4:[a-D-Glcp].R3:[a-D-Glcp])'2'";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), inner);

    polymer.initializeMapOfMonomersAndMapOfIntraConnection();
    // the repeat expands to 4 monomers, plus the 1 lead monomer = 5
    for (int position = 1; position <= 5; position++) {
      Assert.assertNotNull(polymer.getMonomerNotation(position), "Missing monomer at position " + position);
    }
    Assert.assertNull(polymer.getMonomerNotation(6));

    java.util.List<org.helm.notation2.parser.notation.polymer.CarbEdge> edges = polymer.getCarbEdges();
    Assert.assertEquals(edges.size(), 4);
    assertEdge(edges.get(0), 1, "R1", 2, "R4"); // lead -> first repetition, unit 1
    assertEdge(edges.get(1), 2, "R1", 3, "R3"); // within first repetition
    assertEdge(edges.get(2), 3, "R1", 4, "R4"); // first repetition -> second repetition
    assertEdge(edges.get(3), 4, "R1", 5, "R3"); // within second repetition
  }

  @Test
  public void testCARBRepeatRoundTripWithBranch() throws ExceptionState {
    parser = new StateMachineParser();
    // a repeating group whose repeated sub-chain itself contains a branch
    String inner = "([a-D-Glcp].R3:[a-D-Glcp]([a-D-Glcp].R2))'3'";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    Assert.assertEquals(parser.notationContainer.getListOfPolymers().get(0).toHELM2(), inner);
  }

  @Test
  public void testCARBRepeatNonIntegerCountEmitsNoEdges() throws ExceptionState,
      org.helm.notation2.parser.exceptionparser.NotationException {
    parser = new StateMachineParser();
    // an open-ended repeat count ("n") is valid notation and round-trips, but its
    // connectivity is unknown, so it numbers its monomers yet emits no edges - a
    // build-time dead end
    String inner = "([a-D-Glcp].R4:[a-D-Glcp])'n'";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), inner);
    Assert.assertNotNull(polymer.getMonomerNotation(1));
    Assert.assertNotNull(polymer.getMonomerNotation(2));
    Assert.assertNull(polymer.getMonomerNotation(3));
    Assert.assertEquals(polymer.getCarbEdges().size(), 0);
  }

  @Test(expectedExceptions = org.helm.notation2.parser.exceptionparser.NotationException.class)
  public void testCARBPolymerMissingColonIsRejected() throws ExceptionState {
    parser = new StateMachineParser();
    // pre-fix grammar ("R4[...]" with no colon) must now be rejected
    String test = "CARB1{[a-D-Glcp].R4[a-D-Glcp]}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }
  }

  @Test
  public void testCARBPolymerUnknownMonomer() throws ExceptionState {
    parser = new StateMachineParser();
    // "*" is a fully-unknown monomer (count 0..n) with no connection points; a
    // lone unknown polymer body must parse, round-trip, number its one position,
    // and emit no edges.
    String test = "CARB1{*}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), "*");
    Assert.assertNotNull(polymer.getMonomerNotation(1));
    Assert.assertNull(polymer.getMonomerNotation(2));
    Assert.assertEquals(polymer.getCarbEdges().size(), 0);
  }

  @Test
  public void testCARBPolymerUnknownMonomerMidChainBreaksEdges() throws ExceptionState {
    parser = new StateMachineParser();
    // An unknown monomer ("X") mid-chain has no connection points, so no bond can
    // be resolved into or out of it: neither the (1->X) nor the (X->3) linkage is
    // emitted. Only the three real monomers get positions; there are zero edges.
    String inner = "[a-D-Glcp].X.R4:[a-D-Glcp]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertEquals(polymer.toHELM2(), inner);
    Assert.assertNotNull(polymer.getMonomerNotation(3));
    Assert.assertEquals(polymer.getCarbEdges().size(), 0);
  }

  @Test
  public void testCARBPolymerMixtureGroup() throws ExceptionState {
    parser = new StateMachineParser();
    // A mixture "([a-D-Glcp]+[b-D-Glcp])" is an ambiguity group: it parses into a
    // MonomerNotationGroupMixture, round-trips, occupies one position, and emits
    // no concrete edge (its connectivity is unresolvable).
    String inner = "([a-D-Glcp]+[b-D-Glcp])";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertTrue(polymer.getPolymerElements().getListOfElements().get(0)
        instanceof org.helm.notation2.parser.notation.polymer.MonomerNotationGroupMixture);
    Assert.assertEquals(polymer.toHELM2(), inner);
    Assert.assertNotNull(polymer.getMonomerNotation(1));
    Assert.assertEquals(polymer.getCarbEdges().size(), 0);
  }

  @Test
  public void testCARBPolymerOrGroup() throws ExceptionState {
    parser = new StateMachineParser();
    // An or-group "([a-D-Glcp],[b-D-Glcp])" parses into a MonomerNotationGroupOr.
    String inner = "([a-D-Glcp],[b-D-Glcp])";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);
    Assert.assertTrue(polymer.getPolymerElements().getListOfElements().get(0)
        instanceof org.helm.notation2.parser.notation.polymer.MonomerNotationGroupOr);
    Assert.assertEquals(polymer.toHELM2(), inner);
    Assert.assertEquals(polymer.getCarbEdges().size(), 0);
  }

  @Test
  public void testCARBPolymerReinitializationIsIdempotent() throws ExceptionState {
    parser = new StateMachineParser();
    String inner = "[a-D-Glcp].R4:[a-D-Glcp].R6:[a-L-Galp].R3:[a-D-Glcp]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    org.helm.notation2.parser.notation.polymer.PolymerNotation polymer =
        parser.notationContainer.getListOfPolymers().get(0);

    // calling the public read entry points repeatedly must not accumulate
    // stale entries from earlier calls
    Assert.assertEquals(polymer.getMonomerNotation(1), polymer.getMonomerNotation(1));
    int firstEdgeCount = polymer.getCarbEdges().size();
    int secondEdgeCount = polymer.getCarbEdges().size();
    Assert.assertEquals(secondEdgeCount, firstEdgeCount);
    Assert.assertEquals(firstEdgeCount, 3);
    // 3 edges * 2 map entries each (source + target), not accumulated across calls
    Assert.assertEquals(polymer.getMapIntraConnection().size(), 6);
    Assert.assertNull(polymer.getMonomerNotation(5));
  }

  @Test(expectedExceptions = org.helm.notation2.parser.exceptionparser.NotationException.class)
  public void testCARBPolymerMalformedResidueIsRejected() throws ExceptionState {
    parser = new StateMachineParser();
    // "???" does not match any of CarbMonomerParser's recognised residue shapes
    String test = "CARB1{[???]}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }
  }

  /*
   * method to test unchanged input in the simple polymer section
   */
  @Test
  public void testSimpleInput() throws ExceptionState, IOException, JDOMException {
    parser = new StateMachineParser();
    String test =
        "PEPTIDE1{A'3'.A.A.A.A.A.A\"mutation\".A.A.A.A.A.A.A.A.A.A.A.A.A.C.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.D.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E.E}|PEPTIDE2{G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.G.C.S.S.S.S.S.S.S.S.S.P.P.P.P.P.P.P.P.P.K.K.K.K.K.K.K.K.K.K.K.K.K}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    System.out.println(parser.notationContainer.getListOfPolymers().toString());

  }

  /*
   * method to test unchanged input in the simple polymer section
   */
  @Test
  public void testInputGroupOr() throws ExceptionState, IOException, JDOMException {
    parser = new StateMachineParser();
    String test = "PEPTIDE1{A.X.(C:1,B:1)\"Test\"}$$$$";
    parser = new StateMachineParser();
    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    System.out.println(parser.notationContainer.getListOfPolymers().toString());

  }

  /*
   * method to test unchanged input in the simple polymer section
   */
  @Test
  public void testInputGroupMixture() throws ExceptionState, IOException, JDOMException {
    parser = new StateMachineParser();
    String test = "PEPTIDE3{A'3-6'.X\"Test\".(C:1+B:2.2-2.3)\"Test\"}\"Hallo\"$$$$";
    parser = new StateMachineParser();
    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    System.out.println(parser.notationContainer.getListOfPolymers().toString());

  }

  /*
   * method to test unchanged input in the simple polymer section
   */
  @Test
  public void testInputCount() throws ExceptionState, IOException, JDOMException {
    parser = new StateMachineParser();
    String test = "PEPTIDE3{?._.X\"Test\".(C:1+B:2.2-2.3)\"Test\"}\"Hallo\"$$$$";
    parser = new StateMachineParser();
    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    System.out.println(parser.notationContainer.getListOfPolymers().toString());

  }

  /*
   * method to test more complicated monomer units
   */
  @Test
  public void testGroupRepeating() throws ExceptionState, IOException, JDOMException {
    parser = new StateMachineParser();
    String test = "PEPTIDE3{(H'12'\"Test\".G}$$$$";
    parser = new StateMachineParser();
    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    System.out.println(parser.notationContainer.getListOfPolymers().toString());

  }
}
