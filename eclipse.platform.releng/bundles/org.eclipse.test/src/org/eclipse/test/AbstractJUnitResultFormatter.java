/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestExecutionContext;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestResultFormatter;
import org.apache.tools.ant.util.FileUtils;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Contains some common behaviour that's used by our internal
 * {@link TestResultFormatter}s
 */
abstract class AbstractJUnitResultFormatter implements TestResultFormatter {

	protected static String NEW_LINE = System.lineSeparator();
	protected TestExecutionContext context;

	private SysOutErrContentStore sysOutStore;
	private SysOutErrContentStore sysErrStore;

	@Override
	public void sysOutAvailable(byte[] data) {
		if (this.sysOutStore == null) {
			this.sysOutStore = new SysOutErrContentStore(true);
		}
		try {
			this.sysOutStore.store(data);
		} catch (IOException e) {
			handleException(e);
			return;
		}
	}

	@Override
	public void sysErrAvailable(byte[] data) {
		if (this.sysErrStore == null) {
			this.sysErrStore = new SysOutErrContentStore(false);
		}
		try {
			this.sysErrStore.store(data);
		} catch (IOException e) {
			handleException(e);
			return;
		}
	}

	@Override
	public void setContext(TestExecutionContext context) {
		this.context = context;
	}

	/**
	 * @return Returns true if there's any stdout data, that was generated during
	 *         the tests, is available for use. Else returns false.
	 */
	boolean hasSysOut() {
		return this.sysOutStore != null && this.sysOutStore.hasData();
	}

	/**
	 * @return Returns true if there's any stderr data, that was generated during
	 *         the tests, is available for use. Else returns false.
	 */
	boolean hasSysErr() {
		return this.sysErrStore != null && this.sysErrStore.hasData();
	}

	/**
	 * @return Returns a {@link Reader} for reading any stdout data that was
	 *         generated during the test execution. It is expected that the
	 *         {@link #hasSysOut()} be first called to see if any such data is
	 *         available and only if there is, then this method be called
	 * @throws IOException If there's any I/O problem while creating the
	 *                     {@link Reader}
	 */
	Reader getSysOutReader() throws IOException {
		return this.sysOutStore.getReader();
	}

	/**
	 * @return Returns a {@link Reader} for reading any stderr data that was
	 *         generated during the test execution. It is expected that the
	 *         {@link #hasSysErr()} be first called to see if any such data is
	 *         available and only if there is, then this method be called
	 * @throws IOException If there's any I/O problem while creating the
	 *                     {@link Reader}
	 */
	Reader getSysErrReader() throws IOException {
		return this.sysErrStore.getReader();
	}

	/**
	 * Writes out any stdout data that was generated during the test execution. If
	 * there was no such data then this method just returns.
	 *
	 * @param writer The {@link Writer} to use. Cannot be null.
	 * @throws IOException If any I/O problem occurs during writing the data
	 */
	void writeSysOut(Writer writer) throws IOException {
		@SuppressWarnings("resource") // requireNonNull just returns first argument
		Writer w = Objects.requireNonNull(writer, "Writer cannot be null");
		writeFrom(this.sysOutStore, w);
	}

	/**
	 * Writes out any stderr data that was generated during the test execution. If
	 * there was no such data then this method just returns.
	 *
	 * @param writer The {@link Writer} to use. Cannot be null.
	 * @throws IOException If any I/O problem occurs during writing the data
	 */
	void writeSysErr(Writer writer) throws IOException {
		@SuppressWarnings("resource") // requireNonNull just returns first argument
		Writer w = Objects.requireNonNull(writer, "Writer cannot be null");
		writeFrom(this.sysErrStore, w);
	}

	static Optional<TestIdentifier> traverseAndFindTestClass(TestPlan testPlan, TestIdentifier testIdentifier) {
		if (isTestClass(testIdentifier).isPresent()) {
			return Optional.of(testIdentifier);
		}
		Optional<TestIdentifier> parent = testPlan.getParent(testIdentifier);
		return parent.isPresent() ? traverseAndFindTestClass(testPlan, parent.get()) : Optional.empty();
	}

	static Optional<ClassSource> isTestClass(TestIdentifier testIdentifier) {
		if (testIdentifier == null) {
			return Optional.empty();
		}
		Optional<TestSource> source = testIdentifier.getSource();
		if (!source.isPresent()) {
			return Optional.empty();
		}
		TestSource testSource = source.get();
		if (testSource instanceof ClassSource) {
			return Optional.of((ClassSource) testSource);
		}
		return Optional.empty();
	}

	private void writeFrom(SysOutErrContentStore store, Writer writer) throws IOException {
		char[] chars = new char[1024];
		try (Reader reader = store.getReader()) {
			for (int numRead = -1; (numRead = reader.read(chars)) != -1;) {
				writer.write(chars, 0, numRead);
			}
		}
	}

	@Override
	public void close() throws IOException {
		FileUtils.close(this.sysOutStore);
		FileUtils.close(this.sysErrStore);
	}

	protected void handleException(Throwable t) {
		// we currently just log it and move on.
		this.context.getProject()
		.ifPresent(p -> p.log("Exception in listener " + AbstractJUnitResultFormatter.this.getClass().getName(),
				t, Project.MSG_DEBUG));
	}

	/*
	 * A "store" for sysout/syserr content that gets sent to the
	 * AbstractJUnitResultFormatter. This store first uses a relatively decent sized
	 * in-memory buffer for storing the sysout/syserr content. This in-memory buffer
	 * will be used as long as it can fit in the new content that keeps coming in.
	 * When the size limit is reached, this store switches to a file based store by
	 * creating a temporarily file and writing out the already in-memory held buffer
	 * content and any new content that keeps arriving to this store. Once the file
	 * has been created, the in-memory buffer will never be used any more and in
	 * fact is destroyed as soon as the file is created. Instances of this class are
	 * not thread-safe and users of this class are expected to use necessary thread
	 * safety guarantees, if they want to use an instance of this class by multiple
	 * threads.
	 */
	private static final class SysOutErrContentStore implements Closeable {
		private static final int DEFAULT_CAPACITY_IN_BYTES = 50 * 1024; // 50 KB
		private static final Reader EMPTY_READER = new Reader() {
			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				return -1;
			}

			@Override
			public void close() throws IOException {
			}
		};

		private final String tmpFileSuffix;
		private ByteBuffer inMemoryStore = ByteBuffer.allocate(DEFAULT_CAPACITY_IN_BYTES);
		private boolean usingFileStore = false;
		private Path filePath;
		private FileOutputStream fileOutputStream;

		SysOutErrContentStore(boolean isSysOut) {
			this.tmpFileSuffix = isSysOut ? ".sysout" : ".syserr";
		}

		void store(byte[] data) throws IOException {
			if (this.usingFileStore) {
				this.storeToFile(data, 0, data.length);
				return;
			}
			// we haven't yet created a file store and the data can fit in memory,
			// so we write it in our buffer
			try {
				this.inMemoryStore.put(data);
				return;
			} catch (BufferOverflowException boe) {
				// the buffer capacity can't hold this incoming data, so this
				// incoming data hasn't been transferred to the buffer. let's
				// now fall back to a file store
				this.usingFileStore = true;
			}
			// since the content couldn't be transferred into in-memory buffer,
			// we now create a file and transfer already (previously) stored in-memory
			// content into that file, before finally transferring this new content
			// into the file too. We then finally discard this in-memory buffer and
			// just keep using the file store instead
			this.fileOutputStream = createFileStore();
			// first the existing in-memory content
			storeToFile(this.inMemoryStore.array(), 0, this.inMemoryStore.position());
			storeToFile(data, 0, data.length);
			// discard the in-memory store
			this.inMemoryStore = null;
		}

		private void storeToFile(byte[] data, int offset, int length) throws IOException {
			if (this.fileOutputStream == null) {
				// no backing file was created so we can't do anything
				return;
			}
			this.fileOutputStream.write(data, offset, length);
		}

		private FileOutputStream createFileStore() throws IOException {
			this.filePath = Files.createTempFile(null, this.tmpFileSuffix);
			this.filePath.toFile().deleteOnExit();
			return new FileOutputStream(this.filePath.toFile());
		}

		/*
		 * Returns a Reader for reading the sysout/syserr content. If there's no data
		 * available in this store, then this returns a Reader which when used for read
		 * operations, will immediately indicate an EOF.
		 */
		Reader getReader() throws IOException {
			if (this.usingFileStore && this.filePath != null) {
				// we use a FileReader here so that we can use the system default character
				// encoding for reading the contents on sysout/syserr stream, since that's the
				// encoding that System.out/System.err uses to write out the messages
				return new BufferedReader(new FileReader(this.filePath.toFile()));
			}
			if (this.inMemoryStore != null) {
				return new InputStreamReader(
						new ByteArrayInputStream(this.inMemoryStore.array(), 0, this.inMemoryStore.position()));
			}
			// no data to read, so we return an "empty" reader
			return EMPTY_READER;
		}

		/*
		 * Returns true if this store has any data (either in-memory or in a file). Else
		 * returns false.
		 */
		boolean hasData() {
			if (this.inMemoryStore != null && this.inMemoryStore.position() > 0) {
				return true;
			}
			if (this.usingFileStore && this.filePath != null) {
				return true;
			}
			return false;
		}

		@Override
		public void close() throws IOException {
			this.inMemoryStore = null;
			FileUtils.close(this.fileOutputStream);
			FileUtils.delete(this.filePath.toFile());
		}
	}
}
