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
package com.codenameone.examples.bazaar.web.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.FulfilmentServiceOrderDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.RoutingDto;
import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.routing.OrderRouting;
import com.codenameone.examples.bazaar.routing.Router;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The back office's shipping setup: shipping profiles (groups of products
/// with zones and rates of their own, priced separately and added up for a
/// mixed cart), the order routing rules that pick the location fulfilling
/// each line, the fulfilment service (3PL) and the orders handed to it.
/// Zones and rates themselves are edited in the app.
@Controller
public class ShippingOpsPages {
    /// The rule kinds as the page names them.
    static final String[][] KINDS = {
        {Router.CLOSEST, "Closest location to the customer"},
        {Router.PRIORITY, "Location priority list"},
        {Router.MOST_STOCK, "Location with the most stock"},
    };

    private final WebPages pages;
    private final RateSettings rates;
    private final OrderRouting routing;
    private final Stores stores;

    public ShippingOpsPages(WebPages pages, RateSettings rates, OrderRouting routing,
            Stores stores) {
        this.pages = pages;
        this.rates = rates;
        this.routing = routing;
        this.stores = stores;
    }

    @GetMapping("/admin/{storeId}/shipping")
    public String show(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        Shell shell = pages.admin(request, storeId, "Shipping");
        String who = Caller.name();
        String base = "/admin/" + storeId + "/shipping";
        List<ShippingProfileDto> profiles = rates.profiles(who, storeId);
        List<ShippingZoneDto> zones = rates.zones(who, storeId);
        List<String[]> products = rates.productChoices(who, storeId);
        List<LocationDto> places = stores.locations(who, storeId);
        RoutingDto r = routing.routing(who, storeId);
        MarketingView v = new MarketingView();
        v.page = "shipping";
        v.base = "/admin/" + storeId;
        v.heading = "Shipping";
        v.intro = "Products in a shipping profile are priced with that profile's zones and "
                + "rates; a cart mixing profiles pays the sum of each profile's cheapest rate. "
                + "Zones and rates are edited in the app.";
        v.cardsTitle = "Shipping profiles and routing rules";
        for (int iter = 0; iter < profiles.size(); iter++) {
            ShippingProfileDto p = profiles.get(iter);
            ActionCard c = new ActionCard();
            c.title = "Profile: " + p.name();
            c.detail = p.productIds().size() + (p.productIds().size() == 1 ? " product"
                    : " products") + ", zones: " + zoneNames(zones, p.id());
            c.button("Delete", base + "/profiles/" + p.id() + "/delete", true);
            v.cards.add(c);
        }
        for (int iter = 0; iter < r.rules().size(); iter++) {
            RoutingRuleDto rule = r.rules().get(iter);
            ActionCard c = new ActionCard();
            c.title = "Rule " + (iter + 1) + ": " + kindName(rule.kind());
            c.detail = Router.PRIORITY.equals(rule.kind())
                    ? placeNames(places, OrderRouting.ids(rule.locationIds())) : "";
            c.button("Delete", base + "/rules/" + rule.id() + "/delete", true);
            v.cards.add(c);
        }
        FormView add = new FormView("profile", "New shipping profile", base + "/profiles",
                "Create profile");
        add.add("name", "Name", "text", "").hint("For example Oversized or Fragile.");
        productBoxes(add, products, profiles, null);
        v.forms.add(add);
        for (int iter = 0; iter < profiles.size(); iter++) {
            ShippingProfileDto p = profiles.get(iter);
            FormView edit = new FormView("profile-" + p.id(), "Edit profile: " + p.name(),
                    base + "/profiles/" + p.id(), "Save profile");
            edit.add("name", "Name", "text", p.name());
            productBoxes(edit, products, profiles, p);
            v.forms.add(edit);
        }
        FormView settings = new FormView("routing", "Order routing", base + "/routing",
                "Save routing");
        settings.text = "Each line of an online order ships from a location holding its "
                + "units; the rules below choose among them, the first rule deciding and the "
                + "next breaking its ties.";
        settings.check("allowSplit", "Allow an order to ship from several locations",
                r.allowSplit());
        FormField svc = settings.add("service", "Fulfilment service (3PL)", "select",
                r.service());
        svc.options.add(option("", "None", r.service().length() == 0));
        svc.options.add(option(OrderRouting.SimulatedServiceName.NAME,
                "Simulated fulfilment service", OrderRouting.SimulatedServiceName.NAME
                    .equals(r.service())));
        FormField at = settings.add("serviceLocationId", "Location the service runs",
                "select", r.serviceLocationId());
        at.options.add(option("", "None", r.serviceLocationId().length() == 0));
        for (int iter = 0; iter < places.size(); iter++) {
            LocationDto l = places.get(iter);
            at.options.add(option(l.id(), l.name(), l.id().equals(r.serviceLocationId())));
        }
        at.hint("Orders routed to this location are handed to the service, which ships "
                + "them and reports the carrier and tracking number.");
        v.forms.add(settings);
        FormView rule = new FormView("rule", "Add a routing rule", base + "/rules", "Add rule");
        FormField kind = rule.add("kind", "Rule", "select", Router.CLOSEST);
        for (int iter = 0; iter < KINDS.length; iter++) {
            kind.options.add(option(KINDS[iter][0], KINDS[iter][1], iter == 0));
        }
        for (int iter = 0; iter < places.size(); iter++) {
            rule.add("l-" + places.get(iter).id(), "Priority of " + places.get(iter).name(),
                    "number", "");
        }
        rule.text = "For a priority list, number the locations 1, 2, 3; a blank one comes "
                + "after every numbered one.";
        v.forms.add(rule);
        List<FulfilmentServiceOrderDto> handed = routing.serviceOrders(who, storeId);
        v.rowsTitle = "Orders handed to the fulfilment service";
        v.rowsEmpty = "Nothing has been handed to the service yet.";
        v.headA = "Order";
        v.headB = "Status";
        v.headC = "Tracking";
        v.headD = "Reference";
        for (int iter = 0; iter < handed.size(); iter++) {
            FulfilmentServiceOrderDto o = handed.get(iter);
            AdminRow row = new AdminRow();
            row.title = "#" + o.orderNumber();
            row.detail = o.status();
            row.meta = o.trackingNumber().length() == 0 ? "" : o.carrier() + " "
                    + o.trackingNumber();
            row.amount = o.externalId();
            v.rows.add(row);
        }
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        model.addAttribute("view", v);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    private static void productBoxes(FormView form, List<String[]> products,
            List<ShippingProfileDto> profiles, ShippingProfileDto mine) {
        for (int iter = 0; iter < products.size(); iter++) {
            String id = products.get(iter)[0];
            String in = "";
            boolean on = false;
            for (int p = 0; p < profiles.size(); p++) {
                if (profiles.get(p).productIds().contains(id)) {
                    on = mine != null && profiles.get(p).id().equals(mine.id());
                    in = on ? "" : " (now in " + profiles.get(p).name() + ")";
                }
            }
            form.check("p-" + id, products.get(iter)[1] + in, on);
        }
    }

    private static String zoneNames(List<ShippingZoneDto> zones, String profileId) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < zones.size(); iter++) {
            if (profileId.equals(zones.get(iter).profileId())) {
                b.append(b.length() == 0 ? "" : ", ").append(zones.get(iter).name());
            }
        }
        return b.length() == 0 ? "none yet (add them in the app)" : b.toString();
    }

    static String kindName(String kind) {
        for (int iter = 0; iter < KINDS.length; iter++) {
            if (KINDS[iter][0].equals(kind)) {
                return KINDS[iter][1];
            }
        }
        return kind;
    }

    private static String placeNames(List<LocationDto> places, List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            String name = ids.get(iter);
            for (int p = 0; p < places.size(); p++) {
                if (places.get(p).id().equals(ids.get(iter))) {
                    name = places.get(p).name();
                }
            }
            b.append(iter == 0 ? "" : ", ").append(name);
        }
        return b.toString();
    }

    private static AdminRow option(String id, String title, boolean selected) {
        AdminRow r = new AdminRow();
        r.id = id;
        r.title = title;
        r.flag = selected;
        return r;
    }

    // ------------------------------------------------------------- actions

    @PostMapping("/admin/{storeId}/shipping/profiles")
    public String addProfile(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return saveProfile(storeId, null, request);
    }

    @PostMapping("/admin/{storeId}/shipping/profiles/{profileId}")
    public String editProfile(@PathVariable("storeId") String storeId,
            @PathVariable("profileId") String profileId, HttpServer.Request request)
            throws Exception {
        return saveProfile(storeId, profileId, request);
    }

    private String saveProfile(String storeId, String profileId, HttpServer.Request request)
            throws Exception {
        pages.admin(request, storeId, "");
        String who = Caller.name();
        try {
            List<String> ids = new ArrayList<String>();
            List<String[]> products = rates.productChoices(who, storeId);
            for (int iter = 0; iter < products.size(); iter++) {
                if ("1".equals(AdminForms.field(request, "p-" + products.get(iter)[0]))) {
                    ids.add(products.get(iter)[0]);
                }
            }
            int position = 0;
            List<ShippingProfileDto> all = rates.profiles(who, storeId);
            for (int iter = 0; iter < all.size(); iter++) {
                if (all.get(iter).id().equals(profileId)) {
                    position = all.get(iter).position();
                } else if (profileId == null) {
                    position = Math.max(position, all.get(iter).position() + 1);
                }
            }
            String name = AdminForms.field(request, "name").trim();
            if (name.length() == 0) {
                throw new ResponseStatusException(400, "Name the profile");
            }
            rates.saveProfile(who, storeId, new ShippingProfileDto(profileId, name, position,
                    ids));
            WebPages.toast(request, "Shipping profile saved");
        } catch (ResponseStatusException e) {
            failed(request, e);
        }
        return "redirect:/admin/" + storeId + "/shipping";
    }

    @PostMapping("/admin/{storeId}/shipping/profiles/{profileId}/delete")
    public String deleteProfile(@PathVariable("storeId") String storeId,
            @PathVariable("profileId") String profileId, HttpServer.Request request)
            throws Exception {
        pages.admin(request, storeId, "");
        try {
            rates.deleteProfile(Caller.name(), storeId, profileId);
            WebPages.toast(request, "Shipping profile deleted");
        } catch (ResponseStatusException e) {
            failed(request, e);
        }
        return "redirect:/admin/" + storeId + "/shipping";
    }

    @PostMapping("/admin/{storeId}/shipping/routing")
    public String saveRouting(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "");
        try {
            routing.save(Caller.name(), storeId, new RoutingDto(
                    "1".equals(AdminForms.field(request, "allowSplit")),
                    AdminForms.field(request, "serviceLocationId"),
                    AdminForms.field(request, "service"), new ArrayList<RoutingRuleDto>()));
            WebPages.toast(request, "Routing saved");
        } catch (ResponseStatusException e) {
            failed(request, e);
        }
        return "redirect:/admin/" + storeId + "/shipping";
    }

    @PostMapping("/admin/{storeId}/shipping/rules")
    public String addRule(@PathVariable("storeId") String storeId, HttpServer.Request request)
            throws Exception {
        pages.admin(request, storeId, "");
        String who = Caller.name();
        try {
            List<LocationDto> places = stores.locations(who, storeId);
            List<String> ids = new ArrayList<String>();
            List<Integer> ranks = new ArrayList<Integer>();
            for (int iter = 0; iter < places.size(); iter++) {
                String typed = AdminForms.field(request, "l-" + places.get(iter).id()).trim();
                if (typed.length() == 0) {
                    continue;
                }
                int rank = AdminForms.number(typed, 0, "A priority");
                int at = 0;
                while (at < ranks.size() && ranks.get(at).intValue() <= rank) {
                    at++;
                }
                ranks.add(at, Integer.valueOf(rank));
                ids.add(at, places.get(iter).id());
            }
            StringBuilder list = new StringBuilder();
            for (int iter = 0; iter < ids.size(); iter++) {
                list.append(iter == 0 ? "" : ",").append(ids.get(iter));
            }
            List<RoutingRuleDto> rules = routing.routing(who, storeId).rules();
            int position = 0;
            for (int iter = 0; iter < rules.size(); iter++) {
                position = Math.max(position, rules.get(iter).position() + 1);
            }
            routing.saveRule(who, storeId, new RoutingRuleDto(null,
                    AdminForms.field(request, "kind"), list.toString(), position));
            WebPages.toast(request, "Routing rule added");
        } catch (ResponseStatusException e) {
            failed(request, e);
        }
        return "redirect:/admin/" + storeId + "/shipping";
    }

    @PostMapping("/admin/{storeId}/shipping/rules/{ruleId}/delete")
    public String deleteRule(@PathVariable("storeId") String storeId,
            @PathVariable("ruleId") String ruleId, HttpServer.Request request) throws Exception {
        pages.admin(request, storeId, "");
        try {
            routing.deleteRule(Caller.name(), storeId, ruleId);
            WebPages.toast(request, "Routing rule deleted");
        } catch (ResponseStatusException e) {
            failed(request, e);
        }
        return "redirect:/admin/" + storeId + "/shipping";
    }

    private static void failed(HttpServer.Request request, ResponseStatusException e)
            throws IOException {
        int status = e.getStatus();
        if (status == 401 || status == 403 || status == 404) {
            throw e;
        }
        WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
    }
}
