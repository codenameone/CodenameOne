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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.CustomDomainDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Proof that a store owns the custom domain it claims, before Bazaar
/// serves it or names it in links ([SiteUrls#served]).
///
/// Claiming a domain gives the store a fresh token and clears any earlier
/// proof. The store then publishes the token one of two ways and asks for a
/// check ([#verify]):
///
/// - **A file**: `http://<domain>/.well-known/bazaar-verify/<token>` whose
///   body is the token. Bazaar never serves that path itself, so the file has
///   to come from whoever controls the domain today.
/// - **DNS**: a TXT record on `_bazaar-verify.<domain>` reading
///   `bazaar-verify=<token>`, looked up over DNS-over-HTTPS
///   (`bazaar.domains.doh-url`, Cloudflare's resolver by default).
///
/// A claim of a domain another store has *proved* is refused; a domain
/// another store only claimed is taken from it, so nobody can squat on a
/// name they cannot prove. The lookups go through [Probe], which tests
/// replace, and are made outside any transaction.
@Component
public class DomainVerification {
    public static final String WELL_KNOWN = "/.well-known/bazaar-verify/";
    public static final String TXT_PREFIX = "_bazaar-verify.";
    public static final String TXT_VALUE = "bazaar-verify=";

    /// How the published token is looked for.
    public interface Probe {
        /// The body at `url`, or null when it does not answer 200.
        String fetch(String url) throws IOException;

        /// The TXT records of `name`, unquoted; empty for none.
        List<String> txt(String name) throws IOException;
    }

    private final StoreRepository stores;
    private final com.codename1.backend.DataSource db;
    private final StoreAccess access;
    private Probe probe;

    public DomainVerification(StoreRepository stores, com.codename1.backend.DataSource db,
            StoreAccess access, com.codename1.backend.Config config) throws IOException {
        this.stores = stores;
        this.db = db;
        this.access = access;
        this.probe = new WebProbe(config.get("bazaar.domains.doh-url",
                "https://cloudflare-dns.com/dns-query"));
    }

    /// Replaces how the token is looked for; for tests.
    public void useProbe(Probe probe) {
        this.probe = probe;
    }

    @Transactional(readOnly = true)
    public CustomDomainDto status(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return dto(stores.findById(storeId), "");
    }

    /// Claims `domain` for the store (empty removes it): a new token, no proof.
    @Transactional(rollbackFor = IOException.class)
    public CustomDomainDto claim(String username, String storeId, String domain)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Store store = stores.findById(storeId);
        assign(store, domain);
        stores.save(store);
        return dto(store, store.getCustomDomain() == null ? "" : "Publish the token, then verify");
    }

    /// Sets `store`'s domain to `raw` (folded; empty removes it), with a new
    /// token and no proof when it changes. Saving `store` is the caller's,
    /// inside the same transaction.
    public void assign(Store store, String raw) throws IOException {
        String folded = SiteUrls.domain(raw);
        if (folded == null) {
            throw new ResponseStatusException(400, "That is not a hostname");
        }
        String now = store.getCustomDomain() == null ? "" : store.getCustomDomain();
        if (folded.equals(now)) {
            return;
        }
        Store holder = folded.length() == 0 ? null : stores.findByCustomDomain(folded);
        if (holder != null && !holder.getId().equals(store.getId())) {
            if (holder.getDomainVerifiedAt() > 0) {
                throw new ResponseStatusException(400, "Another store uses that domain");
            }
            // Released by a statement that runs now, not by saving `holder`:
            // the session flushes its pending updates in no fixed order, and
            // writing this store's claim first breaks the unique index on
            // store.custom_domain. The proof check is repeated in the WHERE
            // so a claim proved meanwhile is never released.
            int released = db.execute("UPDATE store SET custom_domain = NULL, domain_token = '', "
                    + "domain_verified_at = 0 WHERE id = ? AND domain_verified_at = 0",
                    new Object[] {holder.getId()});
            if (released != 1) {
                throw new ResponseStatusException(400, "Another store uses that domain");
            }
        }
        store.setCustomDomain(folded);
        store.setDomainToken(folded.length() == 0 ? "" : Tokens.token());
        store.setDomainVerifiedAt(0);
    }

    /// Looks for the token now; on finding it the domain is served.
    public CustomDomainDto verify(String username, String storeId) throws IOException {
        Store store = read(username, storeId);
        String domain = store.getCustomDomain();
        if (domain == null) {
            throw new ResponseStatusException(400, "Claim a domain first");
        }
        String token = store.getDomainToken();
        String how = found(probe, domain, token);
        if (how == null) {
            return dto(store, "The token was not found at " + fileUrl(domain, token)
                    + " nor in a TXT record on " + TXT_PREFIX + domain);
        }
        return proved(storeId, domain, token, "Verified by " + how);
    }

    /// How `token` was found published for `domain` ("file", "DNS"), or null.
    public static String found(Probe probe, String domain, String token) {
        if (token == null || token.length() == 0) {
            return null;
        }
        try {
            String body = probe.fetch(fileUrl(domain, token));
            if (body != null && body.trim().equals(token)) {
                return "file";
            }
        } catch (IOException | RuntimeException unreachable) {
            // Not there; try DNS.
        }
        try {
            List<String> records = probe.txt(TXT_PREFIX + domain);
            for (int iter = 0; records != null && iter < records.size(); iter++) {
                if ((TXT_VALUE + token).equals(records.get(iter).trim())) {
                    return "DNS";
                }
            }
        } catch (IOException | RuntimeException unreachable) {
            return null;
        }
        return null;
    }

    @Transactional(readOnly = true)
    private Store read(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return stores.findById(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    private CustomDomainDto proved(String storeId, String domain, String token, String message)
            throws IOException {
        Store store = stores.findById(storeId);
        // The claim may have changed while the lookup ran.
        if (!domain.equals(store.getCustomDomain()) || !token.equals(store.getDomainToken())) {
            return dto(store, "The domain changed during the check; verify again");
        }
        store.setDomainVerifiedAt(System.currentTimeMillis());
        stores.save(store);
        return dto(store, message);
    }

    static String fileUrl(String domain, String token) {
        return "http://" + domain + WELL_KNOWN + token;
    }

    static CustomDomainDto dto(Store s, String message) {
        String domain = s.getCustomDomain() == null ? "" : s.getCustomDomain();
        String token = s.getDomainToken();
        boolean some = domain.length() > 0;
        return new CustomDomainDto(domain, token, some && s.getDomainVerifiedAt() > 0,
                s.getDomainVerifiedAt(), some ? fileUrl(domain, token) : "",
                some ? TXT_PREFIX + domain : "", some ? TXT_VALUE + token : "", message);
    }

    /// The probe over the network: a plain GET, and DNS-over-HTTPS JSON.
    static final class WebProbe implements Probe {
        private final String doh;

        WebProbe(String doh) {
            this.doh = doh;
        }

        @Override
        public String fetch(String url) throws IOException {
            Web.Result r = Web.get(url);
            return r.getStatus() == 200 ? r.getBodyAsString() : null;
        }

        @Override
        public List<String> txt(String name) throws IOException {
            List<String> headers = new ArrayList<String>();
            headers.add("Accept: application/dns-json");
            Web.Result r = Web.request("GET", doh + "?type=TXT&name=" + name, headers, null);
            List<String> out = new ArrayList<String>();
            if (r.getStatus() != 200) {
                return out;
            }
            Object answers = Json.parseObject(r.getBodyAsString()).get("Answer");
            if (!(answers instanceof List)) {
                return out;
            }
            List list = (List) answers;
            for (int iter = 0; iter < list.size(); iter++) {
                Object a = list.get(iter);
                Object data = a instanceof Map ? ((Map) a).get("data") : null;
                if (data instanceof String) {
                    out.add(unquote((String) data));
                }
            }
            return out;
        }

        /// A TXT record's data as DoH writes it: quoted strings, joined.
        static String unquote(String data) {
            StringBuilder b = new StringBuilder();
            boolean in = false;
            for (int iter = 0; iter < data.length(); iter++) {
                char c = data.charAt(iter);
                if (c == '"') {
                    in = !in;
                } else if (in || data.indexOf('"') < 0) {
                    b.append(c);
                }
            }
            return b.toString();
        }
    }
}
