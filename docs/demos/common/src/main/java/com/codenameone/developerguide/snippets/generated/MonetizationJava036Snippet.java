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
import java.util.Calendar;
import java.util.List;


class MonetizationJava036Snippet {


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
    // tag::monetization-java-036[]
    @Override
    public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
     Storage s = Storage.getInstance();
     synchronized(RECEIPTS_KEY) {
     List<Receipt> receipts;
     if (s.exists(RECEIPTS_KEY)) {
     receipts = (List<Receipt>)s.readObject(RECEIPTS_KEY);
     } else {
     receipts = new ArrayList<Receipt>();
     }
     // Check to see if this receipt already exists
     // This probably won't ever happen (that you will be asked to submit an
     // existing receipt, but better safe than sorry
     for (Receipt r : receipts) {
     if (r.getStoreCode().equals(receipt.getStoreCode()) &&
     r.getTransactionId().equals(receipt.getTransactionId())) {
     // If you've already got this receipt, you will this submission.
     return;
     }
     }

     // Now try to find the current expiry date
     Date currExpiry = new Date();
     List<String> lProducts = Arrays.asList(PRODUCTS);
     for (Receipt r : receipts) {
     if (!lProducts.contains(receipt.getSku())) {
     continue;
     }
     if (r.getCancellationDate()!= null) {
     continue;
     }
     if (r.getExpiryDate() == null) {
     continue;
     }
     if (r.getExpiryDate().getTime() > currExpiry.getTime()) {
     currExpiry = r.getExpiryDate();
     }
     }

     // Now set the appropriate expiry date by adding time onto
     // the end of the current expiry date
     Calendar cal = Calendar.getInstance();
     cal.setTime(currExpiry);
     switch (receipt.getSku()) {
     case SKU_WORLD_1_MONTH:
     cal.add(Calendar.MONTH, 1);
     break;
     case SKU_WORLD_1_YEAR:
     cal.add(Calendar.YEAR, 1);
     }
     Date newExpiry = cal.getTime();

     receipt.setExpiryDate(newExpiry);
     receipts.add(receipt);
     s.writeObject(RECEIPTS_KEY, receipts);

     }
     // Make sure this is outside the synchronized block
     callback.onSucess(Boolean.TRUE);
    }
    // end::monetization-java-036[]
    }

    static final String RECEIPTS_KEY = "RECEIPTS.dat";
    String[] PRODUCTS = {"com.example.world"};
    static final String SKU_WORLD_1_MONTH = "com.example.world.month";
    static final String SKU_WORLD_1_YEAR = "com.example.world.year";

}
