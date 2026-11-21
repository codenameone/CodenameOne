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
package com.codenameone.examples.bazaar.journey;

import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.automation.AutomationGraph;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Journey;
import com.codenameone.examples.bazaar.domain.JourneyEnrollment;
import com.codenameone.examples.bazaar.domain.JourneyNodeStat;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Audiences;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.push.PushDevices;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.JourneyEnrollmentRepository;
import com.codenameone.examples.bazaar.repository.JourneyNodeStatRepository;
import com.codenameone.examples.bazaar.repository.JourneyRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.sms.SmsSender;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Customer journeys: a graph of [JourneyCatalog] steps a customer walks
/// through, one [JourneyEnrollment] per customer and journey.
///
/// An enrollment points at the step it runs next. The job takes the due
/// ones, claims each with a conditional update (so two servers never run
/// one), and walks it forward until it reaches a wait (it then sleeps until
/// `resumeAt`) or the end. Every step passed adds one to that node's count,
/// which with the enrollments parked on it is what the stats show.
@Component
public class Journeys {
    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    /// Enrollment states: due to run, parked on a wait, finished.
    public static final String READY = "READY";
    public static final String WAITING = "WAITING";
    public static final String DONE = "DONE";
    /// Steps one enrollment takes in one pass, beyond which it yields.
    static final int MAX_STEPS = 25;
    static final long MINUTE = 60000L;

    private final JourneyRepository journeys;
    private final JourneyEnrollmentRepository enrollments;
    private final JourneyNodeStatRepository stats;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final Audiences audiences;
    private final Campaigns campaigns;
    private final Loyalty loyalty;
    private final Emails emails;
    private final SmsSender sms;
    private final PushSender push;
    private final PushDevices devices;
    private final StoreAccess access;

    public Journeys(JourneyRepository journeys, JourneyEnrollmentRepository enrollments,
            JourneyNodeStatRepository stats, CustomerRepository customers, StoreOrderRepository orders,
            Audiences audiences, Campaigns campaigns, Loyalty loyalty, Emails emails, SmsSender sms,
            PushSender push, PushDevices devices, StoreAccess access) {
        this.journeys = journeys;
        this.enrollments = enrollments;
        this.stats = stats;
        this.customers = customers;
        this.orders = orders;
        this.audiences = audiences;
        this.campaigns = campaigns;
        this.loyalty = loyalty;
        this.emails = emails;
        this.sms = sms;
        this.push = push;
        this.devices = devices;
        this.access = access;
    }

    // -------------------------------------------------------------- merchant

    public List<NodeTypeDto> catalog(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return JourneyCatalog.all();
    }

    @Transactional(readOnly = true)
    public List<JourneyDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Journey> rows = journeys.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<JourneyDto> out = new ArrayList<JourneyDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public JourneyDto get(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return dto(journey(storeId, id));
    }

    JourneyDto dto(Journey j) {
        return new JourneyDto(j.getId(), j.getName(), j.getGraphJson(), j.getStatus(),
                j.getCreatedAt().toEpochMilli(), validate(j.getGraphJson()));
    }

    Journey journey(String storeId, String id) {
        Journey j = id == null ? null : journeys.findById(id);
        if (j == null || !storeId.equals(j.getStoreId())) {
            throw new ResponseStatusException(404, "No such journey");
        }
        return j;
    }

    @Transactional(rollbackFor = IOException.class)
    public JourneyDto save(String username, String storeId, String id, JourneyDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return write(storeId, id, in);
    }

    /// Creates or edits a journey. A draft may be saved half drawn; going
    /// `ACTIVE` needs a graph with no problems.
    public JourneyDto write(String storeId, String id, JourneyDto in) throws IOException {
        if (in == null || Codes.cut(in.name(), 80).length() == 0) {
            throw new ResponseStatusException(400, "Name the journey");
        }
        String graph = in.graphJson() == null ? "" : in.graphJson();
        if (graph.length() > 100000) {
            throw new ResponseStatusException(400, "That journey is too large");
        }
        String status = ACTIVE.equals(in.status()) ? ACTIVE : PAUSED.equals(in.status()) ? PAUSED : DRAFT;
        if (ACTIVE.equals(status)) {
            List<String> problems = validate(graph);
            if (!problems.isEmpty()) {
                throw new ResponseStatusException(400, problems.get(0));
            }
            AutomationGraph g = AutomationGraph.parse(graph);
            List<AutomationGraph.Node> all = g.nodes();
            for (int iter = 0; iter < all.size(); iter++) {
                String seg = AutomationGraph.param(all.get(iter), "segmentId");
                if (seg.length() > 0) {
                    audiences.audience(storeId, seg);
                }
            }
        }
        Journey j = id == null ? null : journey(storeId, id);
        boolean fresh = j == null;
        Instant now = Instant.now();
        if (fresh) {
            j = new Journey();
            j.setId(Ids.next());
            j.setStoreId(storeId);
            j.setCreatedAt(now);
        }
        j.setName(Codes.cut(in.name(), 80));
        j.setGraphJson(graph);
        j.setStatus(status);
        j.setUpdatedAt(now);
        if (fresh) {
            journeys.insert(j);
        } else {
            journeys.save(j);
        }
        return dto(j);
    }

    /// What is wrong with a journey graph: unreadable, not exactly one entry,
    /// unknown steps, a step with nothing after it, a step no path reaches,
    /// or a step missing what it needs.
    public static List<String> validate(String json) {
        AutomationGraph g = AutomationGraph.parse(json);
        List<String> out = new ArrayList<String>(g.errors());
        if (!out.isEmpty()) {
            return out;
        }
        List<AutomationGraph.Node> all = g.nodes();
        AutomationGraph.Node entry = null;
        for (int iter = 0; iter < all.size(); iter++) {
            AutomationGraph.Node n = all.get(iter);
            if (JourneyCatalog.find(n.type) == null) {
                out.add("Step " + n.id + " is not a journey step");
            } else if (JourneyCatalog.isEntry(n.type)) {
                if (entry != null) {
                    out.add("A journey has one entry");
                }
                entry = n;
            }
        }
        if (entry == null) {
            out.add("The journey has no entry");
            return out;
        }
        Set<String> reached = new HashSet<String>();
        List<AutomationGraph.Node> todo = new ArrayList<AutomationGraph.Node>();
        todo.add(entry);
        while (!todo.isEmpty()) {
            AutomationGraph.Node n = todo.remove(todo.size() - 1);
            if (!reached.add(n.id)) {
                continue;
            }
            todo.addAll(g.next(n, null));
        }
        if (loops(g, entry, new HashSet<String>(), new HashSet<String>())) {
            // A loop would run a customer round it every tick, sending each time.
            out.add("The steps loop back on themselves");
        }
        for (int iter = 0; iter < all.size(); iter++) {
            AutomationGraph.Node n = all.get(iter);
            if (JourneyCatalog.find(n.type) == null) {
                continue;
            }
            if (!reached.contains(n.id)) {
                out.add("Step " + n.id + " is not connected to the entry");
            }
            String missing = missing(g, n);
            if (missing != null) {
                out.add("Step " + n.id + " needs " + missing);
            }
        }
        return out;
    }

    /// Whether a path from `n` comes back to a step already on it.
    static boolean loops(AutomationGraph g, AutomationGraph.Node n, Set<String> path, Set<String> done) {
        if (path.contains(n.id)) {
            return true;
        }
        if (!done.add(n.id)) {
            return false;
        }
        path.add(n.id);
        List<AutomationGraph.Node> next = g.next(n, null);
        for (int iter = 0; iter < next.size(); iter++) {
            if (loops(g, next.get(iter), path, done)) {
                return true;
            }
        }
        path.remove(n.id);
        return false;
    }

    static String missing(AutomationGraph g, AutomationGraph.Node n) {
        String t = n.type;
        if (!JourneyCatalog.EXIT.equals(t) && g.next(n, null).isEmpty()) {
            return "a step after it";
        }
        if (JourneyCatalog.ENTRY_SEGMENT.equals(t) && AutomationGraph.param(n, "segmentId").length() == 0) {
            return "a segment";
        }
        if (JourneyCatalog.ENTRY_TRIGGER.equals(t)
                && !JourneyCatalog.ORDER_PAID.equals(value(n, "event", JourneyCatalog.ORDER_PAID))) {
            return "an event it knows (" + JourneyCatalog.ORDER_PAID + ")";
        }
        if (JourneyCatalog.WAIT.equals(t)) {
            long m = AutomationGraph.number(n, "minutes", 0);
            if (m <= 0 || m > 365L * 24 * 60) {
                return "a wait of 1 minute to a year";
            }
        }
        if (JourneyCatalog.SPLIT_CONDITION.equals(t) && AutomationGraph.param(n, "segmentId").length() == 0
                && AutomationGraph.param(n, "tag").length() == 0) {
            return "a segment or a tag";
        }
        if (JourneyCatalog.SPLIT_AB.equals(t)) {
            long p = AutomationGraph.number(n, "percent", 50);
            if (p < 0 || p > 100) {
                return "a percent from 0 to 100";
            }
        }
        if (JourneyCatalog.SEND_EMAIL.equals(t) && (AutomationGraph.param(n, "subject").length() == 0
                || AutomationGraph.param(n, "body").length() == 0)) {
            return "a subject and a message";
        }
        if ((JourneyCatalog.SEND_SMS.equals(t) || JourneyCatalog.SEND_PUSH.equals(t))
                && AutomationGraph.param(n, "body").length() == 0) {
            return "a message";
        }
        if (JourneyCatalog.POINTS_ADD.equals(t)) {
            long p = AutomationGraph.number(n, "points", 0);
            if (p <= 0 || p > 100000) {
                return "1 to 100000 points";
            }
        }
        if (JourneyCatalog.TAG_ADD.equals(t) && AutomationGraph.param(n, "tag").length() == 0) {
            return "a tag";
        }
        return null;
    }

    static String value(AutomationGraph.Node n, String name, String fallback) {
        String v = AutomationGraph.param(n, name);
        return v.length() == 0 ? fallback : v;
    }

    /// Per node: how many passed it and how many are parked on it now.
    @Transactional(readOnly = true)
    public List<JourneyNodeStatDto> stats(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Journey j = journey(storeId, id);
        AutomationGraph g = AutomationGraph.parse(j.getGraphJson());
        List<JourneyNodeStat> rows = stats.findByJourneyId(j.getId());
        List<JourneyEnrollment> es = enrollments.findByJourneyId(j.getId());
        List<AutomationGraph.Node> all = g.nodes();
        List<JourneyNodeStatDto> out = new ArrayList<JourneyNodeStatDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            String nodeId = all.get(iter).id;
            long passed = 0;
            for (int k = 0; k < rows.size(); k++) {
                if (nodeId.equals(rows.get(k).getNodeId())) {
                    passed += rows.get(k).getPassed();
                }
            }
            long current = 0;
            for (int k = 0; k < es.size(); k++) {
                if (nodeId.equals(es.get(k).getNodeId()) && !DONE.equals(es.get(k).getStatus())) {
                    current++;
                }
            }
            out.add(new JourneyNodeStatDto(nodeId, current, passed));
        }
        return out;
    }

    // ------------------------------------------------------------- engine

    @Scheduled(fixedDelay = 60000, initialDelay = 75000, lock = "bazaar-journeys")
    public void job() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("journeys: " + failed);
        }
    }

    /// Enrols new segment members into active journeys, then runs every
    /// due enrollment. Answers how many steps ran.
    public int tick(Instant now) throws IOException {
        List<Journey> active = journeys.findByStatus(ACTIVE);
        for (int iter = 0; iter < active.size(); iter++) {
            Journey j = active.get(iter);
            AutomationGraph.Node entry = entry(AutomationGraph.parse(j.getGraphJson()));
            if (entry != null && JourneyCatalog.ENTRY_SEGMENT.equals(entry.type)) {
                String seg = AutomationGraph.param(entry, "segmentId");
                List<Audiences.Member> members;
                try {
                    members = audiences.members(j.getStoreId(), seg);
                } catch (ResponseStatusException gone) {
                    continue;
                }
                for (int k = 0; k < members.size(); k++) {
                    enrol(j, members.get(k).customerId, now);
                }
            }
        }
        int steps = 0;
        List<JourneyEnrollment> due = new ArrayList<JourneyEnrollment>();
        due.addAll(enrollments.findByStatusAndResumeAtLessThanEqual(READY, now));
        due.addAll(enrollments.findByStatusAndResumeAtLessThanEqual(WAITING, now));
        for (int iter = 0; iter < due.size(); iter++) {
            steps += run(due.get(iter), now);
        }
        return steps;
    }

    static AutomationGraph.Node entry(AutomationGraph g) {
        List<AutomationGraph.Node> all = g.nodes();
        for (int iter = 0; iter < all.size(); iter++) {
            if (JourneyCatalog.isEntry(all.get(iter).type)) {
                return all.get(iter);
            }
        }
        return null;
    }

    /// Puts a customer into a journey once; answers whether they were new.
    public boolean enrol(Journey j, String customerId, Instant now) throws IOException {
        if (customerId == null || customerId.length() == 0
                || enrollments.findByJourneyIdAndCustomerId(j.getId(), customerId) != null) {
            return false;
        }
        AutomationGraph g = AutomationGraph.parse(j.getGraphJson());
        AutomationGraph.Node entry = entry(g);
        if (entry == null) {
            return false;
        }
        JourneyEnrollment e = new JourneyEnrollment();
        e.setId(Ids.next());
        e.setStoreId(j.getStoreId());
        e.setJourneyId(j.getId());
        e.setCustomerId(customerId);
        e.setNodeId(entry.id);
        e.setStatus(READY);
        e.setResumeAt(now);
        e.setEnteredAt(now);
        e.setUpdatedAt(now);
        enrollments.insert(e);
        return true;
    }

    /// The trigger hook: a paid order enrols its customer into every active
    /// journey that starts on [JourneyCatalog#ORDER_PAID].
    public void orderPaid(String orderId) throws IOException {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || o.getCustomerId().length() == 0) {
            return;
        }
        Instant now = Instant.now();
        List<Journey> active = journeys.findByStatus(ACTIVE);
        for (int iter = 0; iter < active.size(); iter++) {
            Journey j = active.get(iter);
            if (!j.getStoreId().equals(o.getStoreId())) {
                continue;
            }
            AutomationGraph.Node entry = entry(AutomationGraph.parse(j.getGraphJson()));
            if (entry != null && JourneyCatalog.ENTRY_TRIGGER.equals(entry.type)
                    && JourneyCatalog.ORDER_PAID.equals(value(entry, "event", JourneyCatalog.ORDER_PAID))) {
                enrol(j, o.getCustomerId(), now);
            }
        }
    }

    /// Walks one enrollment forward. Answers the steps it took.
    int run(JourneyEnrollment e, Instant now) throws IOException {
        Journey j = journeys.findById(e.getJourneyId());
        if (j == null || !ACTIVE.equals(j.getStatus())) {
            return 0;
        }
        Instant claim = Instant.ofEpochMilli(Math.max(now.toEpochMilli(), e.getUpdatedAt().toEpochMilli() + 1));
        if (enrollments.claim(e.getId(), e.getUpdatedAt(), claim) != 1) {
            return 0;
        }
        AutomationGraph g = AutomationGraph.parse(j.getGraphJson());
        Customer c = customers.findById(e.getCustomerId());
        String status = e.getStatus();
        String nodeId = e.getNodeId();
        Instant resume = e.getResumeAt();
        int steps = 0;
        while (steps < MAX_STEPS) {
            AutomationGraph.Node n = g.node(nodeId);
            if (n == null || c == null || JourneyCatalog.EXIT.equals(n.type)) {
                if (n != null) {
                    bump(j.getId(), n.id);
                }
                status = DONE;
                break;
            }
            if (JourneyCatalog.WAIT.equals(n.type) && !WAITING.equals(status)) {
                status = WAITING;
                resume = now.plusMillis(AutomationGraph.number(n, "minutes", 1440) * MINUTE);
                break;
            }
            String branch = act(j, e, n, c);
            bump(j.getId(), n.id);
            steps++;
            List<AutomationGraph.Node> next = g.next(n, branch);
            if (next.isEmpty()) {
                status = DONE;
                break;
            }
            nodeId = next.get(0).id;
            status = READY;
            resume = now;
        }
        JourneyEnrollment fresh = enrollments.findById(e.getId());
        fresh.setNodeId(nodeId);
        fresh.setStatus(status);
        fresh.setResumeAt(resume);
        fresh.setUpdatedAt(claim);
        enrollments.save(fresh);
        return steps;
    }

    /// Runs one step; answers the branch to follow (null for the only one).
    String act(Journey j, JourneyEnrollment e, AutomationGraph.Node n, Customer c) throws IOException {
        String t = n.type;
        String storeId = j.getStoreId();
        if (JourneyCatalog.SPLIT_CONDITION.equals(t)) {
            String seg = AutomationGraph.param(n, "segmentId");
            String tag = AutomationGraph.param(n, "tag");
            boolean yes = seg.length() > 0 ? audiences.matches(storeId, seg, c.getId()) : hasTag(c.getTags(), tag);
            return yes ? JourneyCatalog.YES : JourneyCatalog.NO;
        }
        if (JourneyCatalog.SPLIT_AB.equals(t)) {
            return bucket(e.getId() + ":" + n.id) < AutomationGraph.number(n, "percent", 50)
                    ? JourneyCatalog.A : JourneyCatalog.B;
        }
        try {
            if (JourneyCatalog.SEND_EMAIL.equals(t)) {
                if (c.getEmail().indexOf('@') > 0 && c.isAcceptsMarketing()
                        && campaigns.consented(storeId, c.getEmail(), Campaigns.EMAIL)) {
                    emails.custom(storeId, c.getEmail(), AutomationGraph.param(n, "subject"),
                            AutomationGraph.param(n, "body"));
                }
            } else if (JourneyCatalog.SEND_SMS.equals(t)) {
                if (c.getPhone().length() > 0 && sms.delivers()
                        && campaigns.consented(storeId, c.getPhone(), Campaigns.SMS)) {
                    sms.send(c.getPhone(), AutomationGraph.param(n, "body"));
                }
            } else if (JourneyCatalog.SEND_PUSH.equals(t)) {
                List<PushTarget> to = c.getUsername().length() == 0 ? new ArrayList<PushTarget>()
                        : devices.targets(c.getUsername());
                if (!to.isEmpty()) {
                    push.send(to, PushNotification.builder().title(value(n, "title", j.getName()))
                            .body(AutomationGraph.param(n, "body")).data("kind", "journey")
                            .data("journeyId", j.getId()).build(), null);
                }
            }
        } catch (IOException | RuntimeException failed) {
            // A message that fails is not retried; the customer walks on.
            System.err.println("journey " + j.getId() + " step " + n.id + ": " + failed);
        }
        if (JourneyCatalog.POINTS_ADD.equals(t)) {
            // The ref makes it once per customer and step, whatever reruns.
            loyalty.award(storeId, c.getId(), AutomationGraph.number(n, "points", 0),
                    "JOURNEY:" + e.getId() + ":" + n.id, j.getName());
        } else if (JourneyCatalog.TAG_ADD.equals(t)) {
            String tag = Codes.cut(AutomationGraph.param(n, "tag"), 40);
            if (tag.length() > 0 && !hasTag(c.getTags(), tag)) {
                Customer fresh = customers.findById(c.getId());
                fresh.setTags(fresh.getTags().length() == 0 ? tag : fresh.getTags() + ", " + tag);
                customers.save(fresh);
                c.setTags(fresh.getTags());
            }
        }
        return null;
    }

    static boolean hasTag(String tags, String tag) {
        if (tag == null || tag.length() == 0 || tags == null) {
            return false;
        }
        int start = 0;
        while (start <= tags.length()) {
            int comma = tags.indexOf(',', start);
            int end = comma < 0 ? tags.length() : comma;
            if (tags.substring(start, end).trim().equalsIgnoreCase(tag.trim())) {
                return true;
            }
            if (comma < 0) {
                return false;
            }
            start = comma + 1;
        }
        return false;
    }

    /// 0..99 from a string, the same every time for the same string.
    static int bucket(String key) {
        int h = 17;
        for (int iter = 0; iter < key.length(); iter++) {
            h = h * 31 + key.charAt(iter);
        }
        return (h & 0x7fffffff) % 100;
    }

    void bump(String journeyId, String nodeId) throws IOException {
        List<JourneyNodeStat> rows = stats.findByJourneyId(journeyId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (nodeId.equals(rows.get(iter).getNodeId())) {
                stats.bump(rows.get(iter).getId());
                return;
            }
        }
        JourneyNodeStat s = new JourneyNodeStat();
        s.setId(Ids.next());
        s.setJourneyId(journeyId);
        s.setNodeId(nodeId);
        s.setPassed(1);
        stats.insert(s);
    }
}
