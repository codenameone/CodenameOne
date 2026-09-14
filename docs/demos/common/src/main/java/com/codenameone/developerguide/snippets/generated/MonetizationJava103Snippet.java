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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;


class MonetizationJava103Snippet {

    // tag::monetization-java-103[]
    static final String SKU_WORLD_1_MONTH = "com.codename1.world.month";
    static final String SKU_WORLD_1_YEAR = "com.codename1.world.year";

    // Both periods of the same subscription group. Every Purchase method that
    // asks about status or expiry takes the whole group, so keeping them in
    // one array is what makes the later listings read the way they do.
    static final String[] PRODUCTS = { SKU_WORLD_1_MONTH, SKU_WORLD_1_YEAR };

    // There is deliberately no Purchase field here. Receipts are cached on the
    // instance and loaded from storage the first time one is asked for, while
    // the synchronization that refreshes them is static and may be running on
    // an instance the port created. A held instance therefore keeps answering
    // from the snapshot it loaded; a fresh getInAppPurchase() reads what the
    // last completed synchronization persisted. It is a storage read, not a
    // network call.
    // end::monetization-java-103[]
}
