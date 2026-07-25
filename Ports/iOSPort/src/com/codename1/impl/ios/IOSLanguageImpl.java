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
package com.codename1.impl.ios;

import com.codename1.ai.language.LanguageCandidate;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.SmartReplyMessage;
import com.codename1.impl.LanguageImpl;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** iOS ML Kit language services. */
public final class IOSLanguageImpl extends LanguageImpl {
    private static final int LANGUAGE_ID = 0;
    private static final int TRANSLATION = 1;
    private static final int SMART_REPLY = 2;

    private volatile boolean closed;

    @Override
    public boolean isSupported(String feature, String backendId) {
        if (closed || (!"auto".equals(backendId) && !"ml-kit".equals(backendId))) {
            return false;
        }
        return IOSImplementation.nativeInstance.cn1LanguageIsSupported(
                featureId(feature));
    }

    @Override
    public AsyncResource<LanguageCandidate[]> identify(
            final String text, String backendId, final LanguageOptions options) {
        return identifyInBackground(text, options);
    }

    private static AsyncResource<LanguageCandidate[]> identifyInBackground(
            final String text, final LanguageOptions options) {
        final AsyncResource<LanguageCandidate[]> out =
                new AsyncResource<LanguageCandidate[]>();
        run(out, new NativeCall<LanguageCandidate[]>() {
            public LanguageCandidate[] call() throws Exception {
                String json = IOSImplementation.nativeInstance.cn1LanguageIdentify(
                        text, options.getMinimumConfidence());
                Map root = parse(json);
                List values = list(root, "items");
                LanguageCandidate[] result = new LanguageCandidate[values.size()];
                for (int i = 0; i < result.length; i++) {
                    Map value = (Map) values.get(i);
                    result[i] = new LanguageCandidate(
                            string(value, "language"),
                            number(value, "confidence"));
                }
                return result;
            }
        });
        return out;
    }

    @Override
    public AsyncResource<String> translate(
            final String text, final String sourceLanguage,
            final String targetLanguage, String backendId,
            LanguageOptions options) {
        return translateInBackground(text, sourceLanguage, targetLanguage);
    }

    private static AsyncResource<String> translateInBackground(
            final String text, final String sourceLanguage,
            final String targetLanguage) {
        final AsyncResource<String> out = new AsyncResource<String>();
        run(out, new NativeCall<String>() {
            public String call() throws Exception {
                Map root = parse(IOSImplementation.nativeInstance.cn1LanguageTranslate(
                        text, sourceLanguage, targetLanguage));
                return string(root, "text");
            }
        });
        return out;
    }

    @Override
    public AsyncResource<String[]> suggestReplies(
            SmartReplyMessage[] conversation, String backendId,
            LanguageOptions options) {
        return suggestRepliesInBackground(conversationJson(conversation));
    }

    private static AsyncResource<String[]> suggestRepliesInBackground(
            final String json) {
        final AsyncResource<String[]> out = new AsyncResource<String[]>();
        run(out, new NativeCall<String[]>() {
            public String[] call() throws Exception {
                Map root = parse(IOSImplementation.nativeInstance
                        .cn1LanguageSmartReply(json));
                List values = list(root, "items");
                String[] result = new String[values.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = String.valueOf(values.get(i));
                }
                return result;
            }
        });
        return out;
    }

    private static int featureId(String feature) {
        if ("language-id".equals(feature)) return LANGUAGE_ID;
        if ("translation".equals(feature)) return TRANSLATION;
        if ("smart-reply".equals(feature)) return SMART_REPLY;
        return -1;
    }

    private static String conversationJson(SmartReplyMessage[] conversation) {
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < conversation.length; i++) {
            SmartReplyMessage message = conversation[i];
            Map<String, Object> value = new HashMap<String, Object>();
            value.put("text", message.getText());
            value.put("participant", message.getParticipantId());
            value.put("local", Boolean.valueOf(message.isLocalUser()));
            value.put("timestamp", Long.valueOf(message.getTimestampMillis()));
            values.add(value);
        }
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("items", values);
        return JSONParser.mapToJson(root);
    }

    private static Map parse(String json) throws Exception {
        if (json == null || json.length() == 0) {
            throw new IllegalStateException("ML Kit returned no result");
        }
        Map root = new JSONParser().parseJSON(new StringReader(json));
        Object error = root.get("error");
        if (error != null) {
            throw new IllegalStateException(String.valueOf(error));
        }
        return root;
    }

    private static List list(Map value, String key) {
        Object out = value.get(key);
        return out instanceof List ? (List) out : java.util.Collections.EMPTY_LIST;
    }

    private static String string(Map value, String key) {
        Object out = value.get(key);
        return out == null ? "" : String.valueOf(out);
    }

    private static float number(Map value, String key) {
        Object out = value.get(key);
        return out instanceof Number ? ((Number) out).floatValue() : 0;
    }

    private static <T> void run(final AsyncResource<T> out,
                                final NativeCall<T> call) {
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    final T value = call.call();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.complete(value);
                        }
                    });
                } catch (final Throwable error) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.error(error);
                        }
                    });
                }
            }
        });
    }

    private interface NativeCall<T> {
        T call() throws Exception;
    }

    @Override
    public void close() {
        closed = true;
    }
}
