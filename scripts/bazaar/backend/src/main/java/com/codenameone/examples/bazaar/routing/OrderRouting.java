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
package com.codenameone.examples.bazaar.routing;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.FulfilmentServiceOrderDto;
import com.codenameone.examples.bazaar.api.RoutingDto;
import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A store's order routing: its rules and settings, kept for OWNER and
/// MANAGER, and the decision itself ([#plan]) for everything that places an
/// online order -- checkout, a converted draft, a trade order and a
/// marketplace order. [Router] holds the arithmetic.
///
/// A store that never configured routing (no settings row, no rules) is
/// routed by [Inventory#pickLocation] line by line, exactly as before.
@Component
public class OrderRouting {
    /// The fulfilment services a store can choose; empty is none.
    public static final String[] SERVICES = {"", SimulatedServiceName.NAME};

    private final DataSource db;
    private final StoreAccess access;
    private final LocationRepository locations;
    private final Inventory inventory;

    public OrderRouting(DataSource db, StoreAccess access, LocationRepository locations,
            Inventory inventory) {
        this.db = db;
        this.access = access;
        this.locations = locations;
        this.inventory = inventory;
    }

    /// The name of the simulated fulfilment service, kept here so that this
    /// package does not depend on the fulfilment one.
    public static final class SimulatedServiceName {
        public static final String NAME = "SIMULATED";

        private SimulatedServiceName() {
        }
    }

    // ------------------------------------------------------------ settings

    @Transactional(readOnly = true)
    public RoutingDto routing(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return read(storeId);
    }

    /// The settings and rules of a store, without an access check.
    @Transactional(readOnly = true)
    public RoutingDto read(String storeId) throws IOException {
        Map s = db.queryOne("SELECT allow_split, service, service_location_id FROM "
                + "routing_setting WHERE store_id = ?", new Object[] {storeId});
        List<RoutingRuleDto> rules = new ArrayList<RoutingRuleDto>();
        List rows = db.query("SELECT id, kind, location_ids, position FROM routing_rule "
                + "WHERE store_id = ? ORDER BY position, id", new Object[] {storeId});
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            rules.add(new RoutingRuleDto(Rows.text(r.get("id")), Rows.text(r.get("kind")),
                    Rows.text(r.get("location_ids")), (int) Rows.number(r.get("position"))));
        }
        return new RoutingDto(s == null || Rows.number(s.get("allow_split")) != 0,
                s == null ? "" : Rows.text(s.get("service_location_id")),
                s == null ? "" : Rows.text(s.get("service")), rules);
    }

    @Transactional(rollbackFor = IOException.class)
    public RoutingDto save(String username, String storeId, RoutingDto w) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (w == null) {
            throw new ResponseStatusException(400, "Describe the routing");
        }
        String service = w.service() == null ? "" : w.service().trim();
        boolean known = false;
        for (int iter = 0; iter < SERVICES.length; iter++) {
            known |= SERVICES[iter].equals(service);
        }
        if (!known) {
            throw new ResponseStatusException(400, "That is not a fulfilment service");
        }
        String at = w.serviceLocationId() == null ? "" : w.serviceLocationId().trim();
        if (service.length() > 0 && at.length() == 0) {
            throw new ResponseStatusException(400, "Choose the location the service runs");
        }
        if (at.length() > 0) {
            location(storeId, at);
        }
        db.execute("DELETE FROM routing_setting WHERE store_id = ?", new Object[] {storeId});
        db.execute("INSERT INTO routing_setting (store_id, allow_split, service, "
                + "service_location_id, updated_at) VALUES (?, ?, ?, ?, ?)", new Object[] {storeId,
                    Integer.valueOf(w.allowSplit() ? 1 : 0), service, service.length() == 0 ? ""
                        : at, Long.valueOf(System.currentTimeMillis())});
        return read(storeId);
    }

    private Location location(String storeId, String id) throws IOException {
        Location l = locations.findById(id);
        if (l == null || !storeId.equals(l.getStoreId())) {
            throw new ResponseStatusException(404, "No such location");
        }
        return l;
    }

    @Transactional(rollbackFor = IOException.class)
    public RoutingRuleDto saveRule(String username, String storeId, RoutingRuleDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String kind = w == null || w.kind() == null ? "" : w.kind().trim();
        boolean known = false;
        for (int iter = 0; iter < Router.KINDS.length; iter++) {
            known |= Router.KINDS[iter].equals(kind);
        }
        if (!known) {
            throw new ResponseStatusException(400, "That is not a routing rule");
        }
        List<String> ids = ids(w.locationIds());
        for (int iter = 0; iter < ids.size(); iter++) {
            location(storeId, ids.get(iter));
        }
        if (Router.PRIORITY.equals(kind) && ids.isEmpty()) {
            throw new ResponseStatusException(400, "A priority rule lists its locations");
        }
        String list = Router.PRIORITY.equals(kind) ? join(ids) : "";
        String id = w.id() == null ? "" : w.id();
        int position = Math.max(0, w.position());
        if (id.length() == 0) {
            id = Ids.next();
            db.execute("INSERT INTO routing_rule (id, store_id, kind, location_ids, position) "
                    + "VALUES (?, ?, ?, ?, ?)", new Object[] {id, storeId, kind, list,
                        Integer.valueOf(position)});
        } else {
            rule(storeId, id);
            db.execute("UPDATE routing_rule SET kind = ?, location_ids = ?, position = ? "
                    + "WHERE id = ?", new Object[] {kind, list, Integer.valueOf(position), id});
        }
        return new RoutingRuleDto(id, kind, list, position);
    }

    private Map rule(String storeId, String id) throws IOException {
        Map r = id == null ? null : db.queryOne("SELECT id, kind, location_ids, position FROM "
                + "routing_rule WHERE id = ? AND store_id = ?", new Object[] {id, storeId});
        if (r == null) {
            throw new ResponseStatusException(404, "No such routing rule");
        }
        return r;
    }

    @Transactional(rollbackFor = IOException.class)
    public RoutingRuleDto deleteRule(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Map r = rule(storeId, id);
        db.execute("DELETE FROM routing_rule WHERE id = ?", new Object[] {id});
        return new RoutingRuleDto(id, Rows.text(r.get("kind")), Rows.text(r.get("location_ids")),
                (int) Rows.number(r.get("position")));
    }

    /// What the store handed to its fulfilment service, newest first.
    @Transactional(readOnly = true)
    public List<FulfilmentServiceOrderDto> serviceOrders(String username, String storeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        List rows = db.query("SELECT f.id, f.order_id, o.number, f.service, f.external_id, "
                + "f.status, f.carrier, f.tracking_number, f.created_at FROM "
                + "fulfilment_service_order f JOIN store_order o ON o.id = f.order_id "
                + "WHERE f.store_id = ? ORDER BY f.created_at DESC, f.id", new Object[] {storeId});
        List<FulfilmentServiceOrderDto> out = new ArrayList<FulfilmentServiceOrderDto>();
        for (int iter = 0; iter < rows.size() && iter < 200; iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new FulfilmentServiceOrderDto(Rows.text(r.get("id")),
                    Rows.text(r.get("order_id")), Rows.number(r.get("number")),
                    Rows.text(r.get("service")), Rows.text(r.get("external_id")),
                    Rows.text(r.get("status")), Rows.text(r.get("carrier")),
                    Rows.text(r.get("tracking_number")), Rows.number(r.get("created_at"))));
        }
        return out;
    }

    /// The ids of a comma separated list, trimmed, blanks and repeats dropped.
    public static List<String> ids(String list) {
        List<String> out = new ArrayList<String>();
        if (list == null) {
            return out;
        }
        int start = 0;
        while (true) {
            int comma = list.indexOf(',', start);
            String one = list.substring(start, comma < 0 ? list.length() : comma).trim();
            if (one.length() > 0 && !out.contains(one)) {
                out.add(one);
            }
            if (comma < 0) {
                return out;
            }
            start = comma + 1;
        }
    }

    private static String join(List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            if (iter > 0) {
                b.append(',');
            }
            b.append(ids.get(iter));
        }
        return b.toString();
    }

    // ------------------------------------------------------------ decision

    /// Where each line ships from: `variantIds[i]`, `quantities[i]` units;
    /// a null variant id is a line that holds no stock and gets null. The
    /// whole answer is null for a line when the store has no location that
    /// fulfils online orders. `hasPoint`/`lat`/`lng` are the delivery
    /// address, for CLOSEST.
    @Transactional(readOnly = true)
    public Router.Plan plan(String storeId, String[] variantIds, int[] quantities,
            boolean hasPoint, double lat, double lng) throws IOException {
        RoutingDto settings = read(storeId);
        String[] out = new String[variantIds.length];
        if (settings.rules().isEmpty() && settings.allowSplit()) {
            for (int iter = 0; iter < variantIds.length; iter++) {
                out[iter] = variantIds[iter] == null ? null
                        : inventory.pickLocation(storeId, variantIds[iter], quantities[iter]);
            }
            return new Router.Plan(out, false);
        }
        List<Router.Place> places = new ArrayList<Router.Place>();
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            Location l = all.get(iter);
            if (l.isActive() && l.isFulfilsOnline()) {
                places.add(new Router.Place(l.getId(), l.getLat(), l.getLng()));
            }
        }
        List<Router.Need> needs = new ArrayList<Router.Need>();
        List<Integer> index = new ArrayList<Integer>();
        for (int iter = 0; iter < variantIds.length; iter++) {
            if (variantIds[iter] == null) {
                continue;
            }
            int[] available = new int[places.size()];
            for (int p = 0; p < places.size(); p++) {
                available[p] = inventory.routable(storeId, variantIds[iter], places.get(p).id);
            }
            needs.add(new Router.Need(quantities[iter], available));
            index.add(Integer.valueOf(iter));
        }
        List<Router.Rule> rules = new ArrayList<Router.Rule>();
        for (int iter = 0; iter < settings.rules().size(); iter++) {
            RoutingRuleDto r = settings.rules().get(iter);
            rules.add(new Router.Rule(r.kind(), ids(r.locationIds())));
        }
        Router.Plan plan = Router.route(places, needs, rules, settings.allowSplit(), hasPoint,
                lat, lng);
        for (int iter = 0; iter < index.size(); iter++) {
            out[index.get(iter).intValue()] = plan.locations[iter];
        }
        return new Router.Plan(out, plan.forcedSplit);
    }
}
