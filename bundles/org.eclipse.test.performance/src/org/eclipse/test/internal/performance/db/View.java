/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.db;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class View {
    
    public static void main(String[] args) {
		
		String outFile= null;
		// outfile= "/tmp/dbdump";	//$NON-NLS-1$
		PrintStream ps= null;
		if (outFile != null) {
		    try {
                ps= new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            } catch (FileNotFoundException e) {
                System.err.println("can't create output file"); //$NON-NLS-1$
            }
		}
		if (ps == null)
		    ps= System.out;
		
        // get all Scenarios 
        //Scenario[] scenarios= DB.queryScenarios("relengbuildwin2", "%", "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        Scenario[] scenarios= DB.queryScenarios("relengbuildwin2", "3%", "%RevertJavaEditorTest%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ps.println(scenarios.length + " Scenarios"); //$NON-NLS-1$
        ps.println();
        
        for (int s= 0; s < scenarios.length; s++) {
            scenarios[s].dump(ps);
        }
        
        if (ps != System.out)
            ps.close();
    }
}
