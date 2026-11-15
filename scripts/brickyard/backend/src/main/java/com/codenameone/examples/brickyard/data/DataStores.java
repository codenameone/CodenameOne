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
package com.codenameone.examples.brickyard.data;

import com.codename1.appkit.server.Text;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.DataValueDto;
import com.codenameone.examples.brickyard.domain.DataStoreEntry;
import com.codenameone.examples.brickyard.experience.Experiences;
import com.codenameone.examples.brickyard.repository.DataStoreEntryRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An experience's data stores: named stores, each split into scopes, each a
/// map of keys to JSON values. The game server's `Engine.Host` reaches these
/// through `dataGet`, `dataSet` and `dataList`, which is why the methods take
/// the same three names.
@Component
public class DataStores {
    /// The largest value a key may hold, in UTF-8 bytes.
    public static final int MAX_VALUE_BYTES = 4 * 1024 * 1024;

    private final DataStoreEntryRepository entries;
    private final Experiences experiences;

    public DataStores(DataStoreEntryRepository entries, Experiences experiences) {
        this.entries = entries;
        this.experiences = experiences;
    }

    @Transactional(readOnly = true)
    public DataValueDto get(String experienceId, String store, String scope, String key) {
        guard(experienceId);
        DataStoreEntry d = find(experienceId, store, scope, key);
        if (d == null) {
            throw new ResponseStatusException(404, "No such key");
        }
        return toDto(d);
    }

    @Transactional(rollbackFor = IOException.class)
    public DataValueDto set(String experienceId, String store, String scope, String key, String json)
            throws IOException {
        guard(experienceId);
        check(json);
        return write(experienceId, store, scope, key, json);
    }

    /// Adds `delta` to a numeric value, starting from 0 for a key never written.
    @Transactional(rollbackFor = IOException.class)
    public DataValueDto increment(String experienceId, String store, String scope, String key, long delta)
            throws IOException {
        guard(experienceId);
        DataStoreEntry d = find(experienceId, store, scope, key);
        long current = 0;
        if (d != null) {
            Object value = Json.parse(d.getJsonValue());
            if (!(value instanceof Number)) {
                throw new ResponseStatusException(409, "That key does not hold a number");
            }
            current = ((Number) value).longValue();
        }
        return write(experienceId, store, scope, key, Long.toString(current + delta));
    }

    @Transactional(readOnly = true)
    public List<DataValueDto> list(String experienceId, String store, String scope) {
        guard(experienceId);
        List<DataValueDto> out = new ArrayList<DataValueDto>();
        for (DataStoreEntry d : entries.list(experienceId, name(store, 64, "the store name"),
                name(scope, 64, "the scope"))) {
            out.add(toDto(d));
        }
        return out;
    }

    private DataValueDto write(String experienceId, String store, String scope, String key, String json)
            throws IOException {
        DataStoreEntry d = find(experienceId, store, scope, key);
        if (d == null) {
            d = new DataStoreEntry();
            d.setId(Ids.next());
            d.setExperienceId(experienceId);
            d.setStoreName(store);
            d.setScope(scope);
            d.setEntryKey(key);
            d.setJsonValue(json);
            d.setRevision(1);
            d.setUpdatedAt(Clock.now());
            entries.insert(d);
        } else {
            d.setJsonValue(json);
            d.setRevision(d.getRevision() + 1);
            d.setUpdatedAt(Clock.now());
            entries.save(d);
        }
        return toDto(d);
    }

    private DataStoreEntry find(String experienceId, String store, String scope, String key) {
        List<DataStoreEntry> found = entries.find(experienceId, name(store, 64, "the store name"),
                name(scope, 64, "the scope"), name(key, 128, "the key"));
        return found.isEmpty() ? null : found.get(0);
    }

    private void guard(String experienceId) {
        Access.requireEngineOrOwner(experiences.find(experienceId));
    }

    /// A value must be JSON and at most [#MAX_VALUE_BYTES] long.
    static void check(String json) throws IOException {
        if (json == null) {
            throw new ResponseStatusException(400, "There is no value to store");
        }
        if (json.length() > MAX_VALUE_BYTES || json.getBytes("UTF-8").length > MAX_VALUE_BYTES) {
            throw new ResponseStatusException(413, "A value may be at most 4 MB");
        }
        try {
            Json.parse(json);
        } catch (IOException | RuntimeException notJson) {
            throw new ResponseStatusException(400, "The value is not JSON");
        }
    }

    private static String name(String value, int max, String what) {
        String name = Text.required(value, max, what);
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) < ' ') {
                throw new ResponseStatusException(400, "That name has control characters");
            }
        }
        return name;
    }

    private static DataValueDto toDto(DataStoreEntry d) {
        return new DataValueDto(d.getEntryKey(), d.getJsonValue(), d.getRevision());
    }
}
