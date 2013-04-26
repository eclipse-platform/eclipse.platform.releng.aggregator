/*
 * Copyright (C) 2009-2010, Google Inc.
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * This class was originally copied from
 * org.eclipse.jgit.junit.LocalDiskRepositoryTest
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=378047
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.releng.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.junit.Assert;

/**
 * This class is a modified version of org.eclipse.jgit.junit.LocalDiskRepositoryTestCase
 *
 */
public class LocalDiskRepositoryTest extends TestCase {
	private static int testCount;
	protected PersonIdent committer;
	private final File trash = new File(new File("target"), "trash");
	private final List toClose = new ArrayList();

	protected void setUp() throws Exception {
		committer = new PersonIdent("J. Committer", "jcommitter@example.com");
	}

	protected void tearDown() throws Exception {
		RepositoryCache.clear();
		for (Iterator it = toClose.iterator(); it.hasNext();) {
			Repository r = (Repository) it.next();
			r.close();
		}
		toClose.clear();
	}

	/**
	 * Creates a new empty repository within a new empty working directory.
	 *
	 * @return the newly created repository, opened for access
	 * @throws IOException
	 *             the repository could not be created in the temporary area
	 */
	protected Repository createWorkRepository() throws IOException {
		return createRepository(false /* not bare */);
	}

	/**
	 * Creates a new empty repository.
	 *
	 * @param bare
	 *            true to create a bare repository; false to make a repository
	 *            within its working directory
	 * @return the newly created repository, opened for access
	 * @throws IOException
	 *             the repository could not be created in the temporary area
	 */
	private Repository createRepository(boolean bare) throws IOException {
		File gitdir = createUniqueTestGitDir(bare);
		Repository db = new FileRepository(gitdir);
		Assert.assertFalse(gitdir.exists());
		db.create();
		toClose.add(db);
		return db;
	}

	/**
	 * Creates a new unique directory for a test repository
	 *
	 * @param bare
	 *            true for a bare repository; false for a repository with a
	 *            working directory
	 * @return a unique directory for a test repository
	 * @throws IOException
	 */
	protected File createUniqueTestGitDir(boolean bare) throws IOException {
		String gitdirName = createUniqueTestFolderPrefix();
		if (!bare)
			gitdirName += "/";
		gitdirName += Constants.DOT_GIT;
		File gitdir = new File(trash, gitdirName);
		return gitdir.getCanonicalFile();
	}

	private String createUniqueTestFolderPrefix() {
		return "test" + (System.currentTimeMillis() + "_" + (testCount++));
	}
}
