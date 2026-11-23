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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubItemDto;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.SaleEventDto;
import com.codenameone.examples.bazaar.api.SaleItemDto;
import com.codenameone.examples.bazaar.api.SignupFormDto;
import com.codenameone.examples.bazaar.club.Clubs;
import com.codenameone.examples.bazaar.domain.Campaign;
import com.codenameone.examples.bazaar.domain.CampaignSend;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.repository.AudienceRepository;
import com.codenameone.examples.bazaar.repository.CampaignRepository;
import com.codenameone.examples.bazaar.repository.CampaignSendRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.sale.SaleEvents;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// The growth demo for one store: a three-tier loyalty programme, a paid
/// club, a running sale and an upcoming flash sale, three segments, two
/// campaigns that already went out (with opens, clicks and orders), a
/// signup form and a welcome journey. Once per store: a store that has any
/// segment is left alone.
@Component
public class DemoMarketing {
    static final long DAY = 24L * 3600 * 1000;

    private final AudienceRepository audienceRows;
    private final CampaignRepository campaignRows;
    private final CampaignSendRepository sends;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final VariantRepository variants;
    private final Audiences audiences;
    private final Campaigns campaigns;
    private final SignupForms forms;
    private final Loyalty loyalty;
    private final Clubs clubs;
    private final SaleEvents sales;
    private final Journeys journeys;

    public DemoMarketing(AudienceRepository audienceRows, CampaignRepository campaignRows,
            CampaignSendRepository sends, CustomerRepository customers, StoreOrderRepository orders,
            VariantRepository variants, Audiences audiences, Campaigns campaigns, SignupForms forms,
            Loyalty loyalty, Clubs clubs, SaleEvents sales, Journeys journeys) {
        this.audienceRows = audienceRows;
        this.campaignRows = campaignRows;
        this.sends = sends;
        this.customers = customers;
        this.orders = orders;
        this.variants = variants;
        this.audiences = audiences;
        this.campaigns = campaigns;
        this.forms = forms;
        this.loyalty = loyalty;
        this.clubs = clubs;
        this.sales = sales;
        this.journeys = journeys;
    }

    @Transactional(rollbackFor = IOException.class)
    public void seed(String storeId) throws IOException {
        if (!audienceRows.findByStoreIdOrderByNameAsc(storeId).isEmpty()) {
            return;
        }
        customersFromOrders(storeId);
        Instant now = Instant.now();
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        tiers.add(new LoyaltyTierDto(null, "Bronze", 0, 0, false, 0, "#a0522d", 0));
        tiers.add(new LoyaltyTierDto(null, "Silver", 25000, 500, false, 12, "#90a4ae", 1));
        tiers.add(new LoyaltyTierDto(null, "Gold", 100000, 1000, true, 48, "#f9a825", 2));
        loyalty.save(storeId, new LoyaltyProgramDto(true, 1, 100, LoyaltyMath.DOWN, 100, 500, 500, 365,
                true, "SPEND", 500, 250, tiers));

        // Sorted, so the same pieces go on sale and behind the club every run.
        List<Variant> vs = new ArrayList<Variant>(variants.findByStoreId(storeId));
        java.util.Collections.sort(vs, new java.util.Comparator<Variant>() {
            @Override
            public int compare(Variant a, Variant b) {
                return a.getSku().compareTo(b.getSku());
            }
        });
        ClubDto club = clubs.write(storeId, null, new ClubDto(null, "Insiders", "Ten percent off, first "
                + "look at new arrivals and members-only pieces", 900, 30, 1000, 24, "#6a1b9a", true, 0));
        if (vs.size() > 3) {
            clubs.add(storeId, club.id(), new ClubItemDto(null, vs.get(vs.size() - 1).getProductId(), "",
                    true, 0, 0));
            clubs.add(storeId, club.id(), new ClubItemDto(null, vs.get(vs.size() - 2).getProductId(), "",
                    false, now.toEpochMilli(), now.toEpochMilli() + 3 * DAY));
        }
        if (vs.size() > 7) {
            List<SaleItemDto> running = new ArrayList<SaleItemDto>();
            running.add(new SaleItemDto(vs.get(0).getId(), 0, 0, ""));
            running.add(new SaleItemDto(vs.get(1).getId(), 0, 0, ""));
            sales.insert(storeId, new SaleEventDto(null, "Spring sale", SaleEvents.SALE, SaleEvents.PERCENT,
                    2000, now.toEpochMilli() - DAY, now.toEpochMilli() + 6 * DAY, "", 0, 0, "#2e7d32",
                    running), now);
            List<SaleItemDto> flash = new ArrayList<SaleItemDto>();
            flash.add(new SaleItemDto(vs.get(2).getId(), 0, 0, ""));
            sales.insert(storeId, new SaleEventDto(null, "Friday flash", SaleEvents.FLASH,
                    SaleEvents.PERCENT, 4000, now.toEpochMilli() + 2 * DAY,
                    now.toEpochMilli() + 2 * DAY + 4 * 3600000L, "", 25, 0, "#c62828", flash), now);
        }

        AudienceDto vip = audiences.write(storeId, null, new AudienceDto(null, "Big spenders",
                "{\"op\":\"AND\",\"rules\":[{\"field\":\"totalSpent\",\"op\":\">=\",\"value\":20000}]}", 0));
        AudienceDto lapsed = audiences.write(storeId, null, new AudienceDto(null, "Lapsed 60 days",
                "{\"op\":\"AND\",\"rules\":[{\"field\":\"ordersCount\",\"op\":\">\",\"value\":0},"
                + "{\"field\":\"lastOrderAt\",\"op\":\"notWithinDays\",\"value\":60}]}", 0));
        AudienceDto subscribers = audiences.write(storeId, null, new AudienceDto(null, "Subscribers",
                "{\"op\":\"OR\",\"rules\":[{\"field\":\"acceptsMarketing\",\"op\":\"is\",\"value\":true}]}", 0));

        CampaignDto launch = campaigns.write(storeId, null, new CampaignDto(null, "Spring launch",
                Campaigns.EMAIL, subscribers.id(), "Spring is here", "New season, new pieces", "Our spring "
                + "collection just landed. Take a look before it sells out.", 20, 240, "", 0, "", "#2e7d32",
                Arrays.asList("/collections/new")));
        CampaignDto thanks = campaigns.write(storeId, null, new CampaignDto(null, "VIP thank you",
                Campaigns.EMAIL, vip.id(), "A thank you from us", "", "You are one of our best customers. "
                + "Here is early access to the spring sale.", 0, 0, "", 0, "", "#6a1b9a",
                Arrays.asList("/collections/sale")));
        sent(storeId, launch.id(), "B", now.minusMillis(4 * DAY));
        sent(storeId, thanks.id(), "", now.minusMillis(2 * DAY));

        forms.write(storeId, null, new SignupFormDto(null, "Newsletter", "Ten percent off your first order",
                Arrays.asList("email", "name"), 1000, true, true, "", 0));

        journeys.write(storeId, null, new JourneyDto(null, "Win back lapsed customers", winBack(lapsed.id()),
                Journeys.ACTIVE, 0, null));
    }

    /// Marks a campaign sent and writes the sends it would have made, so
    /// its stats show something: each customer opened about every other
    /// message, clicked one in four, and one in eight ordered.
    /// The seeded orders were placed by no account, so nothing made a
    /// customer of their buyers and the Customers screen, the segments and
    /// the campaigns all read empty. One customer per buyer email, with the
    /// orders and spend those orders add up to, linked back to each order.
    private void customersFromOrders(String storeId) throws IOException {
        if (!customers.findByStoreIdOrderByLastNameAsc(storeId).isEmpty()) {
            return;
        }
        java.util.Map<String, Customer> byEmail = new java.util.LinkedHashMap<String, Customer>();
        List<StoreOrder> linked = new ArrayList<StoreOrder>();
        for (StoreOrder o : orders.findByStoreIdOrderByCreatedAtDesc(storeId)) {
            String email = o.getEmail();
            if (email == null || email.indexOf('@') < 0) {
                continue;
            }
            Customer c = byEmail.get(email);
            if (c == null) {
                c = new Customer();
                c.setId(Ids.next());
                c.setStoreId(storeId);
                c.setUsername(email);
                c.setEmail(email);
                String name = o.getShipName();
                if (name == null || name.length() == 0) {
                    name = email.substring(0, email.indexOf('@'));
                }
                int space = name.lastIndexOf(' ');
                c.setFirstName(space > 0 ? name.substring(0, space) : name);
                c.setLastName(space > 0 ? name.substring(space + 1) : "");
                c.setCreatedAt(o.getCreatedAt());
                byEmail.put(email, c);
            }
            c.setOrdersCount(c.getOrdersCount() + 1);
            c.setSpentCents(c.getSpentCents() + o.getTotalCents());
            c.setUpdatedAt(o.getCreatedAt());
            o.setCustomerId(c.getId());
            linked.add(o);
        }
        for (Customer c : byEmail.values()) {
            customers.insert(c);
        }
        for (StoreOrder o : linked) {
            orders.save(o);
        }
    }

    void sent(String storeId, String campaignId, String winner, Instant at) throws IOException {
        Campaign c = campaignRows.findById(campaignId);
        c.setStatus(Campaigns.SENT);
        c.setScheduledAt(at);
        c.setTestSentAt(winner.length() > 0 ? at : Instant.EPOCH);
        c.setWinner(winner);
        campaignRows.save(c);
        List<Customer> all = customers.findByStoreIdOrderByLastNameAsc(storeId);
        List<StoreOrder> recent = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        int order = 0;
        for (int iter = 0; iter < all.size() && iter < 400; iter++) {
            Customer m = all.get(iter);
            if (m.getEmail().indexOf('@') < 0) {
                continue;
            }
            CampaignSend s = new CampaignSend();
            s.setId(Ids.next());
            s.setStoreId(storeId);
            s.setCampaignId(campaignId);
            s.setCustomerId(m.getId());
            s.setAddress(Tokens.fold(m.getEmail()));
            s.setVariant(winner.length() == 0 ? "A" : iter % 5 == 0 ? "A" : "B");
            s.setToken(Tokens.token());
            s.setStatus(Campaigns.SENT);
            s.setSentAt(at);
            if (iter % 2 == 0) {
                s.setOpenedAt(at.plusMillis(3600000L + iter * 60000L));
            }
            if (iter % 4 == 0) {
                s.setClickedAt(at.plusMillis(3700000L + iter * 60000L));
            }
            if (iter % 8 == 0 && order < recent.size()) {
                s.setOrderId(recent.get(order).getId());
                s.setRevenueCents(recent.get(order).getTotalCents());
                order++;
            }
            sends.insert(s);
        }
    }

    /// Lapsed customers get a mail; three days later those still lapsed get
    /// points and an SMS, the rest a tag.
    static String winBack(String segmentId) {
        return "{\"nodes\":["
                + node("entry", JourneyCatalog.ENTRY_SEGMENT, "\"segmentId\":\"" + segmentId + "\"")
                + "," + node("mail", JourneyCatalog.SEND_EMAIL, "\"subject\":\"We miss you\","
                + "\"body\":\"It has been a while. Here is what is new since your last visit.\"")
                + "," + node("wait", JourneyCatalog.WAIT, "\"minutes\":4320")
                + "," + node("still", JourneyCatalog.SPLIT_CONDITION, "\"segmentId\":\"" + segmentId + "\"")
                + "," + node("points", JourneyCatalog.POINTS_ADD, "\"points\":200")
                + "," + node("sms", JourneyCatalog.SEND_SMS, "\"body\":\"200 bonus points are waiting for you.\"")
                + "," + node("tag", JourneyCatalog.TAG_ADD, "\"tag\":\"won back\"")
                + "," + node("exit", JourneyCatalog.EXIT, "")
                + "],\"edges\":["
                + edge("entry", "out", "mail") + "," + edge("mail", "out", "wait") + ","
                + edge("wait", "out", "still") + "," + edge("still", "yes", "points") + ","
                + edge("still", "no", "tag") + "," + edge("points", "out", "sms") + ","
                + edge("sms", "out", "exit") + "," + edge("tag", "out", "exit") + "]}";
    }

    static String node(String id, String type, String params) {
        return "{\"id\":\"" + id + "\",\"data\":{\"type\":\"" + type + "\""
                + (params.length() == 0 ? "" : "," + params) + "}}";
    }

    static String edge(String from, String port, String to) {
        return "{\"from\":\"" + from + "\",\"fromPort\":\"" + port + "\",\"to\":\"" + to
                + "\",\"toPort\":\"in\"}";
    }
}
