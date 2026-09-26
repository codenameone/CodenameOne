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
package com.codenameone.developerguide.backend.beans;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.ConfigurationProperties;

// tag::backend-bean-properties[]
@Component
@ConfigurationProperties("mail")
public class MailSettings {
    private String host = "localhost";   // kept when mail.host is not set
    private int port = 25;
    private boolean startTls;

    public void setHost(String host) {          // mail.host
        this.host = host;
    }

    public void setPort(int port) {             // mail.port
        this.port = port;
    }

    public void setStartTls(boolean startTls) { // mail.startTls or mail.start-tls
        this.startTls = startTls;
    }
// end::backend-bean-properties[]

    public String describe() {
        return host + ":" + port + (startTls ? " (STARTTLS)" : "");
    }
}
