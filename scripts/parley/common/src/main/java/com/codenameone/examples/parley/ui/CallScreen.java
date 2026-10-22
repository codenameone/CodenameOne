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
package com.codenameone.examples.parley.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.VideoView;
import com.codenameone.examples.parley.chat.Calls;

/// The call screen: who, how it is going, the other side's picture with
/// this phone's own in a corner, and the buttons.
///
/// Rebuilt from the call's state on every change, which is a handful of
/// times a call. The video views are kept across rebuilds, as each is a
/// native view the track draws into.
public final class CallScreen {
    private static Form form;
    private static Form previous;
    private static Calls.Call showing;
    private static VideoView remote;
    private static MediaTrack remoteTrack;
    private static VideoView local;
    private static MediaTrack localTrack;

    private CallScreen() {
    }

    public static void show(Calls.Call call) {
        if (form == null || showing != call) {
            Form current = CN.getCurrentForm();
            if (current != form || previous == null) {
                previous = current;
            }
            showing = call;
            form = new Form(new BorderLayout());
            form.setName("Call");
            form.setUIID("PlCall");
            form.getContentPane().setUIID("PlCall");
            // Nothing under the notch or the home indicator.
            form.getContentPane().setSafeArea(true);
            form.getToolbar().hideToolbar();
            form.setBackCommand(Lang.tr("Back"), null, e -> {
                if (showing != null && Calls.current() == showing) {
                    if (Calls.RINGING.equals(showing.getState())) {
                        Calls.decline();
                    }
                    // Otherwise the call goes on behind the app.
                    goBack();
                }
            });
            build(call);
            form.show();
        } else {
            build(call);
        }
    }

    /// The call changed state: shown again if it is the one on screen.
    public static void refresh(Calls.Call call) {
        if (form != null && showing == call) {
            build(call);
        }
    }

    /// The call is over.
    public static void closed(Calls.Call call) {
        if (showing != call) {
            return;
        }
        releaseViews();
        showing = null;
        Form was = form;
        form = null;
        if (was != null && CN.getCurrentForm() == was) {
            goBack();
        }
    }

    private static void goBack() {
        Form to = previous;
        previous = null;
        if (to != null) {
            to.showBack();
        } else {
            Nav.home();
        }
    }

    /// The caller's initials, large, as the call screen shows them.
    private static Label callAvatar(String name) {
        Label avatar = Ui.avatar(name);
        avatar.setUIID("PlCallAvatar");
        return avatar;
    }

    private static void build(Calls.Call call) {
        form.removeAll();
        Container stage = new Container(new LayeredLayout());
        stage.setName("callStage");

        MediaTrack remoteVideo = call.getRemoteVideo();
        if (remoteVideo != remoteTrack) {
            if (remote != null) {
                remote.release();
            }
            remote = remoteVideo == null ? null : new VideoView(remoteVideo, false);
            remoteTrack = remoteVideo;
        }
        MediaTrack localVideo = call.isCameraOff() ? null : call.getLocalVideo();
        if (localVideo != localTrack) {
            if (local != null) {
                local.release();
            }
            local = localVideo == null ? null : new VideoView(localVideo, true);
            localTrack = localVideo;
        }

        Container who = BoxLayout.encloseY(centred(callAvatar(call.getPeerName())),
                named(new Label(call.getPeerName(), "PlCallName"), "callName"),
                named(new Label(Lang.tr(stateText(call)), "PlCallState"), "callState"));
        if (remote != null) {
            remote.setName("remoteVideo");
            stage.add(remote);
            Container top = new Container(new BorderLayout());
            top.add(BorderLayout.NORTH, who);
            stage.add(top);
        } else {
            stage.add(BorderLayout.centerAbsolute(who));
        }
        if (local != null) {
            local.setName("localVideo");
            local.setUIID("PlLocalVideo");
            local.setPreferredW(CN.convertToPixels(28));
            local.setPreferredH(CN.convertToPixels(38));
            Container corner = new Container(new BorderLayout());
            corner.add(BorderLayout.EAST, BoxLayout.encloseY(local));
            stage.add(corner);
        }
        form.add(BorderLayout.CENTER, stage);
        form.add(BorderLayout.SOUTH, buttons(call));
        form.revalidate();
    }

    private static Component buttons(Calls.Call call) {
        Container bar = new Container(new FlowLayout(Component.CENTER));
        bar.setName("callButtons");
        if (!call.isOutgoing() && Calls.RINGING.equals(call.getState())) {
            bar.add(button(FontImage.MATERIAL_CALL_END, "decline", "PlCallHangUp",
                    Calls::decline));
            bar.add(button(call.isVideo() ? FontImage.MATERIAL_VIDEOCAM : FontImage.MATERIAL_CALL,
                    "answer", "PlCallAnswer", Calls::answer));
            return bar;
        }
        bar.add(button(call.isMuted() ? FontImage.MATERIAL_MIC_OFF : FontImage.MATERIAL_MIC,
                "mute", call.isMuted() ? "PlCallButtonOn" : "PlCallButton", Calls::toggleMute));
        bar.add(button(FontImage.MATERIAL_VOLUME_UP, "speaker",
                call.isSpeaker() ? "PlCallButtonOn" : "PlCallButton", Calls::toggleSpeaker));
        if (call.isVideo()) {
            bar.add(button(call.isCameraOff() ? FontImage.MATERIAL_VIDEOCAM_OFF
                    : FontImage.MATERIAL_VIDEOCAM, "camera",
                    call.isCameraOff() ? "PlCallButtonOn" : "PlCallButton", Calls::toggleCamera));
            bar.add(button(FontImage.MATERIAL_FLIP_CAMERA_IOS, "flipCamera", "PlCallButton",
                    Calls::flipCamera));
        }
        bar.add(button(FontImage.MATERIAL_CALL_END, "hangUp", "PlCallHangUp", Calls::hangUp));
        return bar;
    }

    private static Button button(char icon, String name, String uiid, final Runnable run) {
        Button button = new Button("", uiid);
        button.setName(name);
        FontImage.setMaterialIcon(button, icon, 5.5f);
        button.addActionListener(e -> run.run());
        return button;
    }

    private static String stateText(Calls.Call call) {
        String state = call.getState();
        if (Calls.RINGING.equals(state)) {
            return call.isVideo() ? "Incoming video call" : "Incoming call";
        }
        if (Calls.CALLING.equals(state)) {
            return "Calling...";
        }
        if (Calls.CONNECTING.equals(state)) {
            return "Connecting...";
        }
        if (Calls.CONNECTED.equals(state)) {
            return "End-to-end encrypted";
        }
        return "Call ended";
    }

    private static Component centred(Component c) {
        return FlowLayout.encloseCenter(c);
    }

    private static Label named(Label label, String name) {
        label.setName(name);
        return label;
    }

    private static void releaseViews() {
        if (remote != null) {
            remote.release();
        }
        if (local != null) {
            local.release();
        }
        remote = null;
        local = null;
        remoteTrack = null;
        localTrack = null;
    }
}
