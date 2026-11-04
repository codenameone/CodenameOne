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
package com.codenameone.examples.dash.admin;

import com.codename1.backend.Json;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The admins connected to the game socket right now, so a new flag reaches the
/// console the moment it is raised.
@Component
public class AdminChannel {
    private final Map<String, WebSocketSession> admins = new HashMap<String, WebSocketSession>();

    public synchronized void add(String username, WebSocketSession session) {
        admins.put(username, session);
    }

    public synchronized void remove(String username, WebSocketSession session) {
        if (admins.get(username) == session) {
            admins.remove(username);
        }
    }

    public void flagged(String username, String reason) {
        List<WebSocketSession> to;
        synchronized (this) {
            to = new ArrayList<WebSocketSession>(admins.values());
        }
        String msg = "{\"type\":\"flag\",\"username\":" + Json.write(username) + ",\"reason\":"
                + Json.write(reason) + "}";
        for (WebSocketSession s : to) {
            try {
                if (s.isOpen()) {
                    s.sendText(msg);
                }
            } catch (IOException ignored) {
                // Closing; its close callback removes it.
            }
        }
    }
}
