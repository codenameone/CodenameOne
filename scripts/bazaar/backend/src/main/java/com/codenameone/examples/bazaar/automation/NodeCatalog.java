/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.automation;

import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Every node type an automation may use, with its ports and parameters.
///
/// A node of the graph names its type in its `style` (the style key a
/// `GraphNodeTemplate` gives the nodes it makes), or in `data.type`. Its
/// parameters are entries of its `data` map, or the `value` of an input
/// port with the parameter's name ([AutomationGraph#param]).
public final class NodeCatalog {
    public static final String TRIGGER = "TRIGGER";
    public static final String CONDITION = "CONDITION";
    public static final String ACTION = "ACTION";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_FULFILLED = "order.fulfilled";
    public static final String INVENTORY_LOW = "inventory.low";
    public static final String CUSTOMER_CREATED = "customer.created";
    public static final String CART_ABANDONED = "cart.abandoned";

    public static final String IF_TOTAL_OVER = "if.total_over";
    public static final String IF_CUSTOMER_TAG = "if.customer_tag";
    public static final String IF_PRODUCT_TAG = "if.product_tag";
    public static final String IF_CHANNEL = "if.channel";
    public static final String IF_COUNTRY = "if.country";

    public static final String TAG_ORDER = "tag.order";
    public static final String TAG_CUSTOMER = "tag.customer";
    public static final String NOTIFY_STAFF = "notify.staff";
    public static final String EMAIL_CUSTOMER = "email.customer";
    public static final String DISCOUNT_CODE = "discount.create_code";
    public static final String INVENTORY_REORDER = "inventory.reorder";
    public static final String WEBHOOK_SEND = "webhook.send";
    public static final String WAIT_MINUTES = "wait.minutes";

    public static final String IN = "in";
    public static final String OUT = "out";
    public static final String YES = "yes";
    public static final String NO = "no";

    private static final List<NodeTypeDto> ALL;

    static {
        List<NodeTypeDto> all = new ArrayList<NodeTypeDto>();
        // tag::bazaar-node-catalog[]
        trigger(all, ORDER_CREATED, "Order placed", "An order is placed online or at a register.");
        trigger(all, ORDER_PAID, "Order paid", "An order's payment is taken.");
        trigger(all, ORDER_FULFILLED, "Order fulfilled", "The last item of an order ships.");
        trigger(all, INVENTORY_LOW, "Stock low",
                "A sale takes a variant to its reorder point or below.");
        trigger(all, CUSTOMER_CREATED, "New customer", "A shopper orders for the first time.");
        trigger(all, CART_ABANDONED, "Cart abandoned",
                "A cart with items and a shopper to reach sat untouched for an hour.");
        condition(all, IF_TOTAL_OVER, "Total over", "The order's total is over an amount.",
                param("amount", "Amount", "MONEY", "20000"));
        // end::bazaar-node-catalog[]
        condition(all, IF_CUSTOMER_TAG, "Customer tagged", "The customer carries a tag.",
                param("tag", "Tag", "TEXT", "VIP"));
        condition(all, IF_PRODUCT_TAG, "Product tagged",
                "A product of the order (or the low variant's) carries a tag.",
                param("tag", "Tag", "TEXT", ""));
        condition(all, IF_CHANNEL, "Channel", "The order came from a channel.",
                choice("channel", "Channel", "ONLINE", "ONLINE", "POS"));
        condition(all, IF_COUNTRY, "Country",
                "The order ships to one of these countries (two letter codes, comma separated).",
                param("country", "Countries", "TEXT", "US"));
        action(all, TAG_ORDER, "Tag order", "Adds a tag to the order.",
                param("tag", "Tag", "TEXT", "VIP"));
        action(all, TAG_CUSTOMER, "Tag customer", "Adds a tag to the customer.",
                param("tag", "Tag", "TEXT", "VIP"));
        action(all, NOTIFY_STAFF, "Notify staff", "Pushes a notification to the store's staff.",
                param("message", "Message", "TEXT", "Something needs a look"));
        action(all, EMAIL_CUSTOMER, "Email customer",
                "Records an email to the customer (the sample sends none).",
                param("subject", "Subject", "TEXT", "Thank you"),
                param("message", "Message", "TEXT", ""));
        action(all, DISCOUNT_CODE, "Create discount code",
                "Makes a single-use code for the customer.",
                choice("kind", "Kind", "PERCENT", "PERCENT", "FIXED"),
                param("amount", "Amount (basis points or cents)", "NUMBER", "1000"));
        action(all, INVENTORY_REORDER, "Draft purchase order",
                "Drafts a purchase order for the low variant from its supplier.",
                param("quantity", "Quantity (0: twice the reorder point)", "NUMBER", "0"));
        action(all, WEBHOOK_SEND, "Send webhook", "Delivers the event to one of the store's webhooks.",
                param("webhookId", "Webhook", "TEXT", ""));
        action(all, WAIT_MINUTES, "Wait", "Carries on after a number of minutes.",
                param("minutes", "Minutes", "NUMBER", "60"));
        ALL = Collections.unmodifiableList(all);
    }

    private NodeCatalog() {
    }

    /// Every node type, triggers first, then conditions, then actions.
    public static List<NodeTypeDto> all() {
        return ALL;
    }

    /// The type with this id, or null.
    public static NodeTypeDto find(String type) {
        for (int iter = 0; iter < ALL.size(); iter++) {
            if (ALL.get(iter).type().equals(type)) {
                return ALL.get(iter);
            }
        }
        return null;
    }

    /// Whether `type` is a trigger.
    public static boolean isTrigger(String type) {
        NodeTypeDto t = find(type);
        return t != null && TRIGGER.equals(t.kind());
    }

    /// Whether `type` is a condition, with `yes` and `no` outputs.
    public static boolean isCondition(String type) {
        NodeTypeDto t = find(type);
        return t != null && CONDITION.equals(t.kind());
    }

    private static NodeParamDto param(String name, String label, String kind, String value) {
        return new NodeParamDto(name, label, kind, value, new ArrayList<String>());
    }

    private static NodeParamDto choice(String name, String label, String value,
            String... choices) {
        return new NodeParamDto(name, label, "CHOICE", value, Arrays.asList(choices));
    }

    private static void trigger(List<NodeTypeDto> all, String type, String label, String text) {
        all.add(new NodeTypeDto(type, TRIGGER, label, text, new ArrayList<String>(),
                Arrays.asList(OUT), new ArrayList<NodeParamDto>()));
    }

    private static void condition(List<NodeTypeDto> all, String type, String label, String text,
            NodeParamDto... params) {
        all.add(new NodeTypeDto(type, CONDITION, label, text, Arrays.asList(IN),
                Arrays.asList(YES, NO), Arrays.asList(params)));
    }

    private static void action(List<NodeTypeDto> all, String type, String label, String text,
            NodeParamDto... params) {
        all.add(new NodeTypeDto(type, ACTION, label, text, Arrays.asList(IN), Arrays.asList(OUT),
                Arrays.asList(params)));
    }
}
