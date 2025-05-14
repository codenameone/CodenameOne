package com.codename1.builders;

public class BuildException extends Exception {
    public BuildException(String message) {
        super(message);
    }

    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
