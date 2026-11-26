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
package com.codenameone.examples.bazaar.merchant;

/// What the server's upper case codes are called for people, untranslated:
/// the caller passes the word through `Lang.tr`. A code with no word is shown
/// as it came.
final class Words {
    static final String[] PRODUCT_STATUSES = {"DRAFT", "ACTIVE", "ARCHIVED"};
    static final String[] ORDER_STATUSES = {"OPEN", "CLOSED", "CANCELLED"};
    static final String[] FINANCIAL = {"PENDING", "AUTHORIZED", "PAID", "PARTIALLY_REFUNDED",
        "REFUNDED", "VOIDED"};
    static final String[] FULFILMENT = {"UNFULFILLED", "PARTIAL", "FULFILLED"};
    static final String[] CHANNELS = {"ONLINE", "POS", "DRAFT"};
    static final String[] ROLES = {"OWNER", "MANAGER", "CASHIER", "FULFILMENT"};
    static final String[] DISCOUNT_KINDS = {"PERCENT", "FIXED", "FREE_SHIPPING", "BUY_X_GET_Y"};
    static final String[] RATE_KINDS = {"FLAT", "WEIGHT", "PRICE", "FREE_OVER", "PICKUP", "LOCAL",
        "CARRIER"};
    /// The marketplaces a channel connects to: the server's connector kinds
    /// (`channel.SimulatedConnector.KIND`, `channel.EbayConnector.KIND`).
    static final String[] CHANNEL_KINDS = {"SIMULATED", "EBAY"};
    static final String[] ADJUST_REASONS = {"RESTOCK", "DAMAGE", "CORRECTION", "RETURN", "RECEIVE"};

    private Words() {
    }

    static String of(String code) {
        if (code == null || code.length() == 0) {
            return "";
        }
        String[][] words = {
            {"DRAFT", "Draft"}, {"ACTIVE", "Active"}, {"ARCHIVED", "Archived"},
            {"OPEN", "Open"}, {"CLOSED", "Closed"}, {"CANCELLED", "Cancelled"},
            {"PENDING", "Pending"}, {"AUTHORIZED", "Authorized"}, {"PAID", "Paid"},
            {"PARTIALLY_REFUNDED", "Partially refunded"}, {"REFUNDED", "Refunded"},
            {"VOIDED", "Voided"}, {"UNFULFILLED", "Unfulfilled"}, {"PARTIAL", "Partially fulfilled"},
            {"FULFILLED", "Fulfilled"}, {"ONLINE", "Online"}, {"POS", "Point of sale"},
            {"OWNER", "Owner"}, {"MANAGER", "Manager"}, {"CASHIER", "Cashier"},
            {"FULFILMENT", "Fulfilment"}, {"PERCENT", "Percentage"}, {"FIXED", "Fixed amount"},
            {"FREE_SHIPPING", "Free shipping"}, {"BUY_X_GET_Y", "Buy X get Y"},
            {"FLAT", "Flat rate"}, {"WEIGHT", "By weight"}, {"PRICE", "By order value"},
            {"FREE_OVER", "Free over amount"}, {"PICKUP", "Pickup"}, {"LOCAL", "Local delivery"},
            {"RESTOCK", "Restock"}, {"DAMAGE", "Damaged"}, {"CORRECTION", "Correction"},
            {"RETURN", "Return"}, {"RECEIVE", "Received"}, {"COUNT", "Stock count"},
            {"SALE", "Sale"}, {"RESERVE", "Reserved"}, {"RELEASE", "Released"},
            {"TRANSFER_OUT", "Transfer out"}, {"TRANSFER_IN", "Transfer in"},
            {"POS_SYNC", "Point of sale sync"}, {"SENT", "Sent"}, {"RECEIVED", "Received"},
            {"ORDERED", "Ordered"}, {"PARTIALLY_RECEIVED", "Partially received"},
            {"PACKED", "Packed"}, {"SHIPPED", "Shipped"}, {"IN_TRANSIT", "In transit"},
            {"OUT_FOR_DELIVERY", "Out for delivery"}, {"DELIVERED", "Delivered"},
            {"PICKED_UP", "Picked up"}, {"SHIP", "Ship"}, {"REQUESTED", "Requested"},
            {"APPROVED", "Approved"}, {"DECLINED", "Declined"}, {"TO_PACK", "To pack"},
            {"PUBLISHED", "Published"}, {"REJECTED", "Rejected"}, {"SUSPENDED", "Suspended"},
            {"FAILED", "Failed"}, {"SIMULATED", "Simulated marketplace"}, {"EBAY", "eBay"},
            {"CLOSEST", "Closest location"}, {"PRIORITY", "Location priority"},
            {"MOST_STOCK", "Most stock"}, {"ACCEPTED", "Accepted"},
            {"TOTAL_OVER", "Order total over"}, {"ORDERS_PER_HOUR", "Orders per hour over"},
            {"COUNTRY_MISMATCH", "Shipping and billing countries differ"},
            {"HOLD", "Hold for review"}, {"CANCEL", "Cancel automatically"},
            {"VERIFIED", "Verified"}, {"UNCLAIMED", "No domain"}
        };
        for (int iter = 0; iter < words.length; iter++) {
            if (words[iter][0].equals(code)) {
                return words[iter][1];
            }
        }
        return code;
    }

    static String role(String code) {
        return of(code);
    }

    /// The badge style for a status: good, warning, bad or plain.
    static String badge(String code) {
        if ("ACTIVE".equals(code) || "PAID".equals(code) || "FULFILLED".equals(code)
                || "RECEIVED".equals(code) || "CLOSED".equals(code) || "DELIVERED".equals(code)
                || "PICKED_UP".equals(code) || "APPROVED".equals(code) || "PUBLISHED".equals(code)) {
            return "SvBadgeGood";
        }
        if ("CANCELLED".equals(code) || "VOIDED".equals(code) || "REFUNDED".equals(code)
                || "ARCHIVED".equals(code) || "DECLINED".equals(code) || "REJECTED".equals(code)
                || "SUSPENDED".equals(code) || "FAILED".equals(code)) {
            return "SvBadgeBad";
        }
        if ("PENDING".equals(code) || "UNFULFILLED".equals(code) || "PARTIAL".equals(code)
                || "DRAFT".equals(code) || "PARTIALLY_REFUNDED".equals(code)
                || "AUTHORIZED".equals(code) || "REQUESTED".equals(code)) {
            return "SvBadgeWarn";
        }
        return "SvBadge";
    }

    static String[] labels(String[] codes) {
        String[] out = new String[codes.length];
        for (int iter = 0; iter < codes.length; iter++) {
            out[iter] = of(codes[iter]);
        }
        return out;
    }
}
