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

/**
 * @since 3.1
 */
public class Dimension {
    
    private static Dimension[] fgRegisteredDimensions= new Dimension[100];
    
    private final int fId;
	private final Unit fUnit;
	private final int fMultiplier;
	
	public static Dimension getDimension(int id) {
		return fgRegisteredDimensions[id];
	}
	
	public Dimension(int id) {
		this(id, Unit.CARDINAL, 1);
	}

	public Dimension(int id, Unit unit) {
		this(id, unit, 1);
	}

	public Dimension(int id, Unit unit, int multiplier) {
	    
	    if (fgRegisteredDimensions[id] == null) {
	        fgRegisteredDimensions[id]= this;
	    } else
	        System.err.println("Dimension with id " + id + " already registered");

		fId= id;
		fUnit= unit;
		fMultiplier= multiplier;
	}

	public String getName() {
		return DimensionMessages.getString(fId);
	}
	
    public int getId() {
        return fId;
    }
	
	public Unit getUnit() {
		return fUnit;
	}
	
	public String toString() {
		return "Dimension [name=" + getName() + ", " + fUnit + "]";
	}
	
	public int getMultiplier() {
		return fMultiplier;
	}

	public DisplayValue getDisplayValue(Scalar scalar) {
		return fUnit.getDisplayValue(scalar.getMagnitude(), fMultiplier);
	}
	
	public DisplayValue getDisplayValue(double scalar) {
		return fUnit.getDisplayValue(scalar);
	}

//	/**
//	 * Answer a formatted string for the elapsed time (minutes, hours or days) 
//	 * that is appropriate for the scale of the time.
//	 * 
//	 * @param diff time in milliseconds
//	 * 
//	 * I copied this from karasiuk.utility.TimeIt
//	 */
//	public static String formatedTime(long diff) {
//		if (diff < 0)diff *= -1;
// 		if (diff < 1000)
// 			return String.valueOf(diff) + " milliseconds";
// 		
// 		NumberFormat nf = NumberFormat.getInstance();
// 		nf.setMaximumFractionDigits(1);
//		double d = diff / 1000.0;	
//		if (d < 60)
//			return nf.format(d) + " seconds";
//		
//		d = d / 60.0;
//		if (d < 60.0)
//			return nf.format(d) + " minutes";
//	
//		d = d / 60.0;
//		if (d < 24.0)
//			return nf.format(d) + " hours";
//	
//		d = d / 24.0;
//		return nf.format(d) + " days";
//	}
//	
//	/**
//	 * Answer a number formatted using engineering conventions, K thousands, M millions,
//	 * G billions and T trillions.
//	 * 
//	 * I copied this method from karasiuk.utility.Misc.
//	 */
//	public static String formatEng(long n) {
//		if (n < 1000)
//			return String.valueOf(n);
//		double d = n / 1000.0;
//		NumberFormat nf = NumberFormat.getInstance();
//		nf.setMaximumFractionDigits(1);
//		if (d < 1000.0)
//			return nf.format(d) + "K";
//		
//		d = d / 1000.0;
//		if ( d < 1000.0)
//			return nf.format(d) + "M";
//		
//		d = d / 1000.0;
//		if ( d < 1000.0)
//			return nf.format(d) + "G";
//		
//		d = d / 1000.0;
//		return nf.format(d) + "T";
//	}
//
}
