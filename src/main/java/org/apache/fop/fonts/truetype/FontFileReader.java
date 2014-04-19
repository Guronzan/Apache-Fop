/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: FontFileReader.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Reads a TrueType font file into a byte array and provides file like functions
 * for array access.
 */
public class FontFileReader {

    private int fsize; // file size
    private int current; // current position in file
    private byte[] file;

    /**
     * Initializes class and reads stream. Init does not close stream.
     *
     * @param in
     *            InputStream to read from new array with size + inc
     * @throws IOException
     *             In case of an I/O problem
     */
    private void init(final InputStream in) throws java.io.IOException {
        this.file = IOUtils.toByteArray(in);
        this.fsize = this.file.length;
        this.current = 0;
    }

    /**
     * Constructor
     *
     * @param fileName
     *            filename to read
     * @throws IOException
     *             In case of an I/O problem
     */
    public FontFileReader(final String fileName) throws IOException {
        final File f = new File(fileName);
        final InputStream in = new java.io.FileInputStream(f);
        try {
            init(in);
        } finally {
            in.close();
        }
    }

    /**
     * Constructor
     *
     * @param in
     *            InputStream to read from
     * @throws IOException
     *             In case of an I/O problem
     */
    public FontFileReader(final InputStream in) throws IOException {
        init(in);
    }

    /**
     * Set current file position to offset
     *
     * @param offset
     *            The new offset to set
     * @throws IOException
     *             In case of an I/O problem
     */
    public void seekSet(final long offset) throws IOException {
        if (offset > this.fsize || offset < 0) {
            throw new java.io.EOFException("Reached EOF, file size="
                    + this.fsize + " offset=" + offset);
        }
        this.current = (int) offset;
    }

    /**
     * Set current file position to offset
     *
     * @param add
     *            The number of bytes to advance
     * @throws IOException
     *             In case of an I/O problem
     */
    public void seekAdd(final long add) throws IOException {
        seekSet(this.current + add);
    }

    /**
     * Skip a given number of bytes.
     *
     * @param add
     *            The number of bytes to advance
     * @throws IOException
     *             In case of an I/O problem
     */
    public void skip(final long add) throws IOException {
        seekAdd(add);
    }

    /**
     * Returns current file position.
     *
     * @return int The current position.
     */
    public int getCurrentPos() {
        return this.current;
    }

    /**
     * Returns the size of the file.
     *
     * @return int The filesize
     */
    public int getFileSize() {
        return this.fsize;
    }

    /**
     * Read 1 byte.
     *
     * @return One byte
     * @throws IOException
     *             If EOF is reached
     */
    public byte read() throws IOException {
        if (this.current >= this.fsize) {
            throw new java.io.EOFException("Reached EOF, file size="
                    + this.fsize);
        }

        final byte ret = this.file[this.current++];
        return ret;
    }

    /**
     * Read 1 signed byte.
     *
     * @return One byte
     * @throws IOException
     *             If EOF is reached
     */
    public final byte readTTFByte() throws IOException {
        return read();
    }

    /**
     * Read 1 unsigned byte.
     *
     * @return One unsigned byte
     * @throws IOException
     *             If EOF is reached
     */
    public final int readTTFUByte() throws IOException {
        final byte buf = read();

        if (buf < 0) {
            return 256 + buf;
        } else {
            return buf;
        }
    }

    /**
     * Read 2 bytes signed.
     *
     * @return One signed short
     * @throws IOException
     *             If EOF is reached
     */
    public final short readTTFShort() throws IOException {
        final int ret = (readTTFUByte() << 8) + readTTFUByte();
        final short sret = (short) ret;
        return sret;
    }

    /**
     * Read 2 bytes unsigned.
     *
     * @return One unsigned short
     * @throws IOException
     *             If EOF is reached
     */
    public final int readTTFUShort() throws IOException {
        final int ret = (readTTFUByte() << 8) + readTTFUByte();
        return ret;
    }

    /**
     * Write a USHort at a given position.
     *
     * @param pos
     *            The absolute position to write to
     * @param val
     *            The value to write
     * @throws IOException
     *             If EOF is reached
     */
    public final void writeTTFUShort(final int pos, final int val)
            throws IOException {
        if (pos + 2 > this.fsize) {
            throw new java.io.EOFException("Reached EOF");
        }
        final byte b1 = (byte) (val >> 8 & 0xff);
        final byte b2 = (byte) (val & 0xff);
        this.file[pos] = b1;
        this.file[pos + 1] = b2;
    }

    /**
     * Read 2 bytes signed at position pos without changing current position.
     *
     * @param pos
     *            The absolute position to read from
     * @return One signed short
     * @throws IOException
     *             If EOF is reached
     */
    public final short readTTFShort(final long pos) throws IOException {
        final long cp = getCurrentPos();
        seekSet(pos);
        final short ret = readTTFShort();
        seekSet(cp);
        return ret;
    }

    /**
     * Read 2 bytes unsigned at position pos without changing current position.
     *
     * @param pos
     *            The absolute position to read from
     * @return One unsigned short
     * @throws IOException
     *             If EOF is reached
     */
    public final int readTTFUShort(final long pos) throws IOException {
        final long cp = getCurrentPos();
        seekSet(pos);
        final int ret = readTTFUShort();
        seekSet(cp);
        return ret;
    }

    /**
     * Read 4 bytes.
     *
     * @return One signed integer
     * @throws IOException
     *             If EOF is reached
     */
    public final int readTTFLong() throws IOException {
        long ret = readTTFUByte(); // << 8;
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();

        return (int) ret;
    }

    /**
     * Read 4 bytes.
     *
     * @return One unsigned integer
     * @throws IOException
     *             If EOF is reached
     */
    public final long readTTFULong() throws IOException {
        long ret = readTTFUByte();
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();

        return ret;
    }

    /**
     * Read a NUL terminated ISO-8859-1 string.
     *
     * @return A String
     * @throws IOException
     *             If EOF is reached
     */
    public final String readTTFString() throws IOException {
        int i = this.current;
        while (this.file[i++] != 0) {
            if (i > this.fsize) {
                throw new java.io.EOFException("Reached EOF, file size="
                        + this.fsize);
            }
        }

        final byte[] tmp = new byte[i - this.current];
        System.arraycopy(this.file, this.current, tmp, 0, i - this.current);
        return new String(tmp, "ISO-8859-1");
    }

    /**
     * Read an ISO-8859-1 string of len bytes.
     *
     * @param len
     *            The length of the string to read
     * @return A String
     * @throws IOException
     *             If EOF is reached
     */
    public final String readTTFString(final int len) throws IOException {
        if (len + this.current > this.fsize) {
            throw new java.io.EOFException("Reached EOF, file size="
                    + this.fsize);
        }

        final byte[] tmp = new byte[len];
        System.arraycopy(this.file, this.current, tmp, 0, len);
        this.current += len;
        final String encoding;
        if (tmp.length > 0 && tmp[0] == 0) {
            encoding = "UTF-16BE";
        } else {
            encoding = "ISO-8859-1";
        }
        return new String(tmp, encoding);
    }

    /**
     * Read an ISO-8859-1 string of len bytes.
     *
     * @param len
     *            The length of the string to read
     * @return A String
     * @throws IOException
     *             If EOF is reached
     */
    public final String readTTFString(final int len, final int encodingID)
            throws IOException {
        if (len + this.current > this.fsize) {
            throw new java.io.EOFException("Reached EOF, file size="
                    + this.fsize);
        }

        final byte[] tmp = new byte[len];
        System.arraycopy(this.file, this.current, tmp, 0, len);
        this.current += len;
        final String encoding;
        encoding = "UTF-16BE"; // Use this for all known encoding IDs for now
        return new String(tmp, encoding);
    }

    /**
     * Return a copy of the internal array
     *
     * @param offset
     *            The absolute offset to start reading from
     * @param length
     *            The number of bytes to read
     * @return An array of bytes
     * @throws IOException
     *             if out of bounds
     */
    public byte[] getBytes(final int offset, final int length)
            throws IOException {
        if (offset + length > this.fsize) {
            throw new java.io.IOException("Reached EOF");
        }

        final byte[] ret = new byte[length];
        System.arraycopy(this.file, offset, ret, 0, length);
        return ret;
    }

}