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
	    if (id >= 0 && id < fgRegisteredDimensions.length)
	        return fgRegisteredDimensions[id];
	    return null;
	}
	
	public Dimension(int id) {
		this(id, Unit.CARDINAL, 1);
	}

	public Dimension(int id, Unit unit) {
		this(id, unit, 1);
	}

	public Dimension(int id, Unit unit, int multiplier) {
	    
	    if (id >= 0 && id < fgRegisteredDimensions.length) {
		    if (fgRegisteredDimensions[id] == null) {
		        fgRegisteredDimensions[id]= this;
		    } else
		        System.err.println("Dimension with id " + id + " already registered");
	    }

		fId= id;
		fUnit= unit;
		fMultiplier= multiplier;
	}

    public int getId() {
        return fId;
    }
	
	public Unit getUnit() {
		return fUnit;
	}
	
	public int getMultiplier() {
		return fMultiplier;
	}

	public String getName() {
		return DimensionMessages.getString(fId);
	}
	
	public String toString() {
		return "Dimension [name=" + getName() + ", " + fUnit + "]";
	}
	
	public String getDisplayValue(Scalar scalar) {
		return fUnit.getDisplayValue1(scalar.getMagnitude(), fMultiplier);
	}
	
	public String getDisplayValue(double scalar) {
		return fUnit.getDisplayValue1((double)(scalar / fMultiplier));
	}
}
