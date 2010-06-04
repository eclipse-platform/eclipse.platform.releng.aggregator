/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


public class IBMCopyrightComment extends CopyrightComment {

    private static final int DEFAULT_CREATION_YEAR = 2005;

    private List contributors;
    private int yearRangeStart, yearRangeEnd;
    private String originalText;

    private IBMCopyrightComment(int commentStyle, int creationYear, int revisionYear, List contributors, int yearRangeStart, int yearRangeEnd, String originalText) {
        super(commentStyle, creationYear == -1 ? DEFAULT_CREATION_YEAR : creationYear, revisionYear);
        this.contributors = contributors;
        this.yearRangeStart = yearRangeStart;
        this.yearRangeEnd = yearRangeEnd;
        this.originalText = originalText;
    }

    public static IBMCopyrightComment defaultComment(int commentStyle) {
        return new IBMCopyrightComment(commentStyle, DEFAULT_CREATION_YEAR, -1, null, 0, 0, null);
    }

    /**
     * Create an instance the same as the argument comment but with the revision year
     * updated if needed.  Return the default comment if the argument comment is null
     * or an empty string.  Return null if the argument comment is not recognized as
     * an IBM copyright comment.
     */
    public static IBMCopyrightComment parse(BlockComment comment, int commentStyle) {
        if (comment == null)
            return defaultComment(commentStyle);
        
   	    String body = comment.getContents();

        final String copyrightLabel = "Copyright (c) "; //$NON-NLS-1$
   	    int start = body.indexOf(copyrightLabel);
   	    if (start == -1) return null;
   	    int contrib = body.indexOf("Contributors:", start); //$NON-NLS-1$
   	    int rangeEnd = body.indexOf(" IBM Corp", start); //$NON-NLS-1$ // catch both IBM Corporation and IBM Corp.

   	    if (rangeEnd == -1 || rangeEnd > contrib) // IBM must be on the copyright line, not the contributor line
   	        return null;

   	    int rangeStart = start + copyrightLabel.length();
   	    String yearRange = body.substring(rangeStart, rangeEnd);

   	    int comma = yearRange.indexOf(","); //$NON-NLS-1$
   	    if (comma == -1) {
   	   	    comma = yearRange.indexOf("-"); //$NON-NLS-1$
   	    }

   	    String startStr = comma == -1 ? yearRange : yearRange.substring(0, comma);
   	    if (comma != -1 && Character.isWhitespace(yearRange.charAt(comma))) comma++;
   	    String endStr = comma == -1 ? null : yearRange.substring(comma + 1);

   	    int startYear = -1;
   	    if (startStr != null) {
	   	    try {
	   	        startYear = Integer.parseInt(startStr.trim());
	   	    } catch(NumberFormatException e) {
	   	        // do nothing
	   	    }
   	    }

   	    int endYear = -1;
   	    if (endStr != null) {
   	        try {
   	            endYear = Integer.parseInt(endStr.trim());
   	        } catch(NumberFormatException e) {
   	            // do nothing
   	        }
   	    }

   	    String contribComment = body.substring(contrib);
   	    StringTokenizer tokens = new StringTokenizer(contribComment, "\r\n"); //$NON-NLS-1$
   	    tokens.nextToken();
   	    ArrayList contributors = new ArrayList();
        String linePrefix = getLinePrefix(commentStyle);
   	    while(tokens.hasMoreTokens()) {
   	        String contributor = tokens.nextToken();
   	        if (contributor.indexOf("***********************************") == -1 //$NON-NLS-1$
   	         && contributor.indexOf("###################################") == -1) { //$NON-NLS-1$
   	            int c = contributor.indexOf(linePrefix);
   	            if (c == -1 && linePrefix.equals(" *")) { //$NON-NLS-1$
   	            	// special case: old prefix was "*" and new prefix is " *"
   	            	c = contributor.indexOf("*"); //$NON-NLS-1$
   	            }
   	            if (c == 0) {
					// prefix has to be at the beginning of the line
   	                contributor = contributor.substring(c + linePrefix.length());
   	            }
   	            contributors.add(contributor);
   	        }
   	    }

        return new IBMCopyrightComment(commentStyle, startYear, endYear, contributors, rangeStart, rangeEnd, body);
    }

	/**
	 * Return the body of this copyright comment or null if it cannot be built.
	 */
	public String getCopyrightComment() {
	    String linePrefix = getCommentPrefix();
	    if (linePrefix == null)
	        return null;

	    StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		try {
		    writeCommentStart(writer);
			writeLegal(writer, linePrefix);
			writeContributions(writer, linePrefix);
		    writeCommentEnd(writer);
			return out.toString();
		} finally {
		    writer.close();
		}
	}

	/**
	 * Return the body of the original copyright comment with new dates.
	 */
	public String getOriginalCopyrightComment() {
	    StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		try {
			writer.print(originalText.substring(0, yearRangeStart));
			writer.print(getCreationYear());
			if (hasRevisionYear() && getRevisionYear() != getCreationYear())
		        writer.print(", " + getRevisionYear()); //$NON-NLS-1$
			writer.print(originalText.substring(yearRangeEnd));
			return out.toString();
		} finally {
		    writer.close();
		}
	}

	private void writeLegal(PrintWriter writer, String linePrefix) {
		writer.print(linePrefix + " Copyright (c) " + getCreationYear()); //$NON-NLS-1$
		if (hasRevisionYear() && getRevisionYear() != getCreationYear())
	        writer.print(", " + getRevisionYear()); //$NON-NLS-1$
		writer.println(" IBM Corporation and others."); //$NON-NLS-1$

		writer.println(linePrefix + " All rights reserved. This program and the accompanying materials"); //$NON-NLS-1$
		writer.println(linePrefix + " are made available under the terms of the Eclipse Public License v1.0"); //$NON-NLS-1$
		writer.println(linePrefix + " which accompanies this distribution, and is available at"); //$NON-NLS-1$
		writer.println(linePrefix + " http://www.eclipse.org/legal/epl-v10.html"); //$NON-NLS-1$
	}

	private void writeContributions(PrintWriter writer, String linePrefix) {
		writer.println(linePrefix);
		writer.println(linePrefix + " Contributors:"); //$NON-NLS-1$

		if (contributors == null || contributors.size() <= 0)
		    writer.println(linePrefix + "     IBM Corporation - initial API and implementation"); //$NON-NLS-1$
		else {
			Iterator i = contributors.iterator();
			while (i.hasNext()) {
				String contributor = (String) i.next();
				if (contributor.length() > 0) {
					if (Character.isWhitespace(contributor.charAt(0))) {
					    writer.println(linePrefix + contributor);
					} else {
					    writer.println(linePrefix + " " + contributor);  //$NON-NLS-1$
					}
				} else {
				    writer.println(linePrefix);
				}
			}
		}
	}
}
