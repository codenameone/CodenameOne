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
package com.codenameone.examples.wayline;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.wayline.geo.Geocoder;
import com.codenameone.examples.wayline.geo.PhotonGeocoder;
import com.codenameone.examples.wayline.geo.StubGeocoder;
import com.codenameone.examples.wayline.pay.PaymentProvider;
import com.codenameone.examples.wayline.pay.SimulatedPayments;
import com.codenameone.examples.wayline.pay.StripePayments;
import com.codenameone.examples.wayline.sms.LoggingSmsSender;
import com.codenameone.examples.wayline.sms.SmsSender;
import com.codenameone.examples.wayline.sms.TwilioSmsSender;

import java.io.IOException;

/// The services this server reaches outside itself for, each behind an
/// interface and each chosen here from the settings.
///
/// A server started with none of them configured still works from end to end:
/// text messages are logged, cards are simulated. That is what a checkout
/// runs as, and what the tests run against.
@Configuration
public class Services {
    /// Twilio when its three settings are present -- `wayline.sms.twilio.sid`,
    /// `.token` and `.from`, which are also read from the environment, in upper
    /// case with `_` for each `.` -- and the log otherwise.
    @Bean
    public SmsSender smsSender(@Value("${wayline.sms.twilio.sid:}") String sid,
            @Value("${wayline.sms.twilio.token:}") String token,
            @Value("${wayline.sms.twilio.from:}") String from) throws IOException {
        if (sid.length() > 0 && token.length() > 0 && from.length() > 0) {
            return new TwilioSmsSender(sid, token, from);
        }
        return new LoggingSmsSender();
    }

    /// Photon, at `wayline.geocoder.url`. `wayline.geocoder=stub` selects the
    /// one that knows ten places and asks nobody, and it is what the `test`
    /// profile gets unless told otherwise.
    @Bean
    public Geocoder geocoder(Config config, @Value("${wayline.geocoder:}") String chosen,
            @Value("${wayline.geocoder.url:https://photon.komoot.io}") String address)
            throws IOException {
        String kind = chosen.length() > 0 ? chosen
                : "test".equals(config.getProfile()) ? "stub" : "photon";
        if ("stub".equals(kind)) {
            return new StubGeocoder();
        }
        return new PhotonGeocoder(address);
    }

    /// Stripe when `wayline.payments.stripe.secret` is set (or
    /// `WAYLINE_PAYMENTS_STRIPE_SECRET` in the environment), and otherwise
    /// simulated: test card numbers, and no money moves.
    ///
    /// With Stripe a card is typed into Stripe's own page, which then sends the
    /// browser back to `/pay/return` on this server. That address has to be the
    /// one a phone can reach, so it is `wayline.public.url` when that is set
    /// and the issuer -- the public address of this server, which a deployment
    /// sets anyway -- when it is not.
    @Bean
    public PaymentProvider paymentProvider(
            @Value("${wayline.payments.stripe.secret:}") String secret,
            @Value("${wayline.public.url:}") String publicUrl,
            @Value("${" + SecurityConfig.ISSUER_KEY + ":}") String issuer) throws IOException {
        if (secret.length() == 0) {
            return new SimulatedPayments();
        }
        String address = publicUrl.length() > 0 ? publicUrl : issuer;
        if (address.length() == 0) {
            throw new IOException("wayline.payments.stripe.secret is set, so wayline.public.url "
                    + "must say where this server is reached");
        }
        while (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return new StripePayments(secret, address + "/pay/return");
    }
}
