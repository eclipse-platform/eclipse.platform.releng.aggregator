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

import java.sql.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server {

    public static void main(String[] args) throws Exception {

        try {
            System.out.println("Starting Network Server"); //$NON-NLS-1$
            System.setProperty("cloudscape.drda.startNetworkServer", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            System.setProperty("cloudscape.system.home", "/tmp/cloudscape"); //$NON-NLS-1$ //$NON-NLS-2$

            Class.forName("com.ihost.cs.jdbc.CloudscapeDriver").newInstance(); //$NON-NLS-1$

            /*
            com.ihost.cs.drda.NetworkServerControl server= new com.ihost.cs.drda.NetworkServerControl();
            System.out.println("Testing if Network Server is up and running!");
            for (int i= 0; i < 10; i++) {
                try {
                    server.ping();
                    break;
                } catch (Exception e) {
                    System.out.println("Try #" + i + " " + e.toString());
                    if (i == 9) {
                        System.out.println("Giving up trying to connect to Network Server!");
                        throw e;
                    }
                 }
                Thread.sleep(5000);
            }
            System.out.println("Cloudscape Network Server now running");
            */
            
        } catch (Exception e) {
            System.out.println("Failed to start NetworkServer: " + e); //$NON-NLS-1$
            System.exit(1);
        }

        Connection conn= null;
        try {
            String dbUrl= "jdbc:cloudscape:" + DB.DB_NAME + ";create=true;"; //$NON-NLS-1$ //$NON-NLS-2$
            conn= DriverManager.getConnection(dbUrl);
            System.out.println("Got an embedded connection."); //$NON-NLS-1$
            
            System.out.println("Press [Enter] to stop Server"); //$NON-NLS-1$
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            
        } catch (SQLException sqle) {
            System.out.println("Failure making connection: " + sqle); //$NON-NLS-1$
            sqle.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
            try {
                 DriverManager.getConnection("jdbc:cloudscape:;shutdown=true"); //$NON-NLS-1$
                 System.out.println("Server stopped"); //$NON-NLS-1$
            } catch (SQLException se) {
            	// TODO: should be logged
            }
        }
    }
}
