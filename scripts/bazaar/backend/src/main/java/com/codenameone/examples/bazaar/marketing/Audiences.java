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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.AudienceMemberDto;
import com.codenameone.examples.bazaar.api.AudiencePreviewDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Audience;
import com.codenameone.examples.bazaar.repository.AudienceRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Saved segments: a name and a rule tree [AudienceSql] compiles. Members
/// are worked out when asked, never stored, so a segment always means who
/// matches it now.
@Component
public class Audiences {
    /// The most customers one segment hands to a campaign or a journey.
    static final int MAX_MEMBERS = 50000;
    static final int SAMPLE = 10;

    private final AudienceRepository audiences;
    private final DataSource db;
    private final StoreAccess access;

    public Audiences(AudienceRepository audiences, DataSource db, StoreAccess access) {
        this.audiences = audiences;
        this.db = db;
        this.access = access;
    }

    /// One customer a segment matched.
    public static final class Member {
        public final String customerId;
        public final String email;
        public final String phone;
        public final String username;
        public final String name;
        public final boolean acceptsMarketing;

        Member(Map row) {
            customerId = Rows.text(row.get("id"));
            email = Rows.text(row.get("email"));
            phone = Rows.text(row.get("phone"));
            username = Rows.text(row.get("username"));
            name = (Rows.text(row.get("first_name")) + " " + Rows.text(row.get("last_name"))).trim();
            acceptsMarketing = Rows.number(row.get("accepts_marketing")) != 0;
        }
    }

    @Transactional(readOnly = true)
    public List<AudienceDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Audience> rows = audiences.findByStoreIdOrderByNameAsc(storeId);
        List<AudienceDto> out = new ArrayList<AudienceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Audience a = rows.get(iter);
            out.add(new AudienceDto(a.getId(), a.getName(), a.getRules(), count(storeId, a.getRules())));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public AudienceDto save(String username, String storeId, String id, AudienceDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return write(storeId, id, in);
    }

    public AudienceDto write(String storeId, String id, AudienceDto in) throws IOException {
        if (in == null || Codes.cut(in.name(), 80).length() == 0) {
            throw new ResponseStatusException(400, "Name the segment");
        }
        String rules = in.rules() == null ? "" : in.rules().trim();
        if (rules.length() > 8000) {
            throw new ResponseStatusException(400, "Those rules are too long");
        }
        AudienceSql.compile(rules, storeId, System.currentTimeMillis());
        Audience a = id == null ? null : audience(storeId, id);
        boolean fresh = a == null;
        if (fresh) {
            a = new Audience();
            a.setId(Ids.next());
            a.setStoreId(storeId);
            a.setCreatedAt(Instant.now());
        }
        a.setName(Codes.cut(in.name(), 80));
        a.setRules(rules);
        if (fresh) {
            audiences.insert(a);
        } else {
            audiences.save(a);
        }
        return new AudienceDto(a.getId(), a.getName(), a.getRules(), count(storeId, rules));
    }

    @Transactional(rollbackFor = IOException.class)
    public List<AudienceDto> remove(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        audiences.delete(audience(storeId, id));
        return list(username, storeId);
    }

    public Audience audience(String storeId, String id) {
        Audience a = id == null ? null : audiences.findById(id);
        if (a == null || !storeId.equals(a.getStoreId())) {
            throw new ResponseStatusException(404, "No such segment");
        }
        return a;
    }

    /// How many match the rules, and the top spenders among them.
    public AudiencePreviewDto preview(String username, String storeId, AudienceDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String rules = in == null ? "" : in.rules();
        AudienceSql.Query q = AudienceSql.compile(rules, storeId, System.currentTimeMillis());
        List rows = db.query("SELECT c.id, c.first_name, c.last_name, c.email, c.spent_cents, "
                + "c.orders_count FROM customer c WHERE " + q.where
                + " ORDER BY c.spent_cents DESC, c.id LIMIT " + SAMPLE, q.params.toArray());
        List<AudienceMemberDto> sample = new ArrayList<AudienceMemberDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            sample.add(new AudienceMemberDto(Rows.text(r.get("id")),
                    (Rows.text(r.get("first_name")) + " " + Rows.text(r.get("last_name"))).trim(),
                    Rows.text(r.get("email")), Rows.number(r.get("spent_cents")),
                    (int) Rows.number(r.get("orders_count"))));
        }
        return new AudiencePreviewDto(count(storeId, rules), sample);
    }

    long count(String storeId, String rules) throws IOException {
        AudienceSql.Query q = AudienceSql.compile(rules, storeId, System.currentTimeMillis());
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM customer c WHERE " + q.where,
                q.params.toArray());
        return row == null ? 0 : Rows.number(row.get("n"));
    }

    /// Everyone a saved segment matches now, oldest customer first.
    public List<Member> members(String storeId, String audienceId) throws IOException {
        Audience a = audience(storeId, audienceId);
        AudienceSql.Query q = AudienceSql.compile(a.getRules(), storeId, System.currentTimeMillis());
        List rows = db.query("SELECT c.id, c.email, c.phone, c.username, c.first_name, c.last_name, "
                + "c.accepts_marketing FROM customer c WHERE " + q.where
                + " ORDER BY c.created_at, c.id LIMIT " + MAX_MEMBERS, q.params.toArray());
        List<Member> out = new ArrayList<Member>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(new Member((Map) rows.get(iter)));
        }
        return out;
    }

    /// Whether one customer matches a saved segment now.
    public boolean matches(String storeId, String audienceId, String customerId) throws IOException {
        Audience a = audiences.findById(audienceId);
        if (a == null || !storeId.equals(a.getStoreId())) {
            return false;
        }
        AudienceSql.Query q = AudienceSql.compile(a.getRules(), storeId, System.currentTimeMillis());
        List<Object> params = new ArrayList<Object>(q.params);
        params.add(customerId);
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM customer c WHERE " + q.where
                + " AND c.id = ?", params.toArray());
        return row != null && Rows.number(row.get("n")) > 0;
    }
}
