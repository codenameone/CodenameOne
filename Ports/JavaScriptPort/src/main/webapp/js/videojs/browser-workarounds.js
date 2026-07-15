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
/* workaround browser issues */

var isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
var isEdge = /Edge/.test(navigator.userAgent);

function applyAudioWorkaround() {
    if (isSafari || isEdge) {

        if (isSafari && window.MediaRecorder !== undefined) {
            // this version of Safari has MediaRecorder
            return;
        }

        // see https://github.com/collab-project/videojs-record/issues/295
        options.plugins.record.audioRecorderType = StereoAudioRecorder;
        options.plugins.record.audioSampleRate = 44100;
        options.plugins.record.audioBufferSize = 4096;
        options.plugins.record.audioChannels = 2;
    }
}

function applyVideoWorkaround() {
    // use correct video mimetype for opera
    if (!!window.opera || navigator.userAgent.indexOf('OPR/') !== -1) {
        options.plugins.record.videoMimeType = 'video/webm\;codecs=vp8'; // or vp9
    }
}

function applyScreenWorkaround() {
    // Polyfill in Firefox.
    // See https://blog.mozilla.org/webrtc/getdisplaymedia-now-available-in-adapter-js/
    if (adapter.browserDetails.browser == 'firefox') {
        adapter.browserShim.shimGetDisplayMedia(window, 'screen');
    }
}

function addStartButton() {
    var btn = document.createElement('BUTTON');
    var t = document.createTextNode('Show player');
    btn.onclick = createPlayer;
    btn.appendChild(t);
    document.body.appendChild(btn);
}

function updateContext(opts) {
    // Safari 11 or newer automatically suspends new AudioContext's that aren't
    // created in response to a user-gesture, like a click or tap, so create one
    // here (inc. the script processor)
    var AudioContext = window.AudioContext || window.webkitAudioContext;
    var context = new AudioContext();
    var processor = context.createScriptProcessor(1024, 1, 1);

    opts.plugins.wavesurfer.audioContext = context;
    opts.plugins.wavesurfer.audioScriptProcessor = processor;
}