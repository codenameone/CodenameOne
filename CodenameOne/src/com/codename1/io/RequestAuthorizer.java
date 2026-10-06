/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io;

import com.codename1.util.AsyncResource;

/// Supplies the `Authorization` header of the requests an application sends to its own
/// service, and renews it when the service refuses it.
///
/// An authorizer is given to one request with [ConnectionRequest#setAuthorizer(RequestAuthorizer)]
/// or [com.codename1.io.rest.RequestBuilder#authorizer(RequestAuthorizer)], or to every request
/// under a base URL with [NetworkManager#setAuthorizer(String, RequestAuthorizer)]:
///
/// ```java
/// NetworkManager.getInstance().setAuthorizer("https://api.example.com", authorizer);
/// ```
///
/// [com.codename1.io.oidc.OidcRequestAuthorizer] is the implementation for a service that
/// accepts OAuth 2.0 access tokens.
///
/// #### What happens to a request
///
/// 1. As the request is queued, [#getAuthorization(ConnectionRequest)] is asked for a header
///    value, which travels with the request. A request that already carries an `Authorization`
///    header keeps its own: one the caller set always wins, and so does a default header of
///    [NetworkManager].
/// 2. If the service answers `401`, nothing is delivered yet. The request is held and
///    [#refreshAuthorization(ConnectionRequest, String)] is asked for a new credential.
/// 3. If that succeeds the request is sent once more with the new header, and its answer --
///    whatever it is, another `401` included -- is delivered as usual. There is one renewal per
///    request, so a service that keeps refusing cannot make this loop.
/// 4. If it fails, the request is sent again as it first was, and the service's `401` goes
///    through the request's ordinary error handling exactly as it would have without an
///    authorizer.
///
/// Code that waits for the request -- [NetworkManager#addToQueueAndWait(ConnectionRequest)], the
/// blocking methods of `RequestBuilder`, [NetworkManager#addToQueueAsync(ConnectionRequest)] --
/// keeps waiting through all of it and sees only the final answer.
///
/// An authorizer that knows when its credential expires can skip the refused request
/// altogether: see [Proactive].
///
/// #### Threads
///
/// Every method of an authorizer is called on the event dispatch thread and on no other, so
/// an implementation keeps its credential in plain fields and needs no lock. A network
/// thread never calls one: it sends the header value that was put on the request, on the
/// EDT, when the request was queued or released after a renewal. A request queued from
/// another thread is passed to the EDT first, and so is a `401`, which a network thread is
/// the one to see.
///
/// The header is therefore the one current when the request was queued. If the credential
/// is renewed while the request waits in the queue, the service refuses the old one and the
/// request is sent again with the new one, as step 2 describes.
///
/// [#getAuthorization(ConnectionRequest)] must answer from memory. It must not wait for
/// another request: the EDT would wait on the queue it is filling. Renewing is therefore a
/// separate step, which the authorizer starts and answers later.
public interface RequestAuthorizer {

    /// An authorizer that never adds a header. Set on a request, it keeps the default of
    /// [NetworkManager#setAuthorizer(String, RequestAuthorizer)] away from that one request --
    /// the request that fetches the token itself is the usual case.
    RequestAuthorizer NONE = new RequestAuthorizer() {
        @Override
        public String getAuthorization(ConnectionRequest request) {
            return null;
        }

        @Override
        public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
                String rejectedAuthorization) {
            return null;
        }
    };

    /// The value of the `Authorization` header for a request that is about to be sent, for
    /// example `"Bearer eyJ..."`.
    ///
    /// Called on the event dispatch thread: when the request is queued, and again when it is
    /// sent a second time with a renewed credential. Answer from memory and do not block.
    /// The value is copied to the request; a redirect is sent the same one for as long as
    /// it stays under the base URL the authorizer was registered for.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request being queued
    ///
    /// #### Returns
    ///
    /// the header value, or null to send the request without one
    String getAuthorization(ConnectionRequest request);

    /// Called after the service answered `401` to a request that carried this authorizer's
    /// header. Called on the event dispatch thread, at most once per request.
    ///
    /// Several requests can be refused at the same moment. An implementation should renew its
    /// credential once and give every one of them the same answer, and should recognize a
    /// `rejectedAuthorization` that is no longer the current one: that request was sent before
    /// an earlier renewal finished, and only needs sending again.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request that was refused
    ///
    /// - `rejectedAuthorization`: the header value the service refused
    ///
    /// #### Returns
    ///
    /// a resource that completes with `true` once a different credential is ready, and with
    /// `false` or an error when there is none to be had. Null means the same as `false`
    AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request,
            String rejectedAuthorization);

    /// An authorizer that can tell, before a request is sent, that its credential is about
    /// to stop working, and renew it first.
    ///
    /// Without this a credential is renewed only after the service has refused it, which
    /// costs one refused request for every credential that expires. An authorizer that
    /// implements this is asked when a request is queued -- on the event dispatch thread,
    /// before the request reaches a network thread -- and may hold the request until a new
    /// credential is ready. The request is then queued as usual and
    /// [RequestAuthorizer#getAuthorization(ConnectionRequest)] answers with the new one. A
    /// `401` is still handled as [RequestAuthorizer] describes; this only makes it rare.
    ///
    /// Nothing waits on a thread for it: the request is simply not in the queue yet. Code
    /// that waits for the request waits a little longer, and sees one answer.
    interface Proactive extends RequestAuthorizer {
        /// Called when a request this authorizer covers is queued, on the event dispatch
        /// thread. Answer from memory and do not block: start the renewal and return.
        ///
        /// Several requests can be queued while one renewal runs. An implementation should
        /// renew once and hold them all on the same resource.
        ///
        /// #### Parameters
        ///
        /// - `request`: the request being queued
        ///
        /// #### Returns
        ///
        /// null when the request can be sent as it is; otherwise a resource that completes
        /// when it can -- with any value, or with an error: the request is sent either way,
        /// with whatever [RequestAuthorizer#getAuthorization(ConnectionRequest)] answers
        /// then
        AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request);
    }
}
