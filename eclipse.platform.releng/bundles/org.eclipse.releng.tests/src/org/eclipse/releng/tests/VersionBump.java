/*******************************************************************************
 * Copyright (c) 2024 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.osgi.framework.Version;

public class VersionBump {

	/**
	 * Checks if the version bump needed for given bundle, and if needed, bumps
	 * micro segment in manifest and pom
	 *
	 * @param args first argument is bundle root path to check, second version SDK
	 *             release tag like "R4_31"
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Arguments: first is path to the bundle root, \n" + "second is the release tag");
			System.exit(1);
		}
		run(Paths.get(args[0]), args[1]);
	}

	/**
	 *
	 * @param bundleRoot
	 * @param releaseTag
	 * @return true if manifest and pom (if exists) were updated
	 */
	public static boolean run(Path bundleRoot, String releaseTag) {
		if (!Files.isDirectory(bundleRoot)) {
			System.err.println("Bundle root is not a directory: " + bundleRoot);
			return false;
		}
		if (releaseTag == null || releaseTag.isBlank()) {
			System.err.println("Release tag missing");
			return false;
		}

		String manifestPath = "META-INF/MANIFEST.MF";
		String pomPath = "pom.xml";

		String oldManifestContent = readFileAtTag(bundleRoot, manifestPath, releaseTag);
		String oldTagVersion = getVersionFromManifest(oldManifestContent);
		System.out.println("Version at tag " + releaseTag + ": " + oldTagVersion);

		String currentManifestContent = readCurrentFile(bundleRoot, manifestPath);
		String currentVersion = getVersionFromManifest(currentManifestContent);
		System.out.println("Version checked out : " + currentVersion);

		if (currentVersion != null && Objects.equals(oldTagVersion, currentVersion)) {
			Version cv = new Version(currentVersion);
			Version updated = new Version(cv.getMajor(), cv.getMinor(), cv.getMicro() + 100, cv.getQualifier());
			System.out.println("Update to: " + updated);
			String newManifest = oldManifestContent.replace("Bundle-Version: " + oldTagVersion,
					"Bundle-Version: " + updated);

			writeFile(bundleRoot, manifestPath, newManifest);
			System.out.println("Updated META-INF/MANIFEST.MF in " + bundleRoot);

			String currentPomContent = readCurrentFile(bundleRoot, pomPath);
			if (currentPomContent != null) {
				String newPom = currentPomContent.replace("<version>" + oldTagVersion, "<version>" + updated);
				writeFile(bundleRoot, pomPath, newPom);
				System.out.println("Updated pom.xml in " + bundleRoot);
			}
			return true;
		} else {
			System.out.println("No version bump needed");
			return false;
		}

	}

	private static void writeFile(Path bundleRoot, String manifestPath, String newManifest) {
		try {
			Files.write(bundleRoot.resolve(manifestPath), newManifest.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String readCurrentFile(Path bundleRoot, String relativePath) {
		try {
			return Files.readString(bundleRoot.resolve(relativePath), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Pattern VERSION_PATTERN = Pattern.compile(".*Bundle-Version: (\\d+\\.\\d+\\.\\d+).*",
			Pattern.DOTALL);

	private static String getVersionFromManifest(String oldManifestContent) {
		if (oldManifestContent == null) {
			return null;
		}
		Matcher matcher = VERSION_PATTERN.matcher(oldManifestContent);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

	private static String readFileAtTag(Path filePath, String fileInBundle, String releaseTag) {
		try {
			FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
			repoBuilder.findGitDir(filePath.toFile());
			try (Repository repository = repoBuilder.build()) {
				Path workDir = repository.getWorkTree().toPath();
				Path repoRelativePath = workDir.relativize(filePath.resolve(fileInBundle));
				String pathInGit = repoRelativePath.toString().replace('\\', '/');

				ObjectId treeId = repository.resolve(releaseTag);
				if (treeId == null) {
					System.err.println("Unable to find tag " + releaseTag + " in repo " + workDir);
					return null;
				}
				try (RevWalk revWalk = new RevWalk(repository)) {
					RevCommit commit = revWalk.parseCommit(treeId);
					System.out.println(
							"Checking repo " + workDir + " at commit " + commit.getName() + " : '"
									+ commit.getShortMessage() + "'");
					TreeWalk treeWalk = TreeWalk.forPath(repository, pathInGit, commit.getTree());
					ObjectId blobId = treeWalk.getObjectId(0);
					ObjectLoader objectLoader = loadObject(blobId, repository);
					byte[] bytes = objectLoader.getBytes();
					return new String(bytes, StandardCharsets.UTF_8);
				}
			}
		} catch (IllegalStateException | RevisionSyntaxException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static ObjectLoader loadObject(ObjectId objectId, Repository repository) throws IOException {
		try (ObjectReader objectReader = repository.newObjectReader()) {
			return objectReader.open(objectId);
		}
	}
}
