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
package com.codename1.impl.android.ai;

import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.SmartReplyMessage;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.ArrayList;
import java.util.List;

/** ML Kit Smart Reply; retained only for {@code SmartReply} users. */
final class AndroidSmartReplyAdapter extends AndroidLanguageAdapter {
    @Override
    AsyncResource<String[]> suggestReplies(
            SmartReplyMessage[] conversation, LanguageOptions options) {
        final AsyncResource<String[]> out = new AsyncResource<String[]>();
        final SmartReplyGenerator client = SmartReply.getClient();
        List<TextMessage> messages = new ArrayList<TextMessage>();
        for (int i = 0; i < conversation.length; i++) {
            SmartReplyMessage message = conversation[i];
            messages.add(message.isLocalUser()
                    ? TextMessage.createForLocalUser(
                            message.getText(), message.getTimestampMillis())
                    : TextMessage.createForRemoteUser(
                            message.getText(), message.getTimestampMillis(),
                            message.getParticipantId() == null
                                    ? "remote"
                                    : message.getParticipantId()));
        }
        client.suggestReplies(messages).addOnSuccessListener(
                new OnSuccessListener<SmartReplySuggestionResult>() {
            public void onSuccess(SmartReplySuggestionResult value) {
                List<SmartReplySuggestion> suggestions =
                        value.getSuggestions();
                String[] result = new String[suggestions.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = suggestions.get(i).getText();
                }
                complete(out, result);
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
        return out;
    }
}
