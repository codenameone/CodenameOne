/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel;

/// The values of the catalog's status and kind columns, in one place so the
/// studio, the worker, moderation and playback agree on them.
public final class Kinds {
    public static final String MOVIE = "MOVIE";
    public static final String SERIES = "SERIES";
    public static final String COURSE = "COURSE";

    /// A title the creator is still editing; nobody else sees it.
    public static final String DRAFT = "DRAFT";
    /// Submitted, waiting for a moderator.
    public static final String IN_REVIEW = "IN_REVIEW";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String REJECTED = "REJECTED";
    /// Taken down after publication (a report upheld, or by an admin).
    public static final String REMOVED = "REMOVED";

    public static final String FEATURE = "FEATURE";
    public static final String EPISODE = "EPISODE";
    public static final String LESSON = "LESSON";
    public static final String TRAILER = "TRAILER";
    public static final String AD = "AD";

    public static final String UPLOADING = "UPLOADING";
    public static final String QUEUED = "QUEUED";
    public static final String PROCESSING = "PROCESSING";
    public static final String READY = "READY";
    public static final String FAILED = "FAILED";
    /// The worker's frame hashes matched the blocklist.
    public static final String BLOCKED = "BLOCKED";

    public static final String OPEN = "OPEN";
    public static final String UPHELD = "UPHELD";
    public static final String DISMISSED = "DISMISSED";

    private Kinds() {
    }
}
