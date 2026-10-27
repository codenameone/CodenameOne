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
package com.codenameone.examples.reel.moderation;

import java.util.List;

/// A content classifier: a second opinion on a title's text before a person
/// reviews it, such as a hosted moderation API.
///
/// Reel ships none, because every such service needs an account; the default
/// answers nothing and the keyword blocklist and the frame hash blocklist are
/// the automatic checks. A deployment that has one supplies a bean of this
/// type, and its signals are shown to the moderator with the item and raise its
/// place in the queue. A classifier never publishes or rejects by itself.
public interface ContentClassifier {
    /// Signals for a title's text, such as `violence:0.82`; empty when clean.
    List<String> classify(String name, String synopsis, List<String> tags);
}
