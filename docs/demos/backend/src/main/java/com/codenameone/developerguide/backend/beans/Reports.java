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

import com.codename1.backend.AsyncResult;
import com.codename1.backend.annotations.Async;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Service;
import com.codename1.backend.annotations.ThreadKind;

import java.util.concurrent.Future;

// tag::backend-background[]
@Service
public class Reports {
    @Async
    public Future<String> monthly(String month) {
        return AsyncResult.of(render(month));
    }

    @Async(thread = ThreadKind.VIRTUAL)
    public void notifyWebhooks(String event) {
        // outbound calls that mostly wait
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Berlin")
    public void nightly() {
        // every day at 03:00 Berlin time
    }

    @Scheduled(fixedDelay = 60000, lock = "purge")
    public void purge() {
        // once a minute, on one instance at a time
    }
// end::backend-background[]

    private String render(String month) {
        return "report for " + month;
    }
}
