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
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ManifestReader {

	private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
	// Eclipse headers in MANIFEST.MF:
	private static final String BUNDLE_SYMBOLIC_NAME 		= "Bundle-SymbolicName";
	private static final String BUNDLE_NAME 				= "Bundle-Name";
	private static final String BUNDLE_LOCALIZATION 		= "Bundle-Localization";
	private static final String ECLIPSE_SOURCE_REFERENCES 	= "Eclipse-SourceReferences";
	private static final String FRAGMENT_HOST 				= "Fragment-Host";
	
	private static final String DOT_PROPERTIES 				= ".properties";
	private static final String BUNDLE_PROPERTIES 			= "OSGI-INF/l10n/bundle.properties";

	public static ArtifactInfo read(Path path) throws FileNotFoundException, IOException {
		File file = path.toFile();
		try (ZipFile zip = new ZipFile(file)) {
			ZipEntry entry = zip.getEntry(MANIFEST_MF);
			Manifest mf = new Manifest(zip.getInputStream(entry));
			Attributes mainAttributes = mf.getMainAttributes();
//			printAllMainAttributes(mainAttributes);

			String localization = mainAttributes.getValue(BUNDLE_LOCALIZATION);
			boolean isFragment = mainAttributes.getValue(FRAGMENT_HOST) != null;
			Properties translations = getTranslations(file, localization, isFragment);

			ArtifactInfo info = new ArtifactInfo();
			info.bsn = getSymbolicName(mainAttributes);
			info.scmConnection = mainAttributes.getValue(ECLIPSE_SOURCE_REFERENCES);
			info.name = getBundleName(mainAttributes, translations, isFragment);
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

	static Properties getTranslations(File jarFile, String propFile, boolean isFragment) throws IOException {
		try (JarFile jf = new JarFile(jarFile)) {
			ZipEntry zipEntry = getTranslationsEntry(jf, propFile);
			if (zipEntry == null) {
				if (!isFragment) { // expected for fragments :-/
					if (propFile != null)
						System.err.println("translations "+propFile+DOT_PROPERTIES+" missing for "+jarFile.getName());
					else
						System.err.println("translations "+BUNDLE_PROPERTIES+" missing for "+jarFile.getName());
				}
				return null;
			}
			Properties properties = new Properties();
			properties.load(jf.getInputStream(zipEntry));
			return properties;
		}
	}
	
	static ZipEntry getTranslationsEntry(JarFile jf, String propFile) {
		ZipEntry entry = null;
		if (propFile != null)
			entry = jf.getEntry(propFile+DOT_PROPERTIES);
		if (entry == null)
			entry = jf.getEntry(BUNDLE_PROPERTIES);
		return entry;
	}

	static String getBundleName(Attributes mainAttributes, Properties translations, boolean isFragment) {
		String name = mainAttributes.getValue(BUNDLE_NAME);
		if (name.charAt(0) == '%') {
			if (translations == null) {
				if (isFragment) { // TODO
					System.err.println("Cannot translate fragment name "+name+" for "+getSymbolicName(mainAttributes));
				} else {
					System.err.println("Cannot translate bundle name "+name+" for "+getSymbolicName(mainAttributes));
				}
				return name;
			}
			String translated = translations.getProperty(name.substring(1));
			if (translated != null)
				return translated;
		}
		return name;
	}

	// debugging
	static void printAllMainAttributes(Attributes mainAttributes) {
		for (Entry<Object, Object> entry : mainAttributes.entrySet()) {
			System.out.println(entry.getKey()+" -> "+entry.getValue());
		}
	}
}
