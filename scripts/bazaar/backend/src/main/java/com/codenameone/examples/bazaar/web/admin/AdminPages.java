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
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.docs.DocumentPages;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;

import java.util.List;

/// The merchant back office under `/admin/{store}`: dashboard, orders,
/// products and stock, customers.
///
/// The same shape as the account pages: every link a page, every change a form
/// post answered with 303 to the page it came from and a one-line message, so
/// it all works with scripts off; htmx boosts the links and swaps only a
/// table when one is sorted, filtered or paged. The paths are in the web app's
/// signed-in chain (`WebSecurity`): the MERCHANT role to enter, CSRF on every
/// post, a nonce CSP, `no-store`. Which store and which rows is never this
/// class's decision: [WebPages#admin] answers 404 for a store the caller does
/// not work at, and [AdminData] goes through the services, which check the
/// role at the store and that every row is the store's.
@Controller
public class AdminPages {
    private final WebPages pages;
    private final AdminData data;
    private final WebSessions sessions;
    private final Stores stores;
    private final Parcels parcels;
    private final Fulfillments fulfillments;
    private final DocumentPages documents;

    public AdminPages(WebPages pages, AdminData data, WebSessions sessions, Stores stores,
            Parcels parcels, Fulfillments fulfillments, DocumentPages documents) {
        this.pages = pages;
        this.data = data;
        this.sessions = sessions;
        this.stores = stores;
        this.parcels = parcels;
        this.fulfillments = fulfillments;
        this.documents = documents;
    }

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            AdminView view, DataTable table) throws java.io.IOException {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        view.base = "/admin/" + storeId;
        model.addAttribute("view", view);
        model.addAttribute("table", table == null ? new DataTable() : table);
        return WebPages.view(model, shell, "admin");
    }

    private static AdminView view(String page) {
        AdminView v = new AdminView();
        v.page = page;
        return v;
    }

    /// The signed-in name, after checking this session was not signed out
    /// elsewhere, and that the caller works at `storeId` (a post renders no
    /// shell, so it checks here).
    private void member(HttpServer.Request request, String storeId) throws Exception {
        String me = Caller.name();
        sessions.touch(request, me);
        data.currency(storeId);
    }

    private interface Change {
        void run() throws Exception;
    }

    /// Runs a change and leaves its message for the page `back`. A refusal the
    /// merchant can act on (a quantity too large, a bad amount) is the message
    /// instead; a store or row that is not theirs (404) and a role that may not
    /// do it (403) answer their status, never a cheerful redirect.
    private static String change(HttpServer.Request request, String back, String done,
            Change c) throws Exception {
        try {
            c.run();
            WebPages.toast(request, done);
        } catch (ResponseStatusException e) {
            int s = e.getStatus();
            if (s == 401 || s == 403 || s == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        }
        return "redirect:" + back;
    }

    /// The back office of the first store the caller works at.
    @GetMapping("/admin")
    public String home() throws Exception {
        List<StoreRoleDto> mine = stores.myStores(Caller.name());
        if (mine.isEmpty()) {
            throw new ResponseStatusException(404, "No store yet");
        }
        return "redirect:/admin/" + mine.get(0).store().id();
    }

    @GetMapping("/admin/{storeId}")
    public String dashboard(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        AdminView v = view("dashboard");
        data.currency(storeId);
        data.dashboard(storeId, v);
        return render(model, request, storeId, "Dashboard", v, null);
    }

    /// Any of the four tables as CSV: every row the same filters, search and
    /// sort select, not only the page on screen.
    @GetMapping("/admin/{storeId}/export/{table}")
    public HttpServer.Response export(@PathVariable("storeId") String storeId,
            @PathVariable("table") String table, HttpServer.Request request) throws Exception {
        member(request, storeId);
        String base = "/admin/" + storeId + "/" + table;
        AdminData.Tabled t;
        if ("orders".equals(table)) {
            t = data.orders(storeId, request, base, true);
        } else if ("products".equals(table)) {
            t = data.products(storeId, request, base, true);
        } else if ("inventory".equals(table)) {
            t = data.inventory(storeId, request, base, true);
        } else if ("customers".equals(table)) {
            t = data.customers(storeId, request, base, true);
        } else {
            throw new ResponseStatusException(404, "No such table");
        }
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return AdminData.csv(t.csv, table);
    }

    // --------------------------------------------------------------- orders

    @GetMapping("/admin/{storeId}/orders")
    public String orders(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        if ("ARCHIVED".equals(AdminForms.field(request, "status"))) {
            pages.admin(request, storeId, "");
            return "redirect:/admin/" + storeId + "/archived-orders";
        }
        DataTable t = data.orders(storeId, request, "/admin/" + storeId + "/orders", false).table;
        return render(model, request, storeId, "Orders", view("orders"), t);
    }

    @GetMapping("/admin/{storeId}/orders/{orderId}")
    public String order(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, Model model, HttpServer.Request request)
            throws Exception {
        AdminView v = view("order");
        data.order(storeId, orderId, v);
        parcels.fill(storeId, orderId, v, data.currency(storeId));
        return render(model, request, storeId, v.order.title, v, null);
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/fulfil")
    public String fulfil(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Fulfilment created",
                () -> data.fulfil(storeId, orderId, request));
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/refund")
    public String refund(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Refund issued",
                () -> data.refund(storeId, orderId, request));
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/cancel")
    public String cancel(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Order cancelled",
                () -> data.cancel(storeId, orderId));
    }

    // ------------------------------------------------------------ fulfilment

    /// Packs a parcel; the weight (grams) is optional and prices its label.
    @PostMapping("/admin/{storeId}/orders/{orderId}/parcels/{id}/pack")
    public String pack(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        int grams = AdminForms.number(AdminForms.field(request, "weight"), 0, "the weight");
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Parcel packed",
                () -> fulfillments.pack(Caller.name(), storeId, id, new PackDto(grams)));
    }

    /// Buys the label for the carrier service picked from the rates shown.
    @PostMapping("/admin/{storeId}/orders/{orderId}/parcels/{id}/label")
    public String label(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        String service = AdminForms.field(request, "service");
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Label bought",
                () -> fulfillments.buyLabel(Caller.name(), storeId, id,
                        new LabelRequestDto(service)));
    }

    @PostMapping("/admin/{storeId}/orders/{orderId}/parcels/{id}/ship")
    public String ship(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/orders/" + orderId, "Marked shipped",
                () -> fulfillments.ship(Caller.name(), storeId, id));
    }

    /// The packing slip, through the same document service as the app's
    /// (which checks the role and that the order is the store's).
    @GetMapping("/admin/{storeId}/orders/{orderId}/packing-slip.pdf")
    public HttpServer.Response packingSlip(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        String parcel = AdminForms.field(request, "fulfillment");
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return documents.packingSlip(storeId, orderId, parcel.length() == 0 ? null : parcel);
    }

    @GetMapping("/admin/{storeId}/orders/{orderId}/parcels/{id}/label.pdf")
    public HttpServer.Response labelPdf(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        member(request, storeId);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        return documents.label(storeId, id);
    }

    // ------------------------------------------------------------- products

    @GetMapping("/admin/{storeId}/products")
    public String products(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        DataTable t = data.products(storeId, request, "/admin/" + storeId + "/products", false).table;
        return render(model, request, storeId, "Products", view("products"), t);
    }

    @GetMapping("/admin/{storeId}/products/{productId}")
    public String product(@PathVariable("storeId") String storeId,
            @PathVariable("productId") String productId, Model model, HttpServer.Request request)
            throws Exception {
        AdminView v = view("product");
        data.product(storeId, productId, v);
        return render(model, request, storeId, v.title, v, null);
    }

    @PostMapping("/admin/{storeId}/products/{productId}")
    public String saveProduct(@PathVariable("storeId") String storeId,
            @PathVariable("productId") String productId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/products/" + productId, "Product saved",
                () -> data.saveProduct(storeId, productId, request));
    }

    @PostMapping("/admin/{storeId}/products/{productId}/adjust")
    public String adjust(@PathVariable("storeId") String storeId,
            @PathVariable("productId") String productId, HttpServer.Request request)
            throws Exception {
        member(request, storeId);
        return change(request, "/admin/" + storeId + "/products/" + productId, "Stock adjusted",
                () -> data.adjust(storeId, productId, request));
    }

    // ------------------------------------------------------------ inventory

    @GetMapping("/admin/{storeId}/inventory")
    public String inventory(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        DataTable t = data.inventory(storeId, request, "/admin/" + storeId + "/inventory", false).table;
        return render(model, request, storeId, "Inventory", view("inventory"), t);
    }

    // ------------------------------------------------------------ customers

    @GetMapping("/admin/{storeId}/customers")
    public String customers(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        DataTable t = data.customers(storeId, request, "/admin/" + storeId + "/customers", false).table;
        return render(model, request, storeId, "Customers", view("customers"), t);
    }

    @GetMapping("/admin/{storeId}/customers/{customerId}")
    public String customer(@PathVariable("storeId") String storeId,
            @PathVariable("customerId") String customerId, Model model,
            HttpServer.Request request) throws Exception {
        AdminView v = view("customer");
        String name = data.customer(storeId, customerId, v);
        return render(model, request, storeId, name, v, null);
    }
}
