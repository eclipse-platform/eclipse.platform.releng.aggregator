/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov <loskutov@gmx.de> and others.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TouchBundles {

	final static String FQU_FILE = "forceQualifierUpdate.txt";
	static String ticketMessage;

	/**
	 * Tries to map & touch all bundles that are mentioned in artifactcomparisons.
	 *
	 * @param args first is path to the extracted artifactcomparisons.zip, second
	 *             path to the root directory with all repositories, third one is
	 *             the ticket message
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Arguments: first is path to the artifactcomparisons.zip, \n"
					+ "second is the path to the root directory with all repositories, \n"
					+ "third is the ticket message");
			System.exit(1);
		}
		Path artifactsPath = Paths.get(args[0]);
		File[] badDirs = artifactsPath.toFile().listFiles();
		if (badDirs == null) {
			System.out.println("No files found in " + artifactsPath);
			System.exit(1);
		}

		Path rootReposDir = Paths.get(args[1]);
		File[] gitDirs = rootReposDir.toFile().listFiles();
		if (gitDirs == null) {
			System.out.println("No files found in " + rootReposDir);
			System.exit(1);
		}
		ticketMessage = args[2].strip();
		if (ticketMessage.isBlank()) {
			ticketMessage = "Touching " + FQU_FILE + " to force bundle rebuild\n";
		}

		Map<String, File> badDirMap = Arrays.asList(badDirs).stream().filter(File::isDirectory)
				.collect(Collectors.toMap(File::getName, f -> f));

		Map<String, File> gitDirMap = Arrays.asList(gitDirs).stream().filter(File::isDirectory)
				.collect(Collectors.toMap(File::getName, f -> f));

		if (gitDirMap.containsKey("eclipse.pde")) {
			gitDirMap.put("eclipse.pde.ui", gitDirMap.get("eclipse.pde"));
		}
		if (gitDirMap.containsKey("eclipse.platform.releng.aggregator")) {
			gitDirMap.put("eclipse.platform.releng",
					new File(gitDirMap.get("eclipse.platform.releng.aggregator"), "eclipse.platform.releng"));
		}

		for (Entry<String, File> entry : badDirMap.entrySet()) {
			String dirName = entry.getKey();
			File badDir = entry.getValue();
			File repoDir = gitDirMap.get(dirName);
			if (repoDir == null) {
				System.err.println("Repo " + dirName + " not found in " + rootReposDir);
				continue;
			}

			if (repoDir.isDirectory()) {
				updateRepo(badDir, repoDir);
			} else {
				System.err.println("Repo " + repoDir + " is not a directory in " + rootReposDir);
			}
		}
	}

	private static void updateRepo(File badDir, File repoDir) {
		System.out.println("Checking " + badDir + " -> " + repoDir);
		File[] dirs = badDir.listFiles();
		if (dirs == null) {
			System.err.println("No children at " + badDir);
			return;
		}
		for (File dir : dirs) {
			File gitDir = new File(repoDir, dir.getName());
			if (isBundleWithChanges(dir)) {
				updateFQU(gitDir);
			} else {
				updateRepo(new File(badDir, dir.getName()), gitDir);
			}
		}
	}

	private static void updateFQU(File dir) {
		if (!dir.isDirectory()) {
			System.err.println("\tCan't update non existing directory " + dir);
			return;
		}
		System.out.println("\tUpdating " + dir);
		File fquFile = new File(dir, FQU_FILE);
		try {
			boolean created;
			if (!fquFile.exists()) {
				created = true;
				Files.createFile(fquFile.toPath());
			} else {
				created = false;
			}
			Path path = fquFile.toPath();
			String content = Files.readString(path);
			if (content.endsWith(ticketMessage)) {
				// already updated
				System.out.println("\t\tAlready updated: " + fquFile);
				return;
			}
			if (created) {
				System.out.println("\t\tWill create new file: " + fquFile);
			} else {
				System.out.println("\t\tWill update file: " + fquFile);
			}
			if (content.endsWith("\n")) {
				Files.write(path, ticketMessage.getBytes(), StandardOpenOption.APPEND);
			} else {
				Files.write(path, ("\n" + ticketMessage).getBytes(), StandardOpenOption.APPEND);
			}
		} catch (Exception e) {
			System.err.println("Failed to update file " + fquFile);
		}
	}

	private static boolean isBundleWithChanges(File dir) {
		return dir.isDirectory() && new File(dir, "target").isDirectory();
	}


}
