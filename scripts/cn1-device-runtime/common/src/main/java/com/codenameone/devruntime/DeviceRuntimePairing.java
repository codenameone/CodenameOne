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

import com.codename1.impl.interp.InterpPairingSecret;
import com.codename1.io.Preferences;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
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
 * <p>The IDE prints a six-digit code; the device asks the user to type it. Both
 * ends then derive the same 256-bit secret from that code, the peer id and the
 * device id -- the secret itself never crosses the wire -- and the device
 * challenges the computer to prove it holds the same one. A computer that
 * cannot see the IDE's terminal therefore cannot pair, which is the property
 * worth having: the code proves a human is at both ends.</p>
 *
 * <h2>Every connection after that</h2>
 *
 * <p>Each connection begins with a fresh challenge, so a captured frame
 * authenticates nothing the second time. Only after the computer answers it
 * does the device raise "Approve connection from &lt;name&gt;?" with
 * <em>Once</em>, <em>Always</em> and <em>Deny</em>. Only <em>Always</em> is
 * remembered, and it is remembered per peer, so revoking one computer does not
 * disturb another. "Forget all paired computers" clears the lot -- secrets
 * included, so a forgotten computer has to be let back in by a human.</p>
 *
 * <p>The order matters: authenticate, then ask. Prompting first would let
 * anyone on the network raise a dialog on somebody's phone until they tapped
 * Approve to make it stop.</p>
 *
 * @author Shai Almog
 */
public final class DeviceRuntimePairing {
    /** Peer id -> friendly name, for every computer that has ever paired. */
    private static final String PREF_PAIRED = "cn1devruntime.paired.";

    /** Peer id -> hex shared secret established at pairing. */
    private static final String PREF_SECRET = "cn1devruntime.secret.";

    /** Peer ids the user chose to stop being asked about. */
    private static final String PREF_ALWAYS = "cn1devruntime.always.";

    /** This device's own public identifier, bound into every secret. */
    private static final String PREF_DEVICE_ID = "cn1devruntime.deviceId";

    /// Why the last pairing attempt failed, for the desktop to report.
    private static volatile String lastFailure = "pairing declined on the device";

    private DeviceRuntimePairing() {
    }

    /**
     * This device's identifier: public, stable, and bound into every derived
     * secret so a code typed on one phone cannot pair another.
     *
     * <p>Random rather than a hardware id on purpose. A device id that followed
     * the hardware would be a tracking identifier handed to every computer that
     * ever pushed, and nothing here needs one -- reinstalling the app is
     * supposed to invalidate the pairings it forgot anyway.</p>
     */
    static synchronized String deviceId() {
        String id = Preferences.get(PREF_DEVICE_ID, null);
        if (id == null || id.length() == 0) {
            id = InterpPairingSecret.hex(com.codename1.security.SecureRandom.bytes(16));
            Preferences.set(PREF_DEVICE_ID, id);
        }
        return id;
    }

    /**
     * Asks the user for the code the IDE printed.
     *
     * @return what they typed, or null if they declined or typed nothing
     */
    static String promptForCode(final String peerId, final String peerName) {
        if (!isWellFormedPeerId(peerId) || peerName == null) {
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
                result[0] = typed;
            }
        });
        return result[0];
    }

    /**
     * Whether a peer id is safe to store and to index.
     *
     * <p>The removable index is a tab-separated list, so an id containing a tab
     * would split into pieces on the way out and "forget all paired computers"
     * would delete none of them -- leaving a computer authenticated, and
     * silently approved if the user had chosen Always. Hex is what both push
     * tools generate, so requiring it costs nothing and closes the question
     * rather than escaping around it.</p>
     */
    static boolean isWellFormedPeerId(String peerId) {
        if (peerId == null || peerId.length() == 0 || peerId.length() > 64) {
            return false;
        }
        for (int i = 0; i < peerId.length(); i++) {
            char c = peerId.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    /** Records a computer as paired, with the secret both ends derived. */
    static void completePairing(String peerId, String peerName, byte[] secret) {
        Preferences.set(PREF_PAIRED + peerId, peerName);
        Preferences.set(PREF_SECRET + peerId, InterpPairingSecret.hex(secret));
        remember(peerId);
    }

    /**
     * Tells the user their code was wrong, and says so differently from a
     * denial: a wrong code is a typo to retry, a denial is a decision, and
     * reporting both the same way sends people looking for the wrong problem.
     */
    static void reportCodeMismatch() {
        lastFailure = "the code typed on the device did not match";
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Dialog.show("Device runtime", "That code did not match. "
                        + "Push again to get a new one.", "OK", null);
            }
        });
    }

    static String lastFailure() {
        return lastFailure;
    }

    /// Whether this computer has ever paired with this device.
    ///
    /// Distinct from approval: the desktop needs to tell "you have never paired
    /// with me" apart from "the person said no", because the first is
    /// recoverable by pairing again -- which is exactly what happens after the
    /// runtime is reinstalled and the device forgets while the desktop does not.
    static boolean isPaired(String peerId) {
        return secretFor(peerId) != null;
    }

    /// The secret established with a peer, or null if it has never paired.
    static byte[] secretFor(String peerId) {
        if (!isWellFormedPeerId(peerId)) {
            return null;
        }
        String hex = Preferences.get(PREF_SECRET + peerId, null);
        if (hex == null || hex.length() == 0) {
            return null;
        }
        return InterpPairingSecret.unhex(hex);
    }

    /**
     * Whether an already-authenticated computer may push right now.
     *
     * <p>Returns false for an unknown peer without prompting: an unpaired
     * computer asking for approval would train the user to approve dialogs they
     * have no way to attribute.</p>
     */
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

    /** Drops every pairing, so the next push has to go through pairing again. */
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
                // The secret above all: leaving it behind would let a forgotten
                // computer authenticate, and only the approval prompt would
                // stand between it and running code here.
                Preferences.delete(PREF_SECRET + peerId);
            }
            from = tab + 1;
        }
        Preferences.delete(PREF_PAIRED + "index");
    }

    /** Records a peer id in the removable index. */
    static void remember(String peerId) {
        String index = Preferences.get(PREF_PAIRED + "index", "");
        // Whole entries, not a substring search. Ids are variable-length hex,
        // so a peer could choose one that is a substring of an id already in
        // the index; it would then never be recorded, and "forget all paired
        // computers" would leave its secret and its Always approval in place.
        int from = 0;
        while (from <= index.length()) {
            int tab = index.indexOf('\t', from);
            if (tab < 0) {
                tab = index.length();
            }
            if (peerId.equals(index.substring(from, tab))) {
                return;
            }
            from = tab + 1;
        }
        Preferences.set(PREF_PAIRED + "index", index.length() == 0
                ? peerId : index + "\t" + peerId);
    }
}
