/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebaseui;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.plugins.firebase.FirebaseAuth;
import com.codename1.flutter.plugins.firebase.UserCredential;

import dart.runtime.Funcs;

/**
 * firebase_ui_oauth's {@code OAuthProviderButton}: one provider's sign-in button, which runs
 * {@code signInWithProvider}. Inside a {@link SignInScreen} the screen's actions run on the
 * outcome; a button placed on its own runs none, because the runtime has no
 * {@code FirebaseUIActions} scope to find them in. {@code variant} and {@code action} are
 * accepted and not applied.
 */
public class OAuthProviderButton extends StatelessWidget {
    private Object provider;
    private FirebaseAuth auth;
    Funcs.VoidFunc1<UserCredential> onSignedIn;
    Funcs.VoidFunc1<Object> onError;

    public void provider(Object v) {
        provider = v;
    }

    public void auth(FirebaseAuth v) {
        auth = v;
    }

    public void action(Object v) {
    }

    public void variant(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        final UiOAuthProvider p = provider instanceof UiOAuthProvider ? (UiOAuthProvider) provider : null;
        return Ui.button(p == null ? "Sign in" : p.label(), false, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (p == null) {
                    return;
                }
                FirebaseAuth a = auth == null ? FirebaseAuth.instance : auth;
                a.signInWithProvider(p).then(new Funcs.VoidFunc1<UserCredential>() {
                    @Override
                    public void call(UserCredential c) {
                        if (onSignedIn != null) {
                            onSignedIn.call(c);
                        }
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object err) {
                        if (onError != null) {
                            onError.call(err);
                        }
                    }
                });
            }
        });
    }
}
