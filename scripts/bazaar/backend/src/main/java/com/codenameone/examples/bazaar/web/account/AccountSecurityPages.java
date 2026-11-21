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
package com.codenameone.examples.bazaar.web.account;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.RecoveryCodeRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpEnrollment;
import com.codename1.backend.security.mfa.TotpService;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.web.WebPages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The account's security page: the authenticator app (TOTP), recovery codes
/// and the browser sessions signed in to the account.
///
/// - **Enrolment** makes a secret on a post (never on a page view) and keeps
///   it in the session until the first code confirms it; the page shows the
///   secret and its QR code, drawn inline as SVG. It is refused while a second
///   factor is on: a new secret would silently replace the working one.
/// - **Recovery codes**: ten, made when the enrolment is confirmed (or again,
///   with the password), shown on that one response and stored only as hashes
///   (`RecoveryCodeService`). The second-factor page takes one in place of a code.
/// - **Turning it off** asks for the password again, and deletes the codes.
/// - **Sessions**: where this account is signed in ([WebSessions]), and a button
///   that ends every one but this.
@Controller
public class AccountSecurityPages {
    static final String SECRET = "web.totp.secret";
    static final String URI = "web.totp.uri";

    private final WebPages pages;
    private final TotpService totp;
    private final RecoveryCodeService recovery;
    private final RecoveryCodeRepository recoveryRows;
    private final Accounts accounts;
    private final WebSessions sessions;

    public AccountSecurityPages(WebPages pages, TotpService totp, RecoveryCodeService recovery,
            DataSource db, Accounts accounts, WebSessions sessions) {
        this.pages = pages;
        this.totp = totp;
        this.recovery = recovery;
        this.recoveryRows = new JdbcRecoveryCodeRepository(db);
        this.accounts = accounts;
        this.sessions = sessions;
    }

    private String page(Model model, HttpServer.Request request, List<String> codes)
            throws IOException {
        String me = Caller.name();
        AccountView v = new AccountView();
        v.page = "security";
        v.totpEnabled = totp.isEnabled(me);
        HttpSession session = request.getSession(false);
        if (!v.totpEnabled && session != null && session.getAttribute(SECRET) != null) {
            v.enrolling = true;
            v.secret = String.valueOf(session.getAttribute(SECRET));
            String uri = String.valueOf(session.getAttribute(URI));
            v.qrPath = QrSvg.path(uri);
            v.qrBox = QrSvg.box(uri);
        }
        if (codes != null) {
            v.recoveryCodes.addAll(codes);
        }
        v.recoveryLeft = Integer.toString(v.totpEnabled ? recoveryRows.count(me) : 0);
        String view = AccountPages.render(pages, model, request, "Security", v);
        List<WebSessions.Seen> seen = sessions.list(request, me);
        for (int iter = 0; iter < seen.size(); iter++) {
            WebSessions.Seen s = seen.get(iter);
            AccountItem i = new AccountItem();
            i.title = s.agent.length() == 0 ? "Unknown browser" : s.agent;
            i.detail = s.address;
            i.meta = "Signed in " + Days.iso(s.createdAt);
            i.extra = Days.iso(s.seenAt);
            i.flag = s.current;
            v.sessions.add(i);
        }
        return view;
    }

    private String me(HttpServer.Request request) throws IOException {
        String me = Caller.name();
        sessions.touch(request, me);
        return me;
    }

    private static String back(HttpServer.Request request, String message) {
        WebPages.toast(request, message);
        return "redirect:/account/security";
    }

    @GetMapping("/account/security")
    public String show(Model model, HttpServer.Request request) throws IOException {
        return page(model, request, null);
    }

    @PostMapping("/account/security/totp/start")
    public String start(HttpServer.Request request) throws IOException {
        String me = me(request);
        if (totp.isEnabled(me)) {
            return back(request, "Two-step sign-in is already on");
        }
        TotpEnrollment e = totp.beginEnrollment(me);
        HttpSession session = request.getSession(true);
        session.setAttribute(SECRET, e.getSecret());
        session.setAttribute(URI, e.getOtpauthUri());
        return "redirect:/account/security";
    }

    /// The first code from the app turns the factor on, and this response --
    /// and only this one -- shows the recovery codes.
    @PostMapping("/account/security/totp/confirm")
    public String confirm(Model model, HttpServer.Request request) throws IOException {
        String me = me(request);
        String code = request.param("code");
        if (code == null || !totp.confirmEnrollment(me, code.trim())) {
            return back(request, "That code is not right. Enter the one your app shows now.");
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SECRET);
            session.removeAttribute(URI);
        }
        WebPages.toast(request, "Two-step sign-in is on");
        return page(model, request, recovery.generate(me));
    }

    @PostMapping("/account/security/recovery")
    public String regenerate(Model model, HttpServer.Request request) throws IOException {
        String me = me(request);
        if (!totp.isEnabled(me)) {
            return back(request, "Turn on two-step sign-in first");
        }
        if (!accounts.checkPassword(me, request.param("password"))) {
            return back(request, "That is not your password");
        }
        WebPages.toast(request, "New recovery codes made; the old ones no longer work");
        return page(model, request, recovery.generate(me));
    }

    @PostMapping("/account/security/totp/disable")
    public String disable(HttpServer.Request request) throws IOException {
        String me = me(request);
        if (!accounts.checkPassword(me, request.param("password"))) {
            return back(request, "That is not your password");
        }
        totp.disable(me);
        recoveryRows.replace(me, new ArrayList<String>());
        return back(request, "Two-step sign-in is off");
    }

    @PostMapping("/account/security/sessions/end-others")
    public String endOthers(HttpServer.Request request) throws IOException {
        String me = me(request);
        int n = sessions.endOthers(request, me);
        return back(request, n == 1 ? "Signed out 1 other session"
                : "Signed out " + n + " other sessions");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        return AccountPages.refusal(e);
    }
}
