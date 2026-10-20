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
package com.codenameone.examples.parley.chat;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.ConversationApiServer;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.ConversationEditDto;
import com.codenameone.examples.parley.api.ConversationFlagsDto;
import com.codenameone.examples.parley.api.DeviceAddressDto;
import com.codenameone.examples.parley.api.InviteDto;
import com.codenameone.examples.parley.api.MemberChangeDto;
import com.codenameone.examples.parley.api.NewConversationDto;
import com.codenameone.examples.parley.api.PhotoDto;

import java.util.List;

/// The server's half of `ConversationApi`.
@Component
public class ConversationEndpoint implements ConversationApiServer {
    private final Conversations conversations;

    public ConversationEndpoint(Conversations conversations) {
        this.conversations = conversations;
    }

    @Override
    public List<ConversationDto> list() throws Exception {
        return conversations.list(Caller.name());
    }

    @Override
    public ConversationDto create(NewConversationDto conversation) throws Exception {
        String username = Caller.name();
        try {
            return conversations.create(username, conversation);
        } catch (ResponseStatusException refused) {
            throw refused;
        } catch (RuntimeException raced) {
            // Both people opened the same direct chat at once, and the unique
            // direct key rejected the second commit. Asking again finds the
            // chat that won; a group create cannot race this way, so a second
            // failure is a real one.
            String kind = conversation == null ? null : conversation.kind();
            if (conversation == null || !(kind == null || kind.length() == 0
                    || Conversations.DIRECT.equalsIgnoreCase(kind))) {
                throw raced;
            }
            return conversations.create(username, conversation);
        }
    }

    @Override
    public ConversationDto get(String id) throws Exception {
        return conversations.get(Caller.name(), id);
    }

    @Override
    public ConversationDto edit(String id, ConversationEditDto edit) throws Exception {
        return conversations.edit(Caller.name(), id, edit);
    }

    @Override
    public ConversationDto flags(String id, ConversationFlagsDto flags) throws Exception {
        return conversations.flags(Caller.name(), id, flags);
    }

    @Override
    public ConversationDto savePhoto(String id, PhotoDto photo) throws Exception {
        return conversations.savePhoto(Caller.name(), id, photo);
    }

    @Override
    public PhotoDto photo(String id) throws Exception {
        return conversations.photo(Caller.name(), id);
    }

    @Override
    public ConversationDto addMember(String id, MemberChangeDto member) throws Exception {
        return conversations.addMember(Caller.name(), id, member);
    }

    @Override
    public ConversationDto setRole(String id, String username, MemberChangeDto member)
            throws Exception {
        return conversations.setRole(Caller.name(), id, username, member);
    }

    @Override
    public ConversationDto removeMember(String id, String username) throws Exception {
        return conversations.removeMember(Caller.name(), id, username);
    }

    @Override
    public ConversationDto leave(String id) throws Exception {
        return conversations.leave(Caller.name(), id);
    }

    @Override
    public InviteDto invite(String id) throws Exception {
        return conversations.invite(Caller.name(), id);
    }

    @Override
    public InviteDto preview(String code) throws Exception {
        return conversations.preview(code);
    }

    @Override
    public ConversationDto join(String code) throws Exception {
        return conversations.join(Caller.name(), code);
    }

    @Override
    public List<DeviceAddressDto> devices(String id) throws Exception {
        return conversations.devices(Caller.name(), id);
    }
}
