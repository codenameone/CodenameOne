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
// Each application and each complete bundle has its own cache. Do not activate a
// replacement while an older page still uses its worker/runtime/chunks.
(function () {
    'use strict';
    var prefix = 'cn1-pwa:' + encodeURIComponent(self.registration.scope) + ':';
    var cacheName = prefix + self.cn1Pwa.version;
    var assets = new Set(self.cn1Pwa.assets.map(function (path) {
        return new URL(path, self.registration.scope).href;
    }));
    self.addEventListener('install', function (event) {
        event.waitUntil(caches.open(cacheName).then(function (cache) {
            return cache.addAll(Array.from(assets).map(function (url) {
                return new Request(url, {cache: 'reload'});
            }));
        }));
    });
    self.addEventListener('activate', function (event) {
        event.waitUntil(caches.keys().then(function (keys) {
            return Promise.all(keys.filter(function (key) {
                return key.indexOf(prefix) === 0 && key !== cacheName;
            }).map(function (key) { return caches.delete(key); }));
        }).then(function () { return self.clients.claim(); }));
    });
    self.addEventListener('fetch', function (event) {
        if (event.request.method !== 'GET') return;
        var url = new URL(event.request.url);
        // Query parameters on navigations select application state, not another
        // copy of the shell. Never intercept unknown URLs, APIs or proxy traffic.
        if (event.request.mode === 'navigate') {
            url.search = '';
            if (url.href === self.registration.scope) url = new URL('index.html', url);
        }
        if (!assets.has(url.href)) return;
        event.respondWith(caches.open(cacheName).then(function (cache) {
            return cache.match(url.href).then(function (response) {
                return response || fetch(event.request);
            });
        }));
    });
}());
