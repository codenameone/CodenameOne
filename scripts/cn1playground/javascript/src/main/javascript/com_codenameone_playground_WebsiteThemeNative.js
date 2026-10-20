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

(function(exports){

var o = {};

    function readWebsiteThemePreference() {
        try {
            var parentWindow = (window.parent && window.parent !== window) ? window.parent : null;
            var parentDoc = parentWindow && parentWindow.document ? parentWindow.document : null;
            var parentBody = parentDoc && parentDoc.body ? parentDoc.body : null;
            var classes = parentBody && parentBody.classList ? parentBody.classList : null;
            if (classes) {
                if (classes.contains("dark") || classes.contains("cn1-initializr-dark")) {
                    return true;
                }
                if (classes.contains("light") || classes.contains("cn1-initializr-light")) {
                    return false;
                }
            }

            if (parentWindow && parentWindow.localStorage) {
                var pref = parentWindow.localStorage.getItem("pref-theme");
                if (pref === "dark") {
                    return true;
                }
                if (pref === "light") {
                    return false;
                }
            }

            var mediaWindow = parentWindow || window;
            if (mediaWindow.matchMedia) {
                return mediaWindow.matchMedia("(prefers-color-scheme: dark)").matches;
            }
        } catch (ignored) {
            // Ignore parent access failures and fallback below.
        }

        if (window.matchMedia) {
            return window.matchMedia("(prefers-color-scheme: dark)").matches;
        }

        return false;
    }

    o.isDarkMode_ = function(callback) {
        callback.complete(!!readWebsiteThemePreference());
    };

    o.notifyUiReady_ = function(callback) {
        var sendReady = function() {
            try {
                if (window.parent && window.parent !== window && window.parent.postMessage) {
                    window.parent.postMessage({ type: "cn1-playground-ui-ready" }, "*");
                }
            } catch (ignored) {
                // Ignore cross-origin or sandbox restrictions.
            }
            callback.complete();
        };

        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(function() {
                window.requestAnimationFrame(sendReady);
            });
        } else {
            window.setTimeout(sendReady, 48);
        }
    };

    o.locationHref_ = function(callback) {
        // This native impl runs on the iframe's main thread, where ``window`` is
        // the Playground app frame. Deep-link params (?sample= / ?code= / ?css=)
        // may live either on this frame's URL (when opened directly) or on the
        // parent page that embeds it (the codenameone.com /playground wrapper).
        // Prefer whichever carries a query string so sharing works in both cases.
        try {
            var loc = window.location;
            var origin = loc.origin || (loc.protocol + "//" + loc.host);
            var search = loc.search || "";
            var hash = loc.hash || "";
            try {
                var hasQuery = search && search.length > 1;
                if (!hasQuery && window.parent && window.parent !== window && window.parent.location) {
                    var psearch = window.parent.location.search || "";
                    if (psearch && psearch.length > 1) {
                        search = psearch;
                        if (!hash) {
                            hash = window.parent.location.hash || "";
                        }
                    }
                }
            } catch (ignoredParent) {
                // Cross-origin parent (shouldn't happen same-site) -- fall back.
            }
            callback.complete(origin + (loc.pathname || "/") + search + hash);
        } catch (e) {
            callback.complete("");
        }
    };

    // Inline mode: the host page posts the snippet to show next. Kept here until
    // the app polls it with takeInlineCode, because the message can arrive
    // before the VM is ready to receive it.
    //
    // Code is taken only from the page embedding this frame, and only when that
    // page is on this origin: inline mode runs whatever it is handed, so another
    // site framing it must not be able to hand it anything.
    //
    // Each message carries the host's request id. The id of the code being
    // compiled is recorded when the app TAKES it, not when it arrives: a second
    // snippet can arrive while the first still compiles, and the first one's
    // result must not be reported as the second's.
    var pendingInlineCode = null;
    var pendingInlineId = 0;
    var runningInlineId = 0;
    window.addEventListener("message", function(evt) {
        var data = evt && evt.data;
        if (!evt || evt.source !== window.parent || window.parent === window || evt.origin !== window.location.origin) {
            return;
        }
        if (data && data.type === "cn1-playground-inline-code" && typeof data.code === "string") {
            pendingInlineCode = data.code;
            pendingInlineId = typeof data.id === "number" ? data.id : 0;
        }
    });

    o.takeInlineCode_ = function(callback) {
        var code = pendingInlineCode;
        pendingInlineCode = null;
        if (code !== null) {
            runningInlineId = pendingInlineId;
        }
        callback.complete(code);
    };

    o.notifyInlineResult__boolean_java_lang_String_java_lang_String = function(ok, summary, sourceUrl, callback) {
        try {
            if (window.parent && window.parent !== window && window.parent.postMessage) {
                window.parent.postMessage({ type: "cn1-playground-inline-result", id: runningInlineId, ok: !!ok, summary: summary || "", sourceUrl: sourceUrl || "" }, window.location.origin);
            }
        } catch (ignored) {
            // Ignore cross-origin or sandbox restrictions.
        }
        callback.complete();
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codenameone_playground_WebsiteThemeNative = o;

})(cn1_get_native_interfaces());
