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
package com.codenameone.examples.parley.contact;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.api.ContactDto;
import com.codenameone.examples.parley.api.PhotoDto;
import com.codenameone.examples.parley.api.ReportDto;
import com.codenameone.examples.parley.api.ReportViewDto;
import com.codenameone.examples.parley.chat.Messages;
import com.codenameone.examples.parley.chat.Photos;
import com.codenameone.examples.parley.domain.ContactBlock;
import com.codenameone.examples.parley.domain.Membership;
import com.codenameone.examples.parley.domain.Preference;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.domain.ProfilePhoto;
import com.codenameone.examples.parley.domain.Report;
import com.codenameone.examples.parley.live.LiveHub;
import com.codenameone.examples.parley.repository.ContactBlockRepository;
import com.codenameone.examples.parley.repository.MembershipRepository;
import com.codenameone.examples.parley.repository.PreferenceRepository;
import com.codenameone.examples.parley.repository.ProfilePhotoRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;
import com.codenameone.examples.parley.repository.ReportRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Other people, as the privacy settings let one see them, and the controls
/// between accounts: blocking and reporting.
///
/// "Contacts" here are the people one shares a conversation with. Last seen
/// and the profile photo are each shown to everyone, to contacts or to nobody,
/// as their owner chose; and, as in the messengers people know, somebody who
/// hides their own last seen does not see anybody else's.
@Component
public class Contacts {
    public static final String EVERYONE = "everyone";
    public static final String CONTACTS = "contacts";
    public static final String NOBODY = "nobody";

    private final ProfileRepository profiles;
    private final PreferenceRepository preferences;
    private final MembershipRepository members;
    private final ContactBlockRepository blocks;
    private final ReportRepository reports;
    private final ProfilePhotoRepository photos;
    private final LiveHub live;

    public Contacts(ProfileRepository profiles, PreferenceRepository preferences,
            MembershipRepository members, ContactBlockRepository blocks, ReportRepository reports,
            ProfilePhotoRepository photos, LiveHub live) {
        this.profiles = profiles;
        this.preferences = preferences;
        this.members = members;
        this.blocks = blocks;
        this.reports = reports;
        this.photos = photos;
        this.live = live;
    }

    /// Accounts whose name or e-mail starts with `query`. At least three
    /// characters, and at most twenty answers: a directory is for finding
    /// someone, not for listing everyone.
    @Transactional(readOnly = true)
    public List<ContactDto> search(String viewer, String query) throws IOException {
        String text = query == null ? "" : query.trim();
        if (text.length() < 3) {
            return new ArrayList<ContactDto>();
        }
        if (text.length() > 120 || text.indexOf('%') >= 0 || text.indexOf('_') >= 0) {
            throw new ResponseStatusException(400, "Search for a name or an e-mail");
        }
        List<Profile> found = profiles.search(text + "%");
        List<String> peers = peers(viewer);
        List<ContactDto> out = new ArrayList<ContactDto>();
        for (int iter = 0; iter < found.size() && out.size() < 20; iter++) {
            Profile each = found.get(iter);
            if (!each.getUsername().equals(viewer)) {
                out.add(view(viewer, each, peers));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ContactDto contact(String viewer, String username) throws IOException {
        Profile profile = username == null ? null : profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return view(viewer, profile, peers(viewer));
    }

    // ------------------------------------------------------------ blocks

    @Transactional(readOnly = true)
    public List<ContactDto> blocked(String viewer) throws IOException {
        List<ContactBlock> rows = blocks.findByBlocker(viewer);
        List<String> peers = peers(viewer);
        List<ContactDto> out = new ArrayList<ContactDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Profile profile = profiles.findById(rows.get(iter).getBlocked());
            if (profile != null) {
                out.add(view(viewer, profile, peers));
            }
        }
        return out;
    }

    /// Blocks an account: its messages to the caller are dropped, unseen, and
    /// it no longer sees the caller's last seen or photo.
    @Transactional(rollbackFor = IOException.class)
    public ContactDto block(String viewer, String username) throws IOException {
        Profile profile = username == null ? null : profiles.findById(username);
        if (profile == null || username.equals(viewer)) {
            throw new ResponseStatusException(404, "No such account");
        }
        String id = Messages.blockId(viewer, username);
        if (!blocks.existsById(id)) {
            ContactBlock row = new ContactBlock();
            row.setId(id);
            row.setBlocker(viewer);
            row.setBlocked(username);
            row.setCreatedAt(Instant.now());
            blocks.insert(row);
        }
        return view(viewer, profile, peers(viewer));
    }

    @Transactional(rollbackFor = IOException.class)
    public ContactDto unblock(String viewer, String username) throws IOException {
        Profile profile = username == null ? null : profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        String id = Messages.blockId(viewer, username);
        if (blocks.existsById(id)) {
            blocks.deleteById(id);
        }
        return view(viewer, profile, peers(viewer));
    }

    // ------------------------------------------------------------ reports

    /// Files a report. The server cannot read the conversation, so the
    /// evidence is whatever the reporter chose to decrypt and attach.
    @Transactional(rollbackFor = IOException.class)
    public ReportViewDto report(String reporter, ReportDto report) throws IOException {
        if (report == null || report.username() == null
                || profiles.findById(report.username()) == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        if (report.username().equals(reporter)) {
            throw new ResponseStatusException(400, "You cannot report yourself");
        }
        Report row = new Report();
        row.setId(Ids.next());
        row.setReporter(reporter);
        row.setSubject(report.username());
        row.setConversationId(Text.optional(report.conversationId(), 32, "the conversation"));
        row.setReason(Text.required(report.reason(), 255, "why you are reporting them"));
        row.setEvidence(Text.optional(report.evidence(), 4000, "the messages you attach"));
        row.setCreatedAt(Instant.now());
        row.setStatus("OPEN");
        reports.insert(row);
        java.util.Map<String, Object> frame = new java.util.LinkedHashMap<String, Object>();
        frame.put("type", "admin");
        frame.put("what", "report");
        live.toAdmins(frame);
        return toDto(row);
    }

    /// Everyone who shares a conversation with `username`: the people the
    /// "contacts" audience of a privacy setting means.
    private List<String> peers(String username) {
        List<String> ids = new ArrayList<String>();
        for (Membership mine : members.findByUsername(username)) {
            ids.add(mine.getConversationId());
        }
        List<String> out = new ArrayList<String>();
        if (ids.isEmpty()) {
            return out;
        }
        Set<String> seen = new HashSet<String>();
        for (Membership other : members.findByConversationIdIn(ids)) {
            if (!other.getUsername().equals(username) && seen.add(other.getUsername())) {
                out.add(other.getUsername());
            }
        }
        return out;
    }

    public static ReportViewDto toDto(Report row) {
        return new ReportViewDto(row.getId(), row.getReporter(), row.getSubject(),
                row.getConversationId(), row.getReason(), row.getEvidence(), row.getCreatedAt().toEpochMilli(),
                row.getStatus());
    }

    // ------------------------------------------------------------ photos

    @Transactional(rollbackFor = IOException.class)
    public void savePhoto(String username, PhotoDto photo) throws IOException {
        byte[] data = Photos.image(photo);
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        ProfilePhoto row = photos.findById(username);
        if (row == null) {
            row = new ProfilePhoto();
            row.setUsername(username);
            row.setContentType(photo.contentType());
            row.setUpdatedAt(Instant.now());
            row.setData(data);
            photos.insert(row);
        } else {
            row.setContentType(photo.contentType());
            row.setUpdatedAt(Instant.now());
            row.setData(data);
        }
        profile.setHasPhoto(true);
    }

    @Transactional(readOnly = true)
    public PhotoDto photo(String viewer, String username) throws IOException {
        Profile profile = username == null ? null : profiles.findById(username);
        ProfilePhoto row = profile == null ? null : photos.findById(username);
        if (row == null || !viewer.equals(username)
                && !allowed(viewer, profile, photoSetting(username), peers(viewer))) {
            throw new ResponseStatusException(404, "No photo");
        }
        return new PhotoDto(row.getContentType(), Base64.encode(row.getData()));
    }

    // ------------------------------------------------------------ privacy

    private ContactDto view(String viewer, Profile profile, List<String> peers)
            throws IOException {
        String username = profile.getUsername();
        Preference theirs = preferences.findById(username);
        Preference mine = preferences.findById(viewer);
        long lastSeen = 0L;
        boolean reciprocal = mine == null || !NOBODY.equals(mine.getLastSeen());
        if (reciprocal && allowed(viewer, profile,
                theirs == null ? EVERYONE : theirs.getLastSeen(), peers)) {
            lastSeen = live.online(username) ? -1L : profile.getLastSeenAt().toEpochMilli();
        }
        boolean photo = profile.isHasPhoto()
                && allowed(viewer, profile, photoSetting(username), peers);
        return new ContactDto(username, profile.getDisplayName(), profile.getAbout(), photo,
                lastSeen, blocks.existsById(Messages.blockId(viewer, username)));
    }

    private String photoSetting(String username) {
        Preference theirs = preferences.findById(username);
        return theirs == null ? EVERYONE : theirs.getProfilePhoto();
    }

    /// Whether `viewer` may see something its owner shared with `setting`. A
    /// blocked viewer sees nothing.
    private boolean allowed(String viewer, Profile owner, String setting, List<String> peers)
            throws IOException {
        if (blocks.existsById(Messages.blockId(owner.getUsername(), viewer))) {
            return false;
        }
        if (NOBODY.equals(setting)) {
            return false;
        }
        return !CONTACTS.equals(setting) || peers.contains(owner.getUsername());
    }
}
