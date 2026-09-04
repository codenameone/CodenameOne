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
package com.codename1.continuity;

import com.codename1.io.rest.ErrorCodeHandler;
import com.codename1.io.rest.RequestBuilder;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;

import java.io.IOException;

/// A `StateRelay` over your own HTTPS endpoint, which is all most applications need.
///
/// ```java
/// Continuity.setRelay(new RestStateRelay("https://api.example.com/continuity") {
///     protected String getToken() {
///         return session.getAccessToken();
///     }
///  });
/// ```
///
/// #### The contract
///
/// Two requests against the one URL you supply:
///
/// - `POST` with the state as a JSON body and `Content-Type: application/json`. Store it against
///   the signed-in user, replacing whatever you held for them. Any 2xx means stored.
/// - `GET`, answering with the newest state you hold for that user as the same JSON, or an empty
///   body when you hold none. A 404 also means none.
///
/// The JSON is exactly what `StateCodec.toJson(AppState)` produces, and it is a closed shape: your
/// endpoint stores and returns the document, and never needs to look inside it.
///
/// #### Identity is yours
///
/// Which states belong to the same person is the one question the framework cannot answer, which
/// is why the token comes from `getToken()` rather than from a constructor: it is read at each
/// request, so a session that refreshes its token is followed automatically. Return null for an
/// endpoint that identifies the user some other way -- a cookie, mutual TLS -- and the header is
/// simply omitted.
///
/// #### Threading
///
/// Both methods are called from a background thread and block, which is what the framework
/// expects of a relay. `getToken()` is called on that same thread, so it must not wait on the
/// event dispatch thread.
public class RestStateRelay implements StateRelay {
    private final String url;

    /// Creates a relay against an HTTPS endpoint.
    ///
    /// #### Parameters
    ///
    /// - `url`: the endpoint, which must be HTTPS
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the URL is null, empty or not HTTPS
    public RestStateRelay(String url) {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("A continuity relay needs an endpoint URL.");
        }
        if (url.length() < 8 || !"https://".equals(url.substring(0, 8).toLowerCase())) {
            // Refused rather than passed on. The bearer token goes out on every request and the
            // payload is a description of what the user is doing on their other device; an
            // "http://" typo would put both on the network in the clear wherever a cleartext
            // policy still allows it.
            throw new IllegalArgumentException("A continuity relay endpoint must be HTTPS; got \""
                    + url + "\".");
        }
        this.url = url;
    }

    /// The endpoint this relay talks to.
    ///
    /// #### Returns
    ///
    /// the URL
    public String getUrl() {
        return url;
    }

    /// The bearer token to present, read once per request. The default returns null, which sends
    /// no `Authorization` header.
    ///
    /// #### Returns
    ///
    /// the token, or null for none
    ///
    /// #### Changing accounts
    ///
    /// Install a NEW relay for the new account -- `Continuity.setRelay(StateRelay)` -- rather than
    /// returning a different account's token from the same object. A publish that was authorised
    /// for the previous account can still be between the framework's last check and this read
    /// when the switch happens, and the framework cannot bind a token it is not allowed to read.
    /// What it can recognise is an object that is no longer installed, which it then refuses; an
    /// object that quietly starts answering for someone else looks identical to one that
    /// refreshed its own session.
    protected String getToken() {
        return null;
    }

    @Override
    public void publish(AppState state) throws IOException {
        Response<String> response = auth(Rest.post(url).jsonContent()
                .body(StateCodec.toJson(state))).getAsString();
        int code = response.getResponseCode();
        if (code < 200 || code > 299) {
            throw new IOException("The continuity relay refused the state: HTTP " + code
                    + (response.getResponseErrorMessage() == null ? ""
                            : " " + response.getResponseErrorMessage()));
        }
    }

    @Override
    public AppState fetch() throws IOException {
        Response<String> response = auth(Rest.get(url).jsonContent()).getAsString();
        int code = response.getResponseCode();
        if (code == 404 || code == 204) {
            // Not an error. An endpoint that holds nothing for this user yet is the ordinary
            // state of affairs on a first run, and throwing here would log a failure on every
            // launch until the user's second device wrote something.
            return null;
        }
        if (code < 200 || code > 299) {
            throw new IOException("The continuity relay refused to answer: HTTP " + code
                    + (response.getResponseErrorMessage() == null ? ""
                            : " " + response.getResponseErrorMessage()));
        }
        return StateCodec.fromJson(response.getResponseData());
    }

    /// Adds the bearer token, refusing outright if this relay is no longer the installed one.
    ///
    /// The refusal is HERE, immediately before the token is read, because that is what makes it
    /// worth anything. A worker that was started for one account and reaches the network after
    /// the user has signed out and back in would otherwise send the first account's state
    /// authenticated as the second: getToken() is read at each request, by design, so the same
    /// relay object answers with whoever is signed in NOW.
    ///
    /// Continuity stops such a worker before it calls a relay at all. This is the second line for
    /// the gap that check cannot cover -- it runs on the event thread, and the worker is not it.
    /// Throwing rather than skipping quietly, so the framework records the publish as failed and
    /// keeps owing it, and the state is republished once a relay is installed again.
    private RequestBuilder auth(RequestBuilder b) throws IOException {
        if (!Continuity.isInstalledRelay(this)) {
            throw new IOException("This relay is no longer installed -- Continuity.clear() or "
                    + "setRelay() replaced it. Refusing the request rather than sending one "
                    + "account's state under another account's credentials.");
        }
        // SILENT, because these are housekeeping requests the user never asked for. A request
        // builder sets failSilently only when an error-code handler is registered, and without it
        // ConnectionRequest puts a Retry/Cancel dialog in front of the user for both a failure
        // response and a connection exception. The 404 below is the DOCUMENTED answer for a relay
        // that holds nothing yet, so a correctly implemented endpoint showed every user an error
        // dialog on first run -- for the ordinary case, before this class could read the code and
        // call it an empty relay.
        //
        // The handler itself does nothing on purpose: getAsString() builds its Response from the
        // request's own code and body rather than from these callbacks, so publish() and fetch()
        // still see 404, 204 and everything else exactly as before.
        // NO REDIRECTS, because this request carries a bearer token. A redirect is followed
        // with the same headers, so a 307 would hand the token and the state to whatever host
        // the response names -- an `http://` one included, silently undoing the HTTPS the
        // constructor insists on. A 302 or 303 is not safer, only different: it turns the POST
        // into a GET, and the 2xx that follows makes publish() report a write that never
        // happened.
        //
        // A relay that has moved should say so by being configured with its new URL, which is
        // the application's decision to make and not a header's.
        RequestBuilder quiet = b.followRedirects(false).onErrorCodeString(SILENT);
        String token = getToken();
        return token == null || token.length() == 0 ? quiet : quiet.bearer(token);
    }

    /// Registered on every request purely to make it silent. See auth().
    ///
    /// A constant rather than an anonymous class per request: it captures nothing, and an inner
    /// class would hold its enclosing relay alive for no reason -- which SpotBugs reports as
    /// SIC_INNER_SHOULD_BE_STATIC_ANON.
    private static final ErrorCodeHandler<String> SILENT = new ErrorCodeHandler<String>() {
        @Override
        public void onError(Response<String> errorData) {
            // Deliberately nothing. The caller reads the response code and decides there.
        }
    };
}
