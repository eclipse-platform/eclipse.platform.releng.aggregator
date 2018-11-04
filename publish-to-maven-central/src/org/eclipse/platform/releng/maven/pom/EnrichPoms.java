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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class EnrichPoms {

	private static final String DOT_POM = ".pom";
	private static final String DOT_JAR = ".jar";
	private static final String BAK_SUFFIX = "-bak";


	public static void main(String[] args) throws IOException {
		Path path = FileSystems.getDefault().getPath(args[0]);
		if (!Files.exists(path) || !Files.isDirectory(path))
			throw new IllegalArgumentException(path.toString()+ " is not a directory");
		
		Files.walk(path)
			.filter(EnrichPoms::isArtifact)
			.forEach(EnrichPoms::enrich);
	}
	
	public static boolean isArtifact(Path path) {
		if (Files.isDirectory(path))
			return false;
		if (!path.getFileName().toString().endsWith(DOT_POM))
			return false;
		Path jarPath = getCorrespondingJarPath(path);
		return Files.exists(jarPath);
	}

	public static Path getCorrespondingJarPath(Path pomPath) {
		String fileName = pomPath.getFileName().toString();
		String jarName = fileName.substring(0, fileName.length()-DOT_POM.length())+DOT_JAR; 
		return pomPath.resolveSibling(jarName);
	}

	public static void enrich(Path pomPath) {
		try {
			Path backPath = pomPath.resolveSibling(pomPath.getFileName().toString()+BAK_SUFFIX);
			if (Files.exists(backPath)) {
				System.out.println("Skipping (-bak exists): "+pomPath);
				return;
			}
			Path jarPath = getCorrespondingJarPath(pomPath);
			ArtifactInfo info = ManifestReader.read(jarPath);
			Path newPom = Files.createTempFile(pomPath.getParent(), "", DOT_POM);
			try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(newPom))) {
				boolean copyrightInserted = false;
				boolean detailsInserted = false;
				for (String line : Files.readAllLines(pomPath)) {
					out.write(line);
					out.append('\n');
					if (!copyrightInserted) {
						if (line.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
							out.append(ArtifactInfo.COPYRIGHT);
							copyrightInserted = true;
						}
					}
					if (!detailsInserted) {
						if (line.contains("</description>")) {
							out.append(info.toPomFragment());
							detailsInserted = true;
						}
					}
				}
			}
			if (!Files.exists(backPath))
				Files.move(pomPath, backPath);
			Files.move(newPom, pomPath);
		} catch (IOException e) {
			System.err.println("Failed to rewrite pom "+pomPath+": "+e.getClass()+": "+e.getMessage());
		}
	}
}
