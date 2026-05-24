/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.annotations;

/// Thrown by an `AnnotationProcessor` when it encounters a non-recoverable
/// error. Halts the build via `MojoFailureException` from the orchestrator.
///
/// Recoverable errors (e.g. one malformed annotation among many) should be
/// reported through `ProcessorContext#error` so processing can continue and
/// surface every issue in a single build run.
public class ProcessingException extends Exception {

    private static final long serialVersionUID = 1L;

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
