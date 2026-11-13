/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.mail.EmailVerificationService;
import com.codename1.backend.security.mail.InMemoryOneTimeTokenRepository;
import com.codename1.backend.security.mail.InMemoryVerifiedEmailRepository;
import com.codename1.backend.security.mail.OneTimeTokenService;
import com.codename1.backend.security.mail.PasswordResetService;
import com.codename1.backend.security.mfa.EmailCodeService;
import com.codename1.backend.security.mfa.InMemoryEmailCodeRepository;
import com.codename1.backend.security.mfa.InMemoryRecoveryCodeRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.impl.backend.MailRuntime;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The flows that end in an inbox, through a real chain: the forgotten
/// password pages, the link that proves an address, and the sign-in code that
/// is mailed. Mail lands in the memory transport, which is what a server with
/// no `cn1.mail.url` sends through.
class MailFlowsTest {
    private static final String ADA = "ada@example.com";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, request.pathFrom(0) + " "
                    + (who == null ? "nobody" : who.getName()));
        }
    };

    private static final class Ticking implements Clock {
        long now = 1700000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    private final Ticking clock = new Ticking();
    private final MailSender mail = new MailSender();
    private final OneTimeTokenService tokens = new OneTimeTokenService(
            new InMemoryOneTimeTokenRepository()).clock(clock);
    private final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername(ADA).password("{noop}old password").roles("USER").build(),
            User.withUsername("ray").password("{noop}ray password").roles("USER").build());

    private static Properties settings() {
        Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.mail.from", "Accounts <accounts@example.com>");
        // The code's own count of guesses is what these tests are about, so
        // the sign-in's rate limit is moved out of its way.
        settings.setProperty(MfaConfigurer.ATTEMPTS, "50");
        settings.setProperty(MfaConfigurer.ATTEMPTS_PER_ADDRESS, "50");
        return settings;
    }

    /// The one link in the newest mail.
    private String link(String to) throws Exception {
        List<Mail> sent = mail.sent();
        assertFalse(sent.isEmpty(), "nothing was mailed");
        Mail last = sent.get(sent.size() - 1);
        assertEquals(to, last.getTo().get(0));
        String text = last.getText();
        int at = text.indexOf("http");
        assertTrue(at >= 0, text);
        int end = at;
        while (end < text.length() && text.charAt(end) > ' ') {
            end++;
        }
        return text.substring(at, end);
    }

    /// The path and query of a link, which is what a request is made of.
    private static String target(String link) {
        return link.substring(link.indexOf('/', link.indexOf("//") + 2));
    }

    // -------------------------------------------------------- password reset

    private SecuredServer startReset(Properties settings, String baseUrl) throws Exception {
        PasswordResetService service = new PasswordResetService(tokens, mail, users, users,
                PasswordEncoderFactories.createDelegatingPasswordEncoder());
        return SecuredServer.start(settings, "dev", new Object[] {users}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .passwordReset(reset -> {
                            reset.service(service);
                            if (baseUrl != null) {
                                reset.baseUrl(baseUrl);
                            }
                        }).build());
    }

    @Test
    @DisplayName("a forgotten password is replaced through a mailed link, once")
    void passwordReset() throws Exception {
        try (SecuredServer server = startReset(settings(), null)) {
            mail.clearSent();
            // The generated login page offers the way there, and says what
            // happened to someone a reset sends back.
            String login = server.get("/login").body;
            assertTrue(login.contains("<a href=\"/forgot-password\">Forgot your password?</a>"),
                    login);
            assertFalse(login.contains("Your password was changed"), login);
            assertTrue(server.get("/login?reset").body.contains("Your password was changed"));

            // Both pages are served to nobody.
            Reply ask = server.get("/forgot-password");
            assertEquals(200, ask.status);
            assertTrue(ask.body.contains("name=\"username\""), ask.body);
            assertEquals("no-store", ask.header("Cache-Control"));

            // An account that does not exist is answered as one that does.
            Reply unknown = server.post("/forgot-password", "username=nobody%40example.com");
            assertEquals("302 /forgot-password?sent",
                    unknown.status + " " + unknown.header("Location"));
            assertEquals(0, mail.sent().size());
            Reply known = server.post("/forgot-password", "username=ada%40example.com");
            assertEquals("302 /forgot-password?sent",
                    known.status + " " + known.header("Location"));
            assertEquals(1, mail.sent().size());
            assertEquals(server.get("/forgot-password?sent").body.length() > 0, true);

            // The request named localhost, so the link may.
            String link = link(ADA);
            assertTrue(link.startsWith("http://localhost/reset-password?token="), link);
            Reply form = server.get(target(link));
            assertEquals(200, form.status);
            assertTrue(form.body.contains("name=\"password\"")
                    && form.body.contains("name=\"confirm\""), form.body);
            assertEquals("no-referrer", form.header("Referrer-Policy"));
            String token = link.substring(link.indexOf("token=") + 6);

            // A password that is refused keeps the link alive.
            Reply shortOne = server.post("/reset-password",
                    "token=" + token + "&password=abc&confirm=abc");
            assertEquals("302 /reset-password?token=" + token + "&invalid",
                    shortOne.status + " " + shortOne.header("Location"));
            Reply differ = server.post("/reset-password",
                    "token=" + token + "&password=new+password&confirm=another+one");
            assertEquals("302 /reset-password?token=" + token + "&invalid",
                    differ.status + " " + differ.header("Location"));

            Reply done = server.post("/reset-password",
                    "token=" + token + "&password=new+password&confirm=new+password");
            assertEquals("302 /login?reset", done.status + " " + done.header("Location"));
            // Resetting a password signs nobody in.
            assertEquals("/login", server.get("/me", "Accept", "text/html").header("Location"));

            // The link is spent.
            Reply again = server.post("/reset-password",
                    "token=" + token + "&password=third+password&confirm=third+password");
            assertEquals("302 /forgot-password?expired",
                    again.status + " " + again.header("Location"));

            assertEquals("/login?error", server.post("/login",
                    "username=ada%40example.com&password=old+password").header("Location"));
            // Back to the page that was asked for before the sign-in.
            assertEquals("/me", server.post("/login",
                    "username=ada%40example.com&password=new+password").header("Location"));
            assertEquals("/me " + ADA, server.get("/me").body);

            // What the health report and the outbox gauge read.
            MailRuntime.Probe probe = MailRuntime.probe();
            assertNotNull(probe);
            assertEquals("memory", probe.transport());
            assertTrue(probe.isHealthy());
            assertEquals(0, probe.pending());
        }
    }

    @Test
    @DisplayName("a reset link dies with its half hour, and a made-up one is refused")
    void resetLinksExpire() throws Exception {
        try (SecuredServer server = startReset(settings(), null)) {
            mail.clearSent();
            server.post("/forgot-password", "username=ada%40example.com");
            String token = link(ADA).substring(link(ADA).indexOf("token=") + 6);
            clock.now += 31 * 60000L;
            Reply late = server.post("/reset-password",
                    "token=" + token + "&password=new+password&confirm=new+password");
            assertEquals("302 /forgot-password?expired",
                    late.status + " " + late.header("Location"));
            Reply none = server.get("/reset-password");
            assertEquals("302 /forgot-password?expired",
                    none.status + " " + none.header("Location"));
            Reply invented = server.post("/reset-password",
                    "token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "&password=new+password&confirm=new+password");
            assertEquals("302 /forgot-password?expired",
                    invented.status + " " + invented.header("Location"));
            assertEquals("/", server.post("/login",
                    "username=ada%40example.com&password=old+password").header("Location"));
        }
    }

    @Test
    @DisplayName("asking for links is rate limited, by network and by account")
    void resetRequestsAreLimited() throws Exception {
        try (SecuredServer server = startReset(settings(), null)) {
            mail.clearSent();
            for (int iter = 0 ; iter < 5 ; iter++) {
                assertEquals(302, server.post("/forgot-password",
                        "username=ada%40example.com").status);
            }
            Reply refused = server.post("/forgot-password", "username=ada%40example.com");
            assertEquals(429, refused.status, refused.toString());
            assertNotNull(refused.header("Retry-After"), refused.toString());
            assertEquals(5, mail.sent().size());
        }
    }

    @Test
    @DisplayName("a link is never built from the Host header of a request")
    void theLinkNamesTheConfiguredAddress() throws Exception {
        // Configured in code.
        try (SecuredServer server = startReset(settings(), "https://shop.example.com/")) {
            mail.clearSent();
            server.post("/forgot-password", "username=ada%40example.com",
                    "Host", "attacker.example");
            assertTrue(link(ADA).startsWith("https://shop.example.com/reset-password?token="),
                    link(ADA));
        }
        // Configured in the properties.
        Properties settings = settings();
        settings.setProperty(PasswordResetConfigurer.BASE_URL, "https://app.example.com");
        try (SecuredServer server = startReset(settings, null)) {
            mail.clearSent();
            server.post("/forgot-password", "username=ada%40example.com",
                    "Host", "attacker.example");
            assertTrue(link(ADA).startsWith("https://app.example.com/reset-password?token="),
                    link(ADA));
        }
        // Not configured: localhost is served, and anything else fails
        // naming the setting, before any account is looked at.
        try (SecuredServer server = startReset(settings(), null)) {
            mail.clearSent();
            IllegalStateException refused = assertThrows(IllegalStateException.class,
                    () -> server.post("/forgot-password", "username=ada%40example.com",
                            "Host", "attacker.example"));
            assertTrue(refused.getMessage().contains(PasswordResetConfigurer.BASE_URL),
                    refused.getMessage());
            assertEquals(0, mail.sent().size());
        }
        assertThrows(Exception.class,
                () -> startReset(settings(), "https://shop.example.com/app").close());
    }

    @Test
    @DisplayName("a mail's template is rendered when it is sent, and its text derived from it")
    void templatesRenderAtSendTime() throws Exception {
        try (SecuredServer server = startReset(settings(), null)) {
            mail.clearSent();
            // No templates in this application: the mail says where one goes.
            MailRuntime.install(null);
            IOException none = assertThrows(IOException.class, () -> mail.send(
                    Mail.to(ADA).subject("Hello").template("mail/welcome", new Model()).build()));
            assertTrue(none.getMessage().contains("templates/mail/welcome.html"),
                    none.getMessage());

            // What the generated wiring installs over the compiled templates.
            MailRuntime.install(new MailRuntime.TemplateRenderer() {
                @Override
                public byte[] render(String view, Model model) throws IOException {
                    return ("<p>" + view + " for <a href=\"https://example.com/a\">"
                            + model.getAttribute("name") + "</a></p>").getBytes("UTF-8");
                }
            });
            try {
                mail.send(Mail.to(ADA).subject("Hello")
                        .template("mail/welcome", new Model().addAttribute("name", "Ada"))
                        .build());
                Mail sent = mail.sent().get(0);
                assertEquals("<p>mail/welcome for <a href=\"https://example.com/a\">Ada</a></p>",
                        sent.getHtml());
                assertTrue(sent.getText().contains("mail/welcome for Ada")
                        && sent.getText().contains("https://example.com/a"), sent.getText());
                assertEquals("Accounts <accounts@example.com>", sent.getFrom());

                // Text of the caller's own is kept.
                mail.send(Mail.to(ADA).subject("Hello").text("Plain words")
                        .template("mail/welcome", new Model().addAttribute("name", "Ada"))
                        .build());
                assertEquals("Plain words", mail.sent().get(1).getText());
            } finally {
                MailRuntime.install(null);
            }
        }
    }

    // ---------------------------------------------------- email verification

    @Test
    @DisplayName("following the mailed link proves the address, and opens what asks for it")
    void emailVerification() throws Exception {
        EmailVerificationService service = new EmailVerificationService(tokens, mail,
                new InMemoryVerifiedEmailRepository()).clock(clock);
        try (SecuredServer server = SecuredServer.start(settings(), "dev", new Object[] {users},
                APP, http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/orders").access(service.verified())
                                .anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .emailVerification(verify -> verify.service(service)).build())) {
            mail.clearSent();
            // No reset in this chain: its login page offers none, and has
            // nothing to say to a ?reset somebody typed.
            String login = server.get("/login?reset").body;
            assertFalse(login.contains("Forgot your password")
                    || login.contains("Your password was changed"), login);
            server.post("/login", "username=ray&password=ray+password");
            assertEquals("/me ray", server.get("/me").body);
            assertEquals(403, server.get("/orders").status);

            service.send("ray", "ray@example.com", "http://localhost/verify-email");
            String link = link("ray@example.com");
            assertFalse(service.isVerified("ray"));

            // A link that was never mailed.
            assertEquals(400, server.get("/verify-email?token=nothing").status);
            assertEquals(400, server.get("/verify-email").status);
            assertFalse(service.isVerified("ray"));

            // The real one, followed in a browser that is not signed in.
            String session = server.cookies.remove("CN1SESSION");
            // Opening it proves nothing: a mail gateway opens every link.
            Reply page = server.get(target(link));
            String token = link.substring(link.indexOf("token=") + 6);
            assertEquals(200, page.status);
            assertTrue(page.body.contains("<form method=\"post\" action=\"/verify-email\">")
                    && page.body.contains("name=\"token\" value=\"" + token + "\""), page.body);
            assertEquals("no-store", page.header("Cache-Control"));
            assertFalse(service.isVerified("ray"));
            // Pressing the button does.
            Reply done = server.post("/verify-email", "token=" + token);
            assertEquals("302 /?verified", done.status + " " + done.header("Location"));
            assertTrue(service.isVerified("ray"));
            assertEquals("ray@example.com", service.getVerifiedEmail("ray"));
            assertEquals(400, server.post("/verify-email", "token=" + token).status);
            assertEquals(400, server.post("/verify-email", "token=%22%3E%3Cscript%3E").status);

            server.cookies.put("CN1SESSION", session);
            assertEquals("/orders ray", server.get("/orders").body);
        }
    }

    // ------------------------------------------------------- the mailed code

    private String mailedCode() throws Exception {
        List<Mail> sent = mail.sent();
        assertFalse(sent.isEmpty(), "nothing was mailed");
        String text = sent.get(sent.size() - 1).getText();
        String lead = "Your sign-in code is ";
        int at = text.indexOf(lead);
        assertTrue(at >= 0, text);
        return text.substring(at + lead.length(), at + lead.length() + 6);
    }

    @Test
    @DisplayName("a user whose second factor is a mailed code is sent one and signs in with it")
    void mailedCodeSignIn() throws Exception {
        EmailCodeService codes = new EmailCodeService(new InMemoryEmailCodeRepository(), mail)
                .clock(clock);
        codes.enable(ADA);
        RecoveryCodeService recovery = new RecoveryCodeService(
                new InMemoryRecoveryCodeRepository());
        try (SecuredServer server = SecuredServer.start(settings(), "dev",
                new Object[] {users, codes, recovery}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock)).build())) {
            mail.clearSent();
            // No second factor: straight in.
            assertEquals("/", server.post("/login", "username=ray&password=ray+password")
                    .header("Location"));
            server.cookies.clear();
            assertEquals(0, mail.sent().size());

            Reply first = server.post("/login",
                    "username=ada%40example.com&password=old+password");
            assertEquals("302 /login/mfa", first.status + " " + first.header("Location"));
            // The password was enough to be mailed a code, and for nothing else.
            assertEquals(1, mail.sent().size());
            assertEquals("/login", server.get("/me", "Accept", "text/html").header("Location"));
            Reply page = server.get("/login/mfa");
            assertTrue(page.body.contains("name=\"code\"") && page.body.contains("name=\"send\""),
                    page.body);

            String code = mailedCode();
            String wrong = "000000".equals(code) ? "000001" : "000000";
            Reply refused = server.post("/login/mfa", "code=" + wrong);
            assertEquals("302 /login/mfa?error",
                    refused.status + " " + refused.header("Location"));

            // Another one too soon is not mailed; after the pause it is, and
            // it replaces the first.
            Reply soon = server.post("/login/mfa", "send=1");
            assertEquals("302 /login/mfa?sent", soon.status + " " + soon.header("Location"));
            assertEquals(1, mail.sent().size());
            clock.now += 31000;
            server.post("/login/mfa", "send=1");
            assertEquals(2, mail.sent().size());
            String second = mailedCode();
            if (!second.equals(code)) {
                assertEquals("/login/mfa?error",
                        server.post("/login/mfa", "code=" + code).header("Location"));
            }
            Reply done = server.post("/login/mfa", "code=" + second);
            assertEquals("302 /me", done.status + " " + done.header("Location"));
            assertEquals("/me " + ADA, server.get("/me").body);
            // Spent with the sign-in it made.
            assertFalse(codes.verify(ADA, second));
        }
    }

    @Test
    @DisplayName("a mailed code runs out, and guesses run out before it does")
    void mailedCodesAreBounded() throws Exception {
        EmailCodeService codes = new EmailCodeService(new InMemoryEmailCodeRepository(), mail)
                .clock(clock);
        codes.enable(ADA);
        try (SecuredServer server = SecuredServer.start(settings(), "dev",
                new Object[] {users, codes}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock)).build())) {
            mail.clearSent();
            server.post("/login", "username=ada%40example.com&password=old+password");
            String code = mailedCode();
            String wrong = "000000".equals(code) ? "000001" : "000000";
            for (int iter = 0 ; iter < 5 ; iter++) {
                server.post("/login/mfa", "code=" + wrong);
            }
            // Five wrong guesses and the right one is worth nothing.
            assertEquals("/login/mfa?error",
                    server.post("/login/mfa", "code=" + code).header("Location"));

            clock.now += 31000;
            server.post("/login/mfa", "send=1");
            String late = mailedCode();
            clock.now += 601000;
            Reply expired = server.post("/login/mfa", "code=" + late);
            assertFalse("/".equals(expired.header("Location")), expired.toString());
            assertEquals("/login", server.get("/me", "Accept", "text/html").header("Location"));
        }
    }
}
