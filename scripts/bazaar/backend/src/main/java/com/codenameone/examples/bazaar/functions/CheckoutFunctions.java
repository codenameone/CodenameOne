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
package com.codenameone.examples.bazaar.functions;

import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.WebClient;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.PaymentTypeDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.platform.StoreApiKeys;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Runs the checkout functions of a store's installed apps after the
/// checkout's own logic, and applies what they emit.
///
/// A function runs only while its app is installed with `write_functions`;
/// both are read on every quote, so removing the app or narrowing its grant
/// stops it at once. Functions of one target run in upload order, each
/// seeing what the ones before it did. A function that fails -- out of fuel,
/// too much output, arithmetic out of range, a remote that does not answer
/// in time or answers nonsense -- is logged ([#lastError]) and its whole
/// output dropped, so the checkout behaves as it would without it.
///
/// A remote function is an HTTPS address the app answers: it is posted the
/// same record as JSON ([FunctionInput#toJson]), signed like the app's
/// webhooks, and answers `{"ops":[{"op":"hide","args":["rate-id"]}]}`,
/// checked against the target exactly as a script's emits are. Answers are
/// cached for [#CACHE_MILLIS] by the hash of what was sent.
@Component
public class CheckoutFunctions {
    public static final String SCRIPT = "SCRIPT";
    public static final String REMOTE = "REMOTE";
    static final long CACHE_MILLIS = 60 * 1000L;
    private static final int CACHE_SIZE = 256;
    private static final int PROGRAM_CACHE_SIZE = 512;
    private static final int MAX_REPLY = 64 * 1024;
    private static final int MAX_ERRORS = 5;
    private static final long MAX_PRICE = 1000000000000L;

    /// How a remote function is reached; answers the reply body, or throws.
    public interface Transport {
        String post(String url, List<String> headers, byte[] body, int timeoutMillis)
                throws IOException;
    }

    /// Posts over HTTPS with every timeout set to the budget.
    static final class HttpTransport implements Transport {
        @Override
        public String post(String url, List<String> headers, byte[] body, int timeoutMillis)
                throws IOException {
            String unsafe = com.codenameone.examples.bazaar.web.security.UrlGuard.refuse(url,
                    com.codenameone.examples.bazaar.web.security.UrlGuard.DNS);
            if (unsafe != null) {
                throw new IOException(unsafe);
            }
            WebClient client = new WebClient().setConnectTimeoutMillis(timeoutMillis)
                    .setReadTimeoutMillis(timeoutMillis).setRequestTimeoutMillis(timeoutMillis);
            try {
                com.codename1.backend.Web.Result r = client.request("POST", url,
                        new ArrayList<String>(headers), body);
                if (!r.isSuccess()) {
                    throw new IOException("The function answered " + r.getStatus());
                }
                byte[] reply = r.getBody();
                if (reply != null && reply.length > MAX_REPLY) {
                    throw new IOException("The function's answer is too long");
                }
                return r.getBodyAsString();
            } finally {
                client.close();
            }
        }
    }

    /// One function a quote may run.
    public static final class Active {
        public final String id;
        public final String name;
        public final Target target;
        final String kind;
        final String program;
        final String url;
        final long updatedAt;
        final String appId;

        Active(String id, String name, Target target, String kind, String program, String url,
                long updatedAt, String appId) {
            this.id = id;
            this.name = name;
            this.target = target;
            this.kind = kind;
            this.program = program;
            this.url = url;
            this.updatedAt = updatedAt;
            this.appId = appId;
        }
    }

    private static final class Cached {
        final long at;
        final FunctionOutput output;

        Cached(long at, FunctionOutput output) {
            this.at = at;
            this.output = output;
        }
    }

    private final DataSource db;
    private final Apps apps;
    private Transport transport = new HttpTransport();
    private int fuel = Interpreter.DEFAULT_FUEL;
    private int timeoutMillis = 800;
    private final Map<String, Program> programs = new LinkedHashMap<String, Program>();
    private final Map<String, Cached> replies = new LinkedHashMap<String, Cached>();
    private final Map<String, String> lastErrors = new LinkedHashMap<String, String>();
    private final Map<String, long[]> failures = new LinkedHashMap<String, long[]>();
    private long remoteCalls;

    public CheckoutFunctions(DataSource db, Apps apps) {
        this.db = db;
        this.apps = apps;
    }

    /// Replaces the remote transport; for tests.
    public void useTransport(Transport transport) {
        this.transport = transport;
    }

    /// The instruction budget of one run; for tests.
    public void useFuel(int fuel) {
        this.fuel = fuel;
    }

    /// The remote timeout; for tests.
    public void useTimeoutMillis(int millis) {
        this.timeoutMillis = millis;
    }

    /// Remote calls made so far (a cached answer is not one).
    public synchronized long remoteCalls() {
        return remoteCalls;
    }

    /// Forgets cached remote answers; for tests.
    public synchronized void clearCache() {
        replies.clear();
    }

    /// Why `functionId` last failed, or null.
    public synchronized String lastError(String functionId) {
        return lastErrors.get(functionId);
    }

    /// How often `functionId` failed since the server started.
    public synchronized long failures(String functionId) {
        long[] n = failures.get(functionId);
        return n == null ? 0 : n[0];
    }

    private synchronized void failed(Active fn, String why) {
        String message = Codes.cut(why == null ? "failed" : why, 300);
        if (lastErrors.size() > 4096) {
            lastErrors.clear();
            failures.clear();
        }
        lastErrors.put(fn.id, message);
        long[] n = failures.get(fn.id);
        if (n == null) {
            n = new long[1];
            failures.put(fn.id, n);
        }
        n[0]++;
        System.err.println("[functions] " + fn.target.name + " function " + fn.id + " failed: "
                + message);
    }

    // ------------------------------------------------------------- lookup

    /// The functions that run at `storeId` now: active, of an installed app
    /// that holds `write_functions`.
    public List<Active> active(String storeId) throws IOException {
        List rows = db.query("SELECT f.id AS id, f.name AS name, f.target AS target, "
                + "f.kind AS kind, f.program AS program, f.url AS url, f.updated_at AS updated_at, "
                + "f.app_id AS app_id, i.scopes AS scopes FROM checkout_function f "
                + "JOIN app_installation i ON i.id = f.app_installation_id "
                + "WHERE f.store_id = ? AND f.active = 1 AND i.status = ? "
                + "ORDER BY f.created_at, f.id", new Object[] {storeId, Apps.INSTALLED});
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Active> out = new ArrayList<Active>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            if (!StoreApiKeys.has(StoreApiKeys.scopes(Rows.text(r.get("scopes"))),
                    StoreApiKeys.WRITE_FUNCTIONS)) {
                continue;
            }
            Target t = Target.named(Rows.text(r.get("target")));
            if (t != null) {
                out.add(new Active(Rows.text(r.get("id")), Rows.text(r.get("name")), t,
                        Rows.text(r.get("kind")), Rows.text(r.get("program")),
                        Rows.text(r.get("url")), Rows.number(r.get("updated_at")),
                        Rows.text(r.get("app_id"))));
            }
        }
        return out;
    }

    private static boolean any(List<Active> fns, Target t) {
        for (int iter = 0; iter < fns.size(); iter++) {
            if (fns.get(iter).target == t) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- running

    private Program program(Active fn) throws FunctionException {
        String key = fn.id + ":" + fn.updatedAt;
        synchronized (this) {
            Program p = programs.get(key);
            if (p != null) {
                return p;
            }
        }
        // Verified again on load: the row is trusted no more than an upload.
        Program p = Program.decode(fn.program);
        if (p.target != fn.target) {
            throw new FunctionException("The stored program is for another target");
        }
        synchronized (this) {
            if (programs.size() >= PROGRAM_CACHE_SIZE) {
                programs.clear();
            }
            programs.put(key, p);
        }
        return p;
    }

    /// Runs `fn` over `in`: its output, or null when it failed (logged).
    public FunctionOutput run(Active fn, FunctionInput in) {
        try {
            if (REMOTE.equals(fn.kind)) {
                return remote(fn, in);
            }
            FunctionOutput out = new FunctionOutput();
            Interpreter.run(program(fn), in, fuel, out);
            return out;
        } catch (FunctionException e) {
            failed(fn, e.getMessage());
        } catch (IOException e) {
            failed(fn, e.getMessage());
        } catch (RuntimeException e) {
            failed(fn, String.valueOf(e));
        }
        return null;
    }

    private FunctionOutput remote(Active fn, FunctionInput in)
            throws IOException, FunctionException {
        byte[] body = in.toJson().getBytes("UTF-8");
        String key = fn.id + ":" + fn.updatedAt + ":" + Ids.hex(Crypto.sha256(body));
        long now = System.currentTimeMillis();
        synchronized (this) {
            Cached c = replies.get(key);
            if (c != null && now - c.at < CACHE_MILLIS) {
                return c.output;
            }
            remoteCalls++;
        }
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/json");
        headers.add("X-Bazaar-Function: " + fn.id);
        headers.add("X-Bazaar-Function-Target: " + fn.target.name);
        headers.add("X-Bazaar-Signature: " + Webhooks.signature(apps.signingKeyOf(fn.appId),
                body));
        int budget = timeoutMillis;
        long started = System.currentTimeMillis();
        String reply = transport.post(fn.url, headers, body, budget);
        if (System.currentTimeMillis() - started > budget) {
            // A late answer is a missed one, whatever the transport did.
            throw new FunctionException("The function did not answer within " + budget + " ms");
        }
        FunctionOutput out = parseReply(fn.target, reply);
        synchronized (this) {
            if (replies.size() >= CACHE_SIZE) {
                Iterator<String> oldest = replies.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            replies.put(key, new Cached(now, out));
        }
        return out;
    }

    /// A remote function's answer as operations of `target`, checked as a
    /// script's emits are.
    static FunctionOutput parseReply(Target target, String reply) throws FunctionException {
        if (reply == null || reply.length() > MAX_REPLY) {
            throw new FunctionException("The function's answer is empty or too long");
        }
        Map m;
        try {
            m = Json.parseObject(reply);
        } catch (IOException bad) {
            throw new FunctionException("The function's answer is not JSON");
        }
        Object ops = m == null ? null : m.get("ops");
        if (!(ops instanceof List)) {
            throw new FunctionException("The function's answer has no ops");
        }
        List list = (List) ops;
        FunctionOutput out = new FunctionOutput();
        for (int iter = 0; iter < list.size(); iter++) {
            Object o = list.get(iter);
            if (!(o instanceof Map)) {
                throw new FunctionException("Operation " + iter + " is not an object");
            }
            Map op = (Map) o;
            Object name = op.get("op");
            int e = name instanceof String ? target.emit((String) name) : -1;
            if (e < 0) {
                throw new FunctionException("Operation " + iter + " is not one of a "
                        + target.name + " function");
            }
            String sig = target.signature(e);
            Object args = op.get("args");
            if (!(args instanceof List) || ((List) args).size() != sig.length()) {
                throw new FunctionException("Operation " + iter + " takes " + sig.length()
                        + " arguments");
            }
            List a = (List) args;
            int slot = out.add(e);
            for (int at = 0; at < sig.length(); at++) {
                Object v = a.get(at);
                if (sig.charAt(at) == Target.STRING) {
                    if (!(v instanceof String)) {
                        throw new FunctionException("Argument " + at + " of operation " + iter
                                + " is not a string");
                    }
                    out.text(slot, at, (String) v);
                } else {
                    if (!(v instanceof Number)) {
                        throw new FunctionException("Argument " + at + " of operation " + iter
                                + " is not a number");
                    }
                    Number n = (Number) v;
                    if (n.doubleValue() != (double) n.longValue()) {
                        throw new FunctionException("Argument " + at + " of operation " + iter
                                + " is not a whole number");
                    }
                    out.number(slot, at, n.longValue());
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------- inputs

    /// The record for `target` over `quote`, with `extra` rows in its second
    /// list (filled by the caller).
    static FunctionInput input(Target target, Quote quote, String country, String username,
            int extra) {
        List<PriceLine> lines = quote.priceLines();
        FunctionInput in = target.listCount() > 1 ? target.input(lines.size(), extra)
                : target.input(lines.size());
        long quantity = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            PriceLine pl = lines.get(iter);
            Quote.Line line = quote.lines.get(iter);
            quantity += pl.quantity;
            in.cell(Target.LINES, iter, Target.LINE_QUANTITY, pl.quantity)
                    .cell(Target.LINES, iter, Target.LINE_PRICE, pl.unitCents)
                    .cell(Target.LINES, iter, Target.LINE_TOTAL, pl.netCents())
                    .cell(Target.LINES, iter, Target.LINE_SKU, line.variant.getSku())
                    .cell(Target.LINES, iter, Target.LINE_PRODUCT, pl.productId)
                    .cell(Target.LINES, iter, Target.LINE_VARIANT, pl.variantId)
                    .cell(Target.LINES, iter, Target.LINE_TITLE, line.product.getTitle());
        }
        in.scalar(Target.SUBTOTAL, quote.subtotalCents)
                .scalar(Target.DISCOUNT_SO_FAR, quote.discounts == null ? 0
                        : quote.discounts.totalCents)
                .scalar(Target.QUANTITY, quantity)
                .scalar(Target.CURRENCY, quote.store == null ? "" : quote.store.getCurrency())
                .scalar(Target.COUNTRY, country)
                .scalar(Target.CUSTOMER, username)
                .scalar(Target.CODE, quote.cart == null ? "" : quote.cart.getDiscountCode());
        return in;
    }

    // ------------------------------------------------------------- targets

    /// Discount functions: takes what they emit off the lines, never more
    /// than a line still costs.
    public void discounts(List<Active> fns, Quote quote, String country, String username) {
        if (quote.discounts == null || !any(fns, Target.DISCOUNT)) {
            return;
        }
        List<PriceLine> lines = quote.priceLines();
        for (int f = 0; f < fns.size(); f++) {
            Active fn = fns.get(f);
            if (fn.target != Target.DISCOUNT) {
                continue;
            }
            FunctionOutput out = run(fn, input(Target.DISCOUNT, quote, country, username, 0));
            if (out == null) {
                continue;
            }
            for (int iter = 0; iter < out.size(); iter++) {
                long taken;
                String title;
                if (out.op(iter) == Target.EMIT_DISCOUNT) {
                    List<PriceLine> open = new ArrayList<PriceLine>();
                    long net = 0;
                    for (int l = 0; l < lines.size(); l++) {
                        if (lines.get(l).netCents() > 0) {
                            open.add(lines.get(l));
                            net += lines.get(l).netCents();
                        }
                    }
                    taken = Math.max(0, Math.min(out.number(iter, 0), net));
                    DiscountEngine.allocate(open, taken);
                    title = out.text(iter, 1);
                } else {
                    long index = out.number(iter, 0);
                    if (index < 0 || index >= lines.size()) {
                        continue;
                    }
                    PriceLine line = lines.get((int) index);
                    taken = Math.max(0, Math.min(out.number(iter, 1), line.netCents()));
                    line.discountCents += taken;
                    title = out.text(iter, 2);
                }
                if (taken > 0) {
                    quote.discounts.totalCents += taken;
                    quote.functionDiscounts.add(new Quote.FunctionDiscount(fn.id,
                            Codes.cut(title.length() == 0 ? fn.name : title, 80), taken));
                }
            }
        }
    }

    /// Shipping functions: hides, renames, reprices and reorders `options`.
    public List<ShippingCalculator.Option> shipping(List<Active> fns, Quote quote,
            List<ShippingCalculator.Option> options, String country, String username) {
        if (!any(fns, Target.SHIPPING)) {
            return options;
        }
        List<ShippingCalculator.Option> now = new ArrayList<ShippingCalculator.Option>(options);
        for (int f = 0; f < fns.size(); f++) {
            Active fn = fns.get(f);
            if (fn.target != Target.SHIPPING) {
                continue;
            }
            FunctionInput in = input(Target.SHIPPING, quote, country, username, now.size());
            for (int r = 0; r < now.size(); r++) {
                ShippingCalculator.Option o = now.get(r);
                in.cell(1, r, Target.RATE_ID, o.id).cell(1, r, Target.RATE_NAME, o.name)
                        .cell(1, r, Target.RATE_PRICE, o.priceCents)
                        .cell(1, r, Target.RATE_KIND, o.rate.getKind());
            }
            FunctionOutput out = run(fn, in);
            if (out == null) {
                continue;
            }
            for (int iter = 0; iter < out.size(); iter++) {
                int at = optionIndex(now, out.text(iter, 0));
                if (at < 0) {
                    continue;
                }
                ShippingCalculator.Option o = now.get(at);
                int op = out.op(iter);
                if (op == Target.EMIT_HIDE) {
                    now.remove(at);
                } else if (op == Target.EMIT_RENAME) {
                    String name = Codes.cut(out.text(iter, 1).trim(), 80);
                    if (name.length() > 0) {
                        now.set(at, o.with(name, o.priceCents));
                    }
                } else if (op == Target.EMIT_REPRICE) {
                    long price = out.number(iter, 1);
                    if (price >= 0 && price <= MAX_PRICE) {
                        now.set(at, o.with(o.name, price));
                    }
                } else {
                    now.remove(at);
                    now.add(position(out.number(iter, 1), now.size()), o);
                }
            }
        }
        return now;
    }

    private static int optionIndex(List<ShippingCalculator.Option> options, String id) {
        for (int iter = 0; iter < options.size(); iter++) {
            if (options.get(iter).id.equals(id)) {
                return iter;
            }
        }
        return -1;
    }

    private static int position(long wanted, int size) {
        return (int) Math.max(0, Math.min(wanted, size));
    }

    /// Payment functions: hides and reorders `offered`. A run that would
    /// hide every method is a failure, so the shopper can always pay.
    public List<PaymentTypeDto> payments(List<Active> fns, Quote quote,
            List<PaymentTypeDto> offered, String country, String username) {
        if (!any(fns, Target.PAYMENT)) {
            return offered;
        }
        List<PaymentTypeDto> now = new ArrayList<PaymentTypeDto>(offered);
        for (int f = 0; f < fns.size(); f++) {
            Active fn = fns.get(f);
            if (fn.target != Target.PAYMENT) {
                continue;
            }
            FunctionInput in = input(Target.PAYMENT, quote, country, username, now.size());
            for (int r = 0; r < now.size(); r++) {
                in.cell(1, r, Target.METHOD_TYPE, now.get(r).type());
            }
            FunctionOutput out = run(fn, in);
            if (out == null) {
                continue;
            }
            List<PaymentTypeDto> next = new ArrayList<PaymentTypeDto>(now);
            for (int iter = 0; iter < out.size(); iter++) {
                int at = methodIndex(next, out.text(iter, 0));
                if (at < 0) {
                    continue;
                }
                PaymentTypeDto m = next.remove(at);
                if (out.op(iter) == Target.EMIT_PAYMENT_MOVE) {
                    next.add(position(out.number(iter, 1), next.size()), m);
                }
            }
            if (next.isEmpty()) {
                failed(fn, "The function hid every payment method");
            } else {
                now = next;
            }
        }
        return now;
    }

    private static int methodIndex(List<PaymentTypeDto> methods, String type) {
        for (int iter = 0; iter < methods.size(); iter++) {
            if (methods.get(iter).type().equals(type)) {
                return iter;
            }
        }
        return -1;
    }

    /// Whether `type` is still offered once the payment functions ran.
    public boolean paymentOffered(List<Active> fns, Quote quote, List<PaymentTypeDto> offered,
            String type, String country, String username) {
        if (!any(fns, Target.PAYMENT)) {
            return true;
        }
        return methodIndex(payments(fns, quote, offered, country, username), type) >= 0;
    }

    /// Validation functions: the errors they emit, at most [#MAX_ERRORS]
    /// from each.
    public List<String> validate(List<Active> fns, Quote quote, String country,
            String username) {
        if (!any(fns, Target.VALIDATION)) {
            return Collections.emptyList();
        }
        List<String> errors = new ArrayList<String>();
        for (int f = 0; f < fns.size(); f++) {
            Active fn = fns.get(f);
            if (fn.target != Target.VALIDATION) {
                continue;
            }
            FunctionOutput out = run(fn, input(Target.VALIDATION, quote, country, username, 0));
            for (int iter = 0; out != null && iter < out.size() && iter < MAX_ERRORS; iter++) {
                String message = Codes.cut(out.text(iter, 0).trim(), 200);
                errors.add(message.length() == 0 ? fn.name + " refused the cart" : message);
            }
        }
        return errors;
    }
}
