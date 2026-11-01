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
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.InputDecoration;
import com.codename1.flutter.material.Scaffold;
import com.codename1.flutter.material.TextEditingController;
import com.codename1.flutter.material.TextField;
import com.codename1.flutter.plugins.firebase.EmailAuthProvider;
import com.codename1.flutter.plugins.firebase.FirebaseAuth;
import com.codename1.flutter.plugins.firebase.UserCredential;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.SingleChildScrollView;
import com.codename1.flutter.widgets.Text;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * firebase_ui_auth's {@code SignInScreen}, built from the runtime's Material widgets so it
 * follows the app's theme and text direction: the email form when an
 * {@code EmailAuthProvider} is among the providers (sign in, or create an account with the
 * switch), then one button per Google or Apple provider. A success fires the
 * {@code AuthStateChangeAction}s for {@link SignedIn}, or {@link UserCreated} for a new
 * account; a failure shows the message and fires those for {@link AuthFailed}.
 *
 * <p>{@code headerBuilder} is called with the screen's width as a loose constraint and a
 * shrink offset of 0, since the header does not collapse; {@code subtitleBuilder} and
 * {@code footerBuilder} are called with the current {@link AuthAction}. Accepted and not
 * applied: {@code sideBuilder}, {@code breakpoint}, {@code styles}, {@code headerMaxExtent},
 * {@code oauthButtonVariant}, {@code showPasswordVisibilityToggle} and {@code email}.
 */
public class SignInScreen extends StatefulWidget {
    DartList<Object> providers;
    DartList<Object> actions;
    FirebaseAuth auth;
    Funcs.Func3<BuildContext, Object, Double, Widget> headerBuilder;
    Funcs.Func2<BuildContext, AuthAction, Widget> subtitleBuilder;
    Funcs.Func2<BuildContext, AuthAction, Widget> footerBuilder;
    boolean showAuthActionSwitch = true;
    AuthAction initialAction = AuthAction.signIn;

    public void providers(DartList<Object> v) {
        providers = v;
    }

    public void actions(DartList<Object> v) {
        actions = v;
    }

    public void auth(FirebaseAuth v) {
        auth = v;
    }

    public void headerBuilder(Funcs.Func3<BuildContext, Object, Double, Widget> v) {
        headerBuilder = v;
    }

    public void subtitleBuilder(Funcs.Func2<BuildContext, AuthAction, Widget> v) {
        subtitleBuilder = v;
    }

    public void footerBuilder(Funcs.Func2<BuildContext, AuthAction, Widget> v) {
        footerBuilder = v;
    }

    public void showAuthActionSwitch(Boolean v) {
        showAuthActionSwitch = v == null || v.booleanValue();
    }

    public void initialAuthAction(AuthAction v) {
        if (v != null) {
            initialAction = v;
        }
    }

    public void sideBuilder(Object v) {
    }

    public void breakpoint(Object v) {
    }

    public void styles(Object v) {
    }

    public void headerMaxExtent(Object v) {
    }

    public void oauthButtonVariant(Object v) {
    }

    public void showPasswordVisibilityToggle(Object v) {
    }

    public void email(String v) {
    }

    public void resizeToAvoidBottomInset(Object v) {
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new SignInState();
    }

    static final class SignInState extends State<SignInScreen> {
        private final TextEditingController email = new TextEditingController();
        private final TextEditingController password = new TextEditingController();
        private AuthAction action;
        private String error;
        private boolean busy;

        @Override
        public void initState() {
            action = widget().initialAction;
        }

        private FirebaseAuth auth() {
            return widget().auth == null ? FirebaseAuth.instance : widget().auth;
        }

        private void signedIn(final UserCredential c, final boolean created) {
            busy = false;
            error = null;
            if (mounted()) {
                setState(new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                    }
                });
            }
            AuthStateChangeAction.fireAll(widget().actions, context(),
                    created ? new UserCreated(c) : new SignedIn(c.user()));
        }

        private void failed(final Object err) {
            busy = false;
            error = Ui.message(err);
            if (mounted()) {
                setState(new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                    }
                });
            }
            AuthStateChangeAction.fireAll(widget().actions, context(), new AuthFailed(err));
        }

        private void submit() {
            if (busy) {
                return;
            }
            busy = true;
            final boolean create = action == AuthAction.signUp;
            (create ? auth().createUserWithEmailAndPassword(email.text(), password.text())
                    : auth().signInWithEmailAndPassword(email.text(), password.text()))
                    .then(new Funcs.VoidFunc1<UserCredential>() {
                        @Override
                        public void call(UserCredential c) {
                            signedIn(c, create);
                        }
                    }, new Funcs.VoidFunc1<Object>() {
                        @Override
                        public void call(Object err) {
                            failed(err);
                        }
                    });
        }

        @Override
        public Widget build(BuildContext context) {
            final SignInScreen w = widget();
            DartList<Widget> children = new DartList<Widget>();
            if (w.headerBuilder != null) {
                Widget h = w.headerBuilder.call(context,
                        com.codename1.flutter.rendering.BoxConstraints.loose(Double.POSITIVE_INFINITY, 200),
                        Double.valueOf(0));
                if (h != null) {
                    children.add(h);
                }
            }
            Text title = new Text(action == AuthAction.signUp ? "Register" : "Sign in");
            children.add(Ui.padded(title));
            if (w.subtitleBuilder != null) {
                Widget s = w.subtitleBuilder.call(context, action);
                if (s != null) {
                    children.add(s);
                }
            }
            boolean hasEmail = false;
            DartList<Object> providers = FirebaseUIAuth.orConfigured(w.providers);
            for (int i = 0; i < providers.size(); i++) {
                hasEmail |= providers.get(i) instanceof EmailAuthProvider;
            }
            if (hasEmail) {
                children.add(Ui.padded(field("Email", email, false)));
                children.add(Ui.padded(field("Password", password, true)));
                if (error != null) {
                    children.add(Ui.padded(new Text(error)));
                }
                children.add(Ui.button(action == AuthAction.signUp ? "Register" : "Sign in", true,
                        new Funcs.VoidFunc0() {
                            @Override
                            public void call() {
                                submit();
                            }
                        }));
                if (w.showAuthActionSwitch) {
                    children.add(Ui.button(action == AuthAction.signUp ? "Already have an account? Sign in"
                            : "Don't have an account? Register", false, new Funcs.VoidFunc0() {
                                @Override
                                public void call() {
                                    setState(new Funcs.VoidFunc0() {
                                        @Override
                                        public void call() {
                                            action = action == AuthAction.signUp ? AuthAction.signIn
                                                    : AuthAction.signUp;
                                            error = null;
                                        }
                                    });
                                }
                            }));
                }
            } else if (error != null) {
                children.add(Ui.padded(new Text(error)));
            }
            for (int i = 0; i < providers.size(); i++) {
                Object p = providers.get(i);
                if (p instanceof UiOAuthProvider) {
                    OAuthProviderButton b = new OAuthProviderButton();
                    b.provider(p);
                    b.auth(w.auth);
                    b.onSignedIn = new Funcs.VoidFunc1<UserCredential>() {
                        @Override
                        public void call(UserCredential c) {
                            signedIn(c, false);
                        }
                    };
                    b.onError = new Funcs.VoidFunc1<Object>() {
                        @Override
                        public void call(Object err) {
                            failed(err);
                        }
                    };
                    children.add(b);
                }
            }
            if (w.footerBuilder != null) {
                Widget f = w.footerBuilder.call(context, action);
                if (f != null) {
                    children.add(f);
                }
            }
            Column col = new Column();
            col.children(children);
            Padding pad = new Padding();
            pad.padding(com.codename1.flutter.EdgeInsets.all(16));
            pad.child(col);
            SingleChildScrollView scroll = new SingleChildScrollView();
            scroll.child(pad);
            Scaffold s = new Scaffold();
            s.body(scroll);
            return s;
        }

        private static Widget field(String label, TextEditingController c, boolean obscure) {
            InputDecoration d = new InputDecoration();
            d.labelText(label);
            TextField f = new TextField();
            f.controller(c);
            f.decoration(d);
            f.obscureText(obscure);
            return f;
        }
    }
}
