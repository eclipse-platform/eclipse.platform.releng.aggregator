/********************************************************************************
 * Copyright (c) 2016, 2025 GK Software SE and others.
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
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * To test locally, the /publish-to-maven-central/SDK4Mvn.aggr must be used to
 * produce the Mavenized p2 repository. To do that, you need to install the "CBI
 * Aggregator Editor" from <a href=
 * "https://download.eclipse.org/cbi/updates/p2-aggregator/tools/nightly/latest">https://download.eclipse.org/cbi/updates/p2-aggregator/tools/nightly/latest</a>.
 * Then you can open the SDK4Mvn.aggr in the Aggregator Editor and invoke
 * <em>Build Aggregation</em> from the context menu. That generates results in
 * <code>${user.home}/build</code>. Only the contents of following three folders are
 * actually published to Maven (perhaps equinox in the future), so you can run
 * <code>main</code> for just those folders:
 *
 * <pre>
 * ~/build/final/org/eclipse/pde
 * ~/build/final/org/eclipse/jdt
 * ~/build/final/org/eclipse/platform
 * -test
 * -verbose
 * </pre>
 *
 * The <code>-test</code> will test that the SCM URL actually exists. The
 * <code>-verbose</code> will print the contents to each enriched pom to
 * <code>System.out</code>.
 */
public class EnrichPoms {

	private static final String DOT_POM = ".pom";
	private static final String DOT_JAR = ".jar";
	private static final String BAK_SUFFIX = "-bak";

	static boolean test;

	static boolean verbose;

	public static void main(String[] args) throws IOException {
		Set<Path> paths = new LinkedHashSet<>();
		for (String arg : args) {
			if ("-test".equals(arg)) {
				test = true;
			} else if ("-verbose".equals(arg)) {
				verbose = true;
			} else {
				Path path = FileSystems.getDefault().getPath(arg);
				if (!Files.exists(path) || !Files.isDirectory(path)) {
					throw new IllegalArgumentException(path.toString() + " is not a directory");
				}
				paths.add(path);
			}
		}

		if (paths.isEmpty()) {
			throw new IllegalArgumentException("No directories specified");
		}

		for (Path path : paths) {
			try (Stream<Path> files = Files.walk(path)) {
				int enrichedCount = files.filter(EnrichPoms::isArtifact).mapToInt(EnrichPoms::enrich).sum();
				System.out.println("Enriched " + enrichedCount + " POM files under " + path);
			}
		}
	}

	private static boolean isArtifact(Path path) {
		if (!path.getFileName().toString().endsWith(DOT_POM) || Files.isDirectory(path)) {
			return false;
		}
		Path jarPath = getCorrespondingJarPath(path);
		return Files.exists(jarPath);
	}

	private static Path getCorrespondingJarPath(Path pomPath) {
		String fileName = pomPath.getFileName().toString();
		String jarName = fileName.substring(0, fileName.length() - DOT_POM.length()) + DOT_JAR;
		return pomPath.resolveSibling(jarName);
	}

	public static int enrich(Path pomPath) {
		try {
			Path jarPath = getCorrespondingJarPath(pomPath);
			ArtifactInfo info = ManifestReader.read(jarPath);
			if (info == null) {
				// Don't process features because they aren't published.
				return 0;
			}

			Path backPath = pomPath.resolveSibling(pomPath.getFileName().toString() + BAK_SUFFIX);
			Path newPom = Files.createTempFile(pomPath.getParent(), "", DOT_POM);
			try (Stream<String> lines = Files.lines(Files.exists(backPath) ? backPath : pomPath);
					Writer out = Files.newBufferedWriter(newPom);) {
				boolean detailsInserted = false;
				for (String line : (Iterable<String>) lines::iterator) {
					out.append(line).append('\n');
					if (!detailsInserted && line.contains("</description>")) {
						out.append(info.toPomFragment());
						detailsInserted = true;
					}
				}
			}
			if (verbose) {
				String pomText = Files.readString(newPom);
				System.out.println(pomText);
			}
			if (!Files.exists(backPath)) {
				Files.move(pomPath, backPath);
			}
			Files.move(newPom, pomPath, StandardCopyOption.REPLACE_EXISTING);
			return 1;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to rewrite pom " + pomPath, e);
		}
	}
}
