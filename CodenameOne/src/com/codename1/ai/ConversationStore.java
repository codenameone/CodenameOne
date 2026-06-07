/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ai;

import com.codename1.io.JSONParser;
import com.codename1.io.Storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// JSON-backed persistent conversation history.
///
/// Stores [ChatMessage] lists in [Storage] under a caller-chosen key
/// so apps can rehydrate a `ChatView` after process restart.
/// Multimodal parts ([ImagePart], [ToolResultPart]) are serialized
/// to a lossy text fallback -- image data is not round-tripped (apps
/// that need full multimodal persistence should encode the bytes
/// themselves and keep them in [com.codename1.io.FileSystemStorage]).
public final class ConversationStore {
    private static final String KIND_TEXT = "t";
    private static final String KIND_TOOL_RESULT = "tr";

    private final String storageKey;

    public ConversationStore(String storageKey) {
        if (storageKey == null || storageKey.length() == 0) {
            throw new IllegalArgumentException("storageKey is required");
        }
        this.storageKey = storageKey;
    }

    public void save(List<ChatMessage> messages) throws IOException {
        List<Object> serialized = new ArrayList<Object>(messages == null ? 0 : messages.size());
        if (messages != null) {
            for (ChatMessage m : messages) {
                Map<String, Object> jm = new HashMap<String, Object>();
                jm.put("role", m.getRole().name());
                List<Object> parts = new ArrayList<Object>(m.getParts().size());
                for (MessagePart p : m.getParts()) {
                    Map<String, Object> jp = new HashMap<String, Object>();
                    if (p instanceof TextPart) {
                        jp.put("kind", KIND_TEXT);
                        jp.put("text", ((TextPart) p).getText());
                    } else if (p instanceof ToolResultPart) {
                        ToolResultPart trp = (ToolResultPart) p;
                        jp.put("kind", KIND_TOOL_RESULT);
                        jp.put("toolCallId", trp.getToolCallId());
                        jp.put("resultJson", trp.getResultJson());
                    } else {
                        // ImagePart: lossy. Save a placeholder so
                        // the message order is preserved.
                        jp.put("kind", KIND_TEXT);
                        jp.put("text", "[image]");
                    }
                    parts.add(jp);
                }
                jm.put("parts", parts);
                if (m.getToolCallId() != null) {
                    jm.put("toolCallId", m.getToolCallId());
                }
                serialized.add(jm);
            }
        }
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("messages", serialized);
        byte[] payload = JSONParser.toJson(root).getBytes("UTF-8");
        Storage.getInstance().writeObject(storageKey, payload);
    }

    public List<ChatMessage> load() throws IOException {
        Object raw = Storage.getInstance().readObject(storageKey);
        if (raw == null) {
            return new ArrayList<ChatMessage>();
        }
        if (!(raw instanceof byte[])) {
            // Old-format / accidental overwrite. Treat as empty
            // rather than crashing.
            return new ArrayList<ChatMessage>();
        }
        Map root = JSONParser.parseJSON((byte[]) raw);
        List<Object> serialized = JSONParser.asList(root.get("messages"));
        if (serialized == null) {
            return new ArrayList<ChatMessage>();
        }
        List<ChatMessage> out = new ArrayList<ChatMessage>(serialized.size());
        for (Object rawJm : serialized) {
            Map jm = JSONParser.asMap(rawJm);
            Role role = parseRole(JSONParser.getString(jm, "role"));
            List<MessagePart> parts = new ArrayList<MessagePart>();
            List<Object> jparts = JSONParser.asList(jm.get("parts"));
            if (jparts != null) {
                for (Object rawJp : jparts) {
                    Map jp = JSONParser.asMap(rawJp);
                    String kind = JSONParser.getString(jp, "kind");
                    if (KIND_TOOL_RESULT.equals(kind)) {
                        parts.add(new ToolResultPart(
                                JSONParser.getString(jp, "toolCallId"),
                                JSONParser.getString(jp, "resultJson")));
                    } else {
                        parts.add(new TextPart(JSONParser.getString(jp, "text")));
                    }
                }
            }
            out.add(new ChatMessage(role, parts,
                    null, null, JSONParser.getString(jm, "toolCallId")));
        }
        return out;
    }

    public void clear() {
        Storage.getInstance().deleteStorageFile(storageKey);
    }

    public String getStorageKey() {
        return storageKey;
    }

    private static Role parseRole(String name) {
        if (name == null) {
            return Role.USER;
        }
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException iae) {
            return Role.USER;
        }
    }
}
