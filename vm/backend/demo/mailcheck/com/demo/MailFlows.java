/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.demo;

import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SecuritySchema;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.mail.EmailVerificationService;
import com.codename1.backend.security.mail.InMemoryOneTimeTokenRepository;
import com.codename1.backend.security.mail.InMemoryVerifiedEmailRepository;
import com.codename1.backend.security.mail.JdbcOneTimeTokenRepository;
import com.codename1.backend.security.mail.JdbcVerifiedEmailRepository;
import com.codename1.backend.security.mail.OneTimeTokenRepository;
import com.codename1.backend.security.mail.OneTimeTokenService;
import com.codename1.backend.security.mail.PasswordResetService;
import com.codename1.backend.security.mail.VerifiedEmailRepository;
import com.codename1.backend.security.mfa.EmailCodeRepository;
import com.codename1.backend.security.mfa.EmailCodeService;
import com.codename1.backend.security.mfa.InMemoryEmailCodeRepository;
import com.codename1.backend.security.mfa.JdbcEmailCodeRepository;
import java.util.ArrayList;
import java.util.List;

/// The flows the security layer runs over mail -- one-time tokens, a password
/// reset, proving an address, a mailed sign-in code -- against the transport
/// that keeps mail in memory, with the stores in memory and in a database.
final class MailFlows {
    private final List<String> failures;
    private int passed;
    private final MailSender mail = new MailSender();
    private final long[] now = {1000000000000L};
    private final Clock clock = new Clock() {
        @Override
        public long currentTimeMillis() {
            return now[0];
        }
    };

    private MailFlows(List<String> failures) {
        this.failures = failures;
    }

    /// Runs every check; answers how many passed and adds the rest to
    /// `failures`.
    ///
    /// @param database a database URL to keep the stores in, or null for the
    ///     ones in memory
    static int run(String database, List<String> failures) throws Exception {
        // What the flows mail from; read by the mail support of this process,
        // which has no server and so no application.properties. Set for these
        // checks alone: it would be every other mailer's sender too.
        System.setProperty("cn1.mail.from", "Accounts <accounts@example.com>");
        try {
            return stores(database, failures);
        } finally {
            System.clearProperty("cn1.mail.from");
        }
    }

    private static int stores(String database, List<String> failures) throws Exception {
        MailFlows f = new MailFlows(failures);
        if (database == null) {
            f.all("memory ", new InMemoryOneTimeTokenRepository(),
                    new InMemoryVerifiedEmailRepository(), new InMemoryEmailCodeRepository());
            return f.passed;
        }
        DataSource ds = DataSource.open(database, 2);
        try {
            SecuritySchema.resetTableNames();
            Migrations.of(ds, SecuritySchema.migrations()).migrate();
            String w = database.indexOf("://") > 0
                    ? database.substring(0, database.indexOf(':')) + " " : "sqlite ";
            String[] tables = {"cn1_one_time_token", "cn1_verified_email", "cn1_mfa_email"};
            for (String table : tables) {
                ds.execute("DELETE FROM " + table, new Object[0]);
            }
            f.all(w, new JdbcOneTimeTokenRepository(ds), new JdbcVerifiedEmailRepository(ds),
                    new JdbcEmailCodeRepository(ds));
        } finally {
            ds.close();
        }
        return f.passed;
    }

    private void check(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    private void all(String w, OneTimeTokenRepository tokenStore,
            VerifiedEmailRepository addressStore, EmailCodeRepository codeStore)
            throws Exception {
        OneTimeTokenService tokens = new OneTimeTokenService(tokenStore).clock(clock);
        tokens(w, tokens);
        reset(w, tokens);
        EmailVerificationService verification = verification(w, tokens, addressStore);
        codes(w, codeStore, verification);
    }

    private void tokens(String w, OneTimeTokenService tokens) {
        String a = tokens.create("p", "Ada@example.com", 60);
        check(w + "token length", Integer.valueOf(43), Integer.valueOf(a.length()));
        check(w + "token wrong purpose", null, tokens.consume("q", a));
        check(w + "token survives a wrong purpose", "Ada@example.com", tokens.consume("p", a));
        check(w + "token works once", null, tokens.consume("p", a));
        String b = tokens.create("p", "ada@example.com", 60);
        String c = tokens.create("p", "ADA@example.com", 60);
        check(w + "a newer token replaces the older", null, tokens.consume("p", b));
        check(w + "tokens differ", Boolean.FALSE, Boolean.valueOf(b.equals(c)));
        String other = tokens.create("q", "ada@example.com", 60);
        check(w + "another purpose is another token", "ADA@example.com", tokens.consume("p", c));
        check(w + "and is still good", "ada@example.com", tokens.consume("q", other));
        String d = tokens.create("p", "ada@example.com", 60);
        now[0] += 61000;
        check(w + "token expires", null, tokens.consume("p", d));
        String e = tokens.create("p", "ada@example.com", 60);
        tokens.revoke("p", "ADA@EXAMPLE.COM");
        check(w + "token revoked", null, tokens.consume("p", e));
        check(w + "garbage token", null, tokens.consume("p", "nope"));
        check(w + "null token", null, tokens.consume("p", null));
    }

    private static String linkToken(Mail m) {
        String text = m.getText();
        int at = text.indexOf("token=");
        return at < 0 ? null : text.substring(at + 6, at + 6 + 43);
    }

    private void reset(String w, OneTimeTokenService tokens) throws Exception {
        PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder();
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                new User("ada@example.com", encoder.encode("old password"),
                        new ArrayList<com.codename1.backend.security.GrantedAuthority>()),
                new User("bob", encoder.encode("old password"),
                        new ArrayList<com.codename1.backend.security.GrantedAuthority>()),
                new User("off@example.com", encoder.encode("old password"), false, true, true,
                        true, new ArrayList<com.codename1.backend.security.GrantedAuthority>()));
        PasswordResetService service = new PasswordResetService(tokens, mail, users, users,
                encoder);
        mail.clearSent();
        service.request("nobody@example.com", "https://example.com/reset-password");
        service.request("bob", "https://example.com/reset-password");
        service.request("off@example.com", "https://example.com/reset-password");
        service.request(null, "https://example.com/reset-password");
        check(w + "reset: nothing for no user, no address or a disabled account",
                Integer.valueOf(0), Integer.valueOf(mail.sent().size()));
        service.request("ADA@example.com", "https://example.com/reset-password");
        check(w + "reset: one mail", Integer.valueOf(1), Integer.valueOf(mail.sent().size()));
        Mail m = mail.sent().get(0);
        check(w + "reset: to the user", "[ada@example.com]", String.valueOf(m.getTo()));
        check(w + "reset: subject", "Reset your password", m.getSubject());
        check(w + "reset: link in the text", Boolean.TRUE, Boolean.valueOf(
                m.getText().indexOf("https://example.com/reset-password?token=") > 0));
        check(w + "reset: link in the html", Boolean.TRUE, Boolean.valueOf(
                m.getHtml().indexOf("href=\"https://example.com/reset-password?token=") > 0));
        String token = linkToken(m);
        check(w + "reset: short password refused", Boolean.FALSE,
                Boolean.valueOf(service.reset(token, "short")));
        check(w + "reset: a refused password keeps the link", Boolean.TRUE,
                Boolean.valueOf(service.reset(token, "a new password")));
        check(w + "reset: password changed", Boolean.TRUE, Boolean.valueOf(encoder.matches(
                "a new password", users.loadUserByUsername("ada@example.com").getPassword())));
        check(w + "reset: link works once", Boolean.FALSE,
                Boolean.valueOf(service.reset(token, "another password")));
        check(w + "reset: password unchanged by a used link", Boolean.TRUE,
                Boolean.valueOf(encoder.matches("a new password",
                        users.loadUserByUsername("ada@example.com").getPassword())));
        mail.clearSent();
        service.request("ada@example.com", "https://example.com/reset-password");
        String first = linkToken(mail.sent().get(0));
        service.request("ada@example.com", "https://example.com/reset-password");
        String second = linkToken(mail.sent().get(1));
        check(w + "reset: an older link stops working", Boolean.FALSE,
                Boolean.valueOf(service.reset(first, "another password")));
        now[0] += 31 * 60000L;
        check(w + "reset: link expires", Boolean.FALSE,
                Boolean.valueOf(service.reset(second, "another password")));
        boolean threw = false;
        try {
            service.request("ada@example.com", "/reset-password");
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(w + "reset: a path is not an address to mail", Boolean.TRUE,
                Boolean.valueOf(threw));
        mail.clearSent();
    }

    private EmailVerificationService verification(String w, OneTimeTokenService tokens,
            VerifiedEmailRepository store) throws Exception {
        EmailVerificationService service = new EmailVerificationService(tokens, mail, store)
                .clock(clock);
        mail.clearSent();
        check(w + "verify: unknown user", Boolean.FALSE,
                Boolean.valueOf(service.isVerified("bob")));
        check(w + "verify: user name that is an address", "x@example.com",
                service.emailFor("x@example.com"));
        check(w + "verify: no address for a plain name", null, service.emailFor("bob"));
        service.send("Bob", "bob@example.com", "https://example.com/verify-email");
        check(w + "verify: one mail", Integer.valueOf(1), Integer.valueOf(mail.sent().size()));
        check(w + "verify: to the address", "[bob@example.com]",
                String.valueOf(mail.sent().get(0).getTo()));
        check(w + "verify: not yet", Boolean.FALSE, Boolean.valueOf(service.isVerified("bob")));
        check(w + "verify: an unproved address is not used", null, service.emailFor("bob"));
        String token = linkToken(mail.sent().get(0));
        check(w + "verify: wrong token", null, service.confirm(token.substring(1) + "A"));
        check(w + "verify: confirm", "Bob", service.confirm(token));
        check(w + "verify: proved", Boolean.TRUE, Boolean.valueOf(service.isVerified("BOB")));
        check(w + "verify: proved address", "bob@example.com", service.emailFor("bob"));
        check(w + "verify: link works once", null, service.confirm(token));
        service.send("bob", "robert@example.com", "https://example.com/verify-email");
        check(w + "verify: a new address is unproved", Boolean.FALSE,
                Boolean.valueOf(service.isVerified("bob")));
        check(w + "verify: confirm the new one", "bob",
                service.confirm(linkToken(mail.sent().get(1))));
        check(w + "verify: new address", "robert@example.com", service.getVerifiedEmail("bob"));
        boolean threw = false;
        try {
            service.send("bob", "not an address", "https://example.com/verify-email");
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(w + "verify: bad address refused", Boolean.TRUE, Boolean.valueOf(threw));
        check(w + "verify: a refused address changes nothing", "robert@example.com",
                service.getVerifiedEmail("bob"));
        // An address is proved by the link mailed to it and by no other.
        store.savePending("Carol", "first@example.com", "proof-of-the-first");
        store.savePending("carol", "second@example.com", "proof-of-the-second");
        check(w + "verify: a link to an earlier address proves nothing", Boolean.FALSE,
                Boolean.valueOf(store.markVerified("carol", "proof-of-the-first", 5)));
        check(w + "verify: still unproved", Boolean.FALSE,
                Boolean.valueOf(store.isVerified("carol")));
        check(w + "verify: the link to this address proves it", Boolean.TRUE,
                Boolean.valueOf(store.markVerified("CAROL", "proof-of-the-second", 5)));
        check(w + "verify: and proves it once", Boolean.FALSE,
                Boolean.valueOf(store.markVerified("carol", "proof-of-the-second", 6)));
        check(w + "verify: the address proved", "second@example.com",
                store.isVerified("carol") ? store.findEmail("carol") : null);
        store.delete("carol");
        mail.clearSent();
        return service;
    }

    private static String mailedCode(Mail m) {
        String text = m.getText();
        int at = text.indexOf("code is ");
        return text.substring(at + 8, at + 14);
    }

    private static String wrong(String code) {
        return (code.charAt(0) == '9' ? "0" : "9") + code.substring(1);
    }

    private void codes(String w, EmailCodeRepository store, EmailVerificationService addresses)
            throws Exception {
        EmailCodeService service = new EmailCodeService(store, mail).clock(clock)
                .addressResolver(addresses);
        mail.clearSent();
        check(w + "code: off by default", Boolean.FALSE,
                Boolean.valueOf(service.isEnabled("bob")));
        check(w + "code: nothing to verify", Boolean.FALSE,
                Boolean.valueOf(service.verify("bob", "123456")));
        service.enable("Bob");
        check(w + "code: enabled", Boolean.TRUE, Boolean.valueOf(service.isEnabled("bob")));
        check(w + "code: sent", Boolean.TRUE, Boolean.valueOf(service.send("bob")));
        check(w + "code: to the proved address", "[robert@example.com]",
                String.valueOf(mail.sent().get(0).getTo()));
        String code = mailedCode(mail.sent().get(0));
        check(w + "code: six digits", Boolean.TRUE,
                Boolean.valueOf(EmailCodeService.isCodeShaped(code)));
        check(w + "code: not resent at once", Boolean.FALSE, Boolean.valueOf(service.send("bob")));
        check(w + "code: still one mail", Integer.valueOf(1),
                Integer.valueOf(mail.sent().size()));
        check(w + "code: wrong", Boolean.FALSE,
                Boolean.valueOf(service.verify("bob", wrong(code))));
        check(w + "code: not a code", Boolean.FALSE,
                Boolean.valueOf(service.verify("bob", "abcdef")));
        check(w + "code: right, written with a space", Boolean.TRUE, Boolean.valueOf(
                service.verify("BOB", code.substring(0, 3) + " " + code.substring(3))));
        check(w + "code: works once", Boolean.FALSE, Boolean.valueOf(service.verify("bob", code)));
        check(w + "code: still enabled", Boolean.TRUE, Boolean.valueOf(service.isEnabled("bob")));

        // No wait: the code before it was used to sign in.
        check(w + "code: mailed at once after a sign-in", Boolean.TRUE,
                Boolean.valueOf(service.send("bob")));
        code = mailedCode(mail.sent().get(1));
        service.verify("bob", wrong(code));
        service.verify("bob", wrong(code));
        now[0] += 31000;
        check(w + "code: another one", Boolean.TRUE, Boolean.valueOf(service.send("bob")));
        String older = code;
        code = mailedCode(mail.sent().get(2));
        check(w + "code: the one before it is gone", Boolean.TRUE,
                Boolean.valueOf(older.equals(code) || !service.verify("bob", older)));
        // Three were left, whichever code they are spent on; one went above
        // unless the two codes happened to be the same.
        for (int i = 0; i < 3; i++) {
            service.verify("bob", wrong(code));
        }
        check(w + "code: asking for another is not a way to more tries", Boolean.FALSE,
                Boolean.valueOf(service.verify("bob", code)));
        now[0] += 31000;
        check(w + "code: none is mailed while the tries are used up", Boolean.FALSE,
                Boolean.valueOf(service.send("bob")));
        check(w + "code: still three mails", Integer.valueOf(3),
                Integer.valueOf(mail.sent().size()));
        // The wait ends when the last code runs out, however often one was
        // asked for meanwhile.
        now[0] += 570000;
        check(w + "code: mailed again once the last one ran out", Boolean.TRUE,
                Boolean.valueOf(service.send("bob")));
        code = mailedCode(mail.sent().get(3));
        check(w + "code: and the tries start again", Boolean.TRUE,
                Boolean.valueOf(service.verify("bob", code)));
        now[0] += 601000;
        service.send("bob");
        code = mailedCode(mail.sent().get(4));
        check(w + "code: a later code works", Boolean.TRUE, Boolean.valueOf(service.verify("bob", code)));
        now[0] += 31000;
        service.send("bob");
        code = mailedCode(mail.sent().get(5));
        now[0] += 601000;
        check(w + "code: expires", Boolean.FALSE, Boolean.valueOf(service.verify("bob", code)));
        now[0] += 1000;
        service.send("bob");
        code = mailedCode(mail.sent().get(6));
        service.disable("bob");
        check(w + "code: disabled", Boolean.FALSE, Boolean.valueOf(service.isEnabled("bob")));
        check(w + "code: a waiting code goes with it", Boolean.FALSE,
                Boolean.valueOf(service.verify("bob", code)));
        service.enable("nobody");
        check(w + "code: no address, nothing sent", Boolean.FALSE,
                Boolean.valueOf(service.send("nobody")));
        mail.clearSent();
    }
}
