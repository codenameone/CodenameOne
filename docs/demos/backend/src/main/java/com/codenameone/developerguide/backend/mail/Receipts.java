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
package com.codenameone.developerguide.backend.mail;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailException;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.mvc.Model;
import com.codenameone.developerguide.backend.orders.Order;

import java.io.IOException;

@Component
public class Receipts {
    private final MailSender mail;

    public Receipts(MailSender mail) {
        this.mail = mail;
    }

    // tag::backend-mail-template[]
    public void shipped(Order order, String email) throws IOException {
        Model model = new Model()
                .addAttribute("customer", order.customer)
                .addAttribute("orderId", Long.valueOf(order.id));
        mail.send(Mail.to(email)
                .subject("Your order shipped")
                .template("mail/shipped", model)
                .build());
    }
    // end::backend-mail-template[]

    // tag::backend-mail-attach[]
    public void invoice(Order order, String email, byte[] pdf, byte[] logo) throws IOException {
        mail.send(Mail.to(email)
                .cc("accounts@example.com")
                .replyTo("billing@example.com")
                .subject("Invoice " + order.id)
                .html("<img src=\"cid:logo\" alt=\"Acme\"><p>Your invoice is attached.</p>")
                .inline("logo", "image/png", logo)
                .attach("invoice-" + order.id + ".pdf", "application/pdf", pdf)
                .header("X-Order", String.valueOf(order.id))
                .build());
    }
    // end::backend-mail-attach[]

    // tag::backend-mail-now[]
    public boolean sendCode(String email, String code) throws IOException {
        try {
            mail.sendNow(Mail.to(email)
                    .subject("Your code")
                    .text("Your code is " + code)
                    .build());
            return true;
        } catch (MailException refused) {
            // isPermanent(): the server will never take this mail.
            return false;
        }
    }
    // end::backend-mail-now[]
}
