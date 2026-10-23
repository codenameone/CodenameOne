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
package com.codenameone.examples.bastion.books;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.api.AuditApiServer;
import com.codenameone.examples.bastion.api.AuditEventDto;
import com.codenameone.examples.bastion.api.ChainReportDto;
import com.codenameone.examples.bastion.api.EvidenceDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.JournalLegDto;
import com.codenameone.examples.bastion.api.TrialBalanceDto;
import com.codenameone.examples.bastion.api.TrialBalanceLineDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.AuditEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// [com.codenameone.examples.bastion.api.AuditApi], served: the chain, the
/// trail of one entity, the staff access log and exportable evidence. Staff
/// only; reading is recorded.
@Component
public class AuditTrailEndpoint implements AuditApiServer {
    /// Journal entries read per page of an export. The export itself has no
    /// cap: its digest goes on the chain as the whole journal, so it must be.
    static final int EXPORT_PAGE = 200;

    private final Audit audit;
    private final Books books;

    public AuditTrailEndpoint(Audit audit, Books books) {
        this.audit = audit;
        this.books = books;
    }

    @Override
    public ChainReportDto chain() throws Exception {
        String who = Staff.name();
        ChainReportDto report = audit.report();
        audit.append(who, "AUDIT_VERIFIED", "audit", report.intact() ? "intact"
                : "first broken link #" + report.firstBrokenSeq());
        return report;
    }

    @Override
    public List<AuditEventDto> events(String after, String limit) throws Exception {
        long a = Math.max(0L, BooksEndpoint.number(after, 0L));
        int n = BooksEndpoint.clamp(limit, 100, 500);
        Staff.read(audit, "audit", "events after #" + a);
        List<AuditEventDto> out = new ArrayList<AuditEventDto>();
        for (AuditEvent e : audit.after(a, n)) {
            out.add(dto(e));
        }
        return out;
    }

    @Override
    public List<AuditEventDto> trail(String subject) throws Exception {
        if (subject == null || subject.length() == 0 || subject.length() > 190) {
            throw Refused.with(400, "name-entity", "Name the entity to trace");
        }
        Staff.read(audit, subject, "audit trail");
        return query("SELECT * FROM audit_event WHERE subject = ? OR actor = ? "
                + "ORDER BY seq DESC LIMIT 500", new Object[] {subject, subject});
    }

    @Override
    public List<AuditEventDto> accessLog(String limit) throws Exception {
        int n = BooksEndpoint.clamp(limit, 100, 500);
        Staff.read(audit, "access-log", "last " + n);
        return query("SELECT * FROM audit_event WHERE action IN (?, ?, ?, ?, ?) "
                + "ORDER BY seq DESC LIMIT " + n, new Object[] {"STAFF_READ", "EVIDENCE_EXPORTED",
                "AUDIT_VERIFIED", "LEDGER_VERIFIED", "RECONCILED"});
    }

    @Override
    public EvidenceDto evidence(String name, String format, String asOf) throws Exception {
        boolean json = format != null && format.equalsIgnoreCase("json");
        if (format != null && format.length() > 0 && !json && !format.equalsIgnoreCase("csv")) {
            throw Refused.with(400, "format-csv-json", "The format is csv or json");
        }
        String who = Staff.name();
        long at = BooksEndpoint.to(asOf);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if ("journal".equals(name)) {
            long afterAt = -1L;
            String afterId = "";
            while (true) {
                List<JournalEntryDto> page = books.journalAfter(afterAt, afterId, at, EXPORT_PAGE);
                for (JournalEntryDto e : page) {
                    afterAt = e.at();
                    afterId = e.id();
                    for (JournalLegDto leg : e.legs()) {
                        Map<String, Object> r = new LinkedHashMap<String, Object>();
                        r.put("entry_id", e.id());
                        r.put("posted_at", Staff.iso(e.at()));
                        r.put("kind", e.kind());
                        r.put("initiator", e.initiator());
                        r.put("memo", e.memo());
                        r.put("reverses", e.reverses());
                        r.put("account_id", leg.accountId());
                        r.put("code", leg.code());
                        r.put("account", leg.accountName());
                        r.put("asset", leg.asset());
                        r.put("debit", Long.valueOf(leg.debit()));
                        r.put("credit", Long.valueOf(leg.credit()));
                        rows.add(r);
                    }
                }
                if (page.size() < EXPORT_PAGE) {
                    break;
                }
            }
        } else if ("trial-balance".equals(name)) {
            TrialBalanceDto tb = books.trialBalance(at);
            for (TrialBalanceLineDto l : tb.lines()) {
                Map<String, Object> r = new LinkedHashMap<String, Object>();
                r.put("as_of", Staff.iso(at));
                r.put("code", l.code());
                r.put("account", l.name());
                r.put("class", l.accountClass());
                r.put("asset", l.asset());
                r.put("debit", Long.valueOf(l.debit()));
                r.put("credit", Long.valueOf(l.credit()));
                rows.add(r);
            }
        } else if ("audit-chain".equals(name)) {
            long after = 0;
            while (true) {
                List<AuditEvent> page = audit.after(after, 500);
                for (AuditEvent e : page) {
                    after = e.getSeq();
                    if (e.getAt() > at) {
                        continue;
                    }
                    Map<String, Object> r = new LinkedHashMap<String, Object>();
                    r.put("seq", Long.valueOf(e.getSeq()));
                    r.put("at", Staff.iso(e.getAt()));
                    r.put("actor", e.getActor());
                    r.put("action", e.getAction());
                    r.put("subject", e.getSubject());
                    r.put("detail", e.getDetail());
                    r.put("address", e.getAddress());
                    r.put("prev_hash", e.getPrevHash());
                    r.put("hash", e.getHash());
                    r.put("hash_ok", Boolean.valueOf(Audit.expectedHash(e).equals(e.getHash())));
                    rows.add(r);
                }
                if (page.size() < 500) {
                    break;
                }
            }
        } else {
            throw Refused.with(404, "not-found", "Not found");
        }
        String content = json ? Json.write(rows) : csv(rows);
        String digest = Ids.sha256(content);
        // The digest of exactly what was handed out goes on the chain, so an
        // export can later be shown to be the one the bank produced.
        audit.append(who, "EVIDENCE_EXPORTED", name, (json ? "json" : "csv") + " as of " + at
                + ", " + rows.size() + " rows, sha256 " + digest);
        return new EvidenceDto(name, json ? "json" : "csv", json ? "application/json"
                : "text/csv", System.currentTimeMillis(), who, digest, content);
    }

    private List<AuditEventDto> query(String sql, Object[] params) throws Exception {
        List<AuditEventDto> out = new ArrayList<AuditEventDto>();
        for (Map r : books.rows(sql, params)) {
            out.add(new AuditEventDto(Books.num(r, "seq"), Books.num(r, "at"),
                    Books.str(r, "actor"), Books.str(r, "action"), Books.str(r, "subject"),
                    Books.str(r, "detail"), Books.str(r, "address"), Books.str(r, "prev_hash"),
                    Books.str(r, "hash")));
        }
        return out;
    }

    static AuditEventDto dto(AuditEvent e) {
        return new AuditEventDto(e.getSeq(), e.getAt(), e.getActor(), e.getAction(),
                e.getSubject(), e.getDetail(), e.getAddress(), e.getPrevHash(), e.getHash());
    }

    /// RFC 4180: a field with a comma, quote or line break is quoted, quotes
    /// doubled. A field starting with `=`, `+`, `-`, `@`, a tab or a carriage
    /// return is prefixed with a quote so a spreadsheet does not run it as a
    /// formula (CSV injection, OWASP's list); memos and names are customer
    /// text.
    public static String csv(List<Map<String, Object>> rows) {
        StringBuilder b = new StringBuilder();
        if (rows.isEmpty()) {
            return "";
        }
        boolean first = true;
        for (String key : rows.get(0).keySet()) {
            b.append(first ? "" : ",").append(key);
            first = false;
        }
        b.append("\r\n");
        for (Map<String, Object> r : rows) {
            first = true;
            for (Object v : r.values()) {
                b.append(first ? "" : ",");
                first = false;
                String s = v == null ? "" : String.valueOf(v);
                if (!(v instanceof Number) && s.length() > 0
                        && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
                    s = "'" + s;
                }
                if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0
                        || s.indexOf('\r') >= 0) {
                    StringBuilder q = new StringBuilder("\"");
                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        q.append(c == '"' ? "\"\"" : String.valueOf(c));
                    }
                    s = q.append('"').toString();
                }
                b.append(s);
            }
            b.append("\r\n");
        }
        return b.toString();
    }
}
