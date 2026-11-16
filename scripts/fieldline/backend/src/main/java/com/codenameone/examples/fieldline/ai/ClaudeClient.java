package com.codenameone.examples.fieldline.ai;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The Claude Messages API, over plain HTTP.
///
/// The key stays on this server: the app talks to Fieldline's own endpoints,
/// and only this class talks to Anthropic. Requests are built by the callers as
/// maps in the shape of the API's JSON and sent as they are, so what is sent is
/// readable in one place, [FieldAssistant].
///
/// A client made by [#demo] has no key and [#isLive] answers false; callers
/// then answer from their own rules. [com.codenameone.examples.fieldline.Services]
/// chooses which.
public final class ClaudeClient {
    static final String VERSION = "2026-10-21";

    private final String url;
    private final String key;
    private final String model;

    public ClaudeClient(String url, String key, String model) {
        String base = url == null ? "" : url;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.url = base;
        this.key = key;
        this.model = model;
    }

    /// A client that never calls out.
    public static ClaudeClient demo() {
        return new ClaudeClient(null, null, null);
    }

    public boolean isLive() {
        return key != null && key.length() > 0;
    }

    public String model() {
        return model;
    }

    /// Sends one request to `/v1/messages` and answers the parsed response.
    /// `request` gets the model filled in. Throws for anything but a 2xx, with
    /// the API's own error message, so the caller can fall back and log why.
    public Map message(Map<String, Object> request) throws IOException {
        if (!isLive()) {
            throw new IOException("No model is configured");
        }
        request.put("model", model);
        List<String> headers = new ArrayList<String>();
        headers.add("x-api-key: " + key);
        headers.add("anthropic-version: " + VERSION);
        headers.add("content-type: application/json");
        byte[] body = Json.write(request).getBytes("UTF-8");
        Web.Result answer = Web.request("POST", url + "/v1/messages", headers, body);
        String text = answer.getBodyAsString();
        if (!answer.isSuccess()) {
            String why = text;
            try {
                Object error = Json.parseObject(text).get("error");
                if (error instanceof Map) {
                    Object message = ((Map) error).get("message");
                    why = message == null ? text : String.valueOf(message);
                }
            } catch (IOException notJson) {
                why = text;
            }
            throw new IOException("Claude answered " + answer.getStatus() + ": " + why);
        }
        return Json.parseObject(text);
    }

    /// The text of every text block of a response, joined.
    public static String text(Map response) {
        StringBuilder out = new StringBuilder();
        Object content = response.get("content");
        if (content instanceof List) {
            for (Object block : (List) content) {
                if (block instanceof Map && "text".equals(((Map) block).get("type"))) {
                    Object t = ((Map) block).get("text");
                    if (t != null) {
                        out.append(t);
                    }
                }
            }
        }
        return out.toString();
    }

    public static String stopReason(Map response) {
        Object reason = response.get("stop_reason");
        return reason == null ? "" : String.valueOf(reason);
    }
}
