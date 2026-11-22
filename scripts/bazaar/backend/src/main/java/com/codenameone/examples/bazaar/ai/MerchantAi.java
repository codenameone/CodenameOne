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

package com.codenameone.examples.bazaar.ai;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The merchant app's AI, behind the server: an OpenAI-compatible
/// `chat/completions` the app's `com.codename1.ai` client talks to, so the
/// provider key stays on the server and never ships in the app.
///
/// The app names a task in its first system message -- `bazaar:assistant` or
/// `bazaar:description` -- and the server puts its own instructions there, so
/// a client cannot turn the key into a general-purpose model. The server also
/// fixes the model, caps the reply and the request, and keeps at most a few
/// tools, which are the app's own query functions: the model asks for them and
/// the app runs them over the store's analytics.
///
/// The provider is configured like the SMS one:
///
/// ```properties
/// # openai, anthropic, gemini, or url
/// bazaar.ai.provider=openai
/// # never in the app
/// bazaar.ai.key=...
/// # each provider has a default
/// bazaar.ai.model=gpt-4o-mini
/// # for "url": any compatible server
/// bazaar.ai.url=http://localhost:11434/v1
/// ```
///
/// With no provider the answers come from [AiFallback], rules with the same
/// shape, which is what development and the tests use.
@Component
public class MerchantAi {
    public static final String ASSISTANT = "bazaar:assistant";
    public static final String DESCRIPTION = "bazaar:description";
    static final int MAX_BODY = 64 * 1024;
    static final int MAX_MESSAGES = 40;
    static final int MAX_TOOLS = 8;
    static final int MAX_TEXT = 8000;
    static final int MAX_TOKENS = 700;

    static final String ASSISTANT_PROMPT = "You are the analytics assistant of an online store's "
            + "merchant app. Answer the merchant's question in two to five short sentences, "
            + "with the numbers. Get every number by calling the tools; never guess one. Money "
            + "in the results is in cents: show it in the currency given. If no tool covers "
            + "the question, say what you can answer instead.";
    static final String DESCRIPTION_PROMPT = "You write product descriptions for an online "
            + "store. From the product fields in the user message, write one paragraph of 60 "
            + "to 110 words in plain, friendly language. Do not invent materials, sizes, "
            + "certifications or claims the fields do not support. Reply with the paragraph "
            + "only.";

    @Autowired
    private StoreAccess access;

    @Value("${bazaar.ai.provider:}")
    private String provider;

    @Value("${bazaar.ai.key:}")
    private String key;

    @Value("${bazaar.ai.model:}")
    private String model;

    @Value("${bazaar.ai.url:}")
    private String url;

    /// Whether a provider is configured (otherwise rules answer).
    public boolean live() {
        return base() != null;
    }

    /// The provider's OpenAI-compatible base, or null for the rules.
    String base() {
        String p = provider == null ? "" : provider.trim();
        if ("openai".equalsIgnoreCase(p)) {
            return "https://api.openai.com/v1";
        }
        if ("anthropic".equalsIgnoreCase(p)) {
            return "https://api.anthropic.com/v1";
        }
        if ("gemini".equalsIgnoreCase(p)) {
            return "https://generativelanguage.googleapis.com/v1beta/openai";
        }
        if ("url".equalsIgnoreCase(p) && url != null && url.trim().length() > 0) {
            return url.trim();
        }
        return null;
    }

    String modelName() {
        if (model != null && model.trim().length() > 0) {
            return model.trim();
        }
        String p = provider == null ? "" : provider.trim();
        if ("anthropic".equalsIgnoreCase(p)) {
            return "claude-haiku-4-5";
        }
        if ("gemini".equalsIgnoreCase(p)) {
            return "gemini-2.5-flash";
        }
        return "gpt-4o-mini";
    }

    /// Answers one completion request for the store, as JSON.
    public String complete(String username, String storeId, byte[] body) throws IOException {
        // Staff of this store before anything is read, so another store's
        // route answers 404 whatever the body; the task's own area is checked
        // once the body says which task it is.
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (body == null || body.length == 0 || body.length > MAX_BODY) {
            throw new ResponseStatusException(400, "The request is empty or too large");
        }
        Map request;
        try {
            request = Json.parseObject(new String(body, "UTF-8"));
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(400, "The request is not JSON");
        }
        List<Map<String, Object>> messages = messages(request.get("messages"));
        String task = messages.isEmpty() ? "" : AiFallback.text(messages.get(0));
        boolean assistant = ASSISTANT.equals(task);
        if (!assistant && !DESCRIPTION.equals(task)) {
            throw new ResponseStatusException(400, "Unknown task");
        }
        access.requireArea(username, storeId, assistant ? Perms.REPORTS : Perms.PRODUCTS,
                !assistant, StoreAccess.MANAGERS);
        messages.remove(0);
        List<Object> tools = assistant ? tools(request.get("tools")) : new ArrayList<Object>();
        if (base() == null) {
            return Json.write(assistant ? AiFallback.assistant(messages, toolNames(tools))
                    : AiFallback.description(product(messages)));
        }
        Map<String, Object> system = new LinkedHashMap<String, Object>();
        system.put("role", "system");
        system.put("content", assistant ? ASSISTANT_PROMPT : DESCRIPTION_PROMPT);
        messages.add(0, system);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("model", modelName());
        out.put("messages", messages);
        out.put("max_tokens", MAX_TOKENS);
        if (!tools.isEmpty()) {
            out.put("tools", tools);
        }
        List headers = new ArrayList();
        headers.add("Authorization: Bearer " + key);
        headers.add("Content-Type: application/json");
        Web.Result answer = Web.request("POST", base() + "/chat/completions", headers,
                Json.write(out).getBytes("UTF-8"));
        if (answer.getStatus() < 200 || answer.getStatus() >= 300) {
            // The status only: an upstream error body can echo the request.
            throw new ResponseStatusException(502, "The AI provider answered "
                    + answer.getStatus());
        }
        return answer.getBodyAsString();
    }

    /// The conversation, each message cut to the roles and fields a
    /// completion takes and its text to [#MAX_TEXT].
    static List<Map<String, Object>> messages(Object raw) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (!(raw instanceof List)) {
            return out;
        }
        List list = (List) raw;
        int from = Math.max(0, list.size() - MAX_MESSAGES);
        if (!list.isEmpty() && from > 0) {
            out.add(copy(list.get(0)));
        }
        for (int iter = from; iter < list.size(); iter++) {
            Map<String, Object> m = copy(list.get(iter));
            if (m != null) {
                out.add(m);
            }
        }
        while (out.remove(null)) {
            // drops what was not a message
        }
        return out;
    }

    private static Map<String, Object> copy(Object raw) {
        if (!(raw instanceof Map)) {
            return null;
        }
        Map in = (Map) raw;
        Object role = in.get("role");
        if (!"system".equals(role) && !"user".equals(role) && !"assistant".equals(role)
                && !"tool".equals(role)) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("role", role);
        String text = AiFallback.text(in);
        m.put("content", text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
        if (in.get("tool_call_id") instanceof String) {
            m.put("tool_call_id", in.get("tool_call_id"));
        }
        if (in.get("tool_calls") instanceof List) {
            m.put("tool_calls", in.get("tool_calls"));
        }
        return m;
    }

    static List<Object> tools(Object raw) {
        List<Object> out = new ArrayList<Object>();
        if (raw instanceof List) {
            for (Object t : (List) raw) {
                if (t instanceof Map && out.size() < MAX_TOOLS) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    static List<String> toolNames(List<Object> tools) {
        List<String> out = new ArrayList<String>();
        for (Object t : tools) {
            Object fn = ((Map) t).get("function");
            if (fn instanceof Map && ((Map) fn).get("name") instanceof String) {
                out.add((String) ((Map) fn).get("name"));
            }
        }
        return out;
    }

    /// The product fields of a description request: the JSON of its last
    /// user message.
    static Map<String, Object> product(List<Map<String, Object>> messages) {
        for (int iter = messages.size() - 1; iter >= 0; iter--) {
            if ("user".equals(messages.get(iter).get("role"))) {
                try {
                    return Json.parseObject(AiFallback.text(messages.get(iter)));
                } catch (IOException | RuntimeException e) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("title", AiFallback.text(messages.get(iter)));
                    return m;
                }
            }
        }
        return new LinkedHashMap<String, Object>();
    }
}
