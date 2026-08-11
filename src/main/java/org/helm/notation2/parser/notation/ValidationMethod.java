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
package org.helm.notation2.parser.notation;

import java.io.IOException;
import java.util.regex.Pattern;

import org.helm.notation2.parser.exceptionparser.NotationException;
import org.helm.notation2.parser.notation.polymer.BlobEntity;
import org.helm.notation2.parser.notation.polymer.CarbEntity;
import org.helm.notation2.parser.notation.polymer.CarbMonomerNotationParser;
import org.helm.notation2.parser.notation.polymer.CarbRepeat;
import org.helm.notation2.parser.notation.polymer.ChemEntity;
import org.helm.notation2.parser.notation.polymer.GroupEntity;
import org.helm.notation2.parser.notation.polymer.HELMEntity;
import org.helm.notation2.parser.notation.polymer.MonomerNotation;
import org.helm.notation2.parser.notation.polymer.MonomerNotationGroupElement;
import org.helm.notation2.parser.notation.polymer.MonomerNotationGroupMixture;
import org.helm.notation2.parser.notation.polymer.MonomerNotationGroupOr;
import org.helm.notation2.parser.notation.polymer.MonomerNotationList;
import org.helm.notation2.parser.notation.polymer.MonomerNotationUnit;
import org.helm.notation2.parser.notation.polymer.MonomerNotationUnitRNA;
import org.helm.notation2.parser.notation.polymer.PeptideEntity;
import org.helm.notation2.parser.notation.polymer.RNAEntity;
import org.jdom2.JDOMException;

/**
 * Class containing validation methods
 *
 * @author hecht
 */
public final class ValidationMethod {

	/**
	 * Default constructor.
	 */
	private ValidationMethod() {

	}

	/**
	 * method to decide which of the MonomerNotation classes should be
	 * initialized
	 *
	 * @param str notation
	 * @param type polymer type
	 * @return MonomerNotation if monomer is not valid
	 * @throws NotationException if notation is not valid
	 */
	public static MonomerNotation decideWhichMonomerNotation(String str, String type)
			throws NotationException {
		if (type.equals("CARB")) {
			/*
			 * A CARB monomer token carries a leading "R<n>:" attachment prefix,
			 * an optional trailing ":R<n>" anomeric override, and zero or more
			 * trailing "(...)" branches - it never legally STARTS with "(",
			 * since branches are always written after the "[...]" residue. So a
			 * token that starts with "(" is unambiguously a top-level
			 * group/repeat, not a branch, and is routed accordingly; every
			 * other token is a single monomer parsed by the recursive-descent
			 * token parser.
			 */
			if (str.startsWith("(")) {
				return parseCarbGroupOrRepeat(str, type);
			}
			return CarbMonomerNotationParser.parseToken(str, type);
		}
		MonomerNotation mon;
		/* group ? */
		if (str.startsWith("(") && str.endsWith(")")) {
			String str2 = str.substring(1, str.length() - 1);

			Pattern patternAND = Pattern.compile("\\+");
			Pattern patternOR = Pattern.compile(",");
			/* Mixture of elements */
			if (patternAND.matcher(str).find()) {
				mon = new MonomerNotationGroupMixture(str2, type);
			} /* or - groups */ else if (patternOR.matcher(str).find()) {
				mon = new MonomerNotationGroupOr(str2, type);
			} else {
				if (str.contains(".")) {
					mon = new MonomerNotationList(str2, type);
				} else {
					/* monomer unit is just in brackets */
					if (type.equals("RNA")) {
						mon = new MonomerNotationUnitRNA(str2, type);
					} else {
						if (str2.length() > 1) {
							if (!(str2.startsWith("[") && str2.endsWith("]"))) {
								throw new NotationException("Monomers have to be in brackets: " + str);
							}
						}
						mon = new MonomerNotationUnit(str2, type);
					}
				}

			}
		} else {
			if (type.equals("RNA")) {
				// if (str.startsWith("[") && str.endsWith("]")) {
				// mon = new MonomerNotationUnitRNA(str, type);
				// }
				mon = new MonomerNotationUnitRNA(str, type);
			} else if (!type.equals("BLOB")) {
				if (str.length() > 1) {
					if (!(str.startsWith("[") && str.endsWith("]"))) {
						throw new NotationException("Monomers have to be in brackets: " + str);
					}
				}
				mon = new MonomerNotationUnit(str, type);
			}
			else{
				mon = new MonomerNotationUnit(str, type);
			}

		}
		return mon;
	}

	/**
	 * Routes a CARB token that starts with "(" - a top-level group or repeat, never
	 * a branch (see {@link #decideWhichMonomerNotation}). A top-level "+" makes it a
	 * mixture, a top-level "," makes it an or-group; otherwise the parenthesised
	 * content is a plain CARB sub-chain that forms a repeating group.
	 *
	 * @param str the token, e.g. "(R4:[a-D-Glcp].R3:[a-D-Glcp])" or "([a-D-Glcp]+[b-D-Glcp])"
	 * @param type polymer type, always "CARB"
	 * @return the parsed group / or-group / repeat notation
	 * @throws NotationException if the parentheses are unbalanced or the content is malformed
	 */
	private static MonomerNotation parseCarbGroupOrRepeat(String str, String type) throws NotationException {
		if (!str.endsWith(")")) {
			throw new NotationException("CARB group/repeat is missing a closing parenthesis: " + str);
		}
		String inner = str.substring(1, str.length() - 1);
		/* A top-level "+" is a mixture, a top-level "," an or-group - both reuse the
		 * shared ambiguity-group classes, whose elements are each parsed as CARB
		 * monomers via decideWhichMonomerNotation. Otherwise the parenthesised content
		 * is a plain CARB sub-chain forming a repeating group. */
		if (containsTopLevel(inner, '+')) {
			return new MonomerNotationGroupMixture(inner, type);
		}
		if (containsTopLevel(inner, ',')) {
			return new MonomerNotationGroupOr(inner, type);
		}
		return new CarbRepeat(inner, type);
	}

	/**
	 * @param str string to scan
	 * @param target the character to look for
	 * @return true if {@code target} occurs at bracket/paren depth 0
	 */
	private static boolean containsTopLevel(String str, char target) {
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
			} else if (c == target && bracketDepth == 0 && parenDepth == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * method to decide which of the two Constructors of
	 * MonomerNotationGroupElement should be called
	 *
	 * @param str
	 *            Monomer
	 * @param type
	 *            polymer type of monomer          
	 * @param one
	 *            count of Monomer
	 * @param two
	 *            count of Monomer
	 * @param interval
	 *            Has the monomer instead of one count an interval
	 * @param isDefault
	 *            Is the count of the monomer default = 1
	 * @return MonomerNotationGroupElement
	 * @throws NotationException if notation is not valid
	 */
	public static MonomerNotationGroupElement decideWhichMonomerNotationInGroup(String str, String type, double one,
			double two, boolean interval, boolean isDefault) throws NotationException{
		MonomerNotation element;

		element = decideWhichMonomerNotation(str, type);

		if (interval) {
			return new MonomerNotationGroupElement(element, one, two);
		} else {
			return new MonomerNotationGroupElement(element, one, isDefault);
		}
	}

	/**
	 * method to decide which of the Entities classes should be initialized
	 *
	 * @param str
	 *            polymer ID
	 * @return Entity
	 * @throws NotationException if the HELMEntity is not known, ID is not in the right format
	 */
	public static HELMEntity decideWhichEntity(String str) throws NotationException {

		HELMEntity item;

		if (str.toUpperCase().matches("PEPTIDE[1-9][0-9]*")) {
			item = new PeptideEntity(str.toUpperCase());
		} else if (str.toUpperCase().matches("RNA[1-9][0-9]*")) {
			item = new RNAEntity(str.toUpperCase());
		} else if (str.toUpperCase().matches("BLOB[1-9][0-9]*")) {
			item = new BlobEntity(str.toUpperCase());
		} else if (str.toUpperCase().matches("CHEM[1-9][0-9]*")) {
			item = new ChemEntity(str.toUpperCase());
		} else if (str.toUpperCase().matches("CARB[1-9][0-9]*")) {
			item = new CarbEntity(str.toUpperCase());
		} else if (str.toUpperCase().matches("G[1-9][0-9]*")) {
			item = new GroupEntity(str.toUpperCase());
		} else {
			throw new NotationException("ID is wrong: " + str);
		}

		return item;

	}

}
