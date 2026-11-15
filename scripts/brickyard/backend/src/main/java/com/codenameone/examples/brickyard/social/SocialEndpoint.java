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
package com.codenameone.examples.brickyard.social;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.ChatDto;
import com.codenameone.examples.brickyard.api.FollowDto;
import com.codenameone.examples.brickyard.api.FriendDto;
import com.codenameone.examples.brickyard.api.FriendRequestDto;
import com.codenameone.examples.brickyard.api.InviteDto;
import com.codenameone.examples.brickyard.api.PartyDto;
import com.codenameone.examples.brickyard.api.PresenceDto;
import com.codenameone.examples.brickyard.api.SocialApiServer;
import com.codenameone.examples.brickyard.api.WalletDto;
import java.util.List;

/// The social routes; the work is in [Social]. Sending a friend request is
/// `POST /api/me/friends` on the account API.
@Component
public class SocialEndpoint implements SocialApiServer {
    private final Social social;

    public SocialEndpoint(Social social) {
        this.social = social;
    }

    @Override
    public List<FriendRequestDto> friendRequests() throws Exception {
        return social.incoming(Caller.name());
    }

    @Override
    public FriendDto acceptFriend(String from) throws Exception {
        return social.accept(Caller.name(), from);
    }

    @Override
    public FriendRequestDto declineFriend(String from) throws Exception {
        return social.decline(Caller.name(), from);
    }

    @Override
    public FollowDto follow(String creator) throws Exception {
        return social.follow(Caller.name(), creator, true);
    }

    @Override
    public FollowDto unfollow(String creator) throws Exception {
        return social.follow(Caller.name(), creator, false);
    }

    @Override
    public List<FollowDto> following() throws Exception {
        return social.following(Caller.name());
    }

    @Override
    public PresenceDto setPresence(PresenceDto presence) throws Exception {
        return social.setPresence(Caller.name(), presence);
    }

    @Override
    public List<PresenceDto> friendPresence() throws Exception {
        return social.friendPresence(Caller.name());
    }

    @Override
    public PresenceDto joinFriend(String friend) throws Exception {
        return social.joinTarget(Caller.name(), friend);
    }

    @Override
    public FriendDto block(String username) throws Exception {
        return social.block(Caller.name(), username);
    }

    @Override
    public List<FriendDto> blocks() throws Exception {
        return social.blocks(Caller.name());
    }

    @Override
    public PartyDto createParty() throws Exception {
        return social.createParty(Caller.name());
    }

    @Override
    public PartyDto joinParty(String partyId) throws Exception {
        return social.joinParty(Caller.name(), partyId);
    }

    @Override
    public PartyDto leaveParty(String partyId) throws Exception {
        return social.leaveParty(Caller.name(), partyId);
    }

    @Override
    public PartyDto party() throws Exception {
        return social.myParty(Caller.name());
    }

    @Override
    public ChatDto send(String channel, ChatDto message) throws Exception {
        return social.send(Caller.name(), channel, message);
    }

    @Override
    public List<ChatDto> chat(String channel) throws Exception {
        return social.read(Caller.name(), channel);
    }

    @Override
    public ChatDto sendDm(String username, ChatDto message) throws Exception {
        return social.sendDm(Caller.name(), username, message);
    }

    @Override
    public List<ChatDto> dms(String username) throws Exception {
        return social.dms(Caller.name(), username);
    }

    @Override
    public InviteDto createInvite(InviteDto invite) throws Exception {
        return social.createInvite(Caller.name(), invite);
    }

    @Override
    public WalletDto redeemInvite(InviteDto invite) throws Exception {
        return social.redeem(Caller.name(), invite);
    }
}
