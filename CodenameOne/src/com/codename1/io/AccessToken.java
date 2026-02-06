/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.io;

import com.codename1.compat.java.util.Objects;
import com.codename1.util.DateUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/// This class represent an access token.
///
/// @author Chen
public class AccessToken implements Externalizable {


    private String token;

    /// When the token expires.  Generally this is a numeric string obtained
    /// from the "expires" token of an oauth2 request, signifying the number of seconds
    /// from the time of issue that the token is valid for.
    private String expires;

    /// The expiry date of the token.  Prefer this value over the `#expires` value
    /// since the expires value may just be the number of seconds that the access token is good
    /// for since it was generated - and we may not have that information stored.
    ///
    /// #### Since
    ///
    /// 7.0
    private Date expiryDate;

    private String refreshToken;

    private String identityToken;

    /// Constructor with parameters
    ///
    /// #### Parameters
    ///
    /// - `token`: the token string
    ///
    /// - `expires`: the access token expires date
    public AccessToken(String token, String expires) {
        this(token, expires, null);
    }

    /// Constructor with parameters
    ///
    /// @ince 7.0
    ///
    /// #### Parameters
    ///
    /// - `token`: the token string
    ///
    /// - `expires`: the access token expires date
    ///
    /// - `refreshToken`: The refresh token.
    public AccessToken(String token, String expires, String refreshToken) {
        this(token, expires, refreshToken, null);
    }

    /// Constructor with parameters
    ///
    /// @ince 7.0
    ///
    /// #### Parameters
    ///
    /// - `token`: the token string
    ///
    /// - `expires`: the access token expires date
    ///
    /// - `refreshToken`: The refresh token.
    ///
    /// - `identityToken`: The identity token
    public AccessToken(String token, String expires, String refreshToken, String identityToken) {
        this.token = token;
        this.expires = expires;
        this.expiryDate = parseDate(expires);
        this.refreshToken = refreshToken;
        this.identityToken = identityToken;
    }

    /// #### Since
    ///
    /// 7.0
    public AccessToken() {

    }

    /// #### Parameters
    ///
    /// - `Token`: The token.
    ///
    /// - `expiryDate`: The expiry date.
    ///
    /// #### Since
    ///
    /// 7.0
    public static AccessToken createWithExpiryDate(String token, Date expiryDate) {
        AccessToken out = new AccessToken(token, null);

        out.expiryDate = expiryDate;
        return out;
    }

    /// Parses an integer value as a string.  Automatically truncates at first
    /// decimal place.
    ///
    /// #### Parameters
    ///
    /// - `str`: A numeric string
    ///
    /// #### Returns
    ///
    /// The Long value of the string, or null if it couldn't be parsed.
    private static Long parseLong(String str) {
        if (str == null) {
            return null;
        }
        int decimalPos = str.indexOf('.');
        if (decimalPos >= 0) {
            str = str.substring(0, decimalPos);
        }

        char[] chars = str.toCharArray();
        int len = chars.length;

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (!Character.isDigit(c)) {
                return null;
            }
        }
        return Long.valueOf(Long.parseLong(str));
    }

    /// A utility function that can parse date values received as a string.
    /// Most often these dates are just numbers of seconds as received from
    /// "expires" oauth2 tokens, which indicate the number of seconds an access
    /// token is valid for.
    ///
    /// #### Parameters
    ///
    /// - `dateStr`: The date string.  Value inputs include "3600", "3600.0", "7200.12345", etc..
    ///
    /// #### Returns
    ///
    /// A Date, or null if the input can't be parsed.
    private static Date parseDate(String dateStr) {
        Long longExpires = parseLong(dateStr);
        if (longExpires != null) {
            // There was an expiry date, but it might have been from an expires_in
            // header, which would only hold the number of seconds since
            long l = longExpires.longValue();
            if (l == 0L) {
                return null;
            }
            return new Date(System.currentTimeMillis() + l * 1000L);
        }
        return null;

    }

    /// Simple getter
    ///
    /// #### Returns
    ///
    /// the token string
    public String getToken() {
        return token;
    }

    /// Simple getter
    ///
    /// #### Returns
    ///
    /// the expires date
    public String getExpires() {
        return expires;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(token, out);
        Util.writeUTF(expires, out);
        Util.writeObject(expiryDate, out);
        Util.writeUTF(refreshToken, out);
        Util.writeUTF(identityToken, out);
    }

    @Override
    public void internalize(int version, DataInputStream in) throws IOException {
        token = Util.readUTF(in);
        expires = Util.readUTF(in);
        if (version >= 2) {
            expiryDate = (Date) Util.readObject(in);
        }
        if (version >= 3) {
            refreshToken = Util.readUTF(in);
        }
        if (version >= 4) {
            identityToken = Util.readUTF(in);
        }
    }

    /// Gets refresh token.
    ///
    /// #### Returns
    ///
    /// Refresh token.
    ///
    /// #### Since
    ///
    /// 7.0
    public String getRefreshToken() {
        return refreshToken;
    }

    /// Sets refresh token.
    ///
    /// #### Parameters
    ///
    /// - `refreshToken`: The refresh token.
    ///
    /// #### Since
    ///
    /// 7.0
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String getObjectId() {
        return "AccessToken";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AccessToken) {
            AccessToken tok = (AccessToken) obj;
            return Objects.equals(token, tok.token) && Objects.equals(expiryDate, tok.expiryDate);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.token != null ? this.token.hashCode() : 0);
        hash = 53 * hash + (this.expiryDate != null ? this.expiryDate.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "AccessToken {" + token + ", expires=" + expiryDate + "}";
    }

    /// Gets the expiry date of this token.
    ///
    /// #### Returns
    ///
    /// The expiry date of this token or null.
    ///
    /// #### Since
    ///
    /// 7.0
    public Date getExpiryDate() {
        return expiryDate;
    }

    /// Sets the expiry date of this token.
    ///
    /// #### Parameters
    ///
    /// - `date`: The expiry date of this token.
    ///
    /// #### Since
    ///
    /// 7.0
    public void setExpiryDate(Date date) {
        this.expiryDate = date;
    }

    /// Checks to see if this token is expired.
    ///
    /// #### Returns
    ///
    /// False if no expiry date is set or the expiryDate is before the current time.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isExpired() {
        return expiryDate != null && DateUtil.compare(expiryDate, new Date()) < 0;
    }

    /// #### Returns
    ///
    /// the identityToken
    ///
    /// #### Since
    ///
    /// 7.0
    public String getIdentityToken() {
        return identityToken;
    }

    /// #### Parameters
    ///
    /// - `identityToken`: the identityToken to set
    ///
    /// #### Since
    ///
    /// 7.0
    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

}
