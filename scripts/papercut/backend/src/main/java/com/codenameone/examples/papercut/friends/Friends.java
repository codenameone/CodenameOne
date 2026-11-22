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
package com.codenameone.examples.papercut.friends;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.api.FriendAddDto;
import com.codenameone.examples.papercut.api.FriendDto;
import com.codenameone.examples.papercut.api.FriendsApiServer;
import com.codenameone.examples.papercut.domain.Friend;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.repository.FriendRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codename1.orm.repository.PageRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Friends, and `FriendsApi` on the server. A friendship goes one way: adding
/// somebody puts them on your FRIENDS board, not you on theirs, so nobody has
/// to accept anything and nobody can push themselves onto a stranger's board.
@Component
public class Friends implements FriendsApiServer {
    private final FriendRepository friends;
    private final ProfileRepository profiles;

    public Friends(FriendRepository friends, ProfileRepository profiles) {
        this.friends = friends;
        this.profiles = profiles;
    }

    @Override
    public List<FriendDto> friends() throws Exception {
        return list(Caller.name());
    }

    @Override
    public List<FriendDto> add(FriendAddDto friend) throws Exception {
        return add(Caller.name(), friend == null ? null : friend.name());
    }

    @Override
    public List<FriendDto> remove(FriendAddDto friend) throws Exception {
        return remove(Caller.name(), friend == null ? null : friend.name());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> list(String username) {
        List<FriendDto> out = new ArrayList<FriendDto>();
        for (Friend f : friends.findByUsername(username)) {
            Profile p = profiles.findById(f.getFriend());
            if (p != null) {
                out.add(new FriendDto(p.getUsername(), p.getDisplayName(), p.getBestClassic(), p.getQuill()));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<FriendDto> add(String username, String name) throws IOException {
        Profile other = find(name);
        if (other.getUsername().equals(username)) {
            throw new ResponseStatusException(400, "You are always on your own board");
        }
        String key = key(username, other.getUsername());
        if (friends.findById(key) == null) {
            Friend f = new Friend();
            f.setId(key);
            f.setUsername(username);
            f.setFriend(other.getUsername());
            f.setCreatedAt(Instant.now());
            friends.insert(f);
        }
        return list(username);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<FriendDto> remove(String username, String name) throws IOException {
        Friend f = friends.findById(key(username, find(name).getUsername()));
        if (f != null) {
            friends.delete(f);
        }
        return list(username);
    }

    /// A player by username, else by display name. Display names are not
    /// unique, so one shared by several players is refused rather than guessed.
    private Profile find(String name) {
        String text = Text.required(name, 190, "a player name").trim();
        Profile p = profiles.findById(text);
        if (p != null) {
            return p;
        }
        List<Profile> named = profiles.named(text, new PageRequest(0, 2));
        if (named.isEmpty()) {
            throw new ResponseStatusException(404, "Nobody plays by that name");
        }
        if (named.size() > 1) {
            throw new ResponseStatusException(409, "Several players use that name; use their e-mail");
        }
        return named.get(0);
    }

    /// The row's key: one row per ordered pair, so adding twice is harmless.
    private static String key(String username, String friend) throws IOException {
        return Ids.sha256(username + "\n" + friend);
    }
}
