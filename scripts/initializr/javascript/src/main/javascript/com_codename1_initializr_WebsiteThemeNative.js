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

    function disablePageScroll() {
        // The Codename One app owns scrolling (it has its own styled scrollbar),
        // so suppress scrolling on the host page/iframe to avoid a double scroll.
        try {
            var styles = "html,body{margin:0;padding:0;height:100%;overflow:hidden;overscroll-behavior:none;}";
            var doc = window.document;
            if (doc) {
                if (doc.documentElement) { doc.documentElement.style.overflow = "hidden"; }
                if (doc.body) { doc.body.style.overflow = "hidden"; doc.body.style.margin = "0"; }
                if (!doc.getElementById("cn1-initializr-noscroll")) {
                    var s = doc.createElement("style");
                    s.id = "cn1-initializr-noscroll";
                    s.appendChild(doc.createTextNode(styles));
                    (doc.head || doc.documentElement).appendChild(s);
                }
            }
        } catch (ignored) {
            // Ignore DOM access failures (e.g. sandboxed contexts).
        }
    }

    o.notifyUiReady_ = function(callback) {
        disablePageScroll();
        var sendReady = function() {
            try {
                if (window.parent && window.parent !== window && window.parent.postMessage) {
                    window.parent.postMessage({ type: "cn1-initializr-ui-ready" }, "*");
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

    o.downloadProject__java_lang_String_java_lang_String_java_lang_String_java_lang_String = function(fileName, dataUrl, packageName, template, callback) {
        var anchor = null;
        try {
            var doc = window.document;
            if (!doc || !doc.body || !dataUrl) {
                callback.complete(false);
                return;
            }
            anchor = doc.createElement("a");
            anchor.href = dataUrl;
            anchor.download = fileName || "codename-one-project.zip";
            doc.body.appendChild(anchor);
            anchor.click();
        } catch (downloadError) {
            callback.complete(false);
            return;
        } finally {
            try {
                if (anchor && anchor.parentNode) {
                    anchor.parentNode.removeChild(anchor);
                }
            } catch (ignored) {
                // Cleanup must not change a successful download acknowledgement.
            }
        }

        try {
            if (window.parent && window.parent !== window && window.parent.postMessage) {
                // packageName/template feed the website's anonymous download
                // beacon, which hashes the package name before anything leaves
                // the browser (docs/website/assets/js/cn1-initializr-beacon.js).
                window.parent.postMessage({
                    type: "cn1-initializr-project-downloaded",
                    packageName: packageName ? String(packageName) : "",
                    template: template ? String(template) : ""
                }, "*");
            }
        } catch (ignored) {
            // The download succeeded even if the optional embedding page is unavailable.
        }
        callback.complete(true);
    };

    // "Email me these steps" from the post-download panel. The embedding page
    // (layouts/_default/initializr.html) forwards it to BuildCloud through
    // cn1-initializr-beacon.js, which hashes the package name before it leaves
    // the browser, and ANSWERS with cn1-initializr-steps-result carrying the
    // same id and whether it actually issued the request. postMessage alone
    // proves nothing: on localhost, a PR preview, a page without the beacon,
    // or a browser without fetch/WebCrypto the host drops it, and the panel
    // must not say "Check your inbox" for an email nobody asked for. No answer
    // within STEPS_ACK_TIMEOUT_MS counts as not sent.
    var STEPS_ACK_TIMEOUT_MS = 6000;
    var stepsSeq = 0;

    // The only pages allowed to receive an email address typed into the panel.
    // /initializr-app/ is publicly reachable, so any site could frame it; a
    // postMessage to "*" would hand that site every address. Reading
    // window.parent.location throws for a cross-origin parent, so a frame whose
    // parent is NOT this same Codename One origin gets null here, and the panel
    // neither shows the email field nor sends anything.
    var STEPS_PARENT_ORIGINS = {
        "https://www.codenameone.com": true,
        "https://codenameone.com": true
    };

    function trustedParentOrigin() {
        try {
            if (!window.parent || window.parent === window) {
                return null;
            }
            var origin = window.parent.location.origin;
            return STEPS_PARENT_ORIGINS[origin] ? origin : null;
        } catch (crossOrigin) {
            return null;
        }
    }

    o.canRequestSteps_ = function(callback) {
        callback.complete(trustedParentOrigin() !== null);
    };

    o.requestSteps__java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String = function(email, packageName, template, ide, build, callback) {
        var done = false;
        var id = "steps-" + (++stepsSeq) + "-" + new Date().getTime();
        var parentOrigin = trustedParentOrigin();
        var onAnswer = function(evt) {
            if (!evt || evt.source !== window.parent || evt.origin !== parentOrigin || !evt.data
                    || evt.data.type !== "cn1-initializr-steps-result" || evt.data.id !== id) {
                return;
            }
            finish(evt.data.ok === true);
        };
        var finish = function(ok) {
            if (done) {
                return;
            }
            done = true;
            try {
                window.removeEventListener("message", onAnswer);
            } catch (ignored) {
                // Nothing to clean up.
            }
            callback.complete(ok);
        };
        try {
            if (email && parentOrigin) {
                window.addEventListener("message", onAnswer);
                window.setTimeout(function() { finish(false); }, STEPS_ACK_TIMEOUT_MS);
                window.parent.postMessage({
                    type: "cn1-initializr-steps-request",
                    id: id,
                    email: String(email),
                    packageName: packageName ? String(packageName) : "",
                    template: template ? String(template) : "",
                    ide: ide ? String(ide) : "",
                    build: build ? String(build) : ""
                }, parentOrigin);
                return;
            }
        } catch (ignored) {
            // Cross-origin or sandbox restrictions: report that nothing was sent.
        }
        finish(false);
    };

    // Horizontal clearance (CSS px) the host page's Crisp widget needs so the
    // generate button can sit to its left. Measured, not assumed: the round
    // launcher is ~64px, but a first-time visitor usually sees Crisp folded
    // with a greeting/"chat with us" pill several times wider, and a fixed
    // 96px reservation left the button underneath that, where the folded
    // widget is also awkward to dismiss. So find every visible Crisp element
    // that overlaps the bottom band of this frame (where the action bar is)
    // and clear the leftmost of them. 0 when Crisp is absent, hidden, or not
    // over the bar.
    var CHAT_BAND_PX = 140;   // bottom strip of the frame that holds the action bar
    var CHAT_GAP_PX = 16;     // visual gap between the widget and the button
    var CHAT_MAX_FRACTION = 0.6; // never push the button past 60% of the width

    function chatLauncherClearancePx() {
        try {
            var parentWindow = (window.parent && window.parent !== window) ? window.parent : window;
            var doc = parentWindow.document;
            var client = doc ? doc.querySelector(".crisp-client") : null;
            if (!client || !parentWindow.getComputedStyle) {
                return 0;
            }
            var hidden = function(el) {
                var cs = parentWindow.getComputedStyle(el);
                return !cs || cs.display === "none" || cs.visibility === "hidden"
                    || parseFloat(cs.opacity || "1") === 0;
            };
            if (hidden(client)) {
                return 0;
            }
            var frame = window.frameElement && window.frameElement.getBoundingClientRect
                ? window.frameElement.getBoundingClientRect()
                : { left: 0, right: parentWindow.innerWidth, bottom: parentWindow.innerHeight };
            var bandTop = frame.bottom - CHAT_BAND_PX;
            var minLeft = Infinity;
            var nodes = client.querySelectorAll("*");
            for (var i = 0; i < nodes.length; i++) {
                var r = nodes[i].getBoundingClientRect();
                if (r.width < 1 || r.height < 1 || r.bottom <= bandTop || r.top >= frame.bottom
                        || r.right <= frame.left || r.left >= frame.right || r.left >= minLeft) {
                    continue;
                }
                if (!hidden(nodes[i])) {
                    minLeft = r.left;
                }
            }
            if (minLeft === Infinity) {
                return 0;
            }
            var clearance = Math.ceil(frame.right - minLeft + CHAT_GAP_PX);
            var max = Math.floor((frame.right - frame.left) * CHAT_MAX_FRACTION);
            return Math.max(0, Math.min(clearance, max));
        } catch (ignored) {
            // Cross-origin / sandbox / missing widget: reserve nothing.
            return 0;
        }
    }

    o.chatLauncherClearance_ = function(callback) {
        callback.complete(chatLauncherClearancePx());
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codename1_initializr_WebsiteThemeNative = o;

})(cn1_get_native_interfaces());
