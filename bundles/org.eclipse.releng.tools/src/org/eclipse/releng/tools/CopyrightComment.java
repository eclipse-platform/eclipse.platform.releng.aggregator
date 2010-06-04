/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * Martin Oberhuber (Wind River) - [276255] fix insertion of extra space chars
 * Martin Oberhuber (Wind River) - [234872] avoid rem's that echo to the console
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.PrintWriter;
import java.util.Calendar;


public abstract class CopyrightComment {

    public static final int UNKNOWN_COMMENT = -1;
    public static final int JAVA_COMMENT = 1;
    public static final int PROPERTIES_COMMENT = 2;
    public static final int C_COMMENT = 3;
    public static final int SHELL_MAKE_COMMENT = 4;
    public static final int BAT_COMMENT = 5;
	public static final int JAVASCRIPT_COMMENT = 6;
	public static final int XML_COMMENT = 7;
    
	private int commentStyle;
    private int creationYear = -1;
    private int revisionYear = -1;
    
    protected CopyrightComment(int commentStyle, int creationYear, int revisionYear) {
        this.commentStyle = commentStyle;
        this.creationYear = creationYear == -1 ? (Calendar.getInstance().get(Calendar.YEAR)) : creationYear;
        this.revisionYear = revisionYear;
    }

    public static String getLinePrefix(int commentStyle) {
        switch(commentStyle) {
	        case JAVA_COMMENT:
	        case C_COMMENT:
		    case JAVASCRIPT_COMMENT:
	            return " *";  //$NON-NLS-1$
	        case PROPERTIES_COMMENT:
	            return "#"; //$NON-NLS-1$
	        case SHELL_MAKE_COMMENT:
	            return "#"; //$NON-NLS-1$
	        case BAT_COMMENT:
	            return "@rem"; //$NON-NLS-1$
	        case XML_COMMENT:
	            return "   "; //$NON-NLS-1$
		    default:
	            return null;
        }
	}
    
	protected void writeCommentStart(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
	    case C_COMMENT:
	    case JAVASCRIPT_COMMENT:
			writer.println("/*******************************************************************************"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    case SHELL_MAKE_COMMENT:
			writer.println("#*******************************************************************************"); //$NON-NLS-1$
			break;
	    case BAT_COMMENT:
			writer.println("@rem ***************************************************************************"); //$NON-NLS-1$
			break;
	    case XML_COMMENT:
			writer.println("<!--"); //$NON-NLS-1$
			break;
	    }
	}
	
	protected void writeCommentEnd(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
	    case C_COMMENT:
	    case JAVASCRIPT_COMMENT:
			writer.println(" *******************************************************************************/"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    case SHELL_MAKE_COMMENT:
			writer.println("#*******************************************************************************"); //$NON-NLS-1$
			break;
	    case BAT_COMMENT:
			writer.println("@rem ***************************************************************************"); //$NON-NLS-1$
			break;
	    case XML_COMMENT:
			writer.println(" -->"); //$NON-NLS-1$
			break;
	    }
	}
	
    
    public int getRevisionYear() {
        return revisionYear == -1 ? creationYear : revisionYear;
    }
    
    public boolean hasRevisionYear() {
    	return revisionYear != -1;
    }
    
    public void setRevisionYear(int year) {
        if (revisionYear != -1 || creationYear != year)
            revisionYear = year;
    }

	public int getCreationYear() {
		return creationYear;
	}
	
	public String getCommentPrefix() {
		return getLinePrefix(commentStyle);
	}

	public abstract String getCopyrightComment();
}
