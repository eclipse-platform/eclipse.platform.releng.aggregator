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
package org.eclipse.test.internal.performance.data;

import java.text.NumberFormat;


/**
 * @since 3.1
 */
public class Unit {

	public static final Unit SECOND= new Unit("s", "second", false);  //$NON-NLS-1$
	public static final Unit BYTE= new Unit("byte", "byte", true);  //$NON-NLS-1$
	public static final Unit CARDINAL= new Unit("", "", false);  //$NON-NLS-1$

	private static final int T_DECIMAL= 1000;
	private static final int T_BINARY= 1024;
	
	protected static final String[] PREFIXES= new String[] { "y", "z", "a", "f", "p", "n", "u", "m", "", "k", "M", "G", "T", "P", "E", "Z", "Y" };
	//protected static final String[] FULL_PREFIXES= new String[] { "yocto", "zepto", "atto", "femto", "pico", "nano", "micro", "milli", "", "kilo", "mega", "giga", "tera", "peta", "exa", "zetta", "yotta" };
	protected static final String[] BINARY_PREFIXES= new String[] { "", "", "", "", "", "", "", "", "", "ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi", "Yi" };
	protected static final String[] BINARY_FULL_PREFIXES= new String[] { "", "", "", "", "", "", "", "", "", "kibi", "mebi", "gibi", "tebi", "pebi", "exbi", "zebi", "yobi" };
	
	private final String fShortName;
	private final String fFullName;
	private final boolean fIsBinary;
	private final int fPrecision= 2;
	
	public Unit(String shortName, String fullName, boolean binary) {
		fShortName= shortName;
		fFullName= fullName;
		fIsBinary= binary;
	}
	
	public String getShortName() {
		return fShortName;
	}
	
	public String getFullName() {
		return fFullName;
	}
	
	public String getDisplayValue1(long magnitudel, int multiplier) {
	    
	    //return getDisplayValue1(magnitudel / multiplier);
	    //return Long.toString((double)(magnitudel / multiplier));
	    return getDisplayValue1((double)(magnitudel / multiplier));
	}

	public String getDisplayValue1(double magnitude) {
	    
	    if ("s".equals(fShortName))
	        return formatedTime((long) (magnitude*1000.0));
	    if ("byte".equals(fShortName))
	        return formatEng((long) (magnitude));
	    return Double.toString(magnitude);
	    
	    /*
		int div= fIsBinary ? T_BINARY : T_DECIMAL;
		boolean negative= magnitude < 0;
		double mag= Math.abs(magnitude), ratio= mag / div;
		int divs= PREFIXES.length / 2;
		while (ratio >= 1) {
			mag= ratio;
			divs++;
			ratio= mag / div;
		}
		ratio= mag * div;
		while (ratio > 0.0 && ratio < div) {
			mag= ratio;
			divs--;
			ratio= mag * div;
		}
		
		if (negative)
			mag= -mag;
		
		String[] prefixes= fIsBinary ? BINARY_PREFIXES : PREFIXES;
		NumberFormat format= NumberFormat.getInstance();
		format.setMaximumFractionDigits(fPrecision);
		if (divs > 0 && divs <= prefixes.length)
			return prefixes[divs] + getShortName() + format.format(mag);
		else
			return getShortName() + magnitude;
		*/
	}
	
	public String toString() {
		return "Unit [" + getShortName() + "]";
	}
	
	/**
	 * Answer a formatted string for the elapsed time (minutes, hours or days) 
	 * that is appropriate for the scale of the time.
	 * 
	 * @param diff time in milliseconds
	 * 
	 * I copied this from karasiuk.utility.TimeIt
	 */
	public static String formatedTime(long diff) {
		if (diff < 0)
		    diff *= -1;
		if (diff < 1000)
			return String.valueOf(diff) + " milliseconds";
		
		NumberFormat nf= NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);
		double d = diff / 1000.0;	
		if (d < 60)
			return nf.format(d) + " seconds";
		
		d = d / 60.0;
		if (d < 60.0)
			return nf.format(d) + " minutes";
	
		d = d / 60.0;
		if (d < 24.0)
			return nf.format(d) + " hours";
	
		d = d / 24.0;
		return nf.format(d) + " days";
	}
	
	/**
	 * Answer a number formatted using engineering conventions, K thousands, M millions,
	 * G billions and T trillions.
	 * 
	 * I copied this method from karasiuk.utility.Misc.
	 */
	public String formatEng(long n) {
	    int TSD= fIsBinary ? T_BINARY : T_DECIMAL;
		if (n < TSD)
			return String.valueOf(n);
		double d = n / TSD;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);
		if (d < TSD)
			return nf.format(d) + "K";
		
		d = d / TSD;
		if ( d < TSD)
			return nf.format(d) + "M";
		
		d = d / TSD;
		if ( d < TSD)
			return nf.format(d) + "G";
		
		d = d / TSD;
		return nf.format(d) + "T";
	}

}
