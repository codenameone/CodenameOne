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
package com.codename1.push;

import com.codename1.xml.Element;

/// A convenience class for building a push notification Payload.
///
/// @author shannah
public class PushBuilder {
    private String title;
    private String body;
    private String metaData;
    private String imageUrl;
    private String category;
    private int type;
    private int badge;

    /// Sets the title of the push notification.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title of the push notification.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder title(String title) {
        this.title = title;
        return this;
    }

    /// Sets the badge to set with the push notification.
    ///
    /// #### Parameters
    ///
    /// - `badge`: The badge to set.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder badge(int badge) {
        this.badge = badge;
        return this;
    }

    /// Sets the body of the push notification.
    ///
    /// #### Parameters
    ///
    /// - `body`: The body of the push notification.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder body(String body) {
        this.body = body;
        return this;
    }

    /// Sets the metadata (i.e. content the user shouldn't see) for the push notification.
    ///
    /// #### Parameters
    ///
    /// - `metaData`: The metadata.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder metaData(String metaData) {
        this.metaData = metaData;
        return this;
    }

    /// Sets the URL for an image to send in the push notification.  Make sure you use https:// or the image won't be shown on iOS.
    ///
    /// #### Parameters
    ///
    /// - `imageUrl`: The image URL.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder imageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    /// Sets the category of the push notification.
    ///
    /// #### Parameters
    ///
    /// - `category`: The category.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder category(String category) {
        this.category = category;
        return this;
    }

    /// Sets the type of the push notification.
    ///
    /// **Types**
    ///
    /// TypeDescription
    /// 1Sends only body as the notification message
    /// 2Sends only metadata.  Invisible to user but will be received in push callback
    /// 3Sends both metadata and body.  Body is visible to the user.  Push callback will receive <metadata>;<body>.
    /// 4Sends both title and body. Push callback will receive <title>;<body>
    /// 100Sends only the badge
    /// 101Sends the badge and body.  Push callback will receive the body only.
    /// 102Sends the badge, title, and body. Push callback will receive <title>;<body>
    ///
    /// Both `#imageUrl` and `#category` can be added to any notification type.  If either of these values are non-null,
    /// then the push notification is considered to be a rich push notification, and `#getType()` will return 99,
    /// which signifies that it is a rich push notification.
    ///
    /// #### Parameters
    ///
    /// - `type`: The type of the push notification.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public PushBuilder type(int type) {
        this.type = type;
        return this;
    }

    /// A notification is considered to be a rich push notification if either `#imageUrl` or `#category` is set.
    ///
    /// #### Returns
    ///
    /// True if the notification is a rich notification.
    public boolean isRichPush() {
        return imageUrl != null || category != null;
    }

    /// Gets the type of the notification.  This will return 99 if `#isRichPush()` is true.  Otherwise
    /// it will return the underlying type as set with `#type(int)`.
    ///
    /// #### Returns
    ///
    /// The type of the notification.
    ///
    /// #### See also
    ///
    /// - #type(int)
    ///
    /// - #isRichPush()
    public int getType() {
        if (isRichPush()) {
            return 99;
        }
        return type;
    }


    /// Builds the payload for this rich push notification.
    ///
    /// #### Returns
    ///
    /// The payload that can be passed as the message body to `Push`
    public String build() {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case 0:
            case 1:
            case 5:
                sb.append(body);
                break;
            case 2:
                sb.append(metaData);
                break;
            case 3:
                sb.append(metaData).append(";").append(body);
                break;
            case 4:
                sb.append(title).append(";").append(body);
                break;
            case 6:
            case 100:
                sb.append(badge);
                break;
            case 101:
                sb.append(badge).append(" ").append(body);
                break;

            case 102:
                sb.append(badge).append(";").append(title).append(";").append(body);
                break;
            default:
                break;
        }

        if (isRichPush()) {
            String b = sb.toString();
            sb.setLength(0);
            Element el = new Element("push");
            el.setAttribute("type", "" + type);
            el.setAttribute("body", b);
            if (category != null) {
                el.setAttribute("category", category);
            }
            if (imageUrl != null) {
                Element imgEl = new Element("img");
                imgEl.setAttribute("src", imageUrl);
                el.addChild(imgEl);
            }
            return el.toString();
        }
        return sb.toString();

    }


}
