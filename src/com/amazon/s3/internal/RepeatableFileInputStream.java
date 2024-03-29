package com.amazon.s3.internal;

/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Portions copyright 2006-2009 James Murty. Please see LICENSE.txt
 * for applicable license terms and NOTICE.txt for applicable notices.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

/**
 * A repeatable input stream for files. This input stream can be repeated an
 * unlimited number of times, without any limitation on when a repeat can occur.
 */
public class RepeatableFileInputStream extends InputStream {
	private static final String TAG = "###RepeatableFileInputStream###";

	private final File file;
	private FileInputStream fis = null;
	private long bytesReadPastMarkPoint = 0;
	private long markPoint = 0;

	/**
	 * Creates a repeatable input stream based on a file.
	 * 
	 * @param file
	 *            The file from which this input stream reads data.
	 * 
	 * @throws FileNotFoundException
	 *             If the specified file doesn't exist, or can't be opened.
	 */
	public RepeatableFileInputStream(File file) throws FileNotFoundException {
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null");
		}
		this.fis = new FileInputStream(file);
		this.file = file;
	}

	/**
	 * Returns the File this stream is reading data from.
	 * 
	 * @return the File this stream is reading data from.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Resets the input stream to the last mark point, or the beginning of the
	 * stream if there is no mark point, by creating a new FileInputStream based
	 * on the underlying file.
	 * 
	 * @throws IOException
	 *             when the FileInputStream cannot be re-created.
	 */
	public void reset() throws IOException {
		this.fis.close();
		this.fis = new FileInputStream(file);

		long skipped = 0;
		long toSkip = markPoint;
		while (toSkip > 0) {
			skipped = this.fis.skip(toSkip);
			toSkip -= skipped;
		}

		Log.d(TAG, "Reset to mark point " + markPoint + " after returning "
				+ bytesReadPastMarkPoint + " bytes");

		this.bytesReadPastMarkPoint = 0;
	}

	/**
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		return true;
	}

	/**
	 * @see java.io.InputStream#mark(int)
	 */
	public void mark(int readlimit) {
		this.markPoint += bytesReadPastMarkPoint;
		this.bytesReadPastMarkPoint = 0;

		Log.d(TAG, "Input stream marked at " + this.markPoint + " bytes");

	}

	/**
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		return fis.available();
	}

	/**
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		fis.close();
	}

	/**
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		int byteRead = fis.read();
		if (byteRead != -1) {
			bytesReadPastMarkPoint++;
			return byteRead;
		} else {
			return -1;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = fis.skip(n);
		bytesReadPastMarkPoint += skipped;
		return skipped;
	}

	/**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		int count = fis.read(arg0, arg1, arg2);
		bytesReadPastMarkPoint += count;
		return count;
	}

	public InputStream getWrappedInputStream() {
		return this.fis;
	}

}
