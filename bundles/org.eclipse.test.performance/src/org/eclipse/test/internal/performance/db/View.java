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
package org.eclipse.test.internal.performance.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.eclipse.test.internal.performance.Dimensions;

public class View {

    public static void main(String[] args) throws Exception {
        
        Connection conn= DB.getConnection();
        
        PreparedStatement query1= conn.prepareStatement(
                "select SAMPLE.STARTTIME, SCALAR.VALUE from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG " + //$NON-NLS-1$
            		"where " + //$NON-NLS-1$
                 "SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.HOST = ? and CONFIG.PLATFORM = ? and " + //$NON-NLS-1$
          		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " + //$NON-NLS-1$
          		"DATAPOINT.SAMPLE_ID = SAMPLE.ID and " + //$NON-NLS-1$
            		"SCALAR.DATAPOINT_ID = DATAPOINT.ID and " + //$NON-NLS-1$
            		"SCALAR.DIM_ID = ? " + //$NON-NLS-1$
            		"order by SAMPLE.STARTTIME"	//$NON-NLS-1$
        			);
       
        String name= System.getProperty("os.name", "?"); //$NON-NLS-1$ //$NON-NLS-2$
        String version= System.getProperty("os.version", "?"); //$NON-NLS-1$ //$NON-NLS-2$
        String arch= System.getProperty("os.arch", "?"); //$NON-NLS-1$ //$NON-NLS-2$
        String conf= name + '/' + version + '/' + arch;

        
        query1.setString(1, "localhost");
        query1.setString(2, conf);
        query1.setString(3, "org.eclipse.jdt.ui.tests.performance.views.StartupTest#testStartup()");
        query1.setInt(4, Dimensions.ELAPSED_PROCESS.getId());

        
        
        try {
	        ResultSet rs= query1.executeQuery();
            
	        for (int i= 0; rs.next(); i++) {
	            BigDecimal bigDecimal= rs.getBigDecimal(1);
	            Date d= new Date(bigDecimal.longValue());
	    
	            System.out.println(i + ": " + d + " " + rs.getString(2));
	        }
	        
	        rs.close();
        } finally {
            query1.close();
        }
    }
}
