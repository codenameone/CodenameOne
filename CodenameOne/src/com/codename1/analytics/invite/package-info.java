/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
/// Invite / referral attribution: who invited whom, who installed because of
/// it, and what that invited cohort went on to do.
///
/// [Invites] mints an invite link, shares it through the native share sheet,
/// and -- on the friend's device -- recovers the invite that caused the
/// install. Once attribution resolves it is written as persistent analytics
/// dimensions, so every later event, including the `purchase` event the
/// framework already emits for you, arrives tagged with the campaign and the
/// referrer. Revenue and lifetime value per campaign fall out of the reports
/// you already have.
///
/// ```java
/// // On the inviter's device.
/// Invite invite = Invites.create(InviteRequest.create()
///         .campaign("spring")
///         .channel("share_sheet")
///         .build());
/// Invites.share(invite, "Come and try this with me");
///
/// // On the friend's device, from your start() method.
/// Invites.setInviteListener(new InviteListener() {
///     public void inviteReceived(InviteAttribution attribution) {
///         // Credit attribution.getCampaign() / getCode().
///     }
///
///     public void attributionUnavailable(String reason) {
///         // Ordinary: most installs are not invited.
///     }
/// });
/// Invites.checkForInvite();
/// ```
///
/// Everything here is gated on the analytics consent category of
/// {@link com.codename1.analytics.Analytics}, and nothing is reported until
/// consent is granted.
///
/// This package is deliberately separate from
/// {@link com.codename1.analytics}. The Android half of the attribution links
/// the Play Install Referrer library and declares the permission that binds to
/// it, and the build only does that for applications that actually reference
/// this package -- an application that merely reports analytics carries
/// neither.
package com.codename1.analytics.invite;
