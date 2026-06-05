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
package com.codename1.ads;

/// The reward earned by a user who finished watching a [RewardedAd] or
/// [RewardedInterstitialAd]. The `type` and `amount` are configured in the
/// ad network console for the specific ad unit.
///
/// Note: for production apps that grant valuable rewards you should verify the
/// reward server side rather than trusting the client. See
/// [ServerSideVerificationOptions].
public class RewardItem {
    private final String type;
    private final int amount;

    /// Creates a new reward item.
    ///
    /// #### Parameters
    ///
    /// - `type`: the reward type as configured in the ad network console (e.g. "coins")
    /// - `amount`: the reward amount
    public RewardItem(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    /// The reward type as configured in the ad network console (e.g. "coins").
    public String getType() {
        return type;
    }

    /// The reward amount.
    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "RewardItem{type=" + type + ", amount=" + amount + '}';
    }
}
