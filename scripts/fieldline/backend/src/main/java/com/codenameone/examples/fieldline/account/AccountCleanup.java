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
package com.codenameone.examples.fieldline.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.repository.LiveTicketRepository;
import com.codenameone.examples.fieldline.repository.PreferenceRepository;
import com.codenameone.examples.fieldline.repository.PushDeviceRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;

/// What closing an account does to the rows other parts of the server keep
/// under its name. It has no transaction of its own: [Accounts] calls it
/// inside one.
@Component
public class AccountCleanup {
    private final PreferenceRepository preferences;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;
    private final TechnicianRepository technicians;

    public AccountCleanup(PreferenceRepository preferences, LiveTicketRepository tickets,
            PushDeviceRepository pushDevices, TechnicianRepository technicians) {
        this.preferences = preferences;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
        this.technicians = technicians;
    }

    /// Deletes what an account leaves in the tables keyed by it, and unlinks
    /// the technician it signed in as. The technician stays, with the history
    /// of the work they did: a job's record belongs to the customer too.
    public void removeEverythingOf(String username) {
        preferences.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
        Technician tech = technicians.findByUsername(username);
        if (tech != null) {
            tech.setUsername("");
            tech.setActive(false);
            technicians.save(tech);
        }
    }
}
