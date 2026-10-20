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

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.ConversationEditDto;
import com.codenameone.examples.parley.api.ConversationFlagsDto;
import com.codenameone.examples.parley.api.DeviceAddressDto;
import com.codenameone.examples.parley.api.InviteDto;
import com.codenameone.examples.parley.api.MemberChangeDto;
import com.codenameone.examples.parley.api.MemberDto;
import com.codenameone.examples.parley.api.NewConversationDto;
import com.codenameone.examples.parley.api.PhotoDto;
import com.codenameone.examples.parley.domain.BridgeLink;
import com.codenameone.examples.parley.domain.Conversation;
import com.codenameone.examples.parley.domain.ConversationPhoto;
import com.codenameone.examples.parley.domain.Device;
import com.codenameone.examples.parley.domain.InviteLink;
import com.codenameone.examples.parley.domain.Membership;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.live.LiveHub;
import com.codenameone.examples.parley.repository.BridgeLinkRepository;
import com.codenameone.examples.parley.repository.ConversationPhotoRepository;
import com.codenameone.examples.parley.repository.ConversationRepository;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.InviteLinkRepository;
import com.codenameone.examples.parley.repository.MembershipRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Conversations and who is in them.
///
/// A direct chat has two members and no roles worth the name. A group has an
/// owner, admins and members: admins change its settings and its members, the
/// owner cannot be removed, and an owner who leaves hands the group on. A
/// channel is a group only its admins post to.
///
/// The server knows who is in a conversation -- it has to, to know whose
/// devices a message is encrypted for -- but never what is said there. Group
/// titles, descriptions and photos are the exception, as in every messenger
/// that lets a link preview a group before joining it.
@Component
public class Conversations {
    public static final String DIRECT = "DIRECT";
    public static final String GROUP = "GROUP";
    public static final String CHANNEL = "CHANNEL";

    public static final String OWNER = "OWNER";
    public static final String ADMIN = "ADMIN";
    public static final String MEMBER = "MEMBER";

    /// The demo bot's account. A conversation with it is not end to end
    /// encrypted: the bot is the server, and says so in the app.
    public static final String BOT = "bot@parley.example";

    /// The most members a group may have. Every message is encrypted once per
    /// member device, so a messenger built like this one keeps groups small.
    public static final int MAX_MEMBERS = 256;

    /// How long an invite link works.
    public static final long INVITE_LIFE = 7L * 24 * 60 * 60 * 1000;

    private static final int[] TIMERS = {0, 30, 300, 3600, 86400, 604800};

    private final ConversationRepository conversations;
    private final MembershipRepository members;
    private final InviteLinkRepository invites;
    private final ProfileRepository profiles;
    private final DeviceRepository devices;
    private final BridgeLinkRepository bridges;
    private final ConversationPhotoRepository photos;
    private final Fanout fanout;
    private final LiveHub live;

    public Conversations(ConversationRepository conversations, MembershipRepository members,
            InviteLinkRepository invites, ProfileRepository profiles, DeviceRepository devices,
            BridgeLinkRepository bridges, ConversationPhotoRepository photos, Fanout fanout,
            LiveHub live) {
        this.conversations = conversations;
        this.members = members;
        this.invites = invites;
        this.profiles = profiles;
        this.devices = devices;
        this.bridges = bridges;
        this.photos = photos;
        this.fanout = fanout;
        this.live = live;
    }

    // ------------------------------------------------------------ reading

    @Transactional(readOnly = true)
    public List<ConversationDto> list(String username) throws IOException {
        List<Membership> mine = members.findByUsername(username);
        List<ConversationDto> out = new ArrayList<ConversationDto>();
        for (int iter = 0; iter < mine.size(); iter++) {
            Conversation row = conversations.findById(mine.get(iter).getConversationId());
            if (row != null) {
                out.add(describe(row, mine.get(iter)));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ConversationDto get(String username, String id) throws IOException {
        Membership mine = member(id, username);
        return describe(conversation(id), mine);
    }

    /// Every device of every member: the devices a message to the conversation
    /// has to be encrypted for.
    @Transactional(readOnly = true)
    public List<DeviceAddressDto> devices(String username, String id) {
        member(id, username);
        List<Device> rows = devices.findByUsernameIn(usernames(id));
        List<DeviceAddressDto> out = new ArrayList<DeviceAddressDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(new DeviceAddressDto(rows.get(iter).getUsername(), rows.get(iter).getId()));
        }
        return out;
    }

    /// The usernames of a conversation's members.
    public List<String> usernames(String id) {
        List<Membership> rows = members.findByConversationId(id);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).getUsername());
        }
        return out;
    }

    /// The caller's membership, or a 404: a conversation one is not in does
    /// not exist, as far as one can tell.
    public Membership member(String id, String username) {
        Membership row = id == null ? null : members.findByConversationIdAndUsername(id, username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such conversation");
        }
        return row;
    }

    public Conversation conversation(String id) {
        Conversation row = conversations.findById(id);
        if (row == null) {
            throw new ResponseStatusException(404, "No such conversation");
        }
        return row;
    }

    /// What a conversation is bridged to: TELEGRAM, WHATSAPP, BOT, or empty
    /// for an end-to-end encrypted one.
    public String bridge(String id) {
        List<BridgeLink> links = bridges.findByConversationId(id);
        if (!links.isEmpty()) {
            return links.get(0).getNetwork();
        }
        return members.findByConversationIdAndUsername(id, BOT) != null ? "BOT" : "";
    }

    // ------------------------------------------------------------ creating

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto create(String username, NewConversationDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to create");
        }
        String kind = Text.oneOf(request.kind(), new String[] {DIRECT, GROUP, CHANNEL}, DIRECT,
                "the kind of chat");
        List<String> wanted = new ArrayList<String>();
        for (int iter = 0; request.members() != null && iter < request.members().size(); iter++) {
            String each = request.members().get(iter);
            if (each != null && !each.equals(username) && !wanted.contains(each)) {
                if (profiles.findById(each) == null) {
                    throw new ResponseStatusException(404, "No account " + each);
                }
                wanted.add(each);
            }
        }
        long now = System.currentTimeMillis();
        if (DIRECT.equals(kind)) {
            if (wanted.size() != 1) {
                throw new ResponseStatusException(400, "A direct chat is with one other person");
            }
            String other = wanted.get(0);
            String key = username.compareTo(other) < 0 ? username + " " + other
                    : other + " " + username;
            Conversation existing = conversations.findByDirectKey(key);
            if (existing != null) {
                Membership mine = members.findByConversationIdAndUsername(existing.getId(), username);
                return describe(existing, mine);
            }
            Conversation row = conversation(kind, "", username, now);
            row.setDirectKey(key);
            conversations.insert(row);
            join(row.getId(), username, MEMBER, now);
            join(row.getId(), other, MEMBER, now);
            return describe(row, members.findByConversationIdAndUsername(row.getId(), username));
        }
        if (wanted.size() + 1 > MAX_MEMBERS) {
            throw new ResponseStatusException(400, "A group has at most " + MAX_MEMBERS + " members");
        }
        String title = Text.required(request.title(), 120, "the group's name");
        Conversation row = conversation(kind, title, username, now);
        row.setAdminsOnly(CHANNEL.equals(kind));
        conversations.insert(row);
        join(row.getId(), username, OWNER, now);
        for (int iter = 0; iter < wanted.size(); iter++) {
            join(row.getId(), wanted.get(iter), MEMBER, now);
        }
        notice(row, name(username) + " created \"" + title + "\"");
        changed(row.getId(), usernames(row.getId()));
        return describe(row, members.findByConversationIdAndUsername(row.getId(), username));
    }

    // ------------------------------------------------------------ changing

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto edit(String username, String id, ConversationEditDto edit)
            throws IOException {
        if (edit == null) {
            throw new ResponseStatusException(400, "Nothing to change");
        }
        conversations.hold(id);
        Membership mine = member(id, username);
        Conversation row = conversation(id);
        boolean group = !DIRECT.equals(row.getKind());
        if (group && !admin(mine)) {
            throw new ResponseStatusException(403, "Only admins change the group");
        }
        int timer = edit.disappearingSeconds();
        boolean known = false;
        for (int iter = 0; iter < TIMERS.length; iter++) {
            known |= TIMERS[iter] == timer;
        }
        if (!known) {
            throw new ResponseStatusException(400, "That is not a disappearing-message timer");
        }
        List<String> said = new ArrayList<String>();
        if (group) {
            String title = Text.required(edit.title(), 120, "the group's name");
            if (!title.equals(row.getTitle())) {
                said.add(name(username) + " renamed the group to \"" + title + "\"");
            }
            row.setTitle(title);
            row.setAbout(Text.optional(edit.about(), 255, "the description"));
            if (edit.adminsOnly() != row.isAdminsOnly()) {
                said.add(edit.adminsOnly() ? "Only admins can send messages now"
                        : "Everyone can send messages now");
            }
            row.setAdminsOnly(edit.adminsOnly());
        }
        if (timer != row.getDisappearingSeconds()) {
            said.add(timer == 0 ? name(username) + " turned off disappearing messages"
                    : name(username) + " set messages to disappear after " + span(timer));
        }
        row.setDisappearingSeconds(timer);
        for (int iter = 0; iter < said.size(); iter++) {
            notice(row, said.get(iter));
        }
        changed(id, usernames(id));
        return describe(row, mine);
    }

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto flags(String username, String id, ConversationFlagsDto flags)
            throws IOException {
        Membership mine = member(id, username);
        if (flags != null) {
            mine.setMuted(flags.muted());
            mine.setArchived(flags.archived());
            mine.setPinned(flags.pinned());
        }
        return describe(conversation(id), mine);
    }

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto addMember(String username, String id, MemberChangeDto change)
            throws IOException {
        conversations.hold(id);
        Membership mine = member(id, username);
        Conversation row = group(id);
        if (!admin(mine)) {
            throw new ResponseStatusException(403, "Only admins add members");
        }
        String who = change == null ? null : change.username();
        if (who == null || profiles.findById(who) == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        if (members.findByConversationIdAndUsername(id, who) != null) {
            return describe(row, mine);
        }
        if (members.countByConversationId(id) >= MAX_MEMBERS) {
            throw new ResponseStatusException(409, "The group is full");
        }
        join(id, who, ADMIN.equals(change.role()) ? ADMIN : MEMBER, System.currentTimeMillis());
        notice(row, name(username) + " added " + name(who));
        changed(id, usernames(id));
        return describe(row, mine);
    }

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto setRole(String username, String id, String who, MemberChangeDto change)
            throws IOException {
        conversations.hold(id);
        Membership mine = member(id, username);
        Conversation row = group(id);
        if (!admin(mine)) {
            throw new ResponseStatusException(403, "Only admins change roles");
        }
        Membership theirs = member(id, who);
        if (OWNER.equals(theirs.getRole())) {
            throw new ResponseStatusException(409, "The owner's role cannot be changed");
        }
        String role = Text.oneOf(change == null ? null : change.role(),
                new String[] {ADMIN, MEMBER}, MEMBER, "the role");
        if (!role.equals(theirs.getRole())) {
            theirs.setRole(role);
            notice(row, ADMIN.equals(role) ? name(who) + " is now an admin"
                    : name(who) + " is no longer an admin");
            changed(id, usernames(id));
        }
        return describe(row, mine);
    }

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto removeMember(String username, String id, String who)
            throws IOException {
        conversations.hold(id);
        Membership mine = member(id, username);
        Conversation row = group(id);
        if (!admin(mine)) {
            throw new ResponseStatusException(403, "Only admins remove members");
        }
        Membership theirs = member(id, who);
        if (OWNER.equals(theirs.getRole())) {
            throw new ResponseStatusException(409, "The owner cannot be removed");
        }
        List<String> before = usernames(id);
        members.deleteById(theirs.getId());
        notice(row, name(username) + " removed " + name(who));
        changed(id, before);
        return describe(row, mine);
    }

    /// Leaves a group. A direct chat cannot be left, only archived: the other
    /// person can always write again, unless they are blocked.
    @Transactional(rollbackFor = IOException.class)
    public ConversationDto leave(String username, String id) throws IOException {
        conversations.hold(id);
        Membership mine = member(id, username);
        Conversation row = group(id);
        ConversationDto was = describe(row, mine);
        List<String> before = usernames(id);
        depart(row, mine);
        changed(id, before);
        return was;
    }

    /// Takes an account out of every conversation, for its closure.
    public void removeFromAll(String username) throws IOException {
        List<Membership> mine = members.findByUsername(username);
        for (int iter = 0; iter < mine.size(); iter++) {
            Conversation row = conversations.findById(mine.get(iter).getConversationId());
            if (row != null) {
                depart(row, mine.get(iter));
            }
        }
        members.deleteByUsername(username);
    }

    private void depart(Conversation row, Membership mine) throws IOException {
        String id = row.getId();
        members.deleteById(mine.getId());
        List<Membership> rest = members.findByConversationId(id);
        if (rest.isEmpty()) {
            erase(id);
            return;
        }
        if (OWNER.equals(mine.getRole())) {
            // The longest-serving admin takes over, or failing one, the
            // longest-serving member: a group is never left without an owner.
            Membership heir = null;
            for (int iter = 0; iter < rest.size(); iter++) {
                Membership each = rest.get(iter);
                if (heir == null || rank(each) > rank(heir)
                        || rank(each) == rank(heir) && each.getJoinedAt() < heir.getJoinedAt()) {
                    heir = each;
                }
            }
            heir.setRole(OWNER);
            notice(row, name(mine.getUsername()) + " left; " + name(heir.getUsername())
                    + " is the owner now");
        } else if (!DIRECT.equals(row.getKind())) {
            notice(row, name(mine.getUsername()) + " left");
        }
    }

    private void erase(String id) {
        invites.deleteByConversationId(id);
        bridges.deleteByConversationId(id);
        members.deleteByConversationId(id);
        if (photos.existsById(id)) {
            photos.deleteById(id);
        }
        conversations.deleteById(id);
    }

    // ------------------------------------------------------------ invites

    @Transactional(rollbackFor = IOException.class)
    public InviteDto invite(String username, String id) throws IOException {
        Membership mine = member(id, username);
        Conversation row = group(id);
        if (!admin(mine)) {
            throw new ResponseStatusException(403, "Only admins make invite links");
        }
        long now = System.currentTimeMillis();
        InviteLink link = new InviteLink();
        link.setCode(Ids.next());
        link.setConversationId(id);
        link.setCreatedBy(username);
        link.setCreatedAt(now);
        link.setExpiresAt(now + INVITE_LIFE);
        invites.insert(link);
        return toDto(link, row, members.countByConversationId(id));
    }

    @Transactional(readOnly = true)
    public InviteDto preview(String code) {
        InviteLink link = link(code);
        return toDto(link, conversation(link.getConversationId()),
                members.countByConversationId(link.getConversationId()));
    }

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto join(String username, String code) throws IOException {
        InviteLink link = link(code);
        String id = link.getConversationId();
        conversations.hold(id);
        Conversation row = conversation(id);
        Membership mine = members.findByConversationIdAndUsername(id, username);
        if (mine == null) {
            if (members.countByConversationId(id) >= MAX_MEMBERS) {
                throw new ResponseStatusException(409, "The group is full");
            }
            join(id, username, MEMBER, System.currentTimeMillis());
            notice(row, name(username) + " joined using an invite link");
            changed(id, usernames(id));
            mine = members.findByConversationIdAndUsername(id, username);
        }
        return describe(row, mine);
    }

    private InviteLink link(String code) {
        InviteLink link = code == null ? null : invites.findById(code);
        if (link == null || link.getExpiresAt() > 0
                && link.getExpiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(404, "That invite link does not work any more");
        }
        return link;
    }

    // ------------------------------------------------------------ photos

    @Transactional(rollbackFor = IOException.class)
    public ConversationDto savePhoto(String username, String id, PhotoDto photo)
            throws IOException {
        Membership mine = member(id, username);
        Conversation row = group(id);
        if (!admin(mine)) {
            throw new ResponseStatusException(403, "Only admins change the photo");
        }
        byte[] data = Photos.image(photo);
        ConversationPhoto stored = photos.findById(id);
        if (stored == null) {
            stored = new ConversationPhoto();
            stored.setConversationId(id);
            stored.setContentType(photo.contentType());
            stored.setUpdatedAt(System.currentTimeMillis());
            stored.setData(data);
            photos.insert(stored);
        } else {
            stored.setContentType(photo.contentType());
            stored.setUpdatedAt(System.currentTimeMillis());
            stored.setData(data);
        }
        row.setHasPhoto(true);
        changed(id, usernames(id));
        return describe(row, mine);
    }

    @Transactional(readOnly = true)
    public PhotoDto photo(String username, String id) {
        member(id, username);
        ConversationPhoto stored = photos.findById(id);
        if (stored == null) {
            throw new ResponseStatusException(404, "No photo");
        }
        return new PhotoDto(stored.getContentType(), Base64.encode(stored.getData()));
    }

    // ------------------------------------------------------------ helpers

    private Conversation group(String id) {
        Conversation row = conversation(id);
        if (DIRECT.equals(row.getKind())) {
            throw new ResponseStatusException(400, "That is a direct chat, not a group");
        }
        return row;
    }

    private static Conversation conversation(String kind, String title, String by, long now)
            throws IOException {
        Conversation row = new Conversation();
        row.setId(Ids.next());
        row.setKind(kind);
        row.setTitle(title);
        row.setCreatedBy(by);
        row.setCreatedAt(now);
        row.setLastActivityAt(now);
        // direct_key is unique, so it is what keeps two people to one direct
        // chat even when both start it at once: the second insert fails rather
        // than splitting their history. Every other conversation has a key of
        // its own that no pair of usernames can spell.
        row.setDirectKey("#" + row.getId());
        return row;
    }

    private void join(String id, String username, String role, long now) {
        Membership row = new Membership();
        row.setId(membershipId(id, username));
        row.setConversationId(id);
        row.setUsername(username);
        row.setRole(role);
        row.setJoinedAt(now);
        members.insert(row);
    }

    static String membershipId(String conversationId, String username) {
        try {
            return Ids.sha256(conversationId + "/" + username);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static boolean admin(Membership row) {
        return OWNER.equals(row.getRole()) || ADMIN.equals(row.getRole());
    }

    private static int rank(Membership row) {
        return ADMIN.equals(row.getRole()) ? 1 : 0;
    }

    /// Posts a notice from the server into a conversation and tells its
    /// members' apps to fetch it again: what a bridge being linked or removed
    /// says, since it changes how their apps must send.
    public void announce(String id, String text) throws IOException {
        Conversation row = conversation(id);
        List<String> to = usernames(id);
        notice(row, text);
        changed(id, to);
    }

    private void notice(Conversation row, String text) throws IOException {
        fanout.notice(row.getId(), usernames(row.getId()), text, 0L,
                !DIRECT.equals(row.getKind()));
    }

    /// Tells the members' apps to fetch the conversation again.
    private void changed(String id, List<String> to) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "conversation");
        frame.put("id", id);
        live.toUsers(to, frame);
    }

    private String name(String username) {
        Profile profile = profiles.findById(username);
        return profile == null ? username : profile.getDisplayName();
    }

    static String span(int seconds) {
        if (seconds % 604800 == 0) {
            return plural(seconds / 604800, "week");
        }
        if (seconds % 86400 == 0) {
            return plural(seconds / 86400, "day");
        }
        if (seconds % 3600 == 0) {
            return plural(seconds / 3600, "hour");
        }
        if (seconds % 60 == 0) {
            return plural(seconds / 60, "minute");
        }
        return plural(seconds, "second");
    }

    private static String plural(int n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }

    ConversationDto describe(Conversation row, Membership mine) {
        List<Membership> rows = members.findByConversationId(row.getId());
        List<String> names = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            names.add(rows.get(iter).getUsername());
        }
        Map<String, String> display = new HashMap<String, String>();
        List<Profile> found = names.isEmpty() ? new ArrayList<Profile>()
                : profiles.findByUsernameIn(names);
        for (int iter = 0; iter < found.size(); iter++) {
            display.put(found.get(iter).getUsername(), found.get(iter).getDisplayName());
        }
        List<MemberDto> out = new ArrayList<MemberDto>();
        String title = row.getTitle();
        for (int iter = 0; iter < rows.size(); iter++) {
            Membership each = rows.get(iter);
            String shown = display.get(each.getUsername());
            if (shown == null) {
                shown = each.getUsername();
            }
            out.add(new MemberDto(each.getUsername(), shown, each.getRole()));
            if (DIRECT.equals(row.getKind()) && !each.getUsername().equals(mine.getUsername())) {
                title = shown;
            }
        }
        return new ConversationDto(row.getId(), row.getKind(), title, row.getAbout(), out,
                mine.getRole(), mine.isMuted(), mine.isArchived(), mine.isPinned(),
                row.getDisappearingSeconds(), row.isAdminsOnly(), bridge(row.getId()),
                row.getLastActivityAt(), row.isHasPhoto());
    }

    private static InviteDto toDto(InviteLink link, Conversation row, long count) {
        return new InviteDto(link.getCode(), row.getId(), row.getTitle(), (int) count,
                link.getExpiresAt());
    }
}
