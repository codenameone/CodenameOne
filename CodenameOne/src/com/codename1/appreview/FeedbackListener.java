/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.appreview;

/// Receives the outcome of the Codename One drawn rating widget shown by
/// [com.codename1.appreview.AppReview] when the platform has no native review
/// prompt (or when the user gave a low rating). Register an implementation via
/// [AppReview#setFeedbackListener] to collect feedback through your own
/// channel (e.g. a support backend) instead of the built in e-mail composer.
public interface FeedbackListener {
    /// Invoked when the user picked a rating below the configured high rating
    /// threshold (see [AppReview#setHighRatingThreshold]) and therefore should
    /// be routed to a private feedback flow rather than the public store.
    ///
    /// Returning `true` signals that the listener is presenting its own
    /// feedback experience, so `AppReview` will not show the built in feedback
    /// composer. Returning `false` lets `AppReview` fall back to its default
    /// behaviour (e-mail to the configured support address, if any).
    ///
    /// #### Parameters
    ///
    /// - `rating`: the star value the user selected, from 1 to the rating
    ///   widget's maximum (5 by default).
    ///
    /// #### Returns
    ///
    /// true if the listener handled the low rating itself.
    boolean lowRating(int rating);

    /// Invoked with the free text the user typed in the built in feedback
    /// composer, when [AppReview] is left to handle the low rating flow and a
    /// support e-mail address was configured. Implement this to intercept the
    /// text and deliver it yourself; it is not called when [#lowRating] already
    /// returned `true`.
    ///
    /// #### Parameters
    ///
    /// - `rating`: the star value the user selected.
    ///
    /// - `feedback`: the free text entered by the user, never null but possibly
    ///   empty.
    void feedback(int rating, String feedback);
}
