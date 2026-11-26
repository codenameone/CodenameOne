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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The answers the merchant AI gives when no provider is configured: the
/// same OpenAI `chat.completion` shape a model would send, worked out by
/// rules, so the app behaves the same with or without a key and the tests
/// need no network.
///
/// - **Assistant, on a merchant's question:** it asks for one of the app's own
///   tools, picked by the words in the question among the tools the request
///   offers.
/// - **Assistant, on the tools' results:** it answers with the `summary` each
///   result carries -- the app writes one into every result so this works.
/// - **Description:** a draft built from the product's own fields.
public final class AiFallback {
    /// The model name a fallback answer reports.
    public static final String MODEL = "bazaar-rules";

    private static final String[][] ROUTES = {
        {"channel_mix", "channel", "pos", "point of sale", "marketplace", "app", "web", "online"},
        {"top_products", "product", "best", "top", "seller", "selling", "item"},
        {"conversion_funnel", "funnel", "conver", "view", "visitor", "checkout"},
        {"pipeline_overview", "cart", "pipeline", "abandon", "intent", "remind", "ship", "fulfil"},
        {"sales_summary", "sales", "revenue", "order", "money", "week", "month", "today"},
    };

    private AiFallback() {
    }

    /// The answer to an assistant conversation, as a completion.
    public static Map<String, Object> assistant(List<Map<String, Object>> messages,
            List<String> tools) {
        Map<String, Object> last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        if (last != null && "tool".equals(last.get("role"))) {
            StringBuilder out = new StringBuilder();
            for (int iter = messages.size() - 1; iter >= 0
                    && "tool".equals(messages.get(iter).get("role")); iter--) {
                String summary = summaryOf(text(messages.get(iter)));
                if (summary.length() > 0) {
                    out.insert(0, out.length() == 0 ? summary : summary + "\n\n");
                }
            }
            return completion(out.length() == 0
                    ? "I could not find numbers for that in this store." : out.toString(),
                    null, null);
        }
        String question = last == null ? "" : text(last);
        String tool = pick(question, tools);
        if (tool == null) {
            return completion("Ask me about sales, channels, top products, the funnel or carts.",
                    null, null);
        }
        return completion(null, tool, "{}");
    }

    /// The tool a question asks for, or null when the request offers none.
    static String pick(String question, List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return null;
        }
        for (int iter = 0; iter < ROUTES.length; iter++) {
            if (!tools.contains(ROUTES[iter][0])) {
                continue;
            }
            for (int w = 1; w < ROUTES[iter].length; w++) {
                if (contains(question, ROUTES[iter][w])) {
                    return ROUTES[iter][0];
                }
            }
        }
        return tools.contains("sales_summary") ? "sales_summary" : tools.get(0);
    }

    /// A product description draft from the JSON fields the app sent.
    public static Map<String, Object> description(Map<String, Object> product) {
        String title = field(product, "title");
        String vendor = field(product, "vendor");
        String type = field(product, "type");
        String options = field(product, "options");
        String tags = field(product, "tags");
        StringBuilder out = new StringBuilder();
        out.append(title.length() == 0 ? "This product" : title);
        if (vendor.length() > 0) {
            out.append(" from ").append(vendor);
        }
        // The type is a catalog heading ("Gear"), so it reads as one.
        out.append(type.length() > 0 ? " is made to be used every day, from our " + type
                + " range." : " is made to be used every day.");
        if (options.length() > 0) {
            out.append(" Choose from ").append(options).append('.');
        }
        if (tags.length() > 0) {
            out.append(" Good for ").append(tags).append('.');
        }
        out.append(" Order today and it ships from our store.");
        return completion(out.toString(), null, null);
    }

    static String field(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    /// The `summary` of a tool result, or the result itself when it is not
    /// a JSON object with one.
    static String summaryOf(String result) {
        try {
            Object parsed = com.codename1.backend.Json.parse(result);
            if (parsed instanceof Map) {
                Object s = ((Map) parsed).get("summary");
                if (s != null) {
                    return String.valueOf(s);
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            // Not JSON: the text is the answer.
        }
        return result == null ? "" : result.trim();
    }

    /// The text of a message: a string, or the text parts of an array.
    static String text(Map<String, Object> message) {
        Object c = message.get("content");
        if (c instanceof String) {
            return (String) c;
        }
        StringBuilder out = new StringBuilder();
        if (c instanceof List) {
            for (Object part : (List) c) {
                if (part instanceof Map && ((Map) part).get("text") != null) {
                    out.append(((Map) part).get("text"));
                }
            }
        }
        return out.toString();
    }

    /// Whether `text` holds `word`, ignoring ASCII case.
    static boolean contains(String text, String word) {
        int n = word.length();
        for (int at = 0; at + n <= text.length(); at++) {
            if (text.regionMatches(true, at, word, 0, n)) {
                return true;
            }
        }
        return false;
    }

    static Map<String, Object> completion(String content, String tool, String arguments) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", "assistant");
        message.put("content", content);
        if (tool != null) {
            Map<String, Object> fn = new LinkedHashMap<String, Object>();
            fn.put("name", tool);
            fn.put("arguments", arguments);
            Map<String, Object> call = new LinkedHashMap<String, Object>();
            call.put("id", "call_" + tool);
            call.put("type", "function");
            call.put("function", fn);
            List<Object> calls = new ArrayList<Object>();
            calls.add(call);
            message.put("tool_calls", calls);
        }
        Map<String, Object> choice = new LinkedHashMap<String, Object>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", tool != null ? "tool_calls" : "stop");
        List<Object> choices = new ArrayList<Object>();
        choices.add(choice);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", "bazaar-rules");
        out.put("object", "chat.completion");
        out.put("model", MODEL);
        out.put("choices", choices);
        return out;
    }
}
