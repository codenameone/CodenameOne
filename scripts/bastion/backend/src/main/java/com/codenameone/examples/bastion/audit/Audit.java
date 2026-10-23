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
package com.codenameone.examples.bastion.audit;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.ChainReportDto;
import com.codenameone.examples.bastion.domain.AuditEvent;
import com.codenameone.examples.bastion.domain.AuditHead;
import com.codenameone.examples.bastion.repository.AuditEventRepository;
import com.codenameone.examples.bastion.repository.AuditHeadRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The tamper-evident log of everything that matters: sign-ins, enrolments,
/// every movement of money, every admin action.
///
/// An event is appended in the transaction of the change it records, so the
/// two commit or roll back together -- there is no transfer without its audit
/// row and no audit row for a transfer that did not happen. Each row's hash
/// covers its own fields and the hash of the row before it.
@Component
public class Audit {
    /// The hash the chain starts from.
    public static final String GENESIS =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditEventRepository events;
    private final AuditHeadRepository head;

    public Audit(AuditEventRepository events, AuditHeadRepository head) {
        this.events = events;
        this.head = head;
    }

    /// Records `action` by the current caller, or by `actor` outside a request.
    @Transactional(rollbackFor = Exception.class)
    public AuditEvent append(String actor, String action, String subject, String detail)
            throws IOException {
        if (head.hold() != 1) {
            throw new IllegalStateException("audit_head is missing; V3__audit.sql creates it");
        }
        AuditHead tail = head.findById(AuditHeadRepository.ONLY);
        AuditEvent event = new AuditEvent();
        event.setSeq(tail.getSeq() + 1);
        event.setAt(System.currentTimeMillis());
        event.setActor(clip(actor, 190));
        event.setAction(clip(action, 40));
        event.setSubject(clip(subject, 190));
        event.setDetail(clip(detail, 500));
        event.setAddress(clip(Caller.address(), 64));
        event.setPrevHash(tail.getHash());
        event.setHash(hash(event));
        events.insert(event);
        head.advance(event.getSeq(), event.getHash());
        // The update above bypasses the session, whose copy of the head would
        // otherwise still say the old seq: a second append in the same
        // transaction -- an order and its trades -- would reuse it.
        tail.setSeq(event.getSeq());
        tail.setHash(event.getHash());
        return event;
    }

    /// Walks the whole chain and answers the sequence numbers that do not
    /// hold: a row whose hash is not its own, a row that does not link to the
    /// one before it, a missing number, or a head that disagrees with the
    /// last row. Empty means intact.
    ///
    /// Page by page, each in its own read, so memory stays one page however
    /// long the chain grows (a whole-table read held every row, and the ORM
    /// session a copy of each). The head is read first and the walk stops at
    /// it: rows appended while the walk runs are past the head it compares
    /// against, and are checked on the next run.
    public List<String> verify() throws IOException {
        return report().problems();
    }

    /// [#verify] with what an auditor needs to act on it: how many links
    /// were checked, the head, and the FIRST link that does not hold (0 when
    /// none). Everything after a broken link is suspect -- its hash was
    /// computed from a predecessor that is no longer what was written -- so
    /// the first break is where an investigation starts.
    public ChainReportDto report() throws IOException {
        List<String> problems = new ArrayList<String>();
        long firstBroken = 0;
        long checked = 0;
        AuditHead tail = tail();
        long end = tail == null ? Long.MAX_VALUE : tail.getSeq();
        String prev = GENESIS;
        long expected = 1;
        long after = 0;
        boolean more = true;
        while (more) {
            List<AuditEvent> page = page(after);
            more = !page.isEmpty();
            for (AuditEvent e : page) {
                if (e.getSeq() > end) {
                    more = false;
                    break;
                }
                checked++;
                int before = problems.size();
                if (e.getSeq() != expected) {
                    problems.add("missing #" + expected + " before #" + e.getSeq());
                }
                if (!prev.equals(e.getPrevHash())) {
                    problems.add("#" + e.getSeq() + " does not link to the row before it");
                }
                if (!hash(e).equals(e.getHash())) {
                    problems.add("#" + e.getSeq() + " was changed after it was written");
                }
                if (firstBroken == 0 && problems.size() > before) {
                    firstBroken = e.getSeq() != expected ? expected : e.getSeq();
                }
                prev = e.getHash();
                expected = e.getSeq() + 1;
                after = e.getSeq();
            }
        }
        if (tail != null && (tail.getSeq() != expected - 1 || !tail.getHash().equals(prev))) {
            problems.add("the head says #" + tail.getSeq() + " but the chain ends at #"
                    + (expected - 1));
            if (firstBroken == 0) {
                firstBroken = expected;
            }
        }
        return new ChainReportDto(problems.isEmpty(), checked, tail == null ? 0L : tail.getSeq(),
                tail == null ? GENESIS : tail.getHash(), firstBroken, problems);
    }

    /// Links after `after`, oldest first, at most `limit`.
    @Transactional(readOnly = true)
    public List<AuditEvent> after(long after, int limit) throws IOException {
        List<AuditEvent> page = events.findTop500BySeqGreaterThanOrderBySeqAsc(after);
        return page.size() <= limit ? page : new ArrayList<AuditEvent>(page.subList(0, limit));
    }

    /// The hash an event should carry, for exports that let an auditor
    /// recompute the chain offline.
    public static String expectedHash(AuditEvent e) throws IOException {
        return hash(e);
    }

    @Transactional(readOnly = true)
    private AuditHead tail() throws IOException {
        return head.findById(AuditHeadRepository.ONLY);
    }

    @Transactional(readOnly = true)
    private List<AuditEvent> page(long after) throws IOException {
        return events.findTop500BySeqGreaterThanOrderBySeqAsc(after);
    }

    static String hash(AuditEvent e) throws IOException {
        // Length-prefixed fields: "ab"+"c" and "a"+"bc" must not hash alike.
        StringBuilder b = new StringBuilder();
        field(b, e.getPrevHash());
        field(b, Long.toString(e.getSeq()));
        field(b, Long.toString(e.getAt()));
        field(b, e.getActor());
        field(b, e.getAction());
        field(b, e.getSubject());
        field(b, e.getDetail());
        field(b, e.getAddress());
        return Ids.sha256(b.toString());
    }

    private static void field(StringBuilder b, String value) {
        String v = value == null ? "" : value;
        b.append(v.length()).append(':').append(v).append(';');
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
