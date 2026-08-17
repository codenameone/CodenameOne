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
package com.codenameone.devruntime;

import com.codename1.interp.InterpPairingDigest;
import com.codename1.io.Preferences;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Container;
import com.codename1.ui.Label;

/**
 * Decides whether a computer is allowed to push a program to this device.
 *
 * <h2>Pairing</h2>
 *
 * <p>The IDE prints a six-digit code and sends the device a peer id plus a
 * digest binding that id to the code. The device asks the user to type the
 * code, recomputes the digest, and pairs only if it matches. A computer that
 * cannot see the IDE's terminal therefore cannot pair, which is the property
 * worth having: the code proves a human is at both ends.</p>
 *
 * <h2>Every connection after that</h2>
 *
 * <p>A paired computer still does not get to push silently. Each connection
 * raises "Approve connection from &lt;name&gt;?" with <em>Once</em>,
 * <em>Always</em> and <em>Deny</em>. Only <em>Always</em> is remembered, and it
 * is remembered per peer, so revoking one computer does not disturb another.
 * "Forget all paired computers" clears the lot.</p>
 *
 * <h2>What the digest is and is not</h2>
 *
 * <p>The digest is a plain string hash, not a MAC, and the channel is not
 * encrypted. That is honest for what this is: a loopback link reachable only
 * over USB debugging or the simulator's own loopback, where an attacker who
 * could observe the exchange already has code execution on the machine at one
 * end. It is not adequate for a listener exposed to a network, and the
 * transport must not be moved onto one without replacing this with a real key
 * exchange.</p>
 *
 * @author Shai Almog
 */
public final class DeviceRuntimePairing {
    /** Peer id -> friendly name, for every computer that has ever paired. */
    private static final String PREF_PAIRED = "cn1devruntime.paired.";

    /** Peer ids the user chose to stop being asked about. */
    private static final String PREF_ALWAYS = "cn1devruntime.always.";

    private DeviceRuntimePairing() {
    }

    /**
     * Completes a pairing request.
     *
     * @return the friendly name if the user typed the right code, else null
     */
    static String pair(final String peerId, final String peerName, final String digest) {
        if (peerId == null || peerId.length() == 0 || peerName == null || digest == null) {
            return null;
        }
        final String[] result = new String[1];
        lastFailure = "pairing declined on the device";
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                final TextField code = new TextField("", "000000", 6, TextField.NUMERIC);
                code.getAllStyles().setFont(com.codename1.ui.Font.createSystemFont(
                        com.codename1.ui.Font.FACE_MONOSPACE,
                        com.codename1.ui.Font.STYLE_BOLD,
                        com.codename1.ui.Font.SIZE_LARGE));
                Container body = new Container(BoxLayout.y());
                SpanLabel who = new SpanLabel("Pair with \"" + peerName + "\"?");
                body.add(who)
                    .add(new Label("Type the code shown in the IDE:"))
                    .add(code);
                // Real buttons rather than dialog commands: a Command renders as
                // a line of text at the dialog's edge, which on a phone is too
                // small a target to hit reliably.
                final Dialog prompt = new Dialog("Device runtime");
                prompt.setLayout(new BorderLayout());
                Button pairBtn = new Button("Pair");
                Button denyBtn = new Button("Deny");
                final boolean[] pairPressed = new boolean[1];
                pairBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        pairPressed[0] = true;
                        prompt.dispose();
                    }
                });
                denyBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        prompt.dispose();
                    }
                });
                Container actions = new Container(new com.codename1.ui.layouts.GridLayout(2));
                actions.add(pairBtn).add(denyBtn);
                prompt.add(BorderLayout.CENTER, body);
                prompt.add(BorderLayout.SOUTH, actions);
                prompt.show();
                if (!pairPressed[0]) {
                    lastFailure = "you chose Deny on the device";
                    return;
                }
                String typed = code.getText() == null ? "" : code.getText().trim();
                if (typed.length() == 0) {
                    lastFailure = "no code was typed on the device";
                    Dialog.show("Device runtime", "Type the six digits shown in the IDE.",
                            "OK", null);
                    return;
                }
                if (!digest.equals(InterpPairingDigest.of(typed, peerId))) {
                    // Said differently from a denial: a wrong code is a typo to
                    // retry, and a denial is a decision. Reporting both the same
                    // way sends people looking for the wrong problem.
                    lastFailure = "the code typed on the device did not match";
                    Dialog.show("Device runtime", "That code did not match. "
                            + "Push again to get a new one.", "OK", null);
                    return;
                }
                Preferences.set(PREF_PAIRED + peerId, peerName);
                result[0] = peerName;
            }
        });
        return result[0];
    }

    /**
     * Whether an already-paired computer may push right now.
     *
     * <p>Returns false for an unknown peer without prompting: an unpaired
     * computer asking for approval would train the user to approve dialogs they
     * have no way to attribute.</p>
     */
    /// Whether this computer has ever paired with this device.
    ///
    /// Distinct from approval: the desktop needs to tell "you have never paired
    /// with me" apart from "the person said no", because the first is
    /// recoverable by pairing again -- which is exactly what happens after the
    /// runtime is reinstalled and the device forgets while the desktop does not.
    /// Why the last pairing attempt failed, for the desktop to report.
    private static volatile String lastFailure = "pairing declined on the device";

    static String lastFailure() {
        return lastFailure;
    }

    static boolean isPaired(String peerId) {
        return peerId != null && Preferences.get(PREF_PAIRED + peerId, null) != null;
    }

    static boolean approve(final String peerId) {
        final String name = peerId == null ? null : Preferences.get(PREF_PAIRED + peerId, null);
        if (name == null) {
            return false;
        }
        if (Preferences.get(PREF_ALWAYS + peerId, false)) {
            return true;
        }
        final boolean[] allowed = new boolean[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                // Buttons rather than commands, for the same reason as the
                // pairing prompt: a command is a line of text at the dialog's
                // edge and too small a target on a phone.
                final Dialog prompt = new Dialog("Device runtime");
                prompt.setLayout(new BorderLayout());
                Container body = new Container(BoxLayout.y());
                body.add(new SpanLabel("Approve connection from \"" + name + "\"?"));

                Button once = new Button("Once");
                Button always = new Button("Always");
                Button deny = new Button("Deny");
                final int[] choice = new int[1];
                once.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        choice[0] = 1;
                        prompt.dispose();
                    }
                });
                always.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        choice[0] = 2;
                        prompt.dispose();
                    }
                });
                deny.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        prompt.dispose();
                    }
                });
                Container actions = new Container(new com.codename1.ui.layouts.GridLayout(3));
                actions.add(once).add(always).add(deny);
                prompt.add(BorderLayout.CENTER, body);
                prompt.add(BorderLayout.SOUTH, actions);
                prompt.show();

                if (choice[0] == 2) {
                    Preferences.set(PREF_ALWAYS + peerId, true);
                }
                allowed[0] = choice[0] != 0;
            }
        });
        return allowed[0];
    }

    /** Drops every pairing, so the next push has to go through pair() again. */
    public static void forgetAll() {
        // Preferences has no key enumeration, so the peer ids have to be
        // recorded to be removable. The index is a single key holding a
        // tab-separated list, written whenever a peer pairs.
        String index = Preferences.get(PREF_PAIRED + "index", "");
        int from = 0;
        while (from < index.length()) {
            int tab = index.indexOf('\t', from);
            if (tab < 0) {
                tab = index.length();
            }
            String peerId = index.substring(from, tab);
            if (peerId.length() > 0) {
                Preferences.delete(PREF_PAIRED + peerId);
                Preferences.delete(PREF_ALWAYS + peerId);
            }
            from = tab + 1;
        }
        Preferences.delete(PREF_PAIRED + "index");
    }

    /** Records a peer id in the removable index. */
    static void remember(String peerId) {
        String index = Preferences.get(PREF_PAIRED + "index", "");
        if (index.indexOf(peerId) < 0) {
            Preferences.set(PREF_PAIRED + "index", index.length() == 0
                    ? peerId : index + "\t" + peerId);
        }
    }
}
