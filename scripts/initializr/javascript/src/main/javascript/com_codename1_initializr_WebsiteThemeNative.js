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

    o.downloadProject__java_lang_String_java_lang_String = function(fileName, dataUrl, callback) {
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
                window.parent.postMessage({ type: "cn1-initializr-project-downloaded" }, "*");
            }
        } catch (ignored) {
            // The download succeeded even if the optional embedding page is unavailable.
        }
        callback.complete(true);
    };

    // Horizontal clearance (CSS px) the host page's Crisp chat launcher needs at
    // the bottom-right, so the generate button can be nudged left of it. The
    // default Crisp launcher bubble (~64px) sits ~24px from the page edge;
    // CHAT_CLEARANCE_PX reserves enough to clear it with a small visual gap.
    var CHAT_CLEARANCE_PX = 96;

    function chatLauncherVisible() {
        try {
            var parentWindow = (window.parent && window.parent !== window) ? window.parent : window;
            var doc = parentWindow.document;
            var client = doc ? doc.querySelector(".crisp-client") : null;
            if (!client) {
                return false;
            }
            var cs = parentWindow.getComputedStyle ? parentWindow.getComputedStyle(client) : null;
            if (cs && (cs.display === "none" || cs.visibility === "hidden" || parseFloat(cs.opacity || "1") === 0)) {
                return false;
            }
            return true;
        } catch (ignored) {
            // Cross-origin / sandbox / missing widget: reserve nothing.
            return false;
        }
    }

    o.chatLauncherClearance_ = function(callback) {
        callback.complete(chatLauncherVisible() ? CHAT_CLEARANCE_PX : 0);
    };

    o.isSupported_ = function(callback) {
        callback.complete(true);
    };

exports.com_codename1_initializr_WebsiteThemeNative = o;

})(cn1_get_native_interfaces());
