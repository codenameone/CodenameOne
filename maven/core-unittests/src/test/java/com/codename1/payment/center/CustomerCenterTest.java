/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.payment.center;

import com.codename1.io.JSONParser;
import com.codename1.junit.FormTest;
import com.codename1.payment.CommerceManager;
import com.codename1.payment.CommerceTestBase;
import com.codename1.payment.CustomerInfo;
import com.codename1.payment.Purchase;
import com.codename1.ui.Form;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomerCenterTest extends CommerceTestBase {
    private static final String CONFIG =
            "{\"accepted\":true,\"paths\":[\"MANAGE\",\"restore\",\"REFUND\",\"TELEPORT\",\"CANCEL\",\"SUPPORT\"],"
                    + "\"supportEmail\":\"help@example.test\","
                    + "\"cancelReasons\":[{\"id\":\"price\",\"label\":\"Too expensive\","
                    + "\"offer\":{\"productId\":\"pro_half\",\"offerId\":\"half\",\"text\":\"Half price\"}},"
                    + "{\"id\":\"unused\",\"label\":\"I do not use it\",\"offer\":null},{\"label\":\"no id\"}],"
                    + "\"strings\":{\"en\":{\"path.RESTORE\":\"Bring my purchases back\"}}}";

    private static String paths(List<CustomerCenter.Row> rows) {
        StringBuilder sb = new StringBuilder();
        for (CustomerCenter.Row r : rows) {
            sb.append(r.path).append(':').append(r.sku).append(' ');
        }
        return sb.toString().trim();
    }

    private CustomerInfo subscriber() {
        mock("/customer", "{\"accepted\":true,\"appUserId\":\"u1\",\"activeSubscriptions\":[\"pro_monthly\"]}");
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        cm.refresh();
        return cm.getCustomerInfo();
    }

    private static Purchase store(final boolean refundable) {
        return new Purchase() {
            @Override
            public boolean isRefundable(String sku) {
                return refundable;
            }
        };
    }

    @FormTest
    void aConfigurationMapsToRowsInItsOrder() throws Exception {
        Map config = JSONParser.parseJSON(CONFIG);
        CustomerInfo info = subscriber();
        List<CustomerCenter.Row> rows = CustomerCenter.buildRows(config, info, store(true));
        assertEquals("MANAGE:pro_monthly RESTORE:null REFUND:pro_monthly CANCEL:pro_monthly SUPPORT:null",
                paths(rows));
        assertEquals("Manage subscription", rows.get(0).label);
        assertEquals("Bring my purchases back", rows.get(1).label);
        assertEquals("mailto:help@example.test", CustomerCenter.supportAddress(config));

        // A store that refunds nothing offers no refund.
        assertEquals("MANAGE:pro_monthly RESTORE:null CANCEL:pro_monthly SUPPORT:null",
                paths(CustomerCenter.buildRows(config, info, store(false))));

        List<Map> reasons = CustomerCenter.cancelReasons(config);
        assertEquals(2, reasons.size());
        assertEquals("price", reasons.get(0).get("id"));

        Form form = CustomerCenter.createForm(config, info, store(true), null);
        assertNotNull(byName(form, "CustomerCenterRow_CANCEL"));
        assertEquals(5, byUiid(form, "CustomerCenterRow").size());
        assertEquals(1, byUiid(form, "CustomerCenterSubscription").size());
    }

    @FormTest
    void withNoSubscriptionThereIsNothingToCancelOrRefund() throws Exception {
        Map config = JSONParser.parseJSON(CONFIG);
        config.put("supportUrl", "https://help.example.test");
        assertEquals("MANAGE:null RESTORE:null SUPPORT:null",
                paths(CustomerCenter.buildRows(config, CommerceManager.getInstance().getCustomerInfo(), store(true))));
        assertEquals("https://help.example.test", CustomerCenter.supportAddress(config));
        Form form = CustomerCenter.createForm(config, null, null, null);
        assertEquals(1, byUiid(form, "CustomerCenterEmpty").size());
    }

    @FormTest
    void theDefaultConfigurationManagesAndRestores() {
        Map config = CustomerCenter.defaultConfiguration();
        assertEquals("MANAGE:pro_monthly RESTORE:null",
                paths(CustomerCenter.buildRows(config, subscriber(), null)));
        assertNull(CustomerCenter.supportAddress(config));
        assertTrue(CustomerCenter.cancelReasons(config).isEmpty());
        assertTrue(CustomerCenter.buildRows(null, null, null).isEmpty());
    }

    @FormTest
    void theConfigurationComesFromTheCloudWhenItAccepts() {
        mock("/customer-center", CONFIG);
        assertNotNull(CommerceManager.getInstance().getCustomerCenterConfiguration());
        mock("/customer-center", "{\"paths\":[\"MANAGE\"]}");
        assertNull(CommerceManager.getInstance().getCustomerCenterConfiguration());
    }
}
