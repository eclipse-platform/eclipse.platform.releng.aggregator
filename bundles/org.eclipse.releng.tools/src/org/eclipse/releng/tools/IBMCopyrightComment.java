/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

public class IBMCopyrightComment {

    public static final int UNKNOWN_COMMENT = -1;
    public static final int JAVA_COMMENT = 1;
    public static final int PROPERTIES_COMMENT = 2;

    private static final int DEFAULT_CREATION_YEAR = 2004;

    private int commentStyle = 0;
    private int creationYear = -1;
    private int revisionYear = -1;
    private List contributors;

    private IBMCopyrightComment(int commentStyle, int creationYear, int revisionYear, List contributors) {
        this.commentStyle = commentStyle;
        this.creationYear = creationYear == -1 ? DEFAULT_CREATION_YEAR : creationYear;
        this.revisionYear = revisionYear;
        this.contributors = contributors;
    }

    public static IBMCopyrightComment defaultComment(int commentStyle) {
        return new IBMCopyrightComment(commentStyle, DEFAULT_CREATION_YEAR, -1, null);
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
   	    int start = body.indexOf(copyrightLabel); //$NON-NLS-1$
   	    if (start == -1) return null;
   	    int contrib = body.indexOf("Contributors:", start); //$NON-NLS-1$
   	    int end = body.indexOf(" IBM Corp", start); //$NON-NLS-1$ // catch both IBM Corporation and IBM Corp.

   	    if (end == -1 || end > contrib) // IBM must be on the copyright line, not the contributor line
   	        return null;

   	    String yearRange = body.substring(start + copyrightLabel.length(), end);

   	    int comma = yearRange.indexOf(","); //$NON-NLS-1$

   	    String startStr = comma == -1 ? yearRange : yearRange.substring(0, comma);
   	    String endStr = comma == -1 ? null : yearRange.substring(comma + 1);

   	    int startYear = -1;
   	    if (startStr != null)
	   	    try {
	   	        startYear = Integer.parseInt(startStr.trim());
	   	    } catch(NumberFormatException e) {
	   	        // do nothing
	   	    }

   	    int endYear = -1;
   	    if (endStr != null)
   	        try {
   	            endYear = Integer.parseInt(endStr.trim());
   	        } catch(NumberFormatException e) {
   	            // do nothing
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
   	            if (c != -1)
   	                contributor = contributor.substring(c + linePrefix.length());
   	            contributors.add(contributor.trim());
   	        }
   	    }

        return new IBMCopyrightComment(commentStyle, startYear, endYear, contributors);
    }

    public int getRevisionYear() {
        return revisionYear == -1 ? creationYear : revisionYear;
    }

    public void setRevisionYear(int year) {
        if (revisionYear != -1 || creationYear != year)
            revisionYear = year;
    }

    private static String getLinePrefix(int commentStyle) {
        switch(commentStyle) {
        case JAVA_COMMENT:
            return " * ";  //$NON-NLS-1$
        case PROPERTIES_COMMENT:
            return "# "; //$NON-NLS-1$
        default:
            return null;
        }
	}

	/**
	 * Return the body of this copyright comment or null if it cannot be built.
	 */
	public String getCopyrightComment() {
	    String linePrefix = getLinePrefix(commentStyle);
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

	private void writeCommentStart(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
			writer.println("/*******************************************************************************"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    }
	}

	private void writeLegal(PrintWriter writer, String linePrefix) {
		writer.print(linePrefix + "Copyright (c) " + creationYear); //$NON-NLS-1$
		if (revisionYear != -1 && revisionYear != creationYear)
	        writer.print(", " + revisionYear); //$NON-NLS-1$
		writer.println(" IBM Corporation and others."); //$NON-NLS-1$

		writer.println(linePrefix + "All rights reserved. This program and the accompanying materials"); //$NON-NLS-1$
		writer.println(linePrefix + "are made available under the terms of the Eclipse Public License v1.0"); //$NON-NLS-1$
		writer.println(linePrefix + "which accompanies this distribution, and is available at"); //$NON-NLS-1$
		writer.println(linePrefix + "http://www.eclipse.org/legal/epl-v10.html"); //$NON-NLS-1$
	}

	private void writeContributions(PrintWriter writer, String linePrefix) {
		writer.println(linePrefix);
		writer.println(linePrefix + "Contributors:"); //$NON-NLS-1$

		if (contributors == null || contributors.size() <= 0)
		    writer.println(linePrefix + "    IBM Corporation - initial API and implementation"); //$NON-NLS-1$
		else {
			Iterator i = contributors.iterator();
			while (i.hasNext())
			    writer.println(linePrefix + "    " + (String)i.next());  //$NON-NLS-1$
		}
	}

	private void writeCommentEnd(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
			writer.println(" *******************************************************************************/"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    }
	}
}
