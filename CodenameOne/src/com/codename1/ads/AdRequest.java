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

import java.util.ArrayList;
import java.util.List;

/// Optional targeting metadata attached to an ad load. Build one with the fluent
/// setters and pass it to a load method, or omit it entirely to load with
/// defaults:
///
/// ```java
/// interstitial.load(new AdRequest()
///         .addKeyword("games")
///         .contentUrl("https://example.com/level"));
/// ```
///
/// An `AdRequest` is immutable once a load begins; reuse or discard freely.
public class AdRequest {
    private final List<String> keywords = new ArrayList<String>();
    private String contentUrl;
    private boolean nonPersonalized;

    /// Adds a keyword used to target the ad.
    ///
    /// #### Parameters
    ///
    /// - `keyword`: a keyword describing the current content
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public AdRequest addKeyword(String keyword) {
        if (keyword != null) {
            keywords.add(keyword);
        }
        return this;
    }

    /// The keywords associated with this request, never null.
    public List<String> getKeywords() {
        return keywords;
    }

    /// The keywords as a comma separated string, used by native bridges. Returns
    /// an empty string when no keywords were set.
    public String getKeywordString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(keywords.get(i));
        }
        return b.toString();
    }

    /// Sets a URL describing the content the ad will appear next to, used for
    /// brand safety and contextual targeting.
    ///
    /// #### Parameters
    ///
    /// - `contentUrl`: the URL of the surrounding content
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public AdRequest contentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    /// The content URL associated with this request, may be null.
    public String getContentUrl() {
        return contentUrl;
    }

    /// Requests non-personalized ads regardless of the user's consent state.
    /// This is normally driven automatically by [AdConsent]; set it explicitly
    /// only when you have your own consent mechanism.
    ///
    /// #### Parameters
    ///
    /// - `nonPersonalized`: true to request non personalized ads
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public AdRequest nonPersonalized(boolean nonPersonalized) {
        this.nonPersonalized = nonPersonalized;
        return this;
    }

    /// True when non personalized ads were explicitly requested.
    public boolean isNonPersonalized() {
        return nonPersonalized;
    }
}
