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
     boolean stored;
     synchronized(RECEIPTS_KEY) {
     // readObject() answers null when the entry cannot be read or
     // deserialized, and this runs inside synchronizeReceipts(), so throwing
     // would leave synchronization marked in progress for the rest of the
     // session. An unreadable entry is not an empty one, though: carrying on
     // as if it were would write this single receipt over every receipt the
     // user has already paid for. Report failure and write nothing, and
     // Purchase keeps the receipt pending and retries later.
     boolean entryExists = s.exists(RECEIPTS_KEY);
     Object raw = entryExists ? s.readObject(RECEIPTS_KEY) : null;
     if (entryExists && !(raw instanceof List)) {
     callback.onSucess(Boolean.FALSE);
     return;
     }
     // The container's type says nothing about its elements, and iterating a
     // list holding something else throws before the callback runs, which
     // leaves synchronization stuck for the session. Copy with a check, the
     // same way fetchReceipts() does.
     List<Receipt> receipts = new ArrayList<Receipt>();
     if (raw instanceof List) {
     for (Object o : (List<?>) raw) {
     if (!(o instanceof Receipt)) {
     callback.onSucess(Boolean.FALSE);
     return;
     }
     receipts.add((Receipt) o);
     }
     }
     // Check to see if this receipt already exists. That should not happen,
     // but a store can resend one.
     for (Receipt r : receipts) {
     if (sameReceipt(r, receipt)) {
     // Already stored. Report success, or synchronizeReceipts() never finishes.
     callback.onSucess(Boolean.TRUE);
     return;
     }
     }

     // Now try to find the current expiry date
     Date currExpiry = new Date();
     List<String> lProducts = Arrays.asList(PRODUCTS);
     for (Receipt r : receipts) {
     if (!lProducts.contains(r.getSku())) {
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
     Date newExpiry = null;
     if (SKU_WORLD_1_MONTH.equals(receipt.getSku())) {
     cal.add(Calendar.MONTH, 1);
     newExpiry = cal.getTime();
     } else if (SKU_WORLD_1_YEAR.equals(receipt.getSku())) {
     cal.add(Calendar.YEAR, 1);
     newExpiry = cal.getTime();
     }

     // Purchase submits every receipt to the store, including products outside
     // this subscription group. Only the subscription SKUs get an expiry date:
     // stamping a consumable or a one-off purchase with the subscription's
     // expiry would make isSubscribed() report it as an active subscription.
     if (newExpiry != null) {
     receipt.setExpiryDate(newExpiry);
     }
     receipts.add(receipt);
     stored = s.writeObject(RECEIPTS_KEY, receipts);

     }
     // Make sure this is outside the synchronized block. Report what the
     // write actually did: on a failure Purchase would otherwise record the
     // transaction as processed and drop it from the pending queue, losing a
     // receipt the user paid for.
     callback.onSucess(stored);
    }

    // Two receipts are the same purchase when they come from the same store
    // and carry the same transaction id. The store code is part of that
    // identity because a transaction id is only unique within its own store,
    // as Receipt#getTransactionId() says. A null transaction id is not an
    // identity either: two distinct receipts can both carry one, so fall back
    // to the fields that together identify a purchase.
    //
    // Purchase.receiptsMatch() compares one device's pending queue, where
    // every receipt comes from the same store, so it can lean on the
    // transaction id alone. A ReceiptStore holds receipts from every store
    // and cannot.
    private static boolean sameReceipt(Receipt a, Receipt b) {
     if (!Objects.equals(a.getStoreCode(), b.getStoreCode())) {
     return false;
     }
     String aTx = a.getTransactionId();
     String bTx = b.getTransactionId();
     if (aTx != null && bTx != null) {
     return aTx.equals(bTx);
     }
     if (aTx != null || bTx != null) {
     return false;
     }
     return Objects.equals(a.getSku(), b.getSku())
     && Objects.equals(a.getPurchaseDate(), b.getPurchaseDate())
     && Objects.equals(a.getOrderData(), b.getOrderData());
    }
    // end::monetization-java-036[]
    }

    static final String RECEIPTS_KEY = "RECEIPTS.dat";
    String[] PRODUCTS = {SKU_WORLD_1_MONTH, SKU_WORLD_1_YEAR};
    static final String SKU_WORLD_1_MONTH = "com.example.world.month";
    static final String SKU_WORLD_1_YEAR = "com.example.world.year";

}
