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

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;

/**
 * The <code>Variations</code> class represents a set of key/value pairs
 * that are used to tag data stored in the performance database and to
 * query for data from the databse. 
 */
public class Variations extends Properties {
    
    private static final long serialVersionUID= 1L;

    /**
     * Creates an empty set of key/value pairs.
     */
    public Variations() {
        //
    }

    /**
     * Creates a set of key/value pairs that consists
     * @param config
     * @param build
     * @deprecated Use the default constructor instead and fill in key/value pairs explicitely.
     */
    public Variations(String config, String build) {
        if (config != null)
            put(PerformanceTestPlugin.CONFIG, config);
        if (build != null)
            put(PerformanceTestPlugin.BUILD, build);
    }

    /**
     * Creates a set of key/value pairs by parsing the given string.
     * The strings format is an implementation detail of the database.
     * @param dbRepresentation
     */
    public Variations(String dbRepresentation) {
        parse(this, dbRepresentation);
    }

    public String toExactMatchString() {
        return toDB(this, false);
    }
    
    public String toQueryPattern() {
        return toDB(this, true);
    }

    private static void parse(Properties keys, String s) {
		StringTokenizer st= new StringTokenizer(s, "|"); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token= st.nextToken();
			int i= token.indexOf('=');
			if (i < 1)
			    throw new IllegalArgumentException("Key '" + token + "' is illformed"); //$NON-NLS-1$ //$NON-NLS-2$
			String value= token.substring(i+1);
			token= token.substring(0, i);
			//System.out.println(token + ": <" + value + ">");
			keys.put(token, value);
		}	    
	}
	
	/*
	 * TODO: we need to escape '=' and ';' characters in key/values.
	 */
    private static String toDB(Properties keyValues, boolean asQuery) {
        Set set= keyValues.keySet();
        String[] keys= (String[]) set.toArray(new String[set.size()]);
        Arrays.sort(keys);
        StringBuffer sb= new StringBuffer();
        
        for (int i= 0; i < keys.length; i++) {
            if (asQuery)
                sb.append('%');
            String key= keys[i];
            String value= keyValues.getProperty(key);
            sb.append('|');
            sb.append(key);
            sb.append('=');
            if (value != null)
                sb.append(value);
            sb.append('|');
        }
        if (asQuery)
            sb.append('%');
	    return sb.toString();
    }
}
