/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.ui.util;

import java.io.IOException;
import java.io.InputStream;

/// Deprecated typo-preserving wrapper for {@link MutableResource}.
@Deprecated
public class MutableResouce extends MutableResource {

    public MutableResouce() {
        super();
    }

    MutableResouce(InputStream input) throws IOException {
        super(input);
    }

    public static MutableResouce open(InputStream resource) throws IOException {
        return new MutableResouce(resource);
    }
}
