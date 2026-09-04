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
import com.codename1.analytics.*;
import com.codename1.appreview.*;
import com.codename1.ads.*;
import com.codename1.util.*;
import java.util.*;
import java.util.List;


class MonetizationJava035Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    abstract class Sample implements ReceiptStore {
    // tag::monetization-java-035[]
    // static declarations used by receipt store

    // Storage key where list of receipts are stored
    private static final String RECEIPTS_KEY = "RECEIPTS.dat";

    @Override
    public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
     Storage s = Storage.getInstance();
     Receipt[] found;
     synchronized(RECEIPTS_KEY) {
     // readObject() answers null when the entry cannot be read or
     // deserialized, and Purchase calls this from inside loadReceipts(), so
     // throwing would leave synchronization marked in progress for the rest of
     // the session. Answering with an empty array is worse still: loadReceipts
     // persists a non-null result, so it would overwrite the receipts already
     // known and revoke a live subscription. null fails the fetch and leaves
     // them untouched, and only a genuinely absent entry is empty.
     boolean entryExists = s.exists(RECEIPTS_KEY);
     Object raw = entryExists ? s.readObject(RECEIPTS_KEY) : null;
     if (raw instanceof List) {
     // Checking the container is not checking its contents: a list holding
     // anything else -- a key collision, an older schema, a partial write --
     // makes toArray throw ArrayStoreException, which never reaches the
     // callback and leaves synchronization stuck for the session. Copy
     // element by element and fail the fetch if one does not belong.
     List<?> stored = (List<?>) raw;
     Receipt[] copy = new Receipt[stored.size()];
     int i = 0;
     for (Object o : stored) {
     if (!(o instanceof Receipt)) {
     copy = null;
     break;
     }
     copy[i++] = (Receipt) o;
     }
     found = copy;
     } else if (entryExists) {
     found = null;
     } else {
     found = new Receipt[0];
     }
     }
     // Make sure this is outside the synchronized block
     callback.onSucess(found);
    }
    // end::monetization-java-035[]
    }
}
