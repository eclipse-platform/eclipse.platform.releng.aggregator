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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class DB {
    
    private static final String LOCALHOST= "localhost"; //$NON-NLS-1$
    
    private static final boolean DEBUG= false;
    private static final boolean AGGREGATE= true;
    private static final boolean VARIATIONS= true;
        
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private int fConfigID;
    private int fStoredSamples;
    private boolean fStoreCalled;
    private boolean fIsEmbedded;
    
    
    public static boolean store(Properties config, Sample sample) {
        return getDefault().internalStore(config, sample);
    }
    
    public static DataPoint[] queryDataPoints(String configName, String buildPattern, String scenarioPattern, Dim[] dims) {
        Properties key= new Properties();
        if (configName != null)
            key.put(PerformanceTestPlugin.CONFIG, configName); //$NON-NLS-1$
        key.put(PerformanceTestPlugin.BUILD, buildPattern); //$NON-NLS-1$
        return getDefault().internalQueryDataPoints(key, scenarioPattern, dims);
    }
   
    public static DataPoint[] queryDataPoints(String configName, String buildPattern, String scenarioPattern) {
        Properties key= new Properties();
        if (configName != null)
            key.put(PerformanceTestPlugin.CONFIG, configName); //$NON-NLS-1$
        key.put(PerformanceTestPlugin.BUILD, buildPattern); //$NON-NLS-1$
        return getDefault().internalQueryDataPoints(key, scenarioPattern, null);
    }
   
    public static DataPoint[] queryDataPoints(Properties keys, String scenarioPattern, Dim[] dims) {
        return getDefault().internalQueryDataPoints(keys, scenarioPattern, dims);
    }
   
    public static Scenario[] queryScenarios(String configName, String buildPattern, String scenarioPattern) {
        return queryScenarios(configName, buildPattern, scenarioPattern, null);
    }

    public static Scenario[] queryScenarios(String configName, String buildPattern, String scenarioPattern, Dim[] dimensions) {
        return queryScenarios(configName, new String[] { buildPattern }, scenarioPattern, dimensions);
    }

    public static Scenario[] queryScenarios(String configName, String[] buildPatterns, String scenarioPattern, Dim[] dimensions) {
        
        if ("%".equals(configName)) { //$NON-NLS-1$
            System.err.println("Warning: DB.queryScenarios no longer supports config patters; returning empty array"); //$NON-NLS-1$
            return new Scenario[0];
        }
        
        String[] scenarios= getDefault().internalQueryScenarioNames(configName, scenarioPattern); // get all Scenario names
        if (scenarios == null)
            return new Scenario[0];
        Scenario[] tables= new Scenario[scenarios.length];
        for (int i= 0; i < scenarios.length; i++)
            tables[i]= new Scenario(configName, buildPatterns, scenarios[i], dimensions);
        return tables;
    }

    public static Scenario queryScenario(String config, String[] builds, String scenarioName) {
        return new Scenario(config, builds, scenarioName, null);
    }

    public static void queryBuildNames(List names, String configName, String buildPattern, String scenarioPattern) {
        
        Properties key= new Properties();
        if (configName != null)
            key.put(PerformanceTestPlugin.CONFIG, configName); //$NON-NLS-1$
        key.put(PerformanceTestPlugin.BUILD, buildPattern); //$NON-NLS-1$
        
        getDefault().internalQueryBuildNames(names, key, scenarioPattern);
    }

    public static Connection getConnection() {
        return getDefault().fConnection;
    }
    
    public static void dump() {
        System.err.println("DB.dump: disabled"); //$NON-NLS-1$
    }

    public static boolean isActive() {
        return fgDefault != null && fgDefault.getSQL() != null;
    }
    
    //---- private implementation
    
	/**
	 * Private constructor to block instance creation.
	 */
    private DB() {
        // empty implementation
    }

    synchronized static DB getDefault() {
        if (fgDefault == null) {
            fgDefault= new DB();       
            fgDefault.connect();
            if (PerformanceTestPlugin.getDefault() == null) {
            	// not started as plugin
	            Runtime.getRuntime().addShutdownHook(
	                new Thread() {
	                    public void run() {
	                    	shutdown();
	                    }
	                }
	            );
            }
        }
        return fgDefault;
    }
    
    public static void shutdown() {
        if (DEBUG) System.out.println("DB.shutdown"); //$NON-NLS-1$
        if (fgDefault != null) {
            fgDefault.disconnect();
            fgDefault= null;
        }
    }
   
    SQL getSQL() {
        return fSQL;
    }
    
    private int getConfig(Properties config) throws SQLException {
        if (fConfigID == 0) {
            String configName= config.getProperty(PerformanceTestPlugin.CONFIG, LOCALHOST);
            String buildName= config.getProperty(PerformanceTestPlugin.BUILD);
            fConfigID= fSQL.createConfig(configName, buildName);
        }
        return fConfigID;
    }
        
    private int getVariation(Properties config) throws SQLException {
        String variation= PerformanceTestPlugin.toVariations(config);
        return fSQL.createVariations(variation);
    }
        
    private boolean internalStore(Properties config, Sample sample) {
        
        if (fSQL == null || sample == null)
            return false;
        
		DataPoint[] dataPoints= sample.getDataPoints();
	    int n= dataPoints.length;
		if (n <= 0)
		    return false;

		Scalar[] sc= null;
		long[] averages= null;
	    if (AGGREGATE) {

		    sc= dataPoints[0].getScalars();
		    averages= new long[sc.length];

			Set set= new HashSet();
			for (int j= 0; j < dataPoints.length; j++) {
			    DataPoint dp= dataPoints[j];
			    set.add(new Integer(dp.getStep()));
			}
			switch (set.size()) {
			case 2:	// BEFORE/AFTER pairs -> calculate deltas
				for (int i= 0; i < dataPoints.length-1; i+= 2) {
				    Scalar[] s1= dataPoints[i].getScalars();
				    Scalar[] s2= dataPoints[i+1].getScalars();
				    for (int j= 0; j < sc.length; j++)
				        averages[j] += s2[j].getMagnitude() - s1[j].getMagnitude();
				}
				n= n/2;
				break;
				
			case 1:	// single values
				for (int i= 0; i < dataPoints.length; i++) {
				    Scalar[] s= dataPoints[i].getScalars();
				    for (int j= 0; j < sc.length; j++)
				        averages[j] += s[j].getMagnitude();
				}
				break;
				
			default:	// not expected
	            PerformanceTestPlugin.logError("DB.internalStore: too many steps in DataPoint"); //$NON-NLS-1$
			    return false;
			}
	    }
		
		//System.out.println("store started..."); //$NON-NLS-1$
	    try {
            //long l= System.currentTimeMillis();
            int config_id= getConfig(config);
            int variation_id= getVariation(config);
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            int sample_id= fSQL.createSample(config_id, variation_id, scenario_id, new Timestamp(sample.getStartTime()));

            if (sc != null) {
	            int datapoint_id= fSQL.createDataPoint(sample_id, 0, InternalPerformanceMeter.AVERAGE);
				for (int k= 0; k < sc.length; k++) {
			        int dim_id= sc[k].getDimension().getId();
					fSQL.insertScalar(datapoint_id, dim_id, averages[k] / n);
				}
		    } else {
				for (int i= 0; i < dataPoints.length; i++) {
				    DataPoint dp= dataPoints[i];
		            int datapoint_id= fSQL.createDataPoint(sample_id, i, dp.getStep());
				    Scalar[] scalars= dp.getScalars();
	 			    for (int j= 0; j < scalars.length; j++) {
				        Scalar scalar= scalars[j];
				        int dim_id= scalar.getDimension().getId();
				        long value= scalar.getMagnitude();					
						fSQL.insertScalar(datapoint_id, dim_id, value);
				    }
				}
		    }
			
			fConnection.commit();
            fStoredSamples++;
            fStoreCalled= true;

			//System.err.println(System.currentTimeMillis()-l);

        } catch (SQLException e) {
            PerformanceTestPlugin.log(e);
            try {
                fConnection.rollback();
            } catch (SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
        return true;
    }
    
    /*
     * fixed
     */
    private DataPoint[] internalQueryDataPoints(Properties variations, String scenarioName, Dim[] dims) {
        if (fSQL == null)
            return null;
 
         ResultSet rs= null;
         try {
        	
            //String configName= key.getProperty(PerformanceTestPlugin.CONFIG, LOCALHOST);
            //String buildName= key.getProperty(PerformanceTestPlugin.BUILD);
        	        	
            ArrayList dataPoints= new ArrayList();
            
            /* currently not needed
        	int[] dim_ids= null;
        	if (dims != null) {
        		dim_ids= new int[dims.length];
        		for (int i= 0; i < dims.length; i++)
        			dim_ids[i]= dims[i].getId();
        	}
        	if (dim_ids == null)
        		dim_ids= new int[0];
            */
            
            //rs= fSQL.queryDataPoints(configName, buildName, scenarioName);
            String variationPattern= PerformanceTestPlugin.toVariations(variations);
            rs= fSQL.queryDataPoints(variationPattern, scenarioName);
	        while (rs.next()) {
	            int datapoint_id= rs.getInt(1);
	            int step= rs.getInt(2);

	            HashMap map= new HashMap();      
	            ResultSet rs2= fSQL.queryDataPoints(datapoint_id);
		        while (rs2.next()) {
	                int dim_id= rs2.getInt(1);
	                long value= rs2.getBigDecimal(2).longValue();		            
	                Dim dim= Dim.getDimension(dim_id);
	                if (dim != null)
	                    map.put(dim, new Scalar(dim, value));
		        }
		        if (map.size() > 0)
		            dataPoints.add(new DataPoint(step, map));
		        
	            rs2.close();
	        }			       
	        rs.close();
        	
            int n= dataPoints.size();
            if (DEBUG) System.out.println("query resulted in " + n + " datapoints from DB"); //$NON-NLS-1$ //$NON-NLS-2$
            return (DataPoint[])dataPoints.toArray(new DataPoint[n]);

        } catch (SQLException e) {
            PerformanceTestPlugin.log(e);

        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e1) {
                	// ignored
                }
        }
        return null;
    }
    
    /*
     * Returns array of scenario names
     */
    private String[] internalQueryScenarioNames(String configPattern, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {
            result= fSQL.queryScenarios("%|"+PerformanceTestPlugin.CONFIG+"="+configPattern+"|%", scenarioPattern);
            ArrayList scenarios= new ArrayList();
            for (int i= 0; result.next(); i++)
		        scenarios.add(result.getString(1));
            return (String[])scenarios.toArray(new String[scenarios.size()]);

        } catch (SQLException e) {
	        PerformanceTestPlugin.log(e);

        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                	// ignored
                }
        }
        return null;
    }
    
    /*
     * fixed
     */
    private void internalQueryBuildNames(List buildNames, Properties key, String scenarioPattern) {
        if (fSQL == null)
            return;
        String keyName= PerformanceTestPlugin.toVariations(key);
        ResultSet result= null;
        try {        	
            result= fSQL.queryBuildNames(keyName, scenarioPattern);
            for (int i= 0; result.next(); i++) {
                String kvs= result.getString(1);
                Properties p= new Properties();
                PerformanceTestPlugin.parseVariations(p, kvs);
                String build= p.getProperty(PerformanceTestPlugin.BUILD);
                if (build != null && !buildNames.contains(build))
                    buildNames.add(build);
            }
        } catch (SQLException e) {
	        PerformanceTestPlugin.log(e);

        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                	// ignored
                }
        }
    }
    

    /**
     * dbloc=						embed in home directory
     * dbloc=/tmp/performance			embed given location
     * dbloc=net://localhost			connect to local server
     * dbloc=net://www.eclipse.org	connect to remove server
     */
    private void connect() {

        if (fConnection != null)
            return;
        
        String dbloc= PerformanceTestPlugin.getDBLocation();
        if (dbloc == null)
            return;
                
        String dbname= PerformanceTestPlugin.getDBName();
        String url= null;
        java.util.Properties info= new java.util.Properties();
        info.put("user", PerformanceTestPlugin.getDBUser());	//$NON-NLS-1$
        info.put("password", PerformanceTestPlugin.getDBPassword());	//$NON-NLS-1$
        
        try {            
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                // connect over network
                if (DEBUG) System.out.println("Trying to connect over network..."); //$NON-NLS-1$
                Class.forName("com.ibm.db2.jcc.DB2Driver"); //$NON-NLS-1$
                info.put("retrieveMessagesFromServerOnGetMessage", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                url= dbloc + "/" + dbname;  //$NON-NLS-1$//$NON-NLS-2$
            } else {
                // embedded
                fIsEmbedded= true;
                if (DEBUG) System.out.println("Loading embedded cloudscape..."); //$NON-NLS-1$
                Class.forName("com.ihost.cs.jdbc.CloudscapeDriver"); //$NON-NLS-1$
                File f;
                if (dbloc.length() == 0) {
                    String user_home= System.getProperty("user.home"); //$NON-NLS-1$
                    if (user_home == null)
                        return;
                    f= new File(user_home, "cloudscape"); //$NON-NLS-1$
                } else
                    f= new File(dbloc);
                url= new File(f, dbname).getAbsolutePath();
            }
            info.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            fConnection= DriverManager.getConnection("jdbc:cloudscape:" + url, info); //$NON-NLS-1$
            if (DEBUG) System.out.println("succeeded!"); //$NON-NLS-1$
 
            fConnection.setAutoCommit(false);
            fSQL= new SQL(fConnection);

            doesDBexists();
            
            if (VARIATIONS)
                upgradeVariationSupport();

            if (DEBUG) System.out.println("start prepared statements"); //$NON-NLS-1$
            fSQL.createPreparedStatements();
            if (DEBUG) System.out.println("finish with prepared statements"); //$NON-NLS-1$
                         
        } catch (SQLException ex) {
            PerformanceTestPlugin.logError(ex.getMessage());

        } catch (ClassNotFoundException e) {
	        PerformanceTestPlugin.log(e);
        }
    }
    
    private void disconnect() {
        if (DEBUG && fStoreCalled)
            System.out.println("stored " + fStoredSamples + " new datapoints in DB"); //$NON-NLS-1$ //$NON-NLS-2$
        if (DEBUG) System.out.println("disconnecting from DB"); //$NON-NLS-1$
        if (fSQL != null) {
            try {
                fSQL.dispose();
            } catch (SQLException e1) {
    	        PerformanceTestPlugin.log(e1);
            }
            fSQL= null;
        }
        if (fConnection != null) {
            try {
                fConnection.commit();
            } catch (SQLException e) {
    	        PerformanceTestPlugin.log(e);
            }
            try {
                 fConnection.close();
            } catch (SQLException e) {
    	        PerformanceTestPlugin.log(e);
            }
            fConnection= null;
        }
        
        if (fIsEmbedded) {
	        try {
	            DriverManager.getConnection("jdbc:cloudscape:;shutdown=true"); //$NON-NLS-1$
	        } catch (SQLException e) {
	            if (! "Cloudscape system shutdown.".equals(e.getMessage())) //$NON-NLS-1$
	                e.printStackTrace();
	        }
        }
    }
    
    private void doesDBexists() throws SQLException {
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("SELECT count(*) FROM sys.systables WHERE sys.systables.tablename NOT LIKE 'SYS%' "); //$NON-NLS-1$
	        if (rs.next() && rs.getInt(1) >= 6)
	            return;
	        if (DEBUG) System.out.println("initialising DB"); //$NON-NLS-1$
	        fSQL.initialize();
			fConnection.commit();
	        if (DEBUG) System.out.println("end initialising DB"); //$NON-NLS-1$
        } finally {
            stmt.close();
        }
    }
    
    private void upgradeVariationSupport() throws SQLException {
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("SELECT count(*) FROM sys.systables WHERE sys.systables.tablename = 'VARIATION' "); //$NON-NLS-1$
	        if (rs.next() && rs.getInt(1) > 0)
	            return;
	        if (DEBUG) System.out.println("adding VARIATION support"); //$NON-NLS-1$
	        fSQL.initializeVariations();
			fConnection.commit();
	        if (DEBUG) System.out.println("end VARIATION support"); //$NON-NLS-1$
        } finally {
            stmt.close();
        }
    }

    public static void main(String args[]) {
        DB.dump();
    }
}
