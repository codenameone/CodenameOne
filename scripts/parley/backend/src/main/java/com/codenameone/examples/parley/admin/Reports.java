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
package com.codenameone.examples.parley.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.account.Moderation;
import com.codenameone.examples.parley.api.ReportViewDto;
import com.codenameone.examples.parley.contact.Contacts;
import com.codenameone.examples.parley.domain.Report;
import com.codenameone.examples.parley.repository.ReportRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The reports users file about each other, as the admins work through them.
///
/// A report carries only what its reporter chose to send: the server holds
/// no message it could quote, so the evidence is whatever text the reporter's
/// own device decrypted and attached.
@Component
public class Reports {
    static final String RESOLVED = "RESOLVED";

    private final ReportRepository reports;
    private final Moderation moderation;

    public Reports(ReportRepository reports, Moderation moderation) {
        this.reports = reports;
        this.moderation = moderation;
    }

    /// The reports in `status`, newest first; every report for an empty one.
    @Transactional(readOnly = true)
    public List<ReportViewDto> list(String status) throws IOException {
        List<Report> rows = status == null || status.length() == 0 ? reports.newestFirst()
                : reports.findByStatusOrderByCreatedAtDesc(status);
        List<ReportViewDto> out = new ArrayList<>();
        for (Report row : rows) {
            out.add(Contacts.toDto(row));
        }
        return out;
    }

    /// Closes a report with what was done about it, and records that against
    /// the account it was about.
    @Transactional(rollbackFor = IOException.class)
    public ReportViewDto resolve(String by, String id, String outcome) throws IOException {
        Report row = reports.findById(id);
        if (row == null) {
            throw new ResponseStatusException(404, "There is no such report");
        }
        String said = Text.required(outcome, 255, "what was done about the report");
        row.setStatus(RESOLVED);
        row.setResolvedBy(by);
        row.setOutcome(said);
        moderation.record(row.getSubject(), by, Moderation.REPORT, said);
        return Contacts.toDto(row);
    }
}
