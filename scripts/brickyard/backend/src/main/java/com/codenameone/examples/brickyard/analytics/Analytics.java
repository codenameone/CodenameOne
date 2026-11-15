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
package com.codenameone.examples.brickyard.analytics;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.DayCountDto;
import com.codenameone.examples.brickyard.api.ErrorGroupDto;
import com.codenameone.examples.brickyard.api.EventDto;
import com.codenameone.examples.brickyard.api.RetentionDto;
import com.codenameone.examples.brickyard.api.RevenueDto;
import com.codenameone.examples.brickyard.domain.AnalyticsEvent;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.Ownership;
import com.codenameone.examples.brickyard.domain.Product;
import com.codenameone.examples.brickyard.repository.AnalyticsEventRepository;
import com.codenameone.examples.brickyard.repository.ExperienceRepository;
import com.codenameone.examples.brickyard.repository.OwnershipRepository;
import com.codenameone.examples.brickyard.repository.ProductRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Creator analytics: what engines report, and the dashboard over it.
/// Revenue is read from the purchases themselves, not from reported
/// PURCHASE events, so a dashboard cannot disagree with the ledger.
/// The queries aggregate in memory over a bounded window, which suits a
/// sample; a busy platform would roll events up per day.
@Component
public class Analytics {
    private static final String[] KINDS = {"VISIT", "SESSION", "FUNNEL", "PURCHASE", "ERROR"};
    public static final int MAX_DAYS = 90;

    private final AnalyticsEventRepository events;
    private final ExperienceRepository experiences;
    private final OwnershipRepository owned;
    private final ProductRepository products;

    public Analytics(AnalyticsEventRepository events, ExperienceRepository experiences,
            OwnershipRepository owned, ProductRepository products) {
        this.events = events;
        this.experiences = experiences;
        this.owned = owned;
        this.products = products;
    }

    @Transactional(rollbackFor = IOException.class)
    public EventDto ingest(EventDto e) throws IOException {
        Access.requireEngine();
        if (e == null) {
            throw new ResponseStatusException(400, "No event");
        }
        experience(e.experienceId());
        String kind = null;
        for (String k : KINDS) {
            if (k.equalsIgnoreCase(e.kind())) {
                kind = k;
            }
        }
        if (kind == null) {
            throw new ResponseStatusException(400, "That is not an event kind");
        }
        AnalyticsEvent a = new AnalyticsEvent();
        a.setId(Ids.next());
        a.setExperienceId(e.experienceId());
        a.setUsername(clip(e.username(), 190));
        a.setKind(kind);
        a.setName(clip(e.name(), 64));
        a.setAmount(Math.max(0, e.amount()));
        a.setDetail(clip(e.detail(), 4000));
        a.setVersion(e.version());
        long now = Clock.now();
        a.setDayStart(Clock.dayOf(now));
        a.setCreatedAt(now);
        events.insert(a);
        return e;
    }

    /// An event the backend itself observed (a room's VISIT or SESSION), so
    /// no engine role is asked for: the caller is the server, not a request.
    @Transactional(rollbackFor = IOException.class)
    public void record(String experienceId, String username, String kind, long amount) throws IOException {
        AnalyticsEvent a = new AnalyticsEvent();
        a.setId(Ids.next());
        a.setExperienceId(experienceId);
        a.setUsername(clip(username, 190));
        a.setKind(kind);
        a.setName("");
        a.setAmount(Math.max(0, amount));
        a.setDetail("");
        Experience e = experiences.findById(experienceId);
        a.setVersion(e == null ? 0 : e.getPublishedVersion());
        long now = Clock.now();
        a.setDayStart(Clock.dayOf(now));
        a.setCreatedAt(now);
        events.insert(a);
    }

    private static String clip(String s, int max) {
        String v = s == null ? "" : s;
        return v.length() > max ? v.substring(0, max) : v;
    }

    private Experience experience(String id) {
        Experience e = id == null ? null : experiences.findById(id);
        if (e == null) {
            throw new ResponseStatusException(404, "No such experience");
        }
        return e;
    }

    private void requireDashboard(String id) {
        Experience e = experience(id);
        if (!Access.isOwner(e) && !Access.isStaff()) {
            throw new ResponseStatusException(403, "Only the creator may see these analytics");
        }
    }

    public List<DayCountDto> visits(String id, int days) {
        requireDashboard(id);
        int n = days < 1 ? 7 : Math.min(days, MAX_DAYS);
        long today = Clock.dayOf(Clock.now());
        long from = today - (n - 1) * Clock.DAY;
        Map<Long, Long> counts = new LinkedHashMap<Long, Long>();
        for (long d = from; d <= today; d += Clock.DAY) {
            counts.put(d, 0L);
        }
        for (AnalyticsEvent e : events.of(id, from)) {
            if ("VISIT".equals(e.getKind()) && counts.containsKey(e.getDayStart())) {
                counts.put(e.getDayStart(), counts.get(e.getDayStart()) + 1);
            }
        }
        List<DayCountDto> out = new ArrayList<DayCountDto>();
        for (Map.Entry<Long, Long> c : counts.entrySet()) {
            out.add(new DayCountDto(c.getKey(), c.getValue()));
        }
        return out;
    }

    /// D1 and D7 retention over the last [#MAX_DAYS] days: of the players
    /// first seen long enough ago, the share that visited again exactly one
    /// (seven) days after their first day.
    public RetentionDto retention(String id) {
        requireDashboard(id);
        long today = Clock.dayOf(Clock.now());
        Map<String, Long> first = new HashMap<String, Long>();
        Set<String> visitDays = new HashSet<String>();
        for (AnalyticsEvent e : events.of(id, today - MAX_DAYS * Clock.DAY)) {
            if (!"VISIT".equals(e.getKind()) || e.getUsername().length() == 0) {
                continue;
            }
            Long f = first.get(e.getUsername());
            if (f == null || e.getDayStart() < f) {
                first.put(e.getUsername(), e.getDayStart());
            }
            visitDays.add(e.getUsername() + "|" + e.getDayStart());
        }
        long c1 = 0;
        long r1 = 0;
        long c7 = 0;
        long r7 = 0;
        for (Map.Entry<String, Long> f : first.entrySet()) {
            if (f.getValue() <= today - Clock.DAY) {
                c1++;
                if (visitDays.contains(f.getKey() + "|" + (f.getValue() + Clock.DAY))) {
                    r1++;
                }
            }
            if (f.getValue() <= today - 7 * Clock.DAY) {
                c7++;
                if (visitDays.contains(f.getKey() + "|" + (f.getValue() + 7 * Clock.DAY))) {
                    r7++;
                }
            }
        }
        return new RetentionDto(c1, r1, c7, r7);
    }

    public List<RevenueDto> revenue(String id) {
        requireDashboard(id);
        Map<String, long[]> byProduct = new LinkedHashMap<String, long[]>();
        for (Ownership o : owned.ofExperience(id)) {
            long[] s = byProduct.get(o.getProductId());
            if (s == null) {
                s = new long[2];
                byProduct.put(o.getProductId(), s);
            }
            s[0]++;
            s[1] += o.getPrice();
        }
        List<RevenueDto> out = new ArrayList<RevenueDto>();
        for (Map.Entry<String, long[]> s : byProduct.entrySet()) {
            Product p = products.findById(s.getKey());
            out.add(new RevenueDto(s.getKey(), p == null ? "" : p.getName(), s.getValue()[0], s.getValue()[1]));
        }
        return out;
    }

    /// Script errors of the last 30 days grouped by message and version,
    /// the most frequent first, each with its latest stack.
    public List<ErrorGroupDto> errors(String id) {
        requireDashboard(id);
        Map<String, Object[]> groups = new LinkedHashMap<String, Object[]>();
        for (AnalyticsEvent e : events.of(id, Clock.dayOf(Clock.now()) - 30 * Clock.DAY)) {
            if (!"ERROR".equals(e.getKind())) {
                continue;
            }
            String key = e.getVersion() + "|" + e.getName();
            Object[] g = groups.get(key);
            if (g == null) {
                g = new Object[] {e.getName(), e.getVersion(), 0L, "", 0L};
                groups.put(key, g);
            }
            g[2] = (Long) g[2] + 1;
            g[3] = e.getDetail();
            g[4] = e.getCreatedAt();
        }
        List<ErrorGroupDto> out = new ArrayList<ErrorGroupDto>();
        for (Object[] g : groups.values()) {
            out.add(new ErrorGroupDto((String) g[0], (Integer) g[1], (Long) g[2], (String) g[3], (Long) g[4]));
        }
        Collections.sort(out, (a, b) -> Long.compare(b.count(), a.count()));
        return out;
    }
}
