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
package com.codename1.backend.mail;

import com.codename1.backend.mvc.Model;
import com.codename1.impl.backend.MailRuntime;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One email: who it is for, what it says and what is attached.
///
/// ```java
/// mail.send(Mail.to("ada@example.com")
///         .subject("Your order shipped")
///         .template("mail/shipped", model)
///         .attach("invoice.pdf", "application/pdf", pdf)
///         .build());
/// ```
///
/// This one class is what Spring spreads over `SimpleMailMessage`,
/// `MimeMessage` and `MimeMessageHelper`. It is immutable; everything is
/// checked in [Builder#build], so a mail that exists can be sent: an address
/// that is not one, a header with a line break in it or an attachment with no
/// name fails there, with an IllegalArgumentException, never at the server.
///
/// A mail with an HTML body and no text is given a text alternative derived
/// from the HTML, which is what keeps it out of spam folders and readable in
/// a client that shows text. The body is always UTF-8.
public final class Mail {
    private static final Address[] NONE = new Address[0];
    private static final Part[] NO_PARTS = new Part[0];
    private static final String[] RESERVED = {
        "From", "To", "Cc", "Bcc", "Reply-To", "Subject", "Date", "Message-ID", "MIME-Version",
        "Content-Type", "Content-Transfer-Encoding", "Content-Disposition", "Content-ID",
        "Sender", "Return-Path"
    };

    final Address from;
    final Address replyTo;
    final Address[] to;
    final Address[] cc;
    final Address[] bcc;
    final String subject;
    final String text;
    final String html;
    final String view;
    final Model model;
    final Part[] attachments;
    final Part[] inlines;
    /// Name, value, name, value.
    final String[] headers;

    /// An attachment or an inline part.
    static final class Part {
        final String name;
        final String type;
        final byte[] data;
        final String contentId;

        Part(String name, String type, byte[] data, String contentId) {
            this.name = name;
            this.type = type;
            this.data = data;
            this.contentId = contentId;
        }
    }

    private Mail(Address from, Address replyTo, Address[] to, Address[] cc, Address[] bcc,
                 String subject, String text, String html, String view, Model model,
                 Part[] attachments, Part[] inlines, String[] headers) {
        this.from = from;
        this.replyTo = replyTo;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.text = text;
        this.html = html;
        this.view = view;
        this.model = model;
        this.attachments = attachments;
        this.inlines = inlines;
        this.headers = headers;
    }

    /// Starts a mail to `addresses`, each `user@example.com` or
    /// `Name <user@example.com>`.
    public static Builder to(String... addresses) {
        return new Builder().to(addresses);
    }

    /// Whether `address` is a bare `user@example.com` a mail can be sent to;
    /// answers without throwing.
    public static boolean isAddress(String address) {
        return Address.isValid(address);
    }

    /// Starts a mail with no recipient yet.
    public static Builder builder() {
        return new Builder();
    }

    /// Collects a mail's parts; [#build] checks them.
    public static final class Builder {
        private String from;
        private String replyTo;
        private final List<String> to = new ArrayList<String>();
        private final List<String> cc = new ArrayList<String>();
        private final List<String> bcc = new ArrayList<String>();
        private String subject;
        private String text;
        private String html;
        private String view;
        private Model model;
        private final List<Part> attachments = new ArrayList<Part>();
        private final List<Part> inlines = new ArrayList<Part>();
        private final List<String> headers = new ArrayList<String>();

        Builder() {
        }

        /// The sender. Without one the mail is sent from `cn1.mail.from`.
        public Builder from(String address) {
            this.from = address;
            return this;
        }

        /// Where a reply goes when that is not the sender.
        public Builder replyTo(String address) {
            this.replyTo = address;
            return this;
        }

        /// Adds recipients.
        public Builder to(String... addresses) {
            add(to, addresses);
            return this;
        }

        /// Adds recipients every other recipient can see were copied.
        public Builder cc(String... addresses) {
            add(cc, addresses);
            return this;
        }

        /// Adds recipients no other recipient is told about.
        public Builder bcc(String... addresses) {
            add(bcc, addresses);
            return this;
        }

        private static void add(List<String> list, String[] addresses) {
            if (addresses != null) {
                for (String a : addresses) {
                    list.add(a);
                }
            }
        }

        /// The subject line; any language, one line.
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /// The plain text body.
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /// The HTML body.
        public Builder html(String html) {
            this.html = html;
            this.view = null;
            this.model = null;
            return this;
        }

        /// The HTML body, rendered from the template
        /// `templates/<view>.html` with `model` when the mail is sent. The
        /// text body is derived from what was rendered, unless [#text] gave
        /// one.
        public Builder template(String view, Model model) {
            this.view = view;
            this.model = model == null ? new Model() : model;
            this.html = null;
            return this;
        }

        /// Attaches a file.
        ///
        /// - `name`: the file name the reader sees
        /// - `type`: its media type, such as `application/pdf`; null for
        ///   `application/octet-stream`
        /// - `data`: its content, which the mail keeps and does not copy
        public Builder attach(String name, String type, byte[] data) {
            attachments.add(new Part(name, type, data, null));
            return this;
        }

        /// Embeds an image the HTML body shows with `<img src="cid:contentId">`.
        public Builder inline(String contentId, String type, byte[] data) {
            inlines.add(new Part(null, type, data, contentId));
            return this;
        }

        /// Adds a header of the application's own, such as `List-Unsubscribe`
        /// or `X-Campaign`. The headers a mail is built from -- the addresses,
        /// the subject, the date, the content headers -- cannot be set here.
        public Builder header(String name, String value) {
            headers.add(name);
            headers.add(value);
            return this;
        }

        /// Checks everything and answers the mail, or throws
        /// IllegalArgumentException naming what is wrong.
        public Mail build() {
            Address[] toList = parse(to);
            Address[] ccList = parse(cc);
            Address[] bccList = parse(bcc);
            if (toList.length + ccList.length + bccList.length == 0) {
                throw new IllegalArgumentException("A mail needs at least one recipient");
            }
            if (subject != null) {
                checkLine("The subject", subject);
            }
            if (text == null && html == null && view == null) {
                throw new IllegalArgumentException("A mail needs a body: text(), html() or "
                        + "template()");
            }
            if (!inlines.isEmpty() && html == null && view == null) {
                throw new IllegalArgumentException("An inline part needs an HTML body to "
                        + "show it");
            }
            Part[] files = new Part[attachments.size()];
            for (int i = 0; i < files.length; i++) {
                Part p = attachments.get(i);
                if (p.name == null || p.name.trim().length() == 0) {
                    throw new IllegalArgumentException("An attachment needs a file name");
                }
                if (p.name.length() > 255) {
                    throw new IllegalArgumentException("An attachment's file name is longer "
                            + "than 255 characters");
                }
                int encoded = 0;
                for (int c = 0; c < p.name.length(); c++) {
                    char ch = p.name.charAt(c);
                    boolean plain = ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
                            || ch >= '0' && ch <= '9' || ch == '.' || ch == '-' || ch == '_';
                    encoded += plain ? 1 : ch < 0x80 ? 3 : ch < 0x800 ? 6 : 9;
                }
                if (encoded > 900) {
                    // A name with anything outside ASCII is written three
                    // bytes a byte, but for letters, digits and ._-, on one
                    // line, and a line may be 998.
                    throw new IllegalArgumentException("An attachment's file name is too "
                            + "long once encoded; shorten it to about a hundred characters");
                }
                checkLine("An attachment's file name", p.name);
                files[i] = new Part(p.name, checkType(p.type), checkData(p.data), null);
            }
            Part[] embedded = new Part[inlines.size()];
            for (int i = 0; i < embedded.length; i++) {
                Part p = inlines.get(i);
                if (p.contentId == null || p.contentId.length() == 0) {
                    throw new IllegalArgumentException("An inline part needs a content id");
                }
                for (int c = 0; c < p.contentId.length(); c++) {
                    char ch = p.contentId.charAt(c);
                    if (ch <= ' ' || ch > 126 || ch == '<' || ch == '>') {
                        throw new IllegalArgumentException("An inline part's content id may "
                                + "hold only visible ASCII characters other than < and >");
                    }
                }
                embedded[i] = new Part(null, checkType(p.type), checkData(p.data), p.contentId);
            }
            String[] extra = new String[headers.size()];
            for (int i = 0; i < extra.length; i += 2) {
                extra[i] = checkHeaderName(headers.get(i));
                String value = headers.get(i + 1);
                if (value == null) {
                    throw new IllegalArgumentException("The header " + extra[i] + " has no value");
                }
                checkLine("The header " + extra[i], value);
                if (MimeWriter.longWord(value)) {
                    // A line may be 998 bytes and folds only at a space.
                    throw new IllegalArgumentException("The header " + extra[i] + " has a run "
                            + "of more than 900 characters without a space, which no mail "
                            + "server takes");
                }
                extra[i + 1] = value;
            }
            String plain = text;
            if (plain == null && html != null) {
                plain = HtmlText.strip(html);
            }
            return new Mail(from == null ? null : Address.parse(from),
                    replyTo == null ? null : Address.parse(replyTo), toList, ccList, bccList,
                    subject, plain, html, view, model,
                    files.length == 0 ? NO_PARTS : files,
                    embedded.length == 0 ? NO_PARTS : embedded, extra);
        }

        private static Address[] parse(List<String> list) {
            if (list.isEmpty()) {
                return NONE;
            }
            Address[] out = new Address[list.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = Address.parse(list.get(i));
            }
            return out;
        }

        /// A header value is one line. A line break in one is how a mail
        /// header is injected -- "subject\r\nBcc: someone" -- so it is refused
        /// rather than folded or stripped.
        private static void checkLine(String what, String value) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\r' || c == '\n' || c == 0) {
                    throw new IllegalArgumentException(what + " has a line break or a NUL in "
                            + "it; a header is one line");
                }
            }
        }

        private static String checkHeaderName(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("A header needs a name");
            }
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c <= ' ' || c > 126 || c == ':') {
                    throw new IllegalArgumentException("A header name may hold only visible "
                            + "ASCII characters other than the colon");
                }
            }
            if (name.length() > 76) {
                // A value folds; a name is on one line with what follows it.
                throw new IllegalArgumentException("A header name may be 76 characters");
            }
            for (String r : RESERVED) {
                if (r.equalsIgnoreCase(name)) {
                    throw new IllegalArgumentException("The header " + name + " is written "
                            + "from the mail itself and cannot be set with header()");
                }
            }
            return name;
        }

        private static String checkType(String type) {
            if (type == null) {
                return "application/octet-stream";
            }
            int slash = type.indexOf('/');
            boolean ok = slash > 0 && slash < type.length() - 1;
            for (int i = 0; ok && i < type.length(); i++) {
                char c = type.charAt(i);
                ok = c > ' ' && c <= 126 && c != ';' && c != '"' && c != '\\' && c != ','
                        && c != '(' && c != ')' && c != '<' && c != '>' && c != '@'
                        && (c != '/' || i == slash);
            }
            if (!ok) {
                throw new IllegalArgumentException("\"" + type + "\" is not a media type; "
                        + "one looks like application/pdf");
            }
            return type;
        }

        private static byte[] checkData(byte[] data) {
            if (data == null) {
                throw new IllegalArgumentException("An attachment has no content");
            }
            return data;
        }
    }

    /// This mail with its template rendered and its sender filled in: what is
    /// encoded, sent and kept by a test's capture.
    Mail resolved(Address defaultFrom) throws IOException {
        Address sender = from != null ? from : defaultFrom;
        if (sender == null) {
            throw new MailException("The mail has no sender: call from(), or set "
                    + "cn1.mail.from in application.properties");
        }
        if (view == null && sender == from) { //NOPMD CompareObjectsWithEquals - the same object means nothing to fill in
            return this;
        }
        String body = html;
        String plain = text;
        if (view != null) {
            MailRuntime.TemplateRenderer renderer = MailRuntime.renderer();
            if (renderer == null) {
                throw new MailException("The mail names the template \"" + view + "\" and "
                        + "this application was built with no mail templates: put it in "
                        + "src/main/resources/templates/" + view + ".html");
            }
            try {
                body = WireBuffer.fromUtf8(renderer.render(view, model));
            } catch (IllegalArgumentException err) {
                throw new MailException("The mail names the template \"" + view + "\", which "
                        + "this application does not have: src/main/resources/templates/" + view
                        + ".html", err);
            }
            if (plain == null) {
                plain = HtmlText.strip(body);
            }
        }
        return new Mail(sender, replyTo, to, cc, bcc, subject, plain, body, null, null,
                attachments, inlines, headers);
    }

    /// The addresses the server is told to deliver to: everyone in to, cc and
    /// bcc, once each.
    List<String> recipients() {
        List<String> out = new ArrayList<String>(to.length + cc.length + bcc.length);
        collect(out, to);
        collect(out, cc);
        collect(out, bcc);
        return out;
    }

    private static void collect(List<String> out, Address[] list) {
        for (Address a : list) {
            if (!out.contains(a.spec)) {
                out.add(a.spec);
            }
        }
    }

    /// Whether every address is ASCII; a mail with one that is not can only
    /// go through a server that offers SMTPUTF8.
    boolean isAsciiEnvelope() {
        return (from == null || from.ascii) && ascii(to) && ascii(cc) && ascii(bcc);
    }

    private static boolean ascii(Address[] list) {
        for (Address a : list) {
            if (!a.ascii) {
                return false;
            }
        }
        return true;
    }

    private static List<String> specs(Address[] list) {
        List<String> out = new ArrayList<String>(list.length);
        for (Address a : list) {
            out.add(a.spec);
        }
        return Collections.unmodifiableList(out);
    }

    /// The sender as it was given, or null when the mail takes the default.
    public String getFrom() {
        return from == null ? null : from.toString();
    }

    /// The reply address, or null.
    public String getReplyTo() {
        return replyTo == null ? null : replyTo.toString();
    }

    /// The addresses of the recipients, without their display names.
    public List<String> getTo() {
        return specs(to);
    }

    /// The addresses copied.
    public List<String> getCc() {
        return specs(cc);
    }

    /// The addresses copied without the other recipients being told.
    public List<String> getBcc() {
        return specs(bcc);
    }

    /// The subject, or null.
    public String getSubject() {
        return subject;
    }

    /// The text body. For a mail built from a template this is null until the
    /// mail is sent; the mail a test captures has it.
    public String getText() {
        return text;
    }

    /// The HTML body, or null. For a mail built from a template this is null
    /// until the mail is sent; the mail a test captures has it.
    public String getHtml() {
        return html;
    }

    /// The file names of the attachments, in order.
    public List<String> getAttachmentNames() {
        List<String> out = new ArrayList<String>(attachments.length);
        for (Part p : attachments) {
            out.add(p.name);
        }
        return Collections.unmodifiableList(out);
    }

    /// The content of the attachment called `name`, or null.
    public byte[] getAttachment(String name) {
        for (Part p : attachments) {
            if (p.name.equals(name)) {
                return p.data;
            }
        }
        return null;
    }

    /// The value of a header added with [Builder#header], or null. The name
    /// is matched without regard to case.
    public String getHeader(String name) {
        for (int i = 0; i < headers.length; i += 2) {
            if (headers[i].equalsIgnoreCase(name)) {
                return headers[i + 1];
            }
        }
        return null;
    }
}
