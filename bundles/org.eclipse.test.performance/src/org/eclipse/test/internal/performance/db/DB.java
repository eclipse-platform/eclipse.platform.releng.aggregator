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
import java.util.Properties;

import org.eclipse.test.internal.performance.data.Sample;

public class DB {
    
    public static final String DB_NAME= "perfDB";
    public static final String DB_PATH= "/tmp/cloudscape/";
    
    private static final String HOST= null; // "localhost:1527"
    
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    
    
    public static DB getDefault() {
        if (fgDefault == null) {
            fgDefault= new DB();
            fgDefault.connect(DB_NAME);
        }
        return fgDefault;
    }
    
    private DB() {
    }
    
    private void connect(String dbname) {
        if (fConnection != null)
            return;
        
        try {
            System.out.println("Trying to connect...");
            fConnection= connect2(dbname);
            System.out.println("connected!");
 
            fSQL= new SQL(fConnection);

            doesDBexists();

            System.out.println("start prepared statements");
            fSQL.createPreparedStatements();
            System.out.println("finish with prepared statements");
                         
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void disconnect() {
        if (fSQL != null) {
            try {
                fSQL.dispose();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            fSQL= null;
        }
        if (fConnection != null) {
            try {
                 fConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            fConnection= null;
        }
    }
    
    public SQL getSQL() {
        return fSQL;
    }
    
    private Connection connect2(String dbName) throws ClassNotFoundException, SQLException {
        String url;
        Properties p= new Properties();
        if (HOST != null) {
            // connect over network
            Class.forName("com.ibm.db2.jcc.DB2Driver");
            url="jdbc:cloudscape:net://" + HOST + "/" + dbName + ";retrieveMessagesFromServerOnGetMessage=true;deferPrepares=true;";
            p.setProperty("user", "foo");
            p.setProperty("password", "bar");
        } else {
            // embedded
            Class.forName("com.ihost.cs.jdbc.CloudscapeDriver");
            url= "jdbc:cloudscape:" + DB_PATH + dbName + ";create=true";
        }
        return DriverManager.getConnection(url, p);
    }

    private void doesDBexists() throws SQLException {
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("select count(*) from sys.systables");
	        while (rs.next())
	            if (rs.getInt(1) > 16)
	                return;
	        System.out.println("initialising DB");
	        fSQL.initialize();
	        System.out.println("end initialising DB");
        } finally {
            stmt.close();
        }
    }
    
    public void store(Sample sample) {
        
        
    }

    //---- test --------
    
    public static void main(String args[]) throws Exception {
        
        DB db= DB.getDefault();
        
        try {            
            
            SQL sql= db.getSQL();
            
            System.out.println("adding to DB");
            int config_id= sql.getConfig("burano", "MacOS X");
            int session_id= sql.addSession("3.1", config_id);
            int scenario_id= sql.getScenario("foo.bar.Test.testFoo()");
            int sample_id= sql.createSample(session_id, scenario_id, 0);
            
            int time_id= sql.getDimension("time");
            int memory_id= sql.getDimension("memory");
            
            for (int r= 0; r < 10; r++) {	// runs
	            int draw_id= sql.createDraw(sample_id);
	            for (int d= 0; d < 2; d++) {	// start, end
	                int datapoint_id= sql.createDataPoint(draw_id);
	                sql.createScalar(datapoint_id, time_id, 12345*(d+1));		// time
	                sql.createScalar(datapoint_id, memory_id, 14*(d+1));		// memory
	            }
            }
            System.out.println("end adding to DB");
            
            
            ResultSet result= sql.query("burano", "MacOS X", "3.1", "foo.bar.Test.testFoo()", time_id);
            for (int i= 0; result.next(); i++)
                System.out.println(i + ": " + result.getString(1));
            result.close();
             
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
        }
        
        db.disconnect();        
    }
}
