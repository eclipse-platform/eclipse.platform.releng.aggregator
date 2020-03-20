/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Oberhuber (Wind River) - [276255] fix insertion of extra space chars
 *     Leo Ufimtsev lufimtse@redhat.com - [369991] Major re-write to handle multiple years. + added test cases.
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.releng.tools.preferences.RelEngCopyrightConstants;

/**
 * <h2>Handle incomming 'raw' comments and convert them <br>
 * into a comment format with access to creation/revision year. <br>
 * When retrieving the comment, update the revision year or append one if it's not there.</h2>
 *
 * <p>
 * Tested in {@link org.eclipse.releng.tests.AdvancedCopyrightCommentTestsJunit4}<br>
 * Please verify that tests run after modifications.
 * </p>
 *
 */
public class AdvancedCopyrightComment extends CopyrightComment {

	/** A regex mattern to match years in the range {@code 19** to 23** } */
	private static final String YEAR_REGEX = "(19|20|21|22|23)\\d{2}"; //$NON-NLS-1$

	private static final String DATE_VAR = "${date}"; //$NON-NLS-1$
	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

	/** Everything before the line with the year(s) on it. */
	private String preYearLinesString = null;

	/** The line with the year(s) on it. */
	private String yearLineString = null; // this is updated when we return a comment.

	/** Everything after the line with the year(s) on it. */
	private String postYearLineString = null;

	/** Number of year units matching {@link #YEAR_REGEX YEAR_REGEX} in the comment. e.g '2000, 2011-2014' has 3 */
	private int yearsCount;

	/**
	 * Return the body of this copyright comment or null if it cannot be built.
	 */
	@Override
	public String getCopyrightComment() {

		if ((preYearLinesString != null || postYearLineString != null)) {
			StringBuilder copyrightComment = new StringBuilder();

			// Pre-append everything before the years
			if (preYearLinesString != null) {
				copyrightComment.append(preYearLinesString);
			}

			// Check if the comment has a revised year. Fix the years on the line if so.
			if (hasRevisionYear() && (getRevisionYear() != getCreationYear())) {

				String fixedYearLine;
				if (yearsCount == 1) {
					// Insert a 2nd year '2000' -> '2000-2010'.
					fixedYearLine = insertRevisedYear(yearLineString, getRevisionYear());
				} else {
					// update the last found year on line: '2000 ... 2005' -> '2000 ... 2015'
					fixedYearLine = updateLastYear(yearLineString, getRevisionYear());
					if (fixedYearLine == null) {
						return null; //failed to update last year.
					}
				}

				copyrightComment.append(fixedYearLine);
			} else {
				// Otherwise put back the original year line.
				copyrightComment.append(yearLineString);
			}

			// Post append everything after the year line
			copyrightComment.append(postYearLineString);

			return copyrightComment.toString();
		}

		String linePrefix = getCommentPrefix();
		if (linePrefix == null)
			return null;

		StringWriter out = new StringWriter();

		try (PrintWriter writer = new PrintWriter(out)) {
			writeCommentStart(writer);
			writeLegal(writer, linePrefix);
			writeCommentEnd(writer);
			return out.toString();
		}
	}

	/**
	 * <h1>Parse a raw comment.</h1>
	 * <p>
	 * Create an instance the same as the argument comment but with the revision year <br>
	 * updated if needed. Return the default comment if the argument comment is null <br>
	 * or an empty string.
	 * </p>
	 *
	 * @param comment
	 *            the original comment from the file.
	 * @param commentStyle
	 *            the comment style. {@link CopyrightComment}
	 * @return {@link AdvancedCopyrightComment} an copyright comment with the year updated.
	 *
	 */
	public static AdvancedCopyrightComment parse(BlockComment commentBock, int commentStyle) {
		// If the given comment is empty, return the default comment.
		if (commentBock == null) {
			return defaultComment(commentStyle);
		}

		String comment = commentBock.getContents();

		// identify which line delimiter is used. (for writing back to file )
		String fileLineDelimiter = TextUtilities.determineLineDelimiter(comment, "\n"); //$NON-NLS-1$

		// Split Comment into Seperate lines for easier proccessing:
		String commentLines[] = comment.split("\\r?\\n"); //$NON-NLS-1$

		// lines before the line with the year comment on it.
		StringBuilder preYearLines = new StringBuilder();

		// line with the year(s) on it. 'copyright 2002, 2010-2011 ... etc..
		String yearLine = null;

		// Lines after the line with the year comment on it.
		StringBuilder postYearLines = new StringBuilder();

		// Break down the comment into the three sections.
		boolean yearFound = false;
		String line;
		for (int i = 0; i < commentLines.length; i++) {

			line = commentLines[i]; // for clarity.

			if (yearFound) {
				// We have already found the year line and are just appending the last lines.

				// Conditionally append a newline delimiter.
				if (i != (commentLines.length - 1)) {
					// normally, append a new line.
					postYearLines.append(line + fileLineDelimiter);
				} else {
					// for the last line, only append if the original comment had a newline delimiter.
					Character lastchar = comment.charAt(comment.length() - 1);
					if (Character.isWhitespace(lastchar)) {
						postYearLines.append(line + lastchar);
					} else {
						postYearLines.append(line);
					}
				}

			} else if (line.matches(".*" + YEAR_REGEX + ".*")) { //$NON-NLS-1$ //$NON-NLS-2$
				// We found the line with the copy-right years on it.
				yearFound = true;
				yearLine = line + fileLineDelimiter;
			} else {
				// We are parsting the top part of the comment and have not reached the year-line yet.
				preYearLines.append(line + fileLineDelimiter);
			}
		}

		// The comment didn't contain any years that we can update.
		if (!yearFound) {
			return null;
		}

		// Determine first year.
		int createdYear = getFirstYear(yearLine);
		if (createdYear == 0) {
			return null; //Failed to read a year.
		}


		int yearsOnLine = countYearsOnLine(yearLine);
		// Determine the last year
		int revisedYear;
		if (yearsOnLine == 1) {
			revisedYear = -1;
		} else {
			revisedYear = getLastYear(yearLine);
		}

		return new AdvancedCopyrightComment(commentStyle, createdYear, revisedYear, yearsOnLine,
				preYearLines.toString(), yearLine, postYearLines.toString());
	}

	/**
	 * <p> Construct a new default comment for the file.</p>
	 *
	 * @param commentStyle            As defined in: CopyrightComment
	 * @return                        a newly created comment.
	 */
	public static AdvancedCopyrightComment defaultComment(int commentStyle) {
		return new AdvancedCopyrightComment(commentStyle, -1, -1, 1, null, null, null);
	}


	private AdvancedCopyrightComment(int commentStyle, int creationYear, int revisionYear,
			int yearsCount, String preYearComment, String middleYearsComment, String postYearComment) {
		super(commentStyle, creationYear == -1 ? getPreferenceStore().getInt(
				RelEngCopyrightConstants.CREATION_YEAR_KEY) : creationYear, revisionYear);
		this.preYearLinesString = preYearComment;
		this.yearLineString = middleYearsComment;
		this.postYearLineString = postYearComment;
		this.yearsCount = yearsCount;
	}

	/**
	 * Get the copyright tool preference store.
	 *
	 * @return preference store used the releng plugin.
	 */
	private static IPreferenceStore getPreferenceStore() {
		return RelEngPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Get the copyright statement in form of an array of Strings where
	 * each item is a line of the copyright statement.
	 *
	 * @return String[]  array of lines making up the comment. Containing $date template.
	 */
	private static String[] getLegalLines() {
		StringTokenizer st = new StringTokenizer(getPreferenceStore().getString(
				RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY), NEW_LINE, true);
		ArrayList<String> lines = new ArrayList<>();
		String previous = NEW_LINE;
		while (st.hasMoreTokens()) {
			String current = st.nextToken();
			// add empty lines to array as well
			if (NEW_LINE.equals(previous)) {
				lines.add(current);
			}
			previous = current;
		}
		String[] stringLines = new String[lines.size()];
		stringLines = lines.toArray(stringLines);
		return stringLines;
	}

	/**
	 * Write out the copyright statement, line by line, adding in the created/revision
	 * year as well as comment line prefixes.
	 *
	 * @param writer
	 * @param linePrefix
	 */
	private void writeLegal(PrintWriter writer, String linePrefix) {
		String[] legalLines = getLegalLines();
		for (String currentLine : legalLines) {
			int offset = currentLine.indexOf(DATE_VAR);
			// if this is the line, containing the ${date}, add in the year
			if (offset > -1) {
				writer.print(linePrefix + ' ' + currentLine.substring(0, offset)
						+ getCreationYear());
				if (hasRevisionYear() && getRevisionYear() != getCreationYear()) {
					writer.print(", " + getRevisionYear()); //$NON-NLS-1$
				}
				println(writer,
						currentLine.substring(offset + DATE_VAR.length(), currentLine.length()));
			} else {
				// just write out the line
				if (NEW_LINE.equals(currentLine)) {
					// handle empty lines
					println(writer, linePrefix);
				} else {
					println(writer, linePrefix + ' ' + currentLine);
				}
			}
		}
	}

	/**
	 * Replace the last year in the provided string that matches {@link #YEAR_REGEX}.
	 *
	 * @param line
	 *            the line that contains the year that you want to update.
	 * @param newYear
	 *            the new year that you want to update to.
	 * @return    the string with the last year updated or null if it fails to find a year.
	 */
	private static String updateLastYear(String line, int newYear) {

		Matcher matcher = Pattern.compile(YEAR_REGEX).matcher(line);

		// Find position of last year in the string.
		int lastStart = -1;
		while (matcher.find()) {
			lastStart = matcher.start();
		}

		// Failed to find a year. Return the original line.
		if (lastStart == -1) {
			return null;
		}

		// Insert new year
		String before = line.substring(0, lastStart);
		String after = line.substring(lastStart + 4);
		String updatedLine = before + Integer.toString(newYear) + after;

		return updatedLine;
	}

	/**
	 * In the situation that a line only has a single year 'Copyright 2000 IBM ... '. <br>
	 * append the revision year to make it like: 'Copyright 2000-2010 IBM ... '. <br>
	 *
	 * <p>
	 * This should <b>only</b> be used for lines that have a single year.
	 * </p>
	 *
	 * @param line
	 * @param year
	 * @return
	 */
	private static String insertRevisedYear(String line, int year) {
		Matcher matcher = Pattern.compile(YEAR_REGEX).matcher(line);

		if (!matcher.find())
			return line; // no year found. Return original.

		// Insert new year.
		String before = line.substring(0, matcher.end());
		String after = line.substring(matcher.end());
		String updatedLine = before + ", " + Integer.toString(year) + after; //$NON-NLS-1$

		return updatedLine;
	}

	/**
	 * Given a line with one or multiple years on it, count how many years occur on it.
	 *
	 * @param line
	 * @return
	 */
	private static int countYearsOnLine(String line) {
		Matcher yearMatcher = Pattern.compile(YEAR_REGEX).matcher(line);
		int count = 0;
		while (yearMatcher.find()) {
			count++;
		}
		return count;
	}

	/**
	 * <h1>Get first year in line. </h1>
	 * <p> For example given a line like '2000, 2012, 2011, IMB..' it would return 2000. <br>
	 *
	 * Pre-condition: The line must contain a valid year in the range YEAR_REGEX
	 *
	 * @see #YEAR_REGEX
	 * @param line       Line containing a year.
	 * @return           the first found year. 0 if none found. (caller should check).
	 */
	private static int getFirstYear(String line) {
		Matcher yearMatcher = Pattern.compile(YEAR_REGEX).matcher(line);
		if (yearMatcher.find()) {
			return Integer.parseInt(yearMatcher.group()); // exception never thrown since match only matches integer.
		}
		return 0; //No year was found on this line.
	}

	/**
	 * <h2>Get the last year in a line. '2000, 2012, 2011, IMB..'</h2> Pre-condition: The line must contain a valid year
	 * in the range YEAR_REGEX
	 *
	 * @see #YEAR_REGEX
	 * @param line
	 * @return e.g 2011
	 */
	private static int getLastYear(String line) {
		Matcher yearMatcher = Pattern.compile(YEAR_REGEX).matcher(line);

		int lastYear = -1;
		// loop till the last occurance.
		while (yearMatcher.find()) {
			lastYear = Integer.parseInt(yearMatcher.group()); // exception never thrown since match only matches
															  // integer.
		}
		return lastYear;
	}
}
