/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

/** Reports invalid compressed data. */
public class ZipException extends java.io.IOException {
    public ZipException() {
    }
    public ZipException(String message) {
        super(message);
    }
}
