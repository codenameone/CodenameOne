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
package com.codenameone.examples.bastion.security;

import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;

import java.util.HashMap;
import java.util.Map;

/// A secure store in memory, for the tests: the quiet tier and the prompting
/// (biometric) tier as two maps, and the prompting tier's read answering
/// `promptedReadError` when one is set.
final class MemoryStorage extends SecureStorage {
    final Map<String, String> quiet = new HashMap<>();
    final Map<String, String> prompted = new HashMap<>();
    Throwable promptedReadError;

    @Override
    public boolean set(String account, String value) {
        quiet.put(account, value);
        return true;
    }

    @Override
    public String get(String account) {
        return quiet.get(account);
    }

    @Override
    public boolean remove(String account) {
        return quiet.remove(account) != null;
    }

    @Override
    public AsyncResource<String> get(String reason, String account) {
        AsyncResource<String> out = new AsyncResource<>();
        if (promptedReadError != null) {
            out.error(promptedReadError);
        } else {
            out.complete(prompted.get(account));
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> set(String reason, String account, String value) {
        prompted.put(account, value);
        AsyncResource<Boolean> out = new AsyncResource<>();
        out.complete(Boolean.TRUE);
        return out;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> out = new AsyncResource<>();
        out.complete(Boolean.valueOf(prompted.remove(account) != null));
        return out;
    }
}
