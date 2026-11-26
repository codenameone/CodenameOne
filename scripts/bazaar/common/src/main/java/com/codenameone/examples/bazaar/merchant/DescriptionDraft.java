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

package com.codenameone.examples.bazaar.merchant;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.ChatRequest;
import com.codename1.ai.ChatResponse;
import com.codename1.ai.LlmClient;
import com.codename1.io.JSONParser;
import com.codename1.util.AsyncResult;
import java.util.LinkedHashMap;
import java.util.Map;

/// A product description draft from the product editor's own fields, written
/// by the store's AI route (`bazaar:description`). The server holds the key
/// and the instructions -- one paragraph, nothing the fields do not support --
/// and drafts by rules when no provider is configured. The merchant reads and
/// edits the draft; nothing is saved until they save the product.
final class DescriptionDraft {
    static final String TASK = "bazaar:description";

    private DescriptionDraft() {
    }

    /// The fields the draft may use, as the JSON the server reads. Empty
    /// fields are left out.
    static String fields(String title, String vendor, String type, String tags,
            String options) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        put(m, "title", title);
        put(m, "vendor", vendor);
        put(m, "type", type);
        put(m, "tags", tags);
        put(m, "options", options);
        return JSONParser.mapToJson(m);
    }

    private static void put(Map<String, Object> m, String key, String value) {
        if (value != null && value.trim().length() > 0) {
            m.put(key, value.trim());
        }
    }

    /// Asks for a draft; `done` gets the text, or null when it failed.
    static void draft(LlmClient client, String fieldsJson, final AssistantRun.Done done) {
        ChatRequest req = ChatRequest.builder()
                .addMessage(ChatMessage.system(TASK))
                .addMessage(ChatMessage.user(fieldsJson))
                .build();
        client.chat(req).onResult(new AsyncResult<ChatResponse>() {
            @Override
            public void onReady(ChatResponse r, Throwable error) {
                if (error != null) {
                    done.failed(error);
                } else {
                    done.answered(r.getText() == null ? "" : r.getText().trim());
                }
            }
        });
    }
}
