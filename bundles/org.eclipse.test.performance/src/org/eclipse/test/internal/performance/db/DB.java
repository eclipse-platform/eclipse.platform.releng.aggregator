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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.performance.Dimension;

public class DB {
    
    private static final boolean DEBUG= false;
    private static final boolean AGGREGATE= true;
        
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private int fStoredSamples;
    private boolean fStoreCalled;
    private boolean fIsEmbedded;
    
    
    // Datapaoints
    public static DataPoint[] queryDataPoints(Variations variations, String scenarioName, Set dims) {
        return getDefault().internalQueryDataPoints(variations, scenarioName, dims);
    }
   
    // Scenarios
    /**
     * Return all Scenarios that match the given config, build, and scenario name.
     * @param configName
     * @param buildPattern
     * @param scenarioPattern
     * @return array of scenarios
     * @deprecated Use the Variations based form of this method.
     */
    public static Scenario[] queryScenarios(String configName, String buildPattern, String scenarioPattern) {
        return queryScenarios(configName, new String[] { buildPattern }, scenarioPattern, null);
    }

    /**
     * Return the specified Dimensions of all Scenarios that match the given config, build, and scenario name.
     * @param configName
     * @param buildPatterns
     * @param scenarioPattern
     * @param dimensions
     * @return array of scenarios
     * @deprecated Use the Variations based form of this method.
     */
    public static Scenario[] queryScenarios(String configName, String[] buildPatterns, String scenarioPattern, Dim[] dimensions) {
        
        if ("%".equals(configName)) { //$NON-NLS-1$
            System.err.println("Warning: DB.queryScenarios no longer supports config patters; returning empty array"); //$NON-NLS-1$
            return new Scenario[0];
        }
        
        Variations v= new Variations();
        v.put(PerformanceTestPlugin.CONFIG, configName);
        String[] scenarios= getDefault().internalQueryScenarioNames(v, scenarioPattern); // get all Scenario names
        if (scenarios == null)
            return new Scenario[0];
        Scenario[] tables= new Scenario[scenarios.length];
        for (int i= 0; i < scenarios.length; i++) {
            Variations v2= new Variations();
            v2.put(PerformanceTestPlugin.CONFIG, configName);
            tables[i]= new Scenario(v2, PerformanceTestPlugin.BUILD, buildPatterns, scenarios[i], dimensions);
        }
        return tables;
    }

    /**
     * 
     * @param config
     * @param builds
     * @param scenarioName
     * @return Scenario
     * @deprecated 
     */
    public static Scenario queryScenario(String config, String[] builds, String scenarioName) {
        Variations v= new Variations();
        v.put(PerformanceTestPlugin.CONFIG, config);
        return new Scenario(v, PerformanceTestPlugin.BUILD, builds, scenarioName, null);
    }
    
    // fingerprints
    /**
     * @param variationPatterns
     * @param global
     * @return
     * @deprecated Use querySummaries(Variations, null) instead
     */
    public static SummaryEntry[] querySummaries(Variations variationPatterns, boolean global) {
        return getDefault().internalQuerySummaries(variationPatterns, null);
    }

    public static SummaryEntry[] querySummaries(Variations variationPatterns, String scenarioPattern) {
        return getDefault().internalQuerySummaries(variationPatterns, scenarioPattern);
    }

    /**
     * @param names
     * @param variationPatterns
     * @param scenarioPattern
     * @deprecated Use queryDistinctValues instead
     */
    public static void queryBuildNames(List names, Variations variationPatterns, String scenarioPattern) {
        getDefault().internalQueryDistinctValues(names, PerformanceTestPlugin.BUILD, variationPatterns, scenarioPattern);
    }

    public static void queryDistinctValues(List values, String key, Variations variationPatterns, String scenarioPattern) {
        getDefault().internalQueryDistinctValues(values, key, variationPatterns, scenarioPattern);
    }

    /**
     * Store the data contained in the given sample in the database.
     * The data is tagged with key/value pairs from variations.
     * @param variations used to tag the data in the database
     * @param sample the sample to store
     * @return returns true if data could be stored successfully
     */
    public static boolean store(Variations variations, Sample sample) {
        return getDefault().internalStore(variations, sample);
    }
    
    public static Connection getConnection() {
        return getDefault().fConnection;
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
    
    private boolean internalStore(Variations variations, Sample sample) {
        
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
            int variation_id= fSQL.getVariations(variations);
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            if (sample.isSummary()) {
                boolean isGlobal= sample.isGlobal();
                Dimension[] summaryDimensions= sample.getSummaryDimensions();
                for (int i= 0; i < summaryDimensions.length; i++) {
                    Dimension dimension= summaryDimensions[i];
                    if (dimension instanceof Dim)
                        fSQL.createSummaryEntry(variation_id, scenario_id, ((Dim)dimension).getId(), isGlobal);
                }
                fSQL.setScenarioShortName(scenario_id, sample.getShortname());
            }
            int sample_id= fSQL.createSample(variation_id, scenario_id, new Timestamp(sample.getStartTime()));

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
    
    private DataPoint[] internalQueryDataPoints(Variations variations, String scenarioName, Set dimSet) {
        if (fSQL == null)
            return null;
        
        ResultSet rs= null;
        try {
            ArrayList dataPoints= new ArrayList(); 
            rs= fSQL.queryDataPoints(variations, scenarioName);
	        while (rs.next()) {
	            int datapoint_id= rs.getInt(1);
	            int step= rs.getInt(2);

	            HashMap map= new HashMap();      
	            ResultSet rs2= fSQL.queryScalars(datapoint_id);
		        while (rs2.next()) {
	                int dim_id= rs2.getInt(1);
	                long value= rs2.getBigDecimal(2).longValue();		            
	                Dim dim= Dim.getDimension(dim_id);
	                if (dim != null) {
	                    if (dimSet == null || dimSet.contains(dim))
	                        map.put(dim, new Scalar(dim, value));
	                }
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
    private String[] internalQueryScenarioNames(Variations variations, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {
            result= fSQL.queryScenarios(variations, scenarioPattern);
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
     * 
     */
    private void internalQueryDistinctValues(List values, String key, Variations variations, String scenarioPattern) {
        if (fSQL == null)
            return;
        ResultSet result= null;
        try {        	
            result= fSQL.queryVariations(variations.toExactMatchString(), scenarioPattern);
            for (int i= 0; result.next(); i++) {
                Variations p= new Variations(result.getString(1));
                String build= p.getProperty(key);
                if (build != null && !values.contains(build))
                    values.add(build);
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
    
    private SummaryEntry[] internalQuerySummaries(Variations variationPatterns, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {
            List fingerprints= new ArrayList();
            ResultSet rs;
            if (scenarioPattern != null)
                rs= fSQL.querySummaryEntries(variationPatterns, scenarioPattern);
            else
                rs= fSQL.queryGlobalSummaryEntries(variationPatterns);
            while (rs.next()) {
                String scenarioName= rs.getString(1);
                String shortName= rs.getString(2);
                int dim_id= rs.getInt(3);
                boolean isGlobal= rs.getShort(4) == 1;
                fingerprints.add(new SummaryEntry(scenarioName, shortName, Dim.getDimension(dim_id), isGlobal));
            }
            return (SummaryEntry[])fingerprints.toArray(new SummaryEntry[fingerprints.size()]);
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
        
        try {            
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                info.put("user", PerformanceTestPlugin.getDBUser());	//$NON-NLS-1$
                info.put("password", PerformanceTestPlugin.getDBPassword());	//$NON-NLS-1$
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
			fConnection.commit();

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
}
