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
package com.codenameone.developerguide.backend.wayline;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;

import java.io.IOException;

/// Where the server picks what stands behind each interface.
@Configuration
public class Services {
    // tag::wayline-payment-choice[]
    @Bean
    public PaymentProvider paymentProvider(Config config) throws IOException {
        String address = config.get("wayline.public.url", "");
        String hosted = config.get("wayline.payments.hosted.secret", "");
        if (hosted.length() > 0) {
            return new HostedPayments(hosted, address + "/pay/return");
        }
        return new SimulatedPayments();
    }
    // end::wayline-payment-choice[]

    // tag::wayline-sms-choice[]
    @Bean
    public SmsSender smsSender(@Value("${wayline.sms.gateway.url:}") String url,
            @Value("${wayline.sms.gateway.key:}") String key,
            @Value("${wayline.sms.gateway.from:}") String from) throws IOException {
        if (url.length() > 0 && key.length() > 0 && from.length() > 0) {
            return new GatewaySmsSender(url, key, from);
        }
        return new LoggingSmsSender();
    }
    // end::wayline-sms-choice[]

    // tag::wayline-second-database[]
    /// A second database, opened beside the server's own. Wrapped in a class
    /// of its own so that a bean asking for a `DataSource` still gets the
    /// server's.
    public static final class Archive {
        public final DataSource db;

        Archive(DataSource db) {
            this.db = db;
        }
    }

    @Bean
    public Archive archive(Config config) throws IOException {
        return new Archive(DataSource.open(config.get("wayline.archive.url", ""), 4));
    }
    // end::wayline-second-database[]
}
