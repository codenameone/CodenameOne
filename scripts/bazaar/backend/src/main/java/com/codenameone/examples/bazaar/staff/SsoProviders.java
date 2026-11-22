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
package com.codenameone.examples.bazaar.staff;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.ClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.DefaultOidcUser;
import com.codename1.backend.security.oauth2.client.OAuth2UserService;
import com.codename1.backend.security.oauth2.client.OidcUser;
import com.codename1.backend.security.oauth2.client.OidcUserRequest;
import com.codename1.backend.security.oauth2.client.OidcUserService;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.SsoProviderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.security.UrlGuard;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Single sign-on for a store's staff through the store's own OpenID Connect
/// provider (Okta, Entra ID, Google Workspace, Keycloak or any other).
///
/// The owner gives the provider's issuer, a client id and secret registered
/// there, and which e-mail domains become which role (`example.com=MANAGER,
/// shop.example.com=CASHIER`). The provider's endpoints are read from its
/// discovery document when the provider is saved, through [UrlGuard]: each
/// must be public HTTPS, so a provider cannot be pointed at this server's
/// own network.
///
/// Signing in is the framework's `oauth2Login` on the web chain
/// (`WebSecurity`): the authorization code flow with PKCE, `state` and
/// `nonce`, and the ID token checked against the provider's JWKS, issuer,
/// audience and expiry before anything here runs. This class is the
/// [ClientRegistrationRepository] it reads providers from and the user
/// service it hands the checked token to, which decides who that is:
///
/// - **The same person every time** is the provider and its `sub`, never the
///   e-mail address, which a provider lets its users change.
/// - **A first sign-in** needs `email_verified` and an address in a mapped
///   domain. It opens a staff account for the address and adds it to the
///   store with the mapped role -- MANAGER, CASHIER or FULFILMENT, never
///   OWNER. An address that already has a Bazaar account is refused rather
///   than linked: the provider is the store owner's, and a provider that
///   vouched for anyone's address must not be a way into their account.
/// - **Later sign-ins** keep the role the store gave; removing the member
///   from the store, or disabling the provider, ends their access.
///
/// A store that requires two-step sign-in asks SSO staff for their
/// authenticator app too: `mfa()` on the same chain holds an `oauth2Login`
/// back exactly as it holds a password.
@Component
public class SsoProviders implements ClientRegistrationRepository,
        OAuth2UserService<OidcUserRequest, OidcUser> {
    /// Where a staff member starts signing in through a provider; the
    /// provider's id follows.
    public static final String START = "/auth/sso/start";
    /// Where the provider sends them back; register
    /// `<server>/auth/sso/callback/<provider id>` with the provider.
    public static final String CALLBACK = "/auth/sso/callback";
    static final int MAX_PROVIDERS = 5;

    /// Reads a provider's discovery document. Tests replace it to reach a
    /// provider on this machine, which [UrlGuard] rightly refuses.
    public interface Discovery {
        Map fetch(String issuer) throws IOException;
    }

    private final DataSource db;
    private final StoreAccess access;
    private final Accounts accounts;
    private final JdbcUserDetailsManager users;
    private final StoreStaffRepository staff;
    private final OidcUserService checked = new OidcUserService();
    private Discovery discovery = SsoProviders::discover;
    private boolean guarded = true;

    public SsoProviders(DataSource db, StoreAccess access, Accounts accounts,
            JdbcUserDetailsManager users, StoreStaffRepository staff) {
        this.db = db;
        this.access = access;
        this.accounts = accounts;
        this.users = users;
        this.staff = staff;
    }

    /// For tests.
    /// For tests: a provider on this machine, whose addresses [UrlGuard]
    /// would refuse at sign-in too.
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery == null ? SsoProviders::discover : discovery;
        this.guarded = discovery == null;
    }

    private static boolean unsafe(String url) {
        return !url.startsWith("https://") || UrlGuard.refuse(url, UrlGuard.DNS) != null;
    }

    // ------------------------------------------------------------- owners

    @Transactional(readOnly = true)
    public List<SsoProviderDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        List rows = db.query("SELECT id AS id FROM sso_provider WHERE store_id = ? "
                + "ORDER BY created_at", new Object[] {storeId});
        List<SsoProviderDto> out = new ArrayList<SsoProviderDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(dto(row(Rows.text(((Map) rows.get(iter)).get("id")))));
        }
        return out;
    }

    /// Adds a provider, or changes one when `wanted` has an id. A blank
    /// secret on a change keeps the one stored; the secret is never answered.
    @Transactional(rollbackFor = IOException.class)
    public SsoProviderDto save(String username, String storeId, SsoProviderDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String name = text(wanted.name(), 80, "a name");
        String issuer = text(wanted.issuer(), 300, "the issuer");
        String clientId = text(wanted.clientId(), 200, "the client id");
        String mapping = domainRoles(wanted.domainRoles());
        Map existing = null;
        if (wanted.id() != null && wanted.id().length() > 0) {
            existing = row(wanted.id());
            StoreAccess.requireIn(existing == null ? null : Rows.text(existing.get("store_id")),
                    storeId, "provider");
        }
        String secret = wanted.clientSecret() == null ? "" : wanted.clientSecret().trim();
        if (secret.length() == 0) {
            if (existing == null) {
                throw new ResponseStatusException(400, "Give the client secret");
            }
            secret = Rows.text(existing.get("client_secret"));
        }
        if (secret.length() > 500) {
            throw new ResponseStatusException(400, "The client secret is too long");
        }
        String[] endpoints = endpoints(issuer);
        long now = System.currentTimeMillis();
        String id;
        if (existing == null) {
            Map count = db.queryOne("SELECT COUNT(*) AS n FROM sso_provider WHERE store_id = ?",
                    new Object[] {storeId});
            if (count != null && Rows.number(count.get("n")) >= MAX_PROVIDERS) {
                throw new ResponseStatusException(400, "Remove a provider before adding another");
            }
            id = Ids.next();
            db.execute("INSERT INTO sso_provider (id, store_id, name, issuer, client_id, "
                    + "client_secret, authorization_uri, token_uri, jwks_uri, domain_roles, "
                    + "enabled, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {id, storeId, name, issuer, clientId, secret, endpoints[0],
                        endpoints[1], endpoints[2], mapping,
                        Integer.valueOf(wanted.enabled() ? 1 : 0), Long.valueOf(now)});
        } else {
            id = wanted.id();
            db.execute("UPDATE sso_provider SET name = ?, issuer = ?, client_id = ?, "
                    + "client_secret = ?, authorization_uri = ?, token_uri = ?, jwks_uri = ?, "
                    + "domain_roles = ?, enabled = ? WHERE id = ?", new Object[] {name, issuer,
                        clientId, secret, endpoints[0], endpoints[1], endpoints[2], mapping,
                        Integer.valueOf(wanted.enabled() ? 1 : 0), id});
        }
        return dto(row(id));
    }

    @Transactional(rollbackFor = IOException.class)
    public List<SsoProviderDto> remove(String username, String storeId, String providerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        Map existing = providerId == null ? null : row(providerId);
        StoreAccess.requireIn(existing == null ? null : Rows.text(existing.get("store_id")),
                storeId, "provider");
        db.execute("DELETE FROM sso_identity WHERE provider_id = ?", new Object[] {providerId});
        db.execute("DELETE FROM sso_provider WHERE id = ?", new Object[] {providerId});
        return list(username, storeId);
    }

    /// Whether `username` was opened by another store's provider than
    /// `storeId`'s, and so may not be added to this one.
    public boolean ownedElsewhere(String username, String storeId) throws IOException {
        Map r = db.queryOne("SELECT store_id AS store_id FROM sso_identity WHERE username = ?",
                new Object[] {username});
        return r != null && !Rows.text(r.get("store_id")).equals(storeId);
    }

    // ------------------------------------------------------------ signing in

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        try {
            Map r = registrationId == null ? null : row(registrationId);
            if (r == null || Rows.number(r.get("enabled")) == 0) {
                return null;
            }
            // Checked again now, as webhooks are at delivery: the framework
            // calls the token and key addresses itself, and an address that
            // was public when saved may not be now.
            if (guarded && (unsafe(Rows.text(r.get("token_uri")))
                    || unsafe(Rows.text(r.get("jwks_uri"))))) {
                return null;
            }
            return ClientRegistration.withRegistrationId(registrationId)
                    .clientId(Rows.text(r.get("client_id")))
                    .clientSecret(Rows.text(r.get("client_secret")))
                    .clientName(Rows.text(r.get("name")))
                    .redirectUri("{baseUrl}" + CALLBACK + "/{registrationId}")
                    .authorizationUri(Rows.text(r.get("authorization_uri")))
                    .tokenUri(Rows.text(r.get("token_uri")))
                    .jwkSetUri(Rows.text(r.get("jwks_uri")))
                    .issuerUri(Rows.text(r.get("issuer")))
                    .userNameAttributeName("email")
                    .scope("openid", "email", "profile")
                    .build();
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        // The framework's service first: it holds the user info to the ID
        // token's subject. The token itself was checked before this.
        OidcUser verified = checked.loadUser(request);
        String providerId = request.getClientRegistration().getRegistrationId();
        Jwt token = request.getIdToken();
        try {
            String username = signIn(providerId, token.getSubject(), verified.getClaims());
            List<GrantedAuthority> held = new ArrayList<GrantedAuthority>();
            held.addAll(users.loadUserByUsername(username).getAuthorities());
            held.add(new SimpleGrantedAuthority("OIDC_USER"));
            return new DefaultOidcUser(username, held, token, verified.getClaims());
        } catch (ResponseStatusException refused) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"),
                    refused.getReason());
        } catch (IOException failed) {
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error"),
                    "The sign-in could not be recorded");
        }
    }

    /// The account the provider's user signs in as, opening it and adding it
    /// to the store the first time; see the class comment for the rules.
    @Transactional(rollbackFor = IOException.class)
    public String signIn(String providerId, String subject, Map claims) throws IOException {
        Map provider = row(providerId);
        if (provider == null || Rows.number(provider.get("enabled")) == 0 || subject == null
                || subject.length() == 0) {
            throw new ResponseStatusException(403, "That provider does not sign anyone in");
        }
        String storeId = Rows.text(provider.get("store_id"));
        long now = System.currentTimeMillis();
        Map known = db.queryOne("SELECT id AS id, username AS username FROM sso_identity "
                + "WHERE provider_id = ? AND subject = ?", new Object[] {providerId, subject});
        if (known != null) {
            String username = Rows.text(known.get("username"));
            if (staff.findByStoreIdAndUsername(storeId, username) == null
                    || !accounts.enabled(username)) {
                throw new ResponseStatusException(403, "You no longer work at this store");
            }
            db.execute("UPDATE sso_identity SET last_login_at = ? WHERE id = ?",
                    new Object[] {Long.valueOf(now), Rows.text(known.get("id"))});
            return username;
        }
        Object verified = claims == null ? null : claims.get("email_verified");
        if (!(Boolean.TRUE.equals(verified) || "true".equals(verified))) {
            throw new ResponseStatusException(403, "The provider has not verified your e-mail");
        }
        Object mail = claims.get("email");
        String username = Accounts.email(mail instanceof String ? (String) mail : "");
        String role = roleFor(Rows.text(provider.get("domain_roles")),
                username.substring(username.indexOf('@') + 1));
        if (role == null) {
            throw new ResponseStatusException(403, "Your e-mail domain is not one this store "
                    + "signs in");
        }
        if (accounts.exists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has a Bazaar account. "
                    + "Sign in with its password");
        }
        Object name = claims.get("name");
        accounts.openForSingleSignOn(username, name instanceof String ? (String) name : null);
        StoreStaff member = new StoreStaff();
        member.setId(Ids.next());
        member.setStoreId(storeId);
        member.setUsername(username);
        member.setRole(role);
        member.setDisplayName(Codes.cut(name instanceof String ? (String) name : username, 80));
        member.setCreatedAt(Instant.ofEpochMilli(now));
        staff.insert(member);
        db.execute("INSERT INTO sso_identity (id, store_id, provider_id, issuer, subject, "
                + "username, created_at, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {Ids.next(), storeId, providerId, Rows.text(provider.get("issuer")),
                    subject, username, Long.valueOf(now), Long.valueOf(now)});
        return username;
    }

    // -------------------------------------------------------------- rules

    /// The role `domain` maps to in `mapping`, or null. Domains compare
    /// without regard to ASCII case; the first match wins.
    static String roleFor(String mapping, String domain) {
        List<String[]> pairs = pairs(mapping);
        for (int iter = 0; iter < pairs.size(); iter++) {
            if (pairs.get(iter)[0].equalsIgnoreCase(domain)) {
                return pairs.get(iter)[1];
            }
        }
        return null;
    }

    /// `domain=ROLE` pairs, separated by commas or new lines.
    static List<String[]> pairs(String mapping) {
        List<String[]> out = new ArrayList<String[]>();
        String all = mapping == null ? "" : mapping;
        int start = 0;
        while (start < all.length()) {
            int end = start;
            while (end < all.length() && all.charAt(end) != ',' && all.charAt(end) != '\n') {
                end++;
            }
            String pair = all.substring(start, end).trim();
            start = end + 1;
            if (pair.length() == 0) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                throw new ResponseStatusException(400, "Write each domain as domain=ROLE");
            }
            out.add(new String[] {pair.substring(0, eq).trim(), pair.substring(eq + 1).trim()});
        }
        return out;
    }

    /// The mapping, checked and written back canonically.
    static String domainRoles(String mapping) {
        List<String[]> pairs = pairs(mapping);
        if (pairs.isEmpty()) {
            throw new ResponseStatusException(400, "Map at least one e-mail domain to a role");
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < pairs.size(); iter++) {
            String domain = pairs.get(iter)[0];
            String role = pairs.get(iter)[1];
            if (domain.indexOf('.') < 0 || domain.indexOf('@') >= 0 || domain.length() > 120) {
                throw new ResponseStatusException(400, "That is not an e-mail domain: "
                        + Codes.cut(domain, 40));
            }
            if (!StoreAccess.MANAGER.equals(role) && !StoreAccess.CASHIER.equals(role)
                    && !StoreAccess.FULFILMENT.equals(role)) {
                throw new ResponseStatusException(400, "A domain maps to MANAGER, CASHIER or "
                        + "FULFILMENT");
            }
            if (iter > 0) {
                out.append(',');
            }
            out.append(domain).append('=').append(role);
        }
        return out.toString();
    }

    /// `{authorization, token, jwks}` from the provider's discovery document,
    /// each a public HTTPS address, and the document's issuer the one given.
    private String[] endpoints(String issuer) throws IOException {
        Map doc;
        try {
            doc = discovery.fetch(issuer);
        } catch (IOException unreachable) {
            throw new ResponseStatusException(400, "The provider's discovery document could not "
                    + "be read: " + Codes.cut(unreachable.getMessage(), 120));
        }
        if (doc == null || !issuer.equals(doc.get("issuer"))) {
            throw new ResponseStatusException(400, "The provider's discovery document names "
                    + "another issuer");
        }
        String[] out = new String[3];
        String[] keys = {"authorization_endpoint", "token_endpoint", "jwks_uri"};
        for (int iter = 0; iter < keys.length; iter++) {
            Object v = doc.get(keys[iter]);
            if (!(v instanceof String) || ((String) v).length() == 0) {
                throw new ResponseStatusException(400, "The provider's discovery document has "
                        + "no " + keys[iter]);
            }
            out[iter] = (String) v;
        }
        return out;
    }

    private static Map discover(String issuer) throws IOException {
        String url = (issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer)
                + "/.well-known/openid-configuration";
        if (!url.startsWith("https://")) {
            throw new IOException("The issuer must be an https:// address");
        }
        String unsafe = UrlGuard.refuse(url, UrlGuard.DNS);
        if (unsafe != null) {
            throw new IOException(unsafe);
        }
        Web.Result answer = Web.get(url);
        if (answer.getStatus() != 200) {
            throw new IOException("The provider answered " + answer.getStatus());
        }
        Map doc = Json.parseObject(answer.getBodyAsString());
        String[] keys = {"authorization_endpoint", "token_endpoint", "jwks_uri"};
        for (int iter = 0; iter < keys.length; iter++) {
            Object v = doc == null ? null : doc.get(keys[iter]);
            if (v instanceof String) {
                String endpoint = (String) v;
                String refused = endpoint.startsWith("https://")
                        ? UrlGuard.refuse(endpoint, UrlGuard.DNS) : "it is not https://";
                if (refused != null) {
                    throw new IOException(keys[iter] + " is refused: " + refused);
                }
            }
        }
        return doc;
    }

    private Map row(String id) throws IOException {
        return db.queryOne("SELECT id AS id, store_id AS store_id, name AS name, issuer AS issuer, "
                + "client_id AS client_id, client_secret AS client_secret, "
                + "authorization_uri AS authorization_uri, token_uri AS token_uri, "
                + "jwks_uri AS jwks_uri, domain_roles AS domain_roles, enabled AS enabled "
                + "FROM sso_provider WHERE id = ?", new Object[] {id});
    }

    private static SsoProviderDto dto(Map r) {
        String id = Rows.text(r.get("id"));
        return new SsoProviderDto(id, Rows.text(r.get("name")), Rows.text(r.get("issuer")),
                Rows.text(r.get("client_id")), "", Rows.text(r.get("domain_roles")),
                Rows.number(r.get("enabled")) != 0, START + "/" + id);
    }

    private static String text(String value, int max, String what) {
        String t = value == null ? "" : value.trim();
        if (t.length() == 0) {
            throw new ResponseStatusException(400, "Give " + what);
        }
        if (t.length() > max) {
            throw new ResponseStatusException(400, "That is too long for " + what);
        }
        return t;
    }
}
