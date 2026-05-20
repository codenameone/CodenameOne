/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simulator backing for {@link Biometrics}. State is mutated by the
 * {@code Simulate -> Biometric Simulation} submenu in {@code JavaSEPort};
 * each {@link #authenticate(AuthenticationOptions)} call pops a small modal
 * mimicking the real OS prompt so the developer can see the trigger fire and
 * step through outcomes interactively.
 */
public final class JavaSEBiometrics extends Biometrics {

    /** Simulated outcome of the next (or current) authentication. */
    public enum SimOutcome {
        SUCCEED,
        FAIL,
        CANCEL,
        LOCKED_OUT,
        PERMANENTLY_LOCKED_OUT,
        NOT_ENROLLED,
        PASSCODE_NOT_SET
    }

    // Mutated by JavaSEPort's menu items. Volatile because the menu runs on
    // the AWT EDT and the API may be called from any CN1 thread.
    static volatile boolean simAvailable = false;
    static volatile boolean simFaceEnrolled = false;
    static volatile boolean simTouchEnrolled = false;
    static volatile boolean simIrisEnrolled = false;
    static volatile SimOutcome nextOutcome = SimOutcome.SUCCEED;

    private volatile AsyncResource<Boolean> pending;
    private volatile JDialog pendingDialog;
    private boolean buildHintsInstalled;

    JavaSEBiometrics() {
    }

    /// Mirrors the historical FingerprintScanner cn1lib pattern: the first
    /// time the application touches Biometrics in the simulator, declare
    /// the iOS Face ID usage description build hint on the project. The
    /// device builders also auto-inject what's needed (LocalAuthentication
    /// framework, USE_BIOMETRIC permission), but `NSFaceIDUsageDescription`
    /// must contain app-specific localised text that Apple rejects
    /// placeholder defaults for, so we only add it if the developer hasn't
    /// already supplied one.
    private void installBuildHintsIfNeeded() {
        if (buildHintsInstalled) {
            return;
        }
        buildHintsInstalled = true;
        Map<String, String> existing = Display.getInstance().getProjectBuildHints();
        if (existing == null) {
            return;
        }
        if (!existing.containsKey("ios.NSFaceIDUsageDescription")) {
            Display.getInstance().setProjectBuildHint(
                    "ios.NSFaceIDUsageDescription",
                    "Authenticate to securely access your account");
        }
    }

    @Override
    public boolean isSupported() {
        installBuildHintsIfNeeded();
        return simAvailable;
    }

    @Override
    public boolean canAuthenticate() {
        installBuildHintsIfNeeded();
        return simAvailable
                && (simFaceEnrolled || simTouchEnrolled || simIrisEnrolled);
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        installBuildHintsIfNeeded();
        List<BiometricType> out = new ArrayList<BiometricType>();
        if (!simAvailable) {
            return out;
        }
        if (simTouchEnrolled) {
            out.add(BiometricType.FINGERPRINT);
        }
        if (simFaceEnrolled) {
            out.add(BiometricType.FACE);
        }
        if (simIrisEnrolled) {
            out.add(BiometricType.IRIS);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> authenticate(AuthenticationOptions opts) {
        installBuildHintsIfNeeded();
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (!simAvailable) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Simulator: Biometric Simulation -> Available is unchecked"));
            return result;
        }
        if (!simFaceEnrolled && !simTouchEnrolled && !simIrisEnrolled) {
            result.error(new BiometricException(BiometricError.NOT_ENROLLED,
                    "Simulator: no biometric modality enrolled"));
            return result;
        }
        pending = result;

        final String reason = opts == null || opts.getReason() == null
                ? "Authenticate"
                : opts.getReason();
        final String title = opts == null || opts.getTitle() == null
                ? "Biometric Authentication"
                : opts.getTitle();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showPromptDialog(title, reason, result);
            }
        });
        return result;
    }

    private void showPromptDialog(String title, String reason, final AsyncResource<Boolean> result) {
        final JDialog dlg = new JDialog((java.awt.Frame) null, title, true);
        dlg.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.add(new JLabel("<html><body style='width:260px'>" + escapeHtml(reason)
                + "<br><br><i>Simulator: next outcome = " + nextOutcome.name() + "</i></body></html>"),
                BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton authenticate = new JButton("Authenticate");
        buttons.add(cancel);
        buttons.add(authenticate);
        content.add(buttons, BorderLayout.SOUTH);

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dlg.dispose();
                completePending(result, SimOutcome.CANCEL);
            }
        });
        authenticate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dlg.dispose();
                completePending(result, nextOutcome);
            }
        });
        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        pendingDialog = dlg;
        dlg.setVisible(true);
    }

    private void completePending(final AsyncResource<Boolean> result, final SimOutcome outcome) {
        pendingDialog = null;
        if (pending != result) {
            // Already cancelled by another caller.
            return;
        }
        pending = null;
        // Hop to the EDT (CN1's EDT, not Swing's) before completing so the
        // callback runs on the same thread the app expects.
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                if (result.isDone()) {
                    return;
                }
                switch (outcome) {
                    case SUCCEED:
                        result.complete(Boolean.TRUE);
                        break;
                    case FAIL:
                        result.error(new BiometricException(BiometricError.AUTHENTICATION_FAILED,
                                "Simulator: simulated authentication failure"));
                        break;
                    case CANCEL:
                        result.error(new BiometricException(BiometricError.USER_CANCELED,
                                "Simulator: user cancelled"));
                        break;
                    case LOCKED_OUT:
                        result.error(new BiometricException(BiometricError.LOCKED_OUT,
                                "Simulator: locked out"));
                        break;
                    case PERMANENTLY_LOCKED_OUT:
                        result.error(new BiometricException(BiometricError.PERMANENTLY_LOCKED_OUT,
                                "Simulator: permanently locked out"));
                        break;
                    case NOT_ENROLLED:
                        result.error(new BiometricException(BiometricError.NOT_ENROLLED,
                                "Simulator: no biometric enrolled"));
                        break;
                    case PASSCODE_NOT_SET:
                        result.error(new BiometricException(BiometricError.PASSCODE_NOT_SET,
                                "Simulator: passcode not set"));
                        break;
                    default:
                        result.error(new BiometricException(BiometricError.UNKNOWN));
                }
            }
        });
    }

    @Override
    public boolean stopAuthentication() {
        final AsyncResource<Boolean> r = pending;
        if (r == null) {
            return false;
        }
        final JDialog dlg = pendingDialog;
        if (dlg != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    dlg.dispose();
                }
            });
        }
        completePending(r, SimOutcome.CANCEL);
        return true;
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
