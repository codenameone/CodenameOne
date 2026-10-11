/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

/** Reports invalid compressed data. */
public class DataFormatException extends Exception {
    public DataFormatException() {
    }
    public DataFormatException(String message) {
        super(message);
    }
}
