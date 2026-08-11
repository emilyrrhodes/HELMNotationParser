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
  public void testCARBPolymer() throws ExceptionState {
    parser = new StateMachineParser();
    // R4:/R6: prefixes denote the glycosidic linkage position (C4/C6 hydroxyl)
    // on the preceding monosaccharide that the next monomer's anomeric carbon bonds into
    String test = "CARB1{[a-D-Glcp].R4:[a-D-Glcp].R6:[a-L-Galp].R3:[a-D-Glcp]}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    Assert.assertEquals(parser.notationContainer.getListOfPolymers().size(), 1);
    Assert.assertEquals(parser.notationContainer.getListOfPolymers().get(0).getPolymerElements().getListOfElements().size(), 4);
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
  public void testCARBPolymerKetoseAnomericOverride() throws ExceptionState {
    parser = new StateMachineParser();
    // the middle monomer's own anomeric carbon is R2 (a ketose), not the default R1
    String inner = "[a-D-Glcp].R4:[a-D-Fruf]:R2.R2:[a-L-Galp]";
    String test = "CARB1{" + inner + "}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }

    Assert.assertEquals(parser.notationContainer.getListOfPolymers().get(0).toHELM2(), inner);
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

  @Test(expectedExceptions = org.helm.notation2.parser.exceptionparser.NotationException.class)
  public void testCARBPolymerMissingColonIsRejected() throws ExceptionState {
    parser = new StateMachineParser();
    // pre-fix grammar ("R4[...]" with no colon) must now be rejected
    String test = "CARB1{[a-D-Glcp].R4[a-D-Glcp]}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }
  }

  @Test(expectedExceptions = org.helm.notation2.parser.exceptionparser.NotationException.class)
  public void testCARBPolymerGroupSyntaxIsRejected() throws ExceptionState {
    parser = new StateMachineParser();
    // "(...)" ambiguity group/mixture syntax is not part of CARB's grammar -
    // branching is handled separately via CarbBranch - and must be rejected
    // with a clear NotationException rather than crashing later with a
    // ClassCastException when the polymer's monomer graph is resolved.
    String test = "CARB1{(R1:[a-D-Glcp]+R1:[b-D-Glcp])}$$$$";

    for (int i = 0; i < test.length(); ++i) {
      parser.doAction(test.charAt(i));
    }
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
