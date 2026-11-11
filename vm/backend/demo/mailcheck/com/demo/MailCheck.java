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

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.Transactions;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailAuthenticationException;
import com.codename1.backend.mail.MailException;
import com.codename1.backend.mail.MailSendException;
import com.codename1.backend.mail.Mailer;
import com.codename1.backend.mail.Mailers;
import com.codename1.backend.mail.smtp.SmtpTransport;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// Exercises mail: the MIME encoder, the SMTP client against a mail server
/// that runs inside this process and records the bytes it is sent, the queue,
/// its retries and the outbox. The same checks run on the JVM and on the
/// native build. `CN1_MAILCHECK_URL` (or the first argument) names a real
/// server to send one mail of every shape through as well; its recipients
/// are `CN1_MAILCHECK_TO`.
public class MailCheck {
    private static int passed;
    private static final List<String> failures = new ArrayList<String>();
    /// e acute, built so that this file stays ASCII.
    private static final String ACUTE = String.valueOf((char) 0xe9);

    public static void main(String[] args) throws Exception {
        Mailers.register("smtp", new SmtpTransport.Factory());
        validation();
        capture();
        Server server = new Server();
        try {
            encoding(server);
            session(server);
            refusals(server);
            security(server);
            queue(server, null);
            String file = "mailcheck-" + Long.toString(System.nanoTime(), 36) + ".db";
            queue(server, file);
            outbox(server, file);
            passed += MailFlows.run(null, failures);
            passed += SesCheck.run(failures);
            passed += MailFlows.run(file, failures);
            new java.io.File(file).delete();
            new java.io.File(file + "-shm").delete();
            new java.io.File(file + "-wal").delete();
            String db = System.getenv("CN1_MAILCHECK_DB");
            if (db != null && db.length() > 0) {
                passed += MailFlows.run(db, failures);
                queue(server, db);
                outbox(server, db);
            }
        } finally {
            server.stop();
        }
        String url = args.length > 0 ? args[0] : System.getenv("CN1_MAILCHECK_URL");
        if (url != null && url.length() > 0) {
            real(url);
        }
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (String f : failures) {
            System.out.println("FAIL " + f);
        }
        System.out.println(failures.isEmpty() ? "MAILCHECK OK" : "MAILCHECK FAILED");
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    // ---------------------------------------------------------------- checks

    private static void validation() {
        check("no recipient", Boolean.TRUE, Boolean.valueOf(rejects(Mail.builder().subject("x")
                .text("y"))));
        check("no body", Boolean.TRUE, Boolean.valueOf(rejects(Mail.to("a@example.com")
                .subject("x"))));
        check("bad address", Boolean.TRUE, Boolean.valueOf(rejectsCall(new Runnable() {
            public void run() {
                Mail.to("not an address").subject("s").text("t").build();
            }
        })));
        check("header injection in subject", Boolean.TRUE, Boolean.valueOf(rejectsCall(
                new Runnable() {
                    public void run() {
                        Mail.to("a@example.com").subject("hi\r\nBcc: evil@example.com").text("t").build();
                    }
                })));
        check("header injection in address", Boolean.TRUE, Boolean.valueOf(rejectsCall(
                new Runnable() {
                    public void run() {
                        Mail.to("a@example.com\r\nRCPT TO:<evil@example.com>").subject("s").text("t").build();
                    }
                })));
        check("header injection in header", Boolean.TRUE, Boolean.valueOf(rejectsCall(
                new Runnable() {
                    public void run() {
                        Mail.to("a@example.com").header("X-Tag", "a\nb").subject("s").text("t").build();
                    }
                })));
        check("reserved header", Boolean.TRUE, Boolean.valueOf(rejectsCall(new Runnable() {
            public void run() {
                Mail.to("a@example.com").header("Subject", "two").subject("s").text("t").build();
            }
        })));
        check("a header no line could hold", Boolean.TRUE, Boolean.valueOf(rejectsCall(
                new Runnable() {
                    public void run() {
                        StringBuilder url = new StringBuilder("<https://example.com/");
                        while (url.length() < 1200) {
                            url.append("abcdefghij");
                        }
                        Mail.to("a@example.com").header("List-Unsubscribe", url + ">")
                                .subject("s").text("t").build();
                    }
                })));
        check("angle address in recipient", Boolean.TRUE, Boolean.valueOf(rejectsCall(
                new Runnable() {
                    public void run() {
                        Mail.to("a@example.com> SIZE=1 <b@example.com").subject("s").text("t").build();
                    }
                })));
    }

    private static boolean rejects(Mail.Builder b) {
        try {
            b.build();
            return false;
        } catch (IllegalArgumentException | IllegalStateException err) {
            return true;
        }
    }

    private static boolean rejectsCall(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException | IllegalStateException err) {
            return true;
        }
    }

    /// With no URL, mail is kept and can be read back.
    private static void capture() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.mail.from", "Shop <shop@example.com>");
        Mailer m = Mailers.open(Config.of(p, "test"), null);
        try {
            m.send(Mail.to("a@example.com").cc("c@example.com").subject("Hello")
                    .html("<p>Hi <b>there</b></p><p><a href=\"https://example.com/x\">Open</a></p>")
                    .attach("a.txt", "text/plain", WireBuffer.utf8("file")).build());
            List<Mail> sent = m.sent();
            check("captured count", Integer.valueOf(1), Integer.valueOf(sent.size()));
            Mail got = sent.get(0);
            check("captured from", "Shop <shop@example.com>", got.getFrom());
            check("captured to", "[a@example.com]", String.valueOf(got.getTo()));
            check("captured cc", "[c@example.com]", String.valueOf(got.getCc()));
            check("captured subject", "Hello", got.getSubject());
            check("captured derived text", "Hi there\n\nOpen (https://example.com/x)\n",
                    got.getText());
            check("captured attachment names", "[a.txt]",
                    String.valueOf(got.getAttachmentNames()));
            check("captured attachment", "file", WireBuffer.fromUtf8(got.getAttachment("a.txt")));
            m.clearSent();
            check("cleared", Integer.valueOf(0), Integer.valueOf(m.sent().size()));
            // A < that opens no tag is a sign, and what follows it is text.
            m.send(Mail.to("a@example.com").subject("s")
                    .html("<p>ok</p>Total < 5 items, 3 > 2 and a <").build());
            String derived = m.sent().get(0).getText();
            check("a less-than sign is text", Boolean.TRUE, Boolean.valueOf(derived != null
                    && derived.indexOf("Total < 5 items, 3 > 2 and a <") >= 0));
            m.clearSent();
            boolean threw = false;
            try {
                Mailers.open(Config.of(new Properties(), "test"), null)
                        .sendNow(Mail.to("a@example.com").subject("s").text("t").build());
            } catch (IOException err) {
                threw = true;
            }
            check("no sender is an error", Boolean.TRUE, Boolean.valueOf(threw));
        } finally {
            m.close();
        }
    }

    private static Mailer mailer(Server server, String database, DataSource[] ds,
                                 String... settings) throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.mail.url", "smtp://127.0.0.1:" + server.port);
        p.setProperty("cn1.mail.smtp.starttls", "false");
        p.setProperty("cn1.mail.from", "Shop <shop@example.com>");
        p.setProperty("cn1.mail.domain", "example.com");
        p.setProperty("cn1.mail.retry.backoff", "30");
        p.setProperty("cn1.mail.retry.multiplier", "1");
        p.setProperty("cn1.mail.retry.max-attempts", "3");
        p.setProperty("cn1.mail.outbox.sweep", "200");
        p.setProperty("cn1.mail.timeout.read", "5000");
        for (int i = 0; i < settings.length; i += 2) {
            p.setProperty(settings[i], settings[i + 1]);
        }
        DataSource d = database == null ? null : DataSource.open(database, 4);
        if (ds != null) {
            ds[0] = d;
        }
        return Mailers.open(Config.of(p, "test"), d);
    }

    /// What the encoder writes, read back the way a mail reader would.
    private static void encoding(Server server) throws Exception {
        server.reset();
        Mailer m = mailer(server, null, null);
        try {
            // Plain ASCII: nothing encoded, nothing folded.
            m.sendNow(Mail.to("a@example.com").subject("Plain").text("line one\nline two\n")
                    .build());
            Received r = server.last();
            check("plain from", "shop@example.com", r.from);
            check("plain rcpt", "[a@example.com]", String.valueOf(r.recipients));
            check("plain From", "Shop <shop@example.com>", r.header("From"));
            check("plain To", "a@example.com", r.header("To"));
            check("plain Subject", "Plain", r.header("Subject"));
            check("plain MIME-Version", "1.0", r.header("MIME-Version"));
            check("plain type", "text/plain; charset=UTF-8", r.header("Content-Type"));
            check("plain encoding", "7bit", r.header("Content-Transfer-Encoding"));
            check("plain body", "line one\r\nline two\r\n", r.body());
            String id = r.header("Message-ID");
            check("message id", Boolean.TRUE, Boolean.valueOf(id != null && id.startsWith("<")
                    && id.endsWith("@example.com>") && id.length() == 46));
            String date = r.header("Date");
            check("date", Boolean.TRUE, Boolean.valueOf(date != null && date.endsWith(" +0000")
                    && date.charAt(3) == ','));
            check("plain wire form", "", r.wireProblems());

            // A subject and a name with no place to fold are written so
            // that they have one: no line passes what a server takes.
            StringBuilder word = new StringBuilder();
            while (word.length() < 1500) {
                word.append("unbroken");
            }
            StringBuilder longName = new StringBuilder();
            while (longName.length() < 1100) {
                longName.append("Long Name ");
            }
            m.sendNow(Mail.to(longName.toString().trim() + " <a@example.com>")
                    .subject(word.toString()).text("t").build());
            r = server.last();
            check("long word wire form", "", r.wireProblems());
            check("long word subject", word.toString(), decodeWords(r.header("Subject")));
            check("long name", longName.toString().trim() + " <a@example.com>",
                    decodeWords(r.header("To")));

            // Dots at the start of a line survive the trip.
            String dotted = ".\n..two\n.three.\nfour\n.";
            m.sendNow(Mail.to("a@example.com").subject("Dots").text(dotted).build());
            check("dot stuffing", ".\r\n..two\r\n.three.\r\nfour\r\n.\r\n", server.last().body());

            // Non-ASCII everywhere it may appear.
            String name = "Ren" + ACUTE + "e";
            StringBuilder longSubject = new StringBuilder();
            for (int i = 0; i < 30; i++) {
                longSubject.append("caf").append(ACUTE).append(' ');
            }
            String subject = longSubject.toString().trim();
            StringBuilder longLine = new StringBuilder();
            for (int i = 0; i < 40; i++) {
                longLine.append("d").append(ACUTE).append("j").append((char) 0xe0).append(" vu = ");
            }
            String text = longLine + "\ntrailing space \nFrom the start\n";
            m.sendNow(Mail.to(name + " <a@example.com>", "b@example.com").subject(subject)
                    .text(text).build());
            r = server.last();
            check("utf8 wire form", "", r.wireProblems());
            check("utf8 To", name + " <a@example.com>, b@example.com",
                    decodeWords(r.header("To")));
            check("utf8 Subject", subject, decodeWords(r.header("Subject")));
            check("utf8 encoding", "quoted-printable", r.header("Content-Transfer-Encoding"));
            check("utf8 body", text.replace("\n", "\r\n"),
                    WireBuffer.fromUtf8(quotedPrintable(r.body())));

            // A long ASCII header is folded, never encoded.
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < 40; i++) {
                ascii.append("word").append(i).append(' ');
            }
            m.sendNow(Mail.to("a@example.com").subject(ascii.toString().trim()).text("x").build());
            r = server.last();
            check("folded wire form", "", r.wireProblems());
            check("folded Subject", ascii.toString().trim(), r.header("Subject"));
            check("folded, not encoded", Boolean.FALSE,
                    Boolean.valueOf(r.rawHeader("Subject").indexOf("=?") >= 0));

            // Text and HTML, an inline image and two attachments.
            byte[] image = new byte[5000];
            for (int i = 0; i < image.length; i++) {
                image[i] = (byte) (i * 31 + 7);
            }
            byte[] empty = new byte[0];
            String fileName = "r" + ACUTE + "sum" + ACUTE + " final.pdf";
            m.sendNow(Mail.to("a@example.com").bcc("hidden@example.com").replyTo("help@example.com")
                    .subject("Parts").text("see the picture")
                    .html("<p>see <img src=\"cid:logo\"></p>")
                    .inline("logo", "image/png", image)
                    .attach(fileName, "application/pdf", image)
                    .attach("empty.bin", "application/octet-stream", empty)
                    .header("X-Campaign", "spring").build());
            r = server.last();
            check("parts wire form", "", r.wireProblems());
            check("bcc is a recipient", "[a@example.com, hidden@example.com]",
                    String.valueOf(r.recipients));
            check("bcc is not a header", Boolean.FALSE,
                    Boolean.valueOf(r.data.indexOf("hidden@example.com") >= 0));
            check("reply to", "help@example.com", r.header("Reply-To"));
            check("custom header", "spring", r.header("X-Campaign"));
            Part root = Part.parse(r.data);
            check("mixed", Boolean.TRUE, Boolean.valueOf(root.type.startsWith("multipart/mixed")));
            check("mixed parts", Integer.valueOf(3), Integer.valueOf(root.parts.size()));
            Part alt = root.parts.size() > 0 ? root.parts.get(0) : new Part();
            check("alternative", Boolean.TRUE,
                    Boolean.valueOf(alt.type.startsWith("multipart/alternative")));
            check("alternative parts", Integer.valueOf(2), Integer.valueOf(alt.parts.size()));
            Part related = alt.parts.size() == 2 ? alt.parts.get(1) : new Part();
            check("related", Boolean.TRUE,
                    Boolean.valueOf(related.type.startsWith("multipart/related")));
            check("related parts", Integer.valueOf(2), Integer.valueOf(related.parts.size()));
            if (alt.parts.size() == 2) {
                check("text first", "text/plain; charset=UTF-8", alt.parts.get(0).type);
                check("text part", "see the picture", alt.parts.get(0).text().trim());
            }
            if (related.parts.size() == 2) {
                check("html in related", "text/html; charset=UTF-8", related.parts.get(0).type);
                check("html part", "<p>see <img src=\"cid:logo\"></p>",
                        related.parts.get(0).text().trim());
                Part img = related.parts.get(1);
                check("inline id", "<logo>", img.header("Content-ID"));
                check("inline disposition", Boolean.TRUE, Boolean.valueOf(
                        String.valueOf(img.header("Content-Disposition")).startsWith("inline")));
                check("inline bytes", Boolean.TRUE,
                        Boolean.valueOf(java.util.Arrays.equals(image, img.bytes())));
            }
            if (root.parts.size() == 3) {
                Part pdf = root.parts.get(1);
                check("attachment type", Boolean.TRUE,
                        Boolean.valueOf(pdf.type.startsWith("application/pdf")));
                check("attachment bytes", Boolean.TRUE,
                        Boolean.valueOf(java.util.Arrays.equals(image, pdf.bytes())));
                String disposition = String.valueOf(pdf.header("Content-Disposition"));
                check("attachment name", Boolean.TRUE, Boolean.valueOf(disposition.indexOf(
                        "filename*=UTF-8''r%C3%A9sum%C3%A9%20final.pdf") >= 0));
                check("empty attachment", Integer.valueOf(0),
                        Integer.valueOf(root.parts.get(2).bytes().length));
            }
        } finally {
            m.close();
        }
    }

    /// How the client speaks: one write for the envelope, one connection for
    /// several mails, and the same result without pipelining.
    private static void session(Server server) throws Exception {
        server.reset();
        Mailer m = mailer(server, null, null);
        try {
            for (int i = 0; i < 3; i++) {
                m.sendNow(Mail.to("a@example.com", "b@example.com").subject("n" + i).text("t")
                        .build());
            }
            check("one connection for three mails", Integer.valueOf(1),
                    Integer.valueOf(server.connections.get()));
            check("three mails", Integer.valueOf(3), Integer.valueOf(server.count()));
            check("envelope in one write", Boolean.TRUE, Boolean.valueOf(server.pipelined.get()));
            check("size declared", Boolean.TRUE, Boolean.valueOf(server.sawSize.get()));
        } finally {
            m.close();
        }
        server.reset();
        server.pipelining = false;
        m = mailer(server, null, null);
        try {
            m.sendNow(Mail.to("a@example.com", "b@example.com").subject("one by one").text("t")
                    .build());
            check("without pipelining", "[a@example.com, b@example.com]",
                    String.valueOf(server.last().recipients));
            check("not pipelined", Boolean.FALSE, Boolean.valueOf(server.pipelined.get()));
            // A big body leaves in pieces and arrives whole.
            StringBuilder big = new StringBuilder();
            for (int i = 0; i < 20000; i++) {
                big.append(i % 7 == 0 ? "." : "").append("line ").append(i).append('\n');
            }
            m.sendNow(Mail.to("a@example.com").subject("big").text(big.toString()).build());
            check("big body", big.toString().replace("\n", "\r\n"), server.last().body());
        } finally {
            m.close();
        }
    }

    private static MailException failure(Mailer m, Mail mail) throws IOException {
        try {
            m.sendNow(mail);
            return null;
        } catch (MailException err) {
            return err;
        }
    }

    private static void refusals(Server server) throws Exception {
        for (int round = 0; round < 2; round++) {
            server.reset();
            server.pipelining = round == 0;
            String w = round == 0 ? "pipelined " : "sequential ";
            Mailer m = mailer(server, null, null);
            try {
                MailException err = failure(m, Mail.to("reject@example.com").subject("s").text("t")
                        .build());
                check(w + "refused recipient is permanent", Boolean.TRUE,
                        Boolean.valueOf(err != null && err.isPermanent()));
                check(w + "refused code", Integer.valueOf(550), err instanceof MailSendException
                        ? Integer.valueOf(((MailSendException) err).getCode()) : null);
                err = failure(m, Mail.to("a@example.com", "defer@example.com").subject("s").text("t")
                        .build());
                check(w + "deferred recipient is transient", Boolean.FALSE,
                        err == null ? null : Boolean.valueOf(err.isPermanent()));
                check(w + "nothing sent when one is deferred", Integer.valueOf(0),
                        Integer.valueOf(server.count()));
                err = failure(m, Mail.to("a@example.com", "reject@example.com").subject("s")
                        .text("t").build());
                check(w + "partly refused is permanent", Boolean.TRUE,
                        Boolean.valueOf(err != null && err.isPermanent()));
                check(w + "partly refused is sent to the rest", "[a@example.com]",
                        server.count() == 1 ? String.valueOf(server.last().recipients) : null);
                // The session is still usable after each of those.
                m.sendNow(Mail.to("a@example.com").subject("after").text("t").build());
                check(w + "session survives refusals", Integer.valueOf(2),
                        Integer.valueOf(server.count()));
                server.failData.set(1);
                err = failure(m, Mail.to("a@example.com").subject("s").text("t").build());
                check(w + "451 after the body is transient", Boolean.FALSE,
                        err == null ? null : Boolean.valueOf(err.isPermanent()));
                server.rejectData.set(1);
                err = failure(m, Mail.to("a@example.com").subject("s").text("t").build());
                check(w + "554 after the body is permanent", Boolean.TRUE,
                        Boolean.valueOf(err != null && err.isPermanent()));
                server.maxSize = 2000;
            } finally {
                m.close();
            }
            m = mailer(server, null, null);
            try {
                int before = server.count();
                MailException err = failure(m, Mail.to("a@example.com").subject("s")
                        .attach("big.bin", "application/octet-stream", new byte[4000]).text("t")
                        .build());
                check(w + "too large is permanent", Boolean.TRUE,
                        Boolean.valueOf(err != null && err.isPermanent()));
                check(w + "too large sends nothing", Integer.valueOf(before),
                        Integer.valueOf(server.count()));
                String address = "jos" + ACUTE + "@example.com";
                err = failure(m, Mail.to(address).subject("s").text("t").build());
                check(w + "utf8 address without SMTPUTF8", Boolean.TRUE,
                        Boolean.valueOf(err != null && err.isPermanent()));
                server.maxSize = 0;
            } finally {
                m.close();
            }
            server.smtpUtf8 = true;
            m = mailer(server, null, null);
            try {
                String address = "jos" + ACUTE + "@example.com";
                m.sendNow(Mail.to(address).subject("s").text("t").build());
                check(w + "utf8 address", "[" + address + "]",
                        String.valueOf(server.last().recipients));
                check(w + "SMTPUTF8 asked for", Boolean.TRUE,
                        Boolean.valueOf(server.sawUtf8.get()));
            } finally {
                m.close();
            }
            // A sender whose domain is not ASCII, and no cn1.mail.domain: the
            // Message-ID is still one a header may hold.
            m = mailer(server, null, null, "cn1.mail.domain", "");
            try {
                m.sendNow(Mail.to("a@example.com").from("shop@caf" + ACUTE + ".example")
                        .subject("s").text("t").build());
                String id = server.last().header("Message-ID");
                check(w + "message id of a sender outside ASCII", Boolean.TRUE,
                        Boolean.valueOf(id != null && id.endsWith("@mail.invalid>")));
                check(w + "and the headers after it are headers", "1.0",
                        server.last().header("MIME-Version"));
            } finally {
                m.close();
            }
        }
        // Nothing listening.
        Properties p = new Properties();
        p.setProperty("cn1.mail.url", "smtp://127.0.0.1:1");
        p.setProperty("cn1.mail.from", "shop@example.com");
        Mailer m = Mailers.open(Config.of(p, "test"), null);
        try {
            MailException err = failure(m, Mail.to("a@example.com").subject("s").text("t")
                    .build());
            check("unreachable is transient", Boolean.FALSE,
                    err == null ? null : Boolean.valueOf(err.isPermanent()));
            check("unreachable is unhealthy", Boolean.FALSE, Boolean.valueOf(m.isHealthy()));
        } finally {
            m.close();
        }
    }

    private static void security(Server server) throws Exception {
        Mail mail = Mail.to("a@example.com").subject("s").text("t").build();
        // STARTTLS is required unless switched off.
        server.reset();
        Mailer m = mailer(server, null, null, "cn1.mail.smtp.starttls", "true");
        try {
            MailException err = failure(m, mail);
            check("no STARTTLS is refused", Boolean.TRUE, Boolean.valueOf(err != null
                    && String.valueOf(err.getMessage()).indexOf("STARTTLS") >= 0));
            check("no STARTTLS sends nothing", Integer.valueOf(0),
                    Integer.valueOf(server.count()));
        } finally {
            m.close();
        }
        // A response injected between the go-ahead and the handshake.
        server.reset();
        server.startTls = true;
        server.inject = true;
        m = mailer(server, null, null, "cn1.mail.smtp.starttls", "true");
        try {
            MailException err = failure(m, mail);
            check("injection after STARTTLS is refused", Boolean.TRUE, Boolean.valueOf(err != null
                    && String.valueOf(err.getMessage()).indexOf("inject") >= 0));
            check("injection sends nothing", Integer.valueOf(0), Integer.valueOf(server.count()));
        } finally {
            m.close();
        }
        // A password never crosses a connection that is not encrypted.
        server.reset();
        Properties p = new Properties();
        p.setProperty("cn1.mail.url", "smtp://user:p%40ss@127.0.0.1:" + server.port);
        p.setProperty("cn1.mail.smtp.starttls", "false");
        p.setProperty("cn1.mail.from", "shop@example.com");
        m = Mailers.open(Config.of(p, "test"), null);
        try {
            MailException err = failure(m, mail);
            check("password in the clear is refused", Boolean.TRUE,
                    Boolean.valueOf(err instanceof MailAuthenticationException));
            check("no AUTH was sent", Boolean.FALSE, Boolean.valueOf(server.sawAuth.get()));
        } finally {
            m.close();
        }
    }

    private static boolean await(Server server, int count) throws InterruptedException {
        long end = System.currentTimeMillis() + 10000;
        while (server.count() < count && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return server.count() >= count;
    }

    private static boolean idle(Mailer m) throws InterruptedException {
        long end = System.currentTimeMillis() + 10000;
        while (m.pending() > 0 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return m.pending() == 0;
    }

    private static long rows(DataSource ds, String table) throws IOException {
        List rows = ds.query("SELECT COUNT(*) AS n FROM " + table, new Object[0]);
        return ((Number) ((Map) rows.get(0)).get("n")).longValue();
    }

    /// The queue: send returns at once, a deferred mail is tried again, a
    /// refused one is not, and one that keeps failing is given up on.
    private static void queue(Server server, String database) throws Exception {
        String w = database == null ? "memory queue " : database.indexOf("://") > 0
                ? database.substring(0, database.indexOf(':')) + " queue " : "sqlite queue ";
        server.reset();
        DataSource[] ds = new DataSource[1];
        Mailer m = mailer(server, database, ds);
        try {
            if (ds[0] != null) {
                clear(ds[0]);
            }
            for (int i = 0; i < 5; i++) {
                m.send(Mail.to("a@example.com").subject("q" + i).text("t").build());
            }
            check(w + "delivers", Boolean.TRUE, Boolean.valueOf(await(server, 5)));
            check(w + "in order", "q0 q1 q2 q3 q4", server.subjects());
            check(w + "drains", Boolean.TRUE, Boolean.valueOf(idle(m)));

            server.reset();
            server.failData.set(2);
            m.send(Mail.to("a@example.com").subject("retried").text("t").build());
            check(w + "retries a deferred mail", Boolean.TRUE, Boolean.valueOf(await(server, 1)));
            check(w + "third attempt", Integer.valueOf(3), Integer.valueOf(server.bodies.get()));

            server.reset();
            server.rejectData.set(1);
            m.send(Mail.to("a@example.com").subject("refused").text("t").build());
            check(w + "refused drains", Boolean.TRUE, Boolean.valueOf(idle(m)));
            Thread.sleep(150);
            check(w + "refused is not retried", Integer.valueOf(1),
                    Integer.valueOf(server.bodies.get()));
            check(w + "refused is not delivered", Integer.valueOf(0),
                    Integer.valueOf(server.count()));

            server.reset();
            server.failData.set(100);
            m.send(Mail.to("a@example.com").subject("hopeless").text("t").build());
            check(w + "hopeless drains", Boolean.TRUE, Boolean.valueOf(idle(m)));
            check(w + "gives up after max attempts", Integer.valueOf(3),
                    Integer.valueOf(server.bodies.get()));
            if (ds[0] != null) {
                check(w + "outbox empty", Long.valueOf(0), Long.valueOf(rows(ds[0],
                        "cn1_mail_outbox")));
                check(w + "dead rows", Long.valueOf(2), Long.valueOf(rows(ds[0],
                        "cn1_mail_dead")));
            }
        } finally {
            m.close();
            if (ds[0] != null) {
                ds[0].close();
            }
        }
    }

    private static void clear(DataSource ds) {
        try {
            ds.execute("DELETE FROM cn1_mail_outbox", new Object[0]);
            ds.execute("DELETE FROM cn1_mail_dead", new Object[0]);
        } catch (IOException noTableYet) {
            // created by the first send
        }
    }

    /// The outbox: a mail belongs to the transaction that sent it, and one a
    /// stopped server left behind is sent by the next.
    private static void outbox(Server server, String database) throws Exception {
        String w = database.indexOf("://") > 0
                ? database.substring(0, database.indexOf(':')) + " outbox " : "sqlite outbox ";
        server.reset();
        DataSource[] ds = new DataSource[1];
        Mailer m = mailer(server, database, ds);
        final Mailer mm = m;
        try {
            clear(ds[0]);
            try {
                Transactions.execute(new Transactions.Work() {
                    public Object run() throws Exception {
                        mm.send(Mail.to("a@example.com").subject("rolled back").text("t").build());
                        throw new IllegalStateException("no");
                    }
                });
            } catch (IllegalStateException expected) {
                // the point
            }
            final AtomicInteger during = new AtomicInteger(-1);
            final Server s = server;
            Transactions.execute(new Transactions.Work() {
                public Object run() throws Exception {
                    mm.send(Mail.to("a@example.com").subject("committed").text("t").build());
                    Thread.sleep(300);
                    during.set(s.count());
                    return null;
                }
            });
            check(w + "commit sends", Boolean.TRUE, Boolean.valueOf(await(server, 1)));
            check(w + "nothing before the commit", Integer.valueOf(0),
                    Integer.valueOf(during.get()));
            Thread.sleep(400);
            check(w + "rollback sends nothing", "committed", server.subjects());
            check(w + "drains", Boolean.TRUE, Boolean.valueOf(idle(m)));
            check(w + "table empty", Long.valueOf(0), Long.valueOf(rows(ds[0],
                    "cn1_mail_outbox")));
            // A row another server's sweep took while this one was getting to
            // it is that server's to send: here its time is moved, as a claim
            // moves it, before the commit hands the mail to this process.
            final DataSource d = ds[0];
            Transactions.execute(new Transactions.Work() {
                public Object run() throws Exception {
                    mm.send(Mail.to("a@example.com").subject("claimed").text("t").build());
                    d.execute("UPDATE cn1_mail_outbox SET next_attempt = next_attempt + 3600000",
                            new Object[0]);
                    return null;
                }
            });
            check(w + "claimed row let go", Boolean.TRUE, Boolean.valueOf(idle(m)));
            Thread.sleep(300);
            check(w + "a row another server claimed is not sent here", "committed",
                    server.subjects());
            check(w + "and stays for that server", Long.valueOf(1), Long.valueOf(rows(ds[0],
                    "cn1_mail_outbox")));
            clear(ds[0]);
        } finally {
            m.close();
        }
        // A server that stops with mail it could not send leaves it for the next.
        server.reset();
        server.failData.set(100);
        m = mailer(server, database, ds, "cn1.mail.retry.backoff", "600",
                "cn1.mail.retry.max-attempts", "5");
        try {
            m.send(Mail.to("a@example.com").subject("left behind").text("t").build());
            long end = System.currentTimeMillis() + 5000;
            while (server.bodies.get() < 1 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
        } finally {
            m.close();
            ds[0].close();
        }
        // A server on the same database with no mail server of its own -- a
        // developer's, a test run -- leaves the row alone.
        DataSource shared = DataSource.open(database, 2);
        Properties none = new Properties();
        none.setProperty("cn1.mail.outbox.sweep", "100");
        Mailer silent = Mailers.open(Config.of(none, "test"), shared);
        try {
            Thread.sleep(500);
            check(w + "a server that sends nothing takes no row", Long.valueOf(1),
                    Long.valueOf(rows(shared, "cn1_mail_outbox")));
        } finally {
            silent.close();
            shared.close();
        }
        server.reset();
        m = mailer(server, database, ds);
        try {
            check(w + "left row", Long.valueOf(1), Long.valueOf(rows(ds[0], "cn1_mail_outbox")));
            check(w + "next start sends it", Boolean.TRUE, Boolean.valueOf(await(server, 1)));
            check(w + "the same mail", "left behind", server.subjects());
            long end = System.currentTimeMillis() + 5000;
            while (rows(ds[0], "cn1_mail_outbox") > 0 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            check(w + "row removed", Long.valueOf(0), Long.valueOf(rows(ds[0],
                    "cn1_mail_outbox")));
        } finally {
            m.close();
            ds[0].close();
        }
    }

    /// One mail of every shape through a real server.
    private static void real(String url) throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.mail.url", url);
        p.setProperty("cn1.mail.from", env("CN1_MAILCHECK_FROM", "Mail Check <check@example.com>"));
        String tls = System.getenv("CN1_MAILCHECK_STARTTLS");
        if (tls != null) {
            p.setProperty("cn1.mail.smtp.starttls", tls);
        }
        String ca = System.getenv("CN1_MAILCHECK_CA");
        if (ca != null) {
            p.setProperty("cn1.mail.smtp.ca", ca);
        }
        String to = env("CN1_MAILCHECK_TO", "someone@example.com");
        Mailer m = Mailers.open(Config.of(p, "test"), null);
        try {
            byte[] image = new byte[3000];
            for (int i = 0; i < image.length; i++) {
                image[i] = (byte) (i * 31 + 7);
            }
            m.sendNow(Mail.to(to).subject("Plain").text("line one\n.dot\nline three").build());
            m.sendNow(Mail.to("Ren" + ACUTE + "e <" + to + ">")
                    .subject("Caf" + ACUTE + " " + ACUTE + "t" + ACUTE + " and a rather long "
                            + "subject that has to be folded over more than one line of header")
                    .text("d" + ACUTE + "j" + (char) 0xe0 + " vu")
                    .html("<p>d" + ACUTE + "j" + (char) 0xe0 + " <b>vu</b> <img src=\"cid:logo\"></p>")
                    .inline("logo", "image/png", image)
                    .attach("r" + ACUTE + "sum" + ACUTE + ".bin", "application/octet-stream", image)
                    .build());
            check("real server took both", Boolean.TRUE, Boolean.TRUE);
        } catch (IOException err) {
            check("real server", "sent", String.valueOf(err));
        } finally {
            m.close();
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.length() == 0 ? fallback : v;
    }

    private static void check(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    // ------------------------------------------------------- reading a mail

    private static int hexDigit(char c) {
        return c >= '0' && c <= '9' ? c - '0' : c >= 'A' && c <= 'F' ? c - 'A' + 10
                : c >= 'a' && c <= 'f' ? c - 'a' + 10 : -1;
    }

    /// Decodes a quoted-printable body.
    static byte[] quotedPrintable(String s) {
        byte[] out = new byte[s.length()];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=' && i + 2 < s.length() && s.charAt(i + 1) == '\r'
                    && s.charAt(i + 2) == '\n') {
                i += 2;
            } else if (c == '=' && i + 2 < s.length()) {
                out[n++] = (byte) (hexDigit(s.charAt(i + 1)) * 16 + hexDigit(s.charAt(i + 2)));
                i += 2;
            } else {
                out[n++] = (byte) c;
            }
        }
        byte[] exact = new byte[n];
        System.arraycopy(out, 0, exact, 0, n);
        return exact;
    }

    /// Decodes the `=?UTF-8?B?...?=` words of a header value; white space
    /// between two of them is not part of the text.
    static String decodeWords(String value) throws IOException {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        byte[] carried = new byte[0];
        int i = 0;
        boolean lastWasWord = false;
        while (i < value.length()) {
            int start = value.indexOf("=?UTF-8?B?", i);
            if (start < 0) {
                break;
            }
            int end = value.indexOf("?=", start + 10);
            String between = value.substring(i, start);
            if (!(lastWasWord && between.trim().length() == 0)) {
                out.append(WireBuffer.fromUtf8(carried));
                carried = new byte[0];
                out.append(between);
            }
            byte[] piece = Base64.decode(value.substring(start + 10, end));
            byte[] joined = new byte[carried.length + piece.length];
            System.arraycopy(carried, 0, joined, 0, carried.length);
            System.arraycopy(piece, 0, joined, carried.length, piece.length);
            carried = joined;
            lastWasWord = true;
            i = end + 2;
        }
        out.append(WireBuffer.fromUtf8(carried));
        out.append(value.substring(i));
        return out.toString();
    }

    /// One part of a MIME message.
    static final class Part {
        String head = "";
        String type = "";
        String content = "";
        final List<Part> parts = new ArrayList<Part>();

        static Part parse(String data) {
            Part p = new Part();
            int split = data.indexOf("\r\n\r\n");
            p.head = split < 0 ? data : data.substring(0, split + 2);
            p.content = split < 0 ? "" : data.substring(split + 4);
            String t = p.header("Content-Type");
            p.type = t == null ? "text/plain" : t;
            int b = p.type.indexOf("boundary=\"");
            if (b >= 0) {
                String boundary = "--" + p.type.substring(b + 10, p.type.indexOf('"', b + 10));
                int at = p.content.indexOf(boundary);
                while (at >= 0) {
                    int from = at + boundary.length();
                    if (p.content.startsWith("--", from)) {
                        break;
                    }
                    from += 2;
                    int next = p.content.indexOf("\r\n" + boundary, from);
                    if (next < 0) {
                        break;
                    }
                    p.parts.add(parse(p.content.substring(from, next)));
                    at = next + 2;
                }
            }
            return p;
        }

        String header(String name) {
            return headerOf(head, name, true);
        }

        byte[] bytes() throws IOException {
            String e = header("Content-Transfer-Encoding");
            if ("base64".equalsIgnoreCase(e)) {
                StringBuilder sb = new StringBuilder(content.length());
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c != '\r' && c != '\n') {
                        sb.append(c);
                    }
                }
                return Base64.decode(sb.toString());
            }
            if ("quoted-printable".equalsIgnoreCase(e)) {
                return quotedPrintable(content);
            }
            return WireBuffer.utf8(content);
        }

        String text() throws IOException {
            return WireBuffer.fromUtf8(bytes());
        }
    }

    /// The value of a header, unfolded when asked.
    static String headerOf(String head, String name, boolean unfold) {
        String[] lines = split(head);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > name.length() && line.charAt(name.length()) == ':'
                    && line.regionMatches(true, 0, name, 0, name.length())) {
                StringBuilder sb = new StringBuilder(line.substring(name.length() + 1).trim());
                while (i + 1 < lines.length && lines[i + 1].length() > 0
                        && (lines[i + 1].charAt(0) == ' ' || lines[i + 1].charAt(0) == '\t')) {
                    i++;
                    sb.append(unfold ? "" : "\r\n").append(unfold ? " " + lines[i].trim()
                            : lines[i]);
                }
                return sb.toString();
            }
        }
        return null;
    }

    static String[] split(String text) {
        List<String> lines = new ArrayList<String>();
        int from = 0;
        while (true) {
            int nl = text.indexOf("\r\n", from);
            if (nl < 0) {
                if (from < text.length()) {
                    lines.add(text.substring(from));
                }
                break;
            }
            lines.add(text.substring(from, nl));
            from = nl + 2;
        }
        return lines.toArray(new String[lines.size()]);
    }

    /// A mail the server took.
    static final class Received {
        String from;
        final List<String> recipients = new ArrayList<String>();
        /// The message as sent, dots taken off, each byte one char.
        String data;

        String header(String name) {
            int split = data.indexOf("\r\n\r\n");
            return headerOf(split < 0 ? data : data.substring(0, split + 2), name, true);
        }

        String rawHeader(String name) {
            int split = data.indexOf("\r\n\r\n");
            return headerOf(split < 0 ? data : data.substring(0, split + 2), name, false);
        }

        String body() {
            int split = data.indexOf("\r\n\r\n");
            return split < 0 ? "" : data.substring(split + 4);
        }

        /// What a strict server would object to: empty when nothing.
        String wireProblems() {
            StringBuilder sb = new StringBuilder();
            String[] lines = split(data);
            boolean head = true;
            for (String line : lines) {
                if (line.length() == 0) {
                    head = false;
                }
                if (line.length() > (head ? 78 : 76) && !"7bit".equals(
                        header("Content-Transfer-Encoding"))) {
                    sb.append("long line (").append(line.length()).append("): ").append(line)
                            .append('\n');
                }
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c > 126 || c < 32 && c != '\t') {
                        sb.append("byte ").append((int) c).append(" in: ").append(line)
                                .append('\n');
                        break;
                    }
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------ the mail server

    /// An SMTP server of a few dozen lines: it takes mail, records it, and
    /// misbehaves on request.
    static final class Server implements Runnable {
        final ServerSocket socket;
        final int port;
        final AtomicBoolean stopped = new AtomicBoolean();
        final List<Received> received = new ArrayList<Received>();
        final AtomicInteger connections = new AtomicInteger();
        final AtomicInteger bodies = new AtomicInteger();
        final AtomicInteger failData = new AtomicInteger();
        final AtomicInteger rejectData = new AtomicInteger();
        final AtomicBoolean pipelined = new AtomicBoolean();
        final AtomicBoolean sawSize = new AtomicBoolean();
        final AtomicBoolean sawUtf8 = new AtomicBoolean();
        final AtomicBoolean sawAuth = new AtomicBoolean();
        boolean pipelining = true;
        boolean startTls;
        boolean inject;
        boolean smtpUtf8;
        long maxSize;

        Server() throws IOException {
            socket = ServerSocket.bind("127.0.0.1", 0, 16);
            port = socket.getPort();
            Thread t = new Thread(this, "fake-smtp");
            t.setDaemon(true);
            t.start();
        }

        synchronized void reset() {
            received.clear();
            connections.set(0);
            bodies.set(0);
            failData.set(0);
            rejectData.set(0);
            pipelined.set(false);
            sawSize.set(false);
            sawUtf8.set(false);
            sawAuth.set(false);
            pipelining = true;
            startTls = false;
            inject = false;
            smtpUtf8 = false;
            maxSize = 0;
        }

        synchronized int count() {
            return received.size();
        }

        synchronized Received last() {
            return received.get(received.size() - 1);
        }

        synchronized String subjects() {
            StringBuilder sb = new StringBuilder();
            for (Received r : received) {
                sb.append(sb.length() == 0 ? "" : " ").append(r.header("Subject"));
            }
            return sb.toString();
        }

        void stop() {
            stopped.set(true);
            socket.close();
        }

        public void run() {
            while (!stopped.get()) {
                final int fd = socket.accept();
                if (fd < 0) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException err) {
                        return;
                    }
                    continue;
                }
                connections.incrementAndGet();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            ServerSocket.setBlocking(fd, true);
                            ServerSocket.setTimeout(fd, 20000);
                            serve(fd);
                        } catch (IOException | RuntimeException err) {
                            // The client went away; the checks say what was missing.
                        } finally {
                            ServerSocket.closeFd(fd);
                        }
                    }
                }, "fake-smtp-conn");
                t.setDaemon(true);
                t.start();
            }
        }

        private final class Reader {
            final int fd;
            final byte[] buffer = new byte[65536];
            int position;
            int limit;

            Reader(int fd) {
                this.fd = fd;
            }

            /// A line without its CRLF, each byte one char; null at the end.
            String line() throws IOException {
                StringBuilder sb = new StringBuilder();
                while (true) {
                    if (position >= limit) {
                        int n = ServerSocket.read(fd, buffer, 0, buffer.length);
                        if (n <= 0) {
                            return null;
                        }
                        position = 0;
                        limit = n;
                    }
                    char c = (char) (buffer[position++] & 0xff);
                    if (c == '\n') {
                        int len = sb.length();
                        if (len > 0 && sb.charAt(len - 1) == '\r') {
                            sb.setLength(len - 1);
                        }
                        return sb.toString();
                    }
                    sb.append(c);
                }
            }

            boolean more() {
                return position < limit;
            }
        }

        private void say(int fd, String text) throws IOException {
            byte[] b = WireBuffer.ascii(text + "\r\n");
            ServerSocket.write(fd, b, 0, b.length);
        }

        /// What the client sent as bytes-in-chars, as the text it was.
        private String utf8(String latin) throws IOException {
            byte[] b = new byte[latin.length()];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) latin.charAt(i);
            }
            return WireBuffer.fromUtf8(b);
        }

        private void serve(int fd) throws IOException {
            Reader in = new Reader(fd);
            say(fd, "220 fake.example ESMTP");
            Received current = null;
            while (true) {
                String line = in.line();
                if (line == null) {
                    return;
                }
                if (line.regionMatches(true, 0, "EHLO", 0, 4)) {
                    StringBuilder sb = new StringBuilder("250-fake.example");
                    if (pipelining) {
                        sb.append("\r\n250-PIPELINING");
                    }
                    if (startTls) {
                        sb.append("\r\n250-STARTTLS");
                    }
                    if (smtpUtf8) {
                        sb.append("\r\n250-SMTPUTF8");
                    }
                    sb.append("\r\n250-AUTH PLAIN LOGIN");
                    sb.append("\r\n250 SIZE ").append(maxSize);
                    say(fd, sb.toString());
                } else if (line.regionMatches(true, 0, "STARTTLS", 0, 8)) {
                    // No TLS here: what is checked is that the client refuses
                    // what arrives with the go-ahead.
                    say(fd, inject ? "220 go ahead\r\n250 injected" : "220 go ahead");
                    in.line();
                    return;
                } else if (line.regionMatches(true, 0, "AUTH", 0, 4)) {
                    sawAuth.set(true);
                    say(fd, "235 ok");
                } else if (line.regionMatches(true, 0, "MAIL FROM:", 0, 10)) {
                    if (in.more()) {
                        pipelined.set(true);
                    }
                    if (line.indexOf(" SIZE=") > 0) {
                        sawSize.set(true);
                    }
                    if (line.indexOf(" SMTPUTF8") > 0) {
                        sawUtf8.set(true);
                    }
                    current = new Received();
                    current.from = utf8(line.substring(line.indexOf('<') + 1, line.indexOf('>')));
                    say(fd, "250 ok");
                } else if (line.regionMatches(true, 0, "RCPT TO:", 0, 8)) {
                    String to = utf8(line.substring(line.indexOf('<') + 1, line.indexOf('>')));
                    if (current == null) {
                        say(fd, "503 no sender");
                    } else if (to.startsWith("reject")) {
                        say(fd, "550 no such user");
                    } else if (to.startsWith("defer")) {
                        say(fd, "451 try later");
                    } else {
                        current.recipients.add(to);
                        say(fd, "250 ok");
                    }
                } else if (line.regionMatches(true, 0, "DATA", 0, 4)) {
                    if (current == null || current.recipients.isEmpty()) {
                        say(fd, "554 no valid recipients");
                        continue;
                    }
                    say(fd, "354 go ahead");
                    StringBuilder data = new StringBuilder();
                    while (true) {
                        String l = in.line();
                        if (l == null) {
                            return;
                        }
                        if (".".equals(l)) {
                            break;
                        }
                        data.append(l.startsWith(".") ? l.substring(1) : l).append("\r\n");
                    }
                    bodies.incrementAndGet();
                    if (rejectData.get() > 0) {
                        rejectData.decrementAndGet();
                        say(fd, "554 content refused");
                    } else if (failData.get() > 0) {
                        failData.decrementAndGet();
                        say(fd, "451 try later");
                    } else {
                        current.data = data.toString();
                        synchronized (this) {
                            received.add(current);
                        }
                        say(fd, "250 queued");
                    }
                    current = null;
                } else if (line.regionMatches(true, 0, "RSET", 0, 4)) {
                    current = null;
                    say(fd, "250 ok");
                } else if (line.regionMatches(true, 0, "NOOP", 0, 4)) {
                    say(fd, "250 ok");
                } else if (line.regionMatches(true, 0, "QUIT", 0, 4)) {
                    say(fd, "221 bye");
                    return;
                } else {
                    say(fd, "500 what");
                }
            }
        }
    }
}
