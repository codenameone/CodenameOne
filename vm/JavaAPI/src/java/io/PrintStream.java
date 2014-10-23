/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.io;

import java.util.Arrays;

/**
 * A PrintStream adds functionality to another output stream, namely the ability to print representations of various data values conveniently. Two other features are provided as well. Unlike other output streams, a PrintStream never throws an IOException; instead, exceptional situations merely set an internal flag that can be tested via the checkError method.
 * All characters printed by a PrintStream are converted into bytes using the platform's default character encoding.
 * Since: JDK1.0, CLDC 1.0
 */
public class PrintStream extends FilterOutputStream {
    /**
     * indicates whether or not this PrintStream has incurred an error.
     */
    private boolean ioError;

    /**
     * indicates whether or not this PrintStream should flush its contents after
     * printing a new line.
     */
    private boolean autoFlush;

    private String encoding;

    /**
     * Constructs a new {@code PrintStream} with {@code out} as its target
     * stream. By default, the new print stream does not automatically flush its
     * contents to the target stream when a newline is encountered.
     *
     * @param out
     *            the target output stream.
     * @throws NullPointerException
     *             if {@code out} is {@code null}.
     */
    public PrintStream(OutputStream out) {
        super(out);
        if (out == null) {
            throw new NullPointerException("out == null");
        }
    }

    /**
     * Constructs a new {@code PrintStream} with {@code out} as its target
     * stream. The parameter {@code autoFlush} determines if the print stream
     * automatically flushes its contents to the target stream when a newline is
     * encountered.
     *
     * @param out
     *            the target output stream.
     * @param autoFlush
     *            indicates whether to flush contents upon encountering a
     *            newline sequence.
     * @throws NullPointerException
     *             if {@code out} is {@code null}.
     */
    public PrintStream(OutputStream out, boolean autoFlush) {
        super(out);
        if (out == null) {
            throw new NullPointerException("out == null");
        }
        this.autoFlush = autoFlush;
    }

    /**
     * Constructs a new {@code PrintStream} with {@code out} as its target
     * stream and using the character encoding {@code charsetName} while writing. The
     * parameter {@code autoFlush} determines if the print stream automatically
     * flushes its contents to the target stream when a newline is encountered.
     *
     * @param out
     *            the target output stream.
     * @param autoFlush
     *            indicates whether or not to flush contents upon encountering a
     *            newline sequence.
     * @param charsetName
     *            the non-null string describing the desired character encoding.
     * @throws NullPointerException
     *             if {@code out} or {@code charsetName} are {@code null}.
     * @throws UnsupportedEncodingException
     *             if the encoding specified by {@code charsetName} is not supported.
     */
    public PrintStream(OutputStream out, boolean autoFlush, String charsetName)
            throws UnsupportedEncodingException {
        super(out);
        if (out == null) {
            throw new NullPointerException("out == null");
        } else if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        this.autoFlush = autoFlush;
        /*try {
            if (!Charset.isSupported(charsetName)) {
                throw new UnsupportedEncodingException(charsetName);
            }
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(charsetName);
        }*/
        encoding = charsetName;
    }

    /**
     * Constructs a new {@code PrintStream} with {@code file} as its target. The
     * VM's default character set is used for character encoding.
     *
     * @param file
     *            the target file. If the file already exists, its contents are
     *            removed, otherwise a new file is created.
     * @throws FileNotFoundException
     *             if an error occurs while opening or creating the target file.
     */
    /*public PrintStream(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
    }*/

    /**
     * Constructs a new {@code PrintStream} with {@code file} as its target. The
     * character set named {@code charsetName} is used for character encoding.
     *
     * @param file
     *            the target file. If the file already exists, its contents are
     *            removed, otherwise a new file is created.
     * @param charsetName
     *            the name of the character set used for character encoding.
     * @throws FileNotFoundException
     *             if an error occurs while opening or creating the target file.
     * @throws NullPointerException
     *             if {@code charsetName} is {@code null}.
     * @throws UnsupportedEncodingException
     *             if the encoding specified by {@code charsetName} is not supported.
     */
    /*public PrintStream(File file, String charsetName) throws FileNotFoundException,
            UnsupportedEncodingException {
        super(new FileOutputStream(file));
        if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        encoding = charsetName;
    }*/

    /**
     * Constructs a new {@code PrintStream} with the file identified by
     * {@code fileName} as its target. The VM's default character
     * set is used for character encoding.
     *
     * @param fileName
     *            the target file's name. If the file already exists, its
     *            contents are removed, otherwise a new file is created.
     * @throws FileNotFoundException
     *             if an error occurs while opening or creating the target file.
     */
    /*public PrintStream(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }*/

    /**
     * Constructs a new {@code PrintStream} with the file identified by
     * {@code fileName} as its target. The character set named {@code charsetName} is
     * used for character encoding.
     *
     * @param fileName
     *            the target file's name. If the file already exists, its
     *            contents are removed, otherwise a new file is created.
     * @param charsetName
     *            the name of the character set used for character encoding.
     * @throws FileNotFoundException
     *             if an error occurs while opening or creating the target file.
     * @throws NullPointerException
     *             if {@code charsetName} is {@code null}.
     * @throws UnsupportedEncodingException
     *             if the encoding specified by {@code charsetName} is not supported.
     */
    /*public PrintStream(String fileName, String charsetName)
            throws FileNotFoundException, UnsupportedEncodingException {
        this(new File(fileName), charsetName);
    }*/

    /**
     * Flushes this stream and returns the value of the error flag.
     *
     * @return {@code true} if either an {@code IOException} has been thrown
     *         previously or if {@code setError()} has been called;
     *         {@code false} otherwise.
     * @see #setError()
     */
    public boolean checkError() {
        OutputStream delegate = out;
        if (delegate == null) {
            return ioError;
        }

        flush();
        return ioError;// || delegate.checkError();
    }

    /**
     * Sets the error state of the stream to false.
     * @since 1.6
     */
    protected void clearError() {
        ioError = false;
    }

    /**
     * Closes this print stream. Flushes this stream and then closes the target
     * stream. If an I/O error occurs, this stream's error state is set to
     * {@code true}.
     */
    @Override
    public synchronized void close() {
        flush();
        if (out != null) {
            try {
                out.close();
                out = null;
            } catch (IOException e) {
                setError();
            }
        }
    }

    /**
     * Ensures that all pending data is sent out to the target stream. It also
     * flushes the target stream. If an I/O error occurs, this stream's error
     * state is set to {@code true}.
     */
    @Override
    public synchronized void flush() {
        if (out != null) {
            try {
                out.flush();
                return;
            } catch (IOException e) {
                // Ignored, fall through to setError
            }
        }
        setError();
    }

    /**
     * Put the line separator String onto the print stream.
     */
    private void newline() {
        print("\n");
    }

    /**
     * Prints the string representation of the character array {@code chars}.
     */
    public void print(char[] chars) {
        print(new String(chars, 0, chars.length));
    }

    /**
     * Prints the string representation of the char {@code c}.
     */
    public void print(char c) {
        print(String.valueOf(c));
    }

    /**
     * Prints the string representation of the double {@code d}.
     */
    public void print(double d) {
        print(String.valueOf(d));
    }

    /**
     * Prints the string representation of the float {@code f}.
     */
    public void print(float f) {
        print(String.valueOf(f));
    }

    /**
     * Prints the string representation of the int {@code i}.
     */
    public void print(int i) {
        print(String.valueOf(i));
    }

    /**
     * Prints the string representation of the long {@code l}.
     */
    public void print(long l) {
        print(String.valueOf(l));
    }

    /**
     * Prints the string representation of the Object {@code o}, or {@code "null"}.
     */
    public void print(Object o) {
        print(String.valueOf(o));
    }

    /**
     * Prints a string to the target stream. The string is converted to an array
     * of bytes using the encoding chosen during the construction of this
     * stream. The bytes are then written to the target stream with
     * {@code write(int)}.
     *
     * <p>If an I/O error occurs, this stream's error state is set to {@code true}.
     *
     * @param str
     *            the string to print to the target stream.
     * @see #write(int)
     */
    public synchronized void print(String str) {
        if (out == null) {
            setError();
            return;
        }
        if (str == null) {
            print("null");
            return;
        }

        try {
            if (encoding == null) {
                write(str.getBytes());
            } else {
                write(str.getBytes(encoding));
            }
        } catch (IOException e) {
            setError();
        }
    }

    /**
     * Prints the string representation of the boolean {@code b}.
     */
    public void print(boolean b) {
        print(String.valueOf(b));
    }

    /**
     * Prints a newline.
     */
    public void println() {
        newline();
    }

    /**
     * Prints the string representation of the character array {@code chars} followed by a newline.
     */
    public void println(char[] chars) {
        println(new String(chars, 0, chars.length));
    }

    /**
     * Prints the string representation of the char {@code c} followed by a newline.
     */
    public void println(char c) {
        println(String.valueOf(c));
    }

    /**
     * Prints the string representation of the double {@code d} followed by a newline.
     */
    public void println(double d) {
        println(String.valueOf(d));
    }

    /**
     * Prints the string representation of the float {@code f} followed by a newline.
     */
   public void println(float f) {
        println(String.valueOf(f));
    }

   /**
     * Prints the string representation of the int {@code i} followed by a newline.
     */
    public void println(int i) {
        println(String.valueOf(i));
    }

    /**
     * Prints the string representation of the long {@code l} followed by a newline.
     */
    public void println(long l) {
        println(String.valueOf(l));
    }

    /**
     * Prints the string representation of the Object {@code o}, or {@code "null"},
     * followed by a newline.
     */
    public void println(Object o) {
        println(String.valueOf(o));
    }

    /**
     * Prints a string followed by a newline. The string is converted to an array of bytes using
     * the encoding chosen during the construction of this stream. The bytes are
     * then written to the target stream with {@code write(int)}.
     *
     * <p>If an I/O error occurs, this stream's error state is set to {@code true}.
     *
     * @param str
     *            the string to print to the target stream.
     * @see #write(int)
     */
    public synchronized void println(String str) {
        print(str);
        newline();
    }

    /**
     * Prints the string representation of the boolean {@code b} followed by a newline.
     */
    public void println(boolean b) {
        println(String.valueOf(b));
    }

    /**
     * Sets the error flag of this print stream to true.
     */
    protected void setError() {
        ioError = true;
    }

    /**
     * Writes {@code count} bytes from {@code buffer} starting at {@code offset}
     * to the target stream. If autoFlush is set, this stream gets flushed after
     * writing the buffer.
     *
     * <p>This stream's error flag is set to {@code true} if this stream is closed
     * or an I/O error occurs.
     *
     * @param buffer
     *            the buffer to be written.
     * @param offset
     *            the index of the first byte in {@code buffer} to write.
     * @param length
     *            the number of bytes in {@code buffer} to write.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code count < 0}, or if {@code
     *             offset + count} is bigger than the length of {@code buffer}.
     * @see #flush()
     */
    @Override
    public void write(byte[] buffer, int offset, int length) {
        //Arrays.checkOffsetAndCount(buffer.length, offset, length);
        synchronized (this) {
            if (out == null) {
                setError();
                return;
            }
            try {
                out.write(buffer, offset, length);
                if (autoFlush) {
                    flush();
                }
            } catch (IOException e) {
                setError();
            }
        }
    }

    /**
     * Writes one byte to the target stream. Only the least significant byte of
     * the integer {@code oneByte} is written. This stream is flushed if
     * {@code oneByte} is equal to the character {@code '\n'} and this stream is
     * set to autoFlush.
     * <p>
     * This stream's error flag is set to {@code true} if it is closed or an I/O
     * error occurs.
     *
     * @param oneByte
     *            the byte to be written
     */
    @Override
    public synchronized void write(int oneByte) {
        if (out == null) {
            setError();
            return;
        }
        try {
            out.write(oneByte);
            int b = oneByte & 0xFF;
            // 0x0A is ASCII newline, 0x15 is EBCDIC newline.
            boolean isNewline = b == 0x0A || b == 0x15;
            if (autoFlush && isNewline) {
                flush();
            }
        } catch (IOException e) {
            setError();
        }
    }

    /**
     * Appends the char {@code c}.
     * @return this stream.
     */
    public PrintStream append(char c) {
        print(c);
        return this;
    }

    /**
     * Appends the CharSequence {@code charSequence}, or {@code "null"}.
     * @return this stream.
     */
    public PrintStream append(CharSequence charSequence) {
        if (charSequence == null) {
            print("null");
        } else {
            print(charSequence.toString());
        }
        return this;
    }

    /**
     * Appends a subsequence of CharSequence {@code charSequence}, or {@code "null"}.
     *
     * @param charSequence
     *            the character sequence appended to the target stream.
     * @param start
     *            the index of the first char in the character sequence appended
     *            to the target stream.
     * @param end
     *            the index of the character following the last character of the
     *            subsequence appended to the target stream.
     * @return this stream.
     * @throws IndexOutOfBoundsException
     *             if {@code start > end}, {@code start < 0}, {@code end < 0} or
     *             either {@code start} or {@code end} are greater or equal than
     *             the length of {@code charSequence}.
     */
    /*public PrintStream append(CharSequence charSequence, int start, int end) {
        if (charSequence == null) {
            charSequence = "null";
        }
        print(charSequence.subSequence(start, end).toString());
        return this;
    }*/
}
