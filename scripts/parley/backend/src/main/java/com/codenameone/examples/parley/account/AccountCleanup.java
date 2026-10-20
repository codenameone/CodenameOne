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
package com.codenameone.examples.parley.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.domain.Device;
import com.codenameone.examples.parley.repository.CallLineRepository;
import com.codenameone.examples.parley.repository.CallRecordRepository;
import com.codenameone.examples.parley.repository.ContactBlockRepository;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.EnvelopeRepository;
import com.codenameone.examples.parley.repository.LiveTicketRepository;
import com.codenameone.examples.parley.repository.MediaBlobRepository;
import com.codenameone.examples.parley.repository.MessageLogRepository;
import com.codenameone.examples.parley.repository.OneTimePreKeyRepository;
import com.codenameone.examples.parley.repository.PhoneCodeRepository;
import com.codenameone.examples.parley.repository.PreferenceRepository;
import com.codenameone.examples.parley.repository.ProfilePhotoRepository;
import com.codenameone.examples.parley.repository.PushDeviceRepository;
import com.codenameone.examples.parley.repository.ReportRepository;

import java.io.IOException;
import java.util.List;

/// What closing an account does to the rows other parts of the server keep
/// under its name.
///
/// These are statements over whole sets of rows -- everything of one account
/// in a table -- and not the work of the services that own those tables, which
/// is why they are gathered here. Each deletion is a bulk statement, so
/// whatever the session had loaded before it is stale afterwards and must be
/// read again before it is used. It has no transaction of its own: [Accounts]
/// calls it inside one.
@Component
public class AccountCleanup {
    private final DeviceRepository devices;
    private final OneTimePreKeyRepository preKeys;
    private final EnvelopeRepository envelopes;
    private final Conversations conversations;
    private final ContactBlockRepository blocks;
    private final ReportRepository reports;
    private final MediaBlobRepository media;
    private final ProfilePhotoRepository photos;
    private final PushDeviceRepository pushDevices;
    private final CallRecordRepository calls;
    private final CallLineRepository lines;
    private final MessageLogRepository log;
    private final PreferenceRepository preferences;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;

    public AccountCleanup(DeviceRepository devices, OneTimePreKeyRepository preKeys,
            EnvelopeRepository envelopes, Conversations conversations,
            ContactBlockRepository blocks, ReportRepository reports, MediaBlobRepository media,
            ProfilePhotoRepository photos, PushDeviceRepository pushDevices,
            CallRecordRepository calls, CallLineRepository lines, MessageLogRepository log,
            PreferenceRepository preferences, PhoneCodeRepository codes,
            LiveTicketRepository tickets) {
        this.devices = devices;
        this.preKeys = preKeys;
        this.envelopes = envelopes;
        this.conversations = conversations;
        this.blocks = blocks;
        this.reports = reports;
        this.media = media;
        this.photos = photos;
        this.pushDevices = pushDevices;
        this.calls = calls;
        this.lines = lines;
        this.log = log;
        this.preferences = preferences;
        this.codes = codes;
        this.tickets = tickets;
    }

    /// Deletes everything kept under the account but its profile and sign-in,
    /// which [Accounts] deletes itself.
    ///
    /// Reports the account was the subject of are kept: they are the
    /// reporter's, and the admins' record of why the account was acted on.
    /// Envelopes it sent that are still waiting for someone are kept too --
    /// they are the recipient's messages now.
    public void removeEverythingOf(String username) throws IOException {
        // Leaving every conversation first: it hands a group on to another
        // admin, and says so to the members still in it.
        conversations.removeFromAll(username);
        List<Device> held = devices.findByUsername(username);
        for (int iter = 0; iter < held.size(); iter++) {
            preKeys.deleteByDeviceId(held.get(iter).getId());
        }
        devices.deleteByUsername(username);
        envelopes.deleteByRecipient(username);
        blocks.deleteByBlocker(username);
        blocks.deleteByBlocked(username);
        reports.deleteByReporter(username);
        media.deleteByOwner(username);
        if (photos.existsById(username)) {
            photos.deleteById(username);
        }
        pushDevices.deleteByUsername(username);
        calls.deleteOf(username);
        if (lines.existsById(username)) {
            lines.deleteById(username);
        }
        log.deleteBySender(username);
        preferences.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
    }
}
