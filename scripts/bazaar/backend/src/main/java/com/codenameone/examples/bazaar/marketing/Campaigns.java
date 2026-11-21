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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.CampaignStatsDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Campaign;
import com.codenameone.examples.bazaar.domain.CampaignLink;
import com.codenameone.examples.bazaar.domain.CampaignSend;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.MarketingConsent;
import com.codenameone.examples.bazaar.domain.MarketingKey;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.repository.CampaignLinkRepository;
import com.codenameone.examples.bazaar.repository.CampaignRepository;
import com.codenameone.examples.bazaar.repository.CampaignSendRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.MarketingConsentRepository;
import com.codenameone.examples.bazaar.repository.MarketingKeyRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.sms.SmsSender;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// E-mail and SMS campaigns to a segment.
///
/// A campaign goes `DRAFT -> SCHEDULED -> TESTING (A/B only) -> SENDING ->
/// SENT`, each move a conditional update the job makes. When it starts, one
/// [CampaignSend] row is written per consenting member of the segment, each
/// with a random token; the rows are then sent [#BATCH] at a time, each
/// claimed `QUEUED -> SENDING` before its message goes, so a restart never
/// sends one twice. With an A/B test, `abPercent` of the rows are split
/// between the two subjects and the rest held; once `winnerAfterMinutes`
/// pass, the subject with the better open rate goes to the held rows.
///
/// Every message carries an open pixel, tracked links that redirect only to
/// the campaign's own stored URLs, and an unsubscribe link signed with the
/// store's secret. An order placed within [#ATTRIBUTION_DAYS] of a message
/// the customer opened or clicked is credited to that message.
@Component
public class Campaigns {
    public static final String DRAFT = "DRAFT";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String TESTING = "TESTING";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String CANCELLED = "CANCELLED";
    public static final String QUEUED = "QUEUED";
    public static final String HELD = "HELD";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";
    public static final String EMAIL = "EMAIL";
    public static final String SMS = "SMS";
    public static final String SUBSCRIBED = "SUBSCRIBED";
    public static final String UNSUBSCRIBED = "UNSUBSCRIBED";
    /// Messages one job pass sends, to keep within a mail relay's rate.
    public static final int BATCH = 200;
    public static final long ATTRIBUTION_DAYS = 5;
    static final long DAY = 24L * 3600 * 1000;
    static final long MINUTE = 60000L;
    static final int MAX_LINKS = 5;

    private final CampaignRepository campaigns;
    private final CampaignSendRepository sends;
    private final CampaignLinkRepository links;
    private final MarketingKeyRepository keys;
    private final MarketingConsentRepository consents;
    private final StoreOrderRepository orders;
    private final Audiences audiences;
    private final Emails emails;
    private final SmsSender sms;
    private final SiteUrls urls;
    private final StoreAccess access;

    private final CustomerRepository customers;
    private final com.codenameone.examples.bazaar.market.Translations translations;
    private final com.codenameone.examples.bazaar.notify.ShopperLanguage language;

    public Campaigns(CampaignRepository campaigns, CampaignSendRepository sends,
            CampaignLinkRepository links, MarketingKeyRepository keys,
            MarketingConsentRepository consents, StoreOrderRepository orders, Audiences audiences,
            Emails emails, SmsSender sms, SiteUrls urls, StoreAccess access,
            CustomerRepository customers,
            com.codenameone.examples.bazaar.market.Translations translations,
            com.codenameone.examples.bazaar.notify.ShopperLanguage language) {
        this.translations = translations;
        this.language = language;
        this.customers = customers;
        this.campaigns = campaigns;
        this.sends = sends;
        this.links = links;
        this.keys = keys;
        this.consents = consents;
        this.orders = orders;
        this.audiences = audiences;
        this.emails = emails;
        this.sms = sms;
        this.urls = urls;
        this.access = access;
    }

    // -------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<CampaignDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Campaign> rows = campaigns.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<CampaignDto> out = new ArrayList<CampaignDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    CampaignDto dto(Campaign c) {
        List<CampaignLink> ls = links.findByCampaignIdOrderByPositionAsc(c.getId());
        List<String> urlsOut = new ArrayList<String>();
        for (int iter = 0; iter < ls.size(); iter++) {
            urlsOut.add(ls.get(iter).getUrl());
        }
        return new CampaignDto(c.getId(), c.getName(), c.getChannel(), c.getAudienceId(),
                c.getSubjectA(), c.getSubjectB(), c.getBody(), c.getAbPercent(),
                c.getWinnerAfterMinutes(), c.getStatus(), c.getScheduledAt().toEpochMilli(),
                c.getWinner(), c.getColour(), urlsOut);
    }

    Campaign campaign(String storeId, String id) {
        Campaign c = id == null ? null : campaigns.findById(id);
        if (c == null || !storeId.equals(c.getStoreId())) {
            throw new ResponseStatusException(404, "No such campaign");
        }
        return c;
    }

    @Transactional(rollbackFor = IOException.class)
    public CampaignDto save(String username, String storeId, String id, CampaignDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return write(storeId, id, in);
    }

    /// Creates a draft (`id` null) or edits one; only a draft or a campaign
    /// still waiting for its time can change.
    public CampaignDto write(String storeId, String id, CampaignDto in) throws IOException {
        if (in == null || Codes.cut(in.name(), 120).length() == 0) {
            throw new ResponseStatusException(400, "Name the campaign");
        }
        String channel = SMS.equals(in.channel()) ? SMS : EMAIL;
        audiences.audience(storeId, in.audienceId());
        if (EMAIL.equals(channel) && Codes.cut(in.subjectA(), 160).length() == 0) {
            throw new ResponseStatusException(400, "Give the message a subject");
        }
        String body = in.body() == null ? "" : in.body();
        if (body.trim().length() == 0 || body.length() > (SMS.equals(channel) ? 480 : 20000)) {
            throw new ResponseStatusException(400, "Write the message, within the length allowed");
        }
        boolean ab = in.subjectB() != null && in.subjectB().trim().length() > 0;
        if (ab && (in.abPercent() < 2 || in.abPercent() > 50 || in.winnerAfterMinutes() < 5
                || in.winnerAfterMinutes() > 10080)) {
            throw new ResponseStatusException(400,
                    "An A/B test goes to 2 to 50 percent and picks a winner after 5 minutes to 7 days");
        }
        List<String> wanted = in.links() == null ? new ArrayList<String>() : in.links();
        if (wanted.size() > MAX_LINKS) {
            throw new ResponseStatusException(400, "At most " + MAX_LINKS + " links");
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            String u = wanted.get(iter);
            if (u == null || u.length() > 500 || !(u.startsWith("https://") || u.startsWith("http://")
                    || u.startsWith("/"))) {
                throw new ResponseStatusException(400, "A link is a web address or a store path");
            }
        }
        Campaign c = id == null ? null : campaign(storeId, id);
        boolean fresh = c == null;
        if (fresh) {
            c = new Campaign();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setStatus(DRAFT);
            c.setCreatedAt(Instant.now());
        } else if (!DRAFT.equals(c.getStatus()) && !SCHEDULED.equals(c.getStatus())) {
            throw new ResponseStatusException(409, "That campaign has already gone out");
        }
        c.setName(Codes.cut(in.name(), 120));
        c.setChannel(channel);
        c.setAudienceId(in.audienceId());
        c.setSubjectA(Codes.cut(in.subjectA(), 160));
        c.setSubjectB(ab ? Codes.cut(in.subjectB(), 160) : "");
        c.setBody(body);
        c.setAbPercent(ab ? in.abPercent() : 0);
        c.setWinnerAfterMinutes(ab ? in.winnerAfterMinutes() : 0);
        c.setColour(Tokens.colour(in.colour(), "#1565c0"));
        if (fresh) {
            campaigns.insert(c);
        } else {
            campaigns.save(c);
            List<CampaignLink> old = links.findByCampaignIdOrderByPositionAsc(c.getId());
            for (int iter = 0; iter < old.size(); iter++) {
                links.delete(old.get(iter));
            }
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            CampaignLink l = new CampaignLink();
            l.setId(Ids.next());
            l.setCampaignId(c.getId());
            l.setPosition(iter);
            l.setUrl(wanted.get(iter));
            links.insert(l);
        }
        return dto(c);
    }

    @Transactional(rollbackFor = IOException.class)
    public CampaignDto schedule(String username, String storeId, String id, long at)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Campaign c = campaign(storeId, id);
        if (!DRAFT.equals(c.getStatus()) && !SCHEDULED.equals(c.getStatus())) {
            throw new ResponseStatusException(409, "That campaign has already gone out");
        }
        long now = System.currentTimeMillis();
        if (at > now + 366 * DAY) {
            throw new ResponseStatusException(400, "Schedule it within a year");
        }
        // A time in the past means now.
        c.setScheduledAt(Instant.ofEpochMilli(Math.max(at, now)));
        c.setStatus(SCHEDULED);
        campaigns.save(c);
        return dto(c);
    }

    /// Sends one copy of an e-mail campaign to `to` (the merchant's own
    /// address), subject marked as a test. Nothing is recorded: no send row,
    /// no tracked links, no stats, and the campaign's state does not move.
    /// Answers the campaign as it stands.
    public CampaignDto sendTest(String username, String storeId, String id, String to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Campaign c = campaign(storeId, id);
        if (!EMAIL.equals(c.getChannel())) {
            throw new ResponseStatusException(409, "Only an e-mail campaign sends a test");
        }
        if (to == null || !com.codename1.backend.mail.Mail.isAddress(to.trim())) {
            throw new ResponseStatusException(400, "Write the address to send the test to");
        }
        emails.custom(storeId, to.trim(), "[Test] " + c.getSubjectA(), c.getBody());
        return dto(c);
    }

    @Transactional(rollbackFor = IOException.class)
    public CampaignDto cancel(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Campaign c = campaign(storeId, id);
        if (campaigns.move(c.getId(), SCHEDULED, CANCELLED) != 1
                && campaigns.move(c.getId(), DRAFT, CANCELLED) != 1
                && campaigns.move(c.getId(), TESTING, CANCELLED) != 1
                && campaigns.move(c.getId(), SENDING, CANCELLED) != 1) {
            throw new ResponseStatusException(409, "That campaign has finished");
        }
        return dto(campaigns.findById(c.getId()));
    }

    @Transactional(readOnly = true)
    public CampaignStatsDto stats(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return stats(campaign(storeId, id));
    }

    public CampaignStatsDto stats(Campaign c) {
        List<CampaignSend> rows = sends.findByCampaignId(c.getId());
        long sent = 0;
        long opened = 0;
        long clicked = 0;
        long ordered = 0;
        long revenue = 0;
        long[] perVariant = new long[4];
        for (int iter = 0; iter < rows.size(); iter++) {
            CampaignSend s = rows.get(iter);
            boolean out = SENT.equals(s.getStatus());
            boolean open = Tokens.set(s.getOpenedAt()) || Tokens.set(s.getClickedAt());
            if (out) {
                sent++;
            }
            if (open) {
                opened++;
            }
            if (Tokens.set(s.getClickedAt())) {
                clicked++;
            }
            if (s.getOrderId().length() > 0) {
                ordered++;
                revenue += s.getRevenueCents();
            }
            int base = "B".equals(s.getVariant()) ? 2 : "A".equals(s.getVariant()) ? 0 : -1;
            if (base >= 0 && out) {
                perVariant[base]++;
                if (open) {
                    perVariant[base + 1]++;
                }
            }
        }
        return new CampaignStatsDto(c.getId(), rows.size(), sent, opened, clicked, ordered, revenue,
                perVariant[0], perVariant[1], perVariant[2], perVariant[3], c.getWinner());
    }

    /// "A" or "B": the subject with the higher open rate, compared by cross
    /// multiplication so no division is needed; a tie keeps A.
    public static String winner(long sentA, long openedA, long sentB, long openedB) {
        if (sentB == 0) {
            return "A";
        }
        if (sentA == 0) {
            return "B";
        }
        return openedB * sentA > openedA * sentB ? "B" : "A";
    }

    // ----------------------------------------------------------------- job

    @Scheduled(fixedDelay = 60000, initialDelay = 60000, lock = "bazaar-campaigns")
    public void job() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("campaigns: " + failed);
        }
    }

    /// Starts campaigns whose time came, picks A/B winners, sends a batch of
    /// each sending campaign, and closes those with nothing left. Answers
    /// how many messages went out.
    public int tick(Instant now) throws IOException {
        int sent = 0;
        List<Campaign> due = campaigns.findByStatusAndScheduledAtLessThanEqual(SCHEDULED, now);
        for (int iter = 0; iter < due.size(); iter++) {
            start(due.get(iter).getId(), now);
        }
        List<Campaign> testing = campaigns.findByStatus(TESTING);
        for (int iter = 0; iter < testing.size(); iter++) {
            Campaign c = testing.get(iter);
            sent += batch(c, now);
            if (c.getTestSentAt().toEpochMilli() + c.getWinnerAfterMinutes() * MINUTE <= now.toEpochMilli()) {
                pickWinner(c.getId());
            }
        }
        List<Campaign> sending = campaigns.findByStatus(SENDING);
        for (int iter = 0; iter < sending.size(); iter++) {
            Campaign c = sending.get(iter);
            int n = batch(c, now);
            sent += n;
            if (sends.findByCampaignIdAndStatus(c.getId(), QUEUED).isEmpty()) {
                campaigns.move(c.getId(), SENDING, SENT);
            }
        }
        return sent;
    }

    /// Writes one row per consenting member of the segment and moves the
    /// campaign on. The move comes first, so a second job cannot start it too.
    @Transactional(rollbackFor = IOException.class)
    boolean start(String campaignId, Instant now) throws IOException {
        Campaign c = campaigns.findById(campaignId);
        boolean ab = c.getSubjectB().length() > 0 && c.getAbPercent() > 0;
        if (campaigns.move(c.getId(), SCHEDULED, ab ? TESTING : SENDING) != 1) {
            return false;
        }
        List<Audiences.Member> members = audiences.members(c.getStoreId(), c.getAudienceId());
        List<Audiences.Member> reach = new ArrayList<Audiences.Member>();
        for (int iter = 0; iter < members.size(); iter++) {
            Audiences.Member m = members.get(iter);
            String address = address(c, m);
            if (address.length() > 0 && m.acceptsMarketing && consented(c.getStoreId(), address, c.getChannel())) {
                reach.add(m);
            }
        }
        long test = ab ? Math.max(2, reach.size() * c.getAbPercent() / 100) : 0;
        for (int iter = 0; iter < reach.size(); iter++) {
            Audiences.Member m = reach.get(iter);
            CampaignSend s = new CampaignSend();
            s.setId(Ids.next());
            s.setStoreId(c.getStoreId());
            s.setCampaignId(c.getId());
            s.setCustomerId(m.customerId);
            s.setAddress(address(c, m));
            s.setToken(Tokens.token());
            if (!ab) {
                s.setVariant("A");
                s.setStatus(QUEUED);
            } else if (iter < test) {
                // Members come oldest first; alternating keeps both halves alike.
                s.setVariant(iter % 2 == 0 ? "A" : "B");
                s.setStatus(QUEUED);
            } else {
                s.setStatus(HELD);
            }
            sends.insert(s);
        }
        if (ab) {
            c = campaigns.findById(c.getId());
            c.setTestSentAt(now);
            campaigns.save(c);
        }
        return true;
    }

    static String address(Campaign c, Audiences.Member m) {
        return SMS.equals(c.getChannel()) ? Tokens.digits(m.phone).length() >= 6 ? m.phone : ""
                : com.codename1.backend.mail.Mail.isAddress(Tokens.fold(m.email)) ? Tokens.fold(m.email) : "";
    }

    /// Whether an address has not opted out of the channel.
    public boolean consented(String storeId, String address, String channel) {
        MarketingConsent c = consents.findByStoreIdAndAddressAndChannel(storeId, consentKey(address, channel),
                channel);
        return c == null || !UNSUBSCRIBED.equals(c.getStatus());
    }

    static String consentKey(String address, String channel) {
        return SMS.equals(channel) ? Tokens.digits(address) : Tokens.mailbox(address);
    }

    @Transactional(rollbackFor = IOException.class)
    void pickWinner(String campaignId) {
        Campaign c = campaigns.findById(campaignId);
        if (!TESTING.equals(c.getStatus())) {
            return;
        }
        CampaignStatsDto st = stats(c);
        String w = winner(st.sentA(), st.openedA(), st.sentB(), st.openedB());
        if (campaigns.move(c.getId(), TESTING, SENDING) != 1) {
            return;
        }
        c = campaigns.findById(c.getId());
        c.setWinner(w);
        campaigns.save(c);
        List<CampaignSend> held = sends.findByCampaignIdAndStatus(c.getId(), HELD);
        for (int iter = 0; iter < held.size(); iter++) {
            CampaignSend s = held.get(iter);
            s.setVariant(w);
            s.setStatus(QUEUED);
            sends.save(s);
        }
    }

    int batch(Campaign c, Instant now) throws IOException {
        List<CampaignSend> queued = sends.findByCampaignIdAndStatus(c.getId(), QUEUED);
        if (queued.isEmpty()) {
            return 0;
        }
        String secret = secret(c.getStoreId());
        List<CampaignLink> ls = links.findByCampaignIdOrderByPositionAsc(c.getId());
        int sent = 0;
        Map<String, Map<String, String>> variants = new HashMap<String, Map<String, String>>();
        for (int iter = 0; iter < queued.size() && iter < BATCH; iter++) {
            CampaignSend s = queued.get(iter);
            if (sends.move(s.getId(), QUEUED, SENDING, now) != 1) {
                continue;
            }
            String status;
            try {
                status = deliver(c, s, ls, secret, variants) ? SENT : SKIPPED;
            } catch (IOException | RuntimeException failed) {
                status = FAILED;
            }
            sends.move(s.getId(), SENDING, status, now);
            if (SENT.equals(status)) {
                sent++;
            }
        }
        return sent;
    }

    /// The language a subscriber reads: by address for e-mail, by the
    /// customer with that phone number for SMS ([ShopperLanguage#shopper]).
    String languageOf(Campaign c, CampaignSend s) {
        if (SMS.equals(c.getChannel())) {
            List<Customer> found = customers.findByStoreIdAndPhone(c.getStoreId(), s.getAddress());
            Customer k = found == null || found.isEmpty() ? null : found.get(0);
            return k == null ? language.store(c.getStoreId())
                    : language.shopper(c.getStoreId(), k.getUsername(), k.getEmail());
        }
        return language.shopper(c.getStoreId(), null, s.getAddress());
    }

    /// The merchant's translation of the message for `lang` and the
    /// subject variant `ab` ("A" or "B"), or null for the original. Only a
    /// whole translation is used -- body and, for e-mail, that subject -- so
    /// a subscriber never gets a translated body under an untranslated
    /// subject. Looked up once per batch and language.
    Map<String, String> variant(Campaign c, String lang, String ab,
            Map<String, Map<String, String>> cache) throws IOException {
        String key = lang + "/" + ab;
        if (!cache.containsKey(key)) {
            String[] needed = SMS.equals(c.getChannel()) ? new String[] {"body"}
                    : new String[] {"body", "B".equals(ab) ? "subjectB" : "subjectA"};
            cache.put(key, translations.complete(c.getStoreId(),
                    com.codenameone.examples.bazaar.market.Translations.CAMPAIGN, c.getId(), lang,
                    needed));
        }
        return cache.get(key);
    }

    boolean deliver(Campaign c, CampaignSend s, List<CampaignLink> ls, String secret,
            Map<String, Map<String, String>> variants) throws IOException {
        // Consent is asked again at the last moment: an unsubscribe can
        // arrive between the rows being written and the batch going out.
        if (!consented(c.getStoreId(), s.getAddress(), c.getChannel())) {
            return false;
        }
        String unsubscribe = urls.absolute(null, "/api/shop/m/u/" + s.getToken() + "?s="
                + Tokens.sign(secret, "u:" + s.getToken()));
        boolean b = "B".equals(s.getVariant());
        Map<String, String> tr = variant(c, languageOf(c, s), b ? "B" : "A", variants);
        String body = tr == null ? c.getBody() : tr.get("body");
        if (SMS.equals(c.getChannel())) {
            if (!sms.delivers()) {
                return false;
            }
            sms.send(s.getAddress(), body + " Stop: " + unsubscribe);
            return true;
        }
        List<String[]> tracked = new ArrayList<String[]>();
        for (int iter = 0; iter < ls.size(); iter++) {
            tracked.add(new String[] {iter == 0 ? "Shop now" : "Read more",
                    urls.absolute(null, "/api/shop/m/c/" + s.getToken() + "/" + ls.get(iter).getPosition())});
        }
        String subject = tr != null ? tr.get(b ? "subjectB" : "subjectA")
                : b ? c.getSubjectB() : c.getSubjectA();
        return emails.campaign(c.getStoreId(), s.getAddress(), subject, body, tracked,
                urls.absolute(null, "/api/shop/m/o/" + s.getToken() + ".gif"), unsubscribe);
    }

    /// The store's signing secret, made on first use.
    @Transactional(rollbackFor = IOException.class)
    public String secret(String storeId) throws IOException {
        MarketingKey k = keys.findByStoreId(storeId);
        if (k != null) {
            return k.getSecret();
        }
        k = new MarketingKey();
        k.setId(Ids.next());
        k.setStoreId(storeId);
        k.setSecret(Ids.next() + Ids.next());
        keys.insert(k);
        return k.getSecret();
    }

    // ------------------------------------------------------------ tracking

    /// Records an open; only the first counts.
    public void opened(String token) {
        CampaignSend s = token == null ? null : sends.findByToken(token);
        if (s != null) {
            sends.open(s.getId(), Instant.now(), Instant.EPOCH);
        }
    }

    /// Records a click and answers where it goes: one of the campaign's own
    /// links, never an address from the request; null for anything else.
    public String clicked(String token, int position) {
        CampaignSend s = token == null ? null : sends.findByToken(token);
        if (s == null) {
            return null;
        }
        CampaignLink l = links.findByCampaignIdAndPosition(s.getCampaignId(), position);
        if (l == null) {
            return null;
        }
        Instant now = Instant.now();
        sends.click(s.getId(), now, Instant.EPOCH);
        sends.open(s.getId(), now, Instant.EPOCH);
        return l.getUrl().startsWith("/") ? urls.absolute(null, l.getUrl()) : l.getUrl();
    }

    /// Opts the address a message went to out of its channel, when the
    /// signature is the store's. Answers whether it did.
    @Transactional(rollbackFor = IOException.class)
    public boolean unsubscribe(String token, String signature) throws IOException {
        CampaignSend s = token == null ? null : sends.findByToken(token);
        if (s == null || !Tokens.verify(secret(s.getStoreId()), "u:" + token, signature)) {
            return false;
        }
        Campaign c = campaigns.findById(s.getCampaignId());
        setConsent(s.getStoreId(), s.getAddress(), c == null ? EMAIL : c.getChannel(), UNSUBSCRIBED,
                "campaign:" + s.getCampaignId());
        // The customer the message was written for, whatever address their
        // row now holds: segments select on accepts_marketing.
        Customer who = s.getCustomerId().length() == 0 ? null : customers.findById(s.getCustomerId());
        if (who != null && who.isAcceptsMarketing()) {
            who.setAcceptsMarketing(false);
            who.setUpdatedAt(Instant.now());
            customers.save(who);
        }
        return true;
    }

    /// Records an address's consent for a channel.
    @Transactional(rollbackFor = IOException.class)
    public void setConsent(String storeId, String address, String channel, String status, String source)
            throws IOException {
        String key = consentKey(address, channel);
        MarketingConsent m = consents.findByStoreIdAndAddressAndChannel(storeId, key, channel);
        boolean fresh = m == null;
        if (fresh) {
            m = new MarketingConsent();
            m.setId(Ids.next());
            m.setStoreId(storeId);
            m.setAddress(key);
            m.setChannel(channel);
        }
        m.setStatus(status);
        m.setSource(Codes.cut(source, 80));
        m.setCreatedAt(Instant.now());
        if (fresh) {
            consents.insert(m);
        } else {
            consents.save(m);
        }
        if (EMAIL.equals(channel)) {
            marketingFlag(storeId, Tokens.fold(address), SUBSCRIBED.equals(status));
        }
    }

    /// Keeps the store's customer row in step with an e-mail consent: a
    /// segment selects on `accepts_marketing`, so an address that opted in
    /// through a form is a customer who accepts marketing -- made here when
    /// the store has never seen it -- and one that opted out is not. The
    /// consent row itself (source and time) is the record of how.
    void marketingFlag(String storeId, String email, boolean on) throws IOException {
        if (email.length() == 0 || email.length() > 190) {
            return;
        }
        Customer c = customers.findByStoreIdAndEmail(storeId, email);
        Instant now = Instant.now();
        if (c == null) {
            if (!on) {
                return;
            }
            c = new Customer();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setEmail(email);
            c.setAcceptsMarketing(true);
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            customers.insert(c);
            return;
        }
        if (c.isAcceptsMarketing() != on) {
            c.setAcceptsMarketing(on);
            c.setUpdatedAt(now);
            customers.save(c);
        }
    }

    /// Credits an order to the most recent message its customer opened or
    /// clicked in the [#ATTRIBUTION_DAYS] before it. Once per order and once
    /// per message.
    public boolean attribute(String orderId) {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || o.getCustomerId().length() == 0) {
            return false;
        }
        Instant since = o.getCreatedAt().minusMillis(ATTRIBUTION_DAYS * DAY);
        List<CampaignSend> rows = sends.findByStoreIdAndCustomerIdAndSentAtGreaterThanEqual(
                o.getStoreId(), o.getCustomerId(), since);
        CampaignSend best = null;
        for (int iter = 0; iter < rows.size(); iter++) {
            CampaignSend s = rows.get(iter);
            if (orderId.equals(s.getOrderId())) {
                return false;
            }
            boolean engaged = Tokens.set(s.getOpenedAt()) || Tokens.set(s.getClickedAt());
            if (engaged && SENT.equals(s.getStatus()) && s.getOrderId().length() == 0
                    && !s.getSentAt().isAfter(o.getCreatedAt())
                    && (best == null || s.getSentAt().isAfter(best.getSentAt()))) {
                best = s;
            }
        }
        return best != null && sends.attribute(best.getId(), orderId, o.getTotalCents()) == 1;
    }
}
