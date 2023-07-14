/********************************************************************************
 * Copyright (c) 2016, 2018 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial implementation
 ********************************************************************************/
package org.eclipse.platform.releng.maven.pom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ManifestReader {

	private static final String FEATURE_XML = "feature.xml";
	private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
	// Eclipse headers in MANIFEST.MF:
	private static final String BUNDLE_SYMBOLIC_NAME 		= "Bundle-SymbolicName";
	private static final String ECLIPSE_SOURCE_REFERENCES 	= "Eclipse-SourceReferences";
	
	public static ArtifactInfo read(Path path) throws FileNotFoundException, IOException {
		File file = path.toFile();
		try (ZipFile zip = new ZipFile(file)) {
			ZipEntry featureEntry = zip.getEntry(FEATURE_XML);
			if (featureEntry != null) {
				return null;
			}
			
			ZipEntry entry = zip.getEntry(MANIFEST_MF);
			Manifest mf = new Manifest(zip.getInputStream(entry));
			Attributes mainAttributes = mf.getMainAttributes();
//			printAllMainAttributes(mainAttributes);


			ArtifactInfo info = new ArtifactInfo();
			info.bsn = getSymbolicName(mainAttributes);
			info.scmConnection = mainAttributes.getValue(ECLIPSE_SOURCE_REFERENCES);
			return info;
		}
	}

	public static String getSymbolicName(Attributes mainAttributes) {
		String bsn = mainAttributes.getValue(BUNDLE_SYMBOLIC_NAME);
		int semi = bsn.indexOf(';');
		if (semi != -1)
			return bsn.substring(0, semi); // cut off ;singleton etc...
		return bsn;
	}

	// debugging
	static void printAllMainAttributes(Attributes mainAttributes) {
		for (Entry<Object, Object> entry : mainAttributes.entrySet()) {
			System.out.println(entry.getKey()+" -> "+entry.getValue());
		}
	}
}
