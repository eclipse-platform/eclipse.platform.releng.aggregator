package org.eclipse.test.internal.performance.db;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


public class DBHelpers {
        
    private Connection fConnection;
    
    
    public static void main(String[] args) throws SQLException {
                
        //System.setProperty("eclipse.perf.dbloc", "net://localhost"); //$NON-NLS-1$ //$NON-NLS-2$

        DBHelpers db= new DBHelpers();
        
		String outFile= null;
		//outFile= "out.txt";	//$NON-NLS-1$
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

		long start= System.currentTimeMillis();
		
		
		db.view(ps, "localhost", "%", "%");	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		db.dumpVariations(ps);
		db.dumpScenarios(ps, "%"); //$NON-NLS-1$
        db.dumpSizes();
		db.dumpAll(ps, 100);
		
        System.out.println("time: " + ((System.currentTimeMillis()-start)/1000.0)); //$NON-NLS-1$
        
        if (ps != System.out)
            ps.close();
    }

    DBHelpers() {
        fConnection= DB.getConnection();
    }

    private void dumpVariations(PrintStream ps) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select KEYVALPAIRS from VARIATION order by KEYVALPAIRS"); //$NON-NLS-1$
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void dumpScenarios(PrintStream ps, String pattern) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select NAME from SCENARIO where NAME like ? order by NAME"); //$NON-NLS-1$
        stmt.setString(1, pattern);
        ResultSet rs= stmt.executeQuery();
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void dumpSize(String table) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select Count(*) from " + table); //$NON-NLS-1$
        if (rs.next())
            System.out.println(table + ": " + rs.getInt(1)); //$NON-NLS-1$
        rs.close();   
        stmt.close();
    }

    private void dumpSizes() throws SQLException {
        if (fConnection == null)
            return;    
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("SELECT sys.systables.tablename FROM sys.systables WHERE sys.systables.tablename NOT LIKE 'SYS%' "); //$NON-NLS-1$
	        while (rs.next()) {
	            dumpSize(rs.getString(1));
	        }
        } finally {
            stmt.close();
        }
    }
    
    void dumpAll(PrintStream ps, int maxRow) throws SQLException {
        if (fConnection == null)
            return;

        if (maxRow < 0)
            maxRow= 1000000;
		Statement stmt= fConnection.createStatement();
		Statement stmt2= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("SELECT sys.systables.tablename FROM sys.systables WHERE sys.systables.tablename NOT LIKE 'SYS%' "); //$NON-NLS-1$
	        while (rs.next()) {
	            String tablename= rs.getString(1);
	            ps.print(tablename + '(');
		        ResultSet rs2= stmt2.executeQuery("SELECT * FROM " + tablename); //$NON-NLS-1$
	            ResultSetMetaData metaData= rs2.getMetaData();
	            int n= metaData.getColumnCount();
	            for (int i= 0; i < n; i++) {
	                ps.print(metaData.getColumnLabel(i+1));
	                if (i < n-1)
	                    ps.print(", "); //$NON-NLS-1$
	            }
	            ps.println("):"); //$NON-NLS-1$
		        for (int r= 0; rs2.next() && r < maxRow; r++) {
		            for (int i= 0; i < n; i++)
		                ps.print(' ' + rs2.getString(i+1));
		            ps.println();
		        }
	            ps.println();
	        }
	        rs.close();
        } finally {
            stmt.close();
            stmt2.close();
        }
    }         
    
    private void view(PrintStream ps, String config, String buildPattern, String scenarioPattern) throws SQLException {
        Scenario[] scenarios= DB.queryScenarios(config, buildPattern, scenarioPattern); 
        ps.println(scenarios.length + " Scenarios"); //$NON-NLS-1$
        ps.println();
        for (int s= 0; s < scenarios.length; s++)
            scenarios[s].dump(ps);
    }
}
