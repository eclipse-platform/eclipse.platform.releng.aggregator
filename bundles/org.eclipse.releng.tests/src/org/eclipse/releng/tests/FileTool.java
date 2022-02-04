/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * A tool for performing operations on files.
 */
public class FileTool {

	/**
	 * A zip filter which is used to filter out unwanted entries while extracting a zip file.
	 *
	 * @see FileTool#unzip
	 */
	public interface IZipFilter {
		/**
		 * Returns a boolean indicating whether the entry with the
		 * specified name should be extracted from the zip file.
		 *
		 * @param fullEntryName the full entry name; includes full
		 * path segments for nested zip entries
		 * @param entryName the partial entry name; only includes
		 * path segments from the currect zip entry
		 * @param depth a number greater than or equal to zero
		 * which specifies the depth of the current nested zip
		 * entry
		 * @return a boolean indicating whether the entry with the
		 * specified name should be extracted from the zip file
		 */
		boolean shouldExtract(String fullEntryName, String entryName, int depth);
		/**
		 * Returns a boolean indicating whether the entry (which
		 * is a zip/jar file) with the specified name should be
		 * extracted from the zip file and then unzipped.
		 *
		 * @param fullEntryName the full entry name; includes full
		 * path segments for nested zip entries
		 * @param entryName the partial entry name; only includes
		 * path segments from the currect zip entry
		 * @param depth a number greater than or equal to zero
		 * which specifies the depth of the current nested zip
		 * entry
		 * @return a boolean indicating whether the entry (which
		 * is a zip/jar file) with the specified name should be
		 * extracted from the zip file and then unzipped
		 */
		boolean shouldUnzip(String fullEntryName, String entryName, int depth);
	}
	/**
	 * Returns the given file path with its separator
	 * character changed from the given old separator to the
	 * given new separator.
	 *
	 * @param path a file path
	 * @param oldSeparator a path separator character
	 * @param newSeparator a path separator character
	 * @return the file path with its separator character
	 * changed from the given old separator to the given new
	 * separator
	 */
	public static String changeSeparator(String path, char oldSeparator, char newSeparator){
		return path.replace(oldSeparator, newSeparator);
	}
	/**
	 * Unzips the given zip file to the given destination directory
	 * extracting only those entries the pass through the given
	 * filter.
	 *
	 * @param filter filters out unwanted zip entries
	 * @param zipFile the zip file to unzip
	 * @param dstDir the destination directory
	 */
	public static void unzip(IZipFilter filter, ZipFile zipFile, File dstDir) throws IOException {
		unzip(filter, zipFile, dstDir, dstDir, 0);
	}

	private static void unzip(IZipFilter filter, ZipFile zipFile, File rootDstDir, File dstDir, int depth) throws IOException {

		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		try (zipFile) {
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.isDirectory()){
					continue;
				}
				String entryName = entry.getName();
				File file = new File(dstDir, FileTool.changeSeparator(entryName, '/', File.separatorChar));
				String destCanPath = dstDir.getCanonicalPath();
				String fileCanPath = file.getCanonicalPath();
				if (!fileCanPath.startsWith(destCanPath + File.separatorChar)) {
					throw new IOException("Entry is out side of target dir: " + entryName);
				}
				String fullEntryName = FileTool.changeSeparator(file.toString().substring(rootDstDir.toString().length() + 1), File.separatorChar, '/');
				if(!(filter == null || filter.shouldExtract(fullEntryName, entryName, depth))){
					continue;
				}
				file.getParentFile().mkdirs();
				try (InputStream src = zipFile.getInputStream(entry);
					OutputStream dst = new FileOutputStream(file)){
					src.transferTo(dst);
				}
				if((entryName.endsWith(".zip") || entryName.endsWith(".jar")) && (filter == null || filter.shouldUnzip(fullEntryName, entryName, depth))) {
					String fileName = file.getName();
					String dirName = fileName.substring(0, fileName.length() - 4) + "_" + fileName.substring(fileName.length() - 3);
					ZipFile innerZipFile = null;
					try {
						innerZipFile = new ZipFile(file);
						File innerDstDir = new File(file.getParentFile(), dirName);
						unzip(filter, innerZipFile, rootDstDir, innerDstDir, depth + 1);
						file.delete();
					} catch (IOException e) {
						if(innerZipFile != null){
							try {
								innerZipFile.close();
								System.out.println("Could not unzip: " + fileName + ". InnerZip = " + innerZipFile.getName() + ". Lenght: " + innerZipFile.getName().length());
							} catch(IOException e2){
							}
						} else
							System.out.println("Could not unzip: " + fileName + ". InnerZip = <null>");
						e.printStackTrace();
					}

				}
			}
		}
	}
	/**
	 * Unzips the inner zip files in the given destination directory
	 * extracting only those entries the pass through the given
	 * filter.
	 *
	 * @param filter filters out unwanted zip entries
	 * @param dstDir the destination directory
	 */
	public static void unzip(IZipFilter filter, File dstDir) {
		unzip(filter, dstDir, dstDir, 0);
	}

	private static void unzip(IZipFilter filter, File rootDstDir, File dstDir, int depth) {

		File[] entries = rootDstDir.listFiles();
		if (entries == null) {
			entries = new File[0];
		}
		for (File entry : entries) {
			if (entry.isDirectory()) {
				unzip(filter, entry, dstDir, depth);
			}
			String entryName = entry.getName();
			File file = new File(dstDir, FileTool.changeSeparator(entryName, '/', File.separatorChar));
			if (entryName.endsWith(".zip") || entryName.endsWith(".jar")) {
				String fileName = file.getName();
				String dirName = fileName.substring(0, fileName.length() - 4) + "_"
						+ fileName.substring(fileName.length() - 3);
				ZipFile innerZipFile = null;
				try {
					innerZipFile = new ZipFile(entry);
					File innerDstDir = new File(entry.getParentFile(), dirName);
					unzip(filter, innerZipFile, rootDstDir, innerDstDir, depth + 1);
					// entry.delete();
				} catch (IOException e) {
					if (innerZipFile != null) {
						try {
							innerZipFile.close();
							System.out.println("Could not unzip: " + fileName + ". InnerZip = " + innerZipFile.getName()
									+ ". Lenght: " + innerZipFile.getName().length());
						} catch (IOException e2) {
						}
					} else
						System.out.println("Could not unzip: " + fileName + ". InnerZip = <null>");
					e.printStackTrace();
				}

			}
		}
	}
}
