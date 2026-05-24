package com.codename1.samples;

import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.SystemBrowser;
import com.codename1.social.AppleSignIn;
import com.codename1.social.AppleSignInCallback;
import com.codename1.social.AppleSignInResult;
import com.codename1.social.Auth0Connect;
import com.codename1.social.FacebookConnect;
import com.codename1.social.FirebaseAuth;
import com.codename1.social.GoogleConnect;
import com.codename1.social.MicrosoftConnect;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.util.SuccessCallback;

import static com.codename1.ui.CN.updateNetworkThreadCount;

/**
 * Universal sign-in demo: one button per identity provider, all driven through
 * the modern OidcClient / system-browser stack.
 *
 * <p>To run, replace the placeholder credentials below with your own and either
 * launch via the Codename One simulator or run on device. All providers can
 * be exercised on every platform that has a system browser; the native
 * AppleSignIn path additionally requires iOS 13+.
 */
public class UniversalSignInDemo {

    // -------------------------------------------------------------------
    // CREDENTIALS -- replace with your own. Treat client IDs as public,
    // client secrets and Firebase keys as semi-public (anyone running the
    // simulator can read them; production apps should fetch tenant-specific
    // values from a backend at runtime).
    // -------------------------------------------------------------------

    private static final String GOOGLE_CLIENT_ID =
            "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com";
    private static final String GOOGLE_REDIRECT_URI =
            "com.codename1.samples.signin:/oauth2redirect";

    private static final String MICROSOFT_CLIENT_ID =
            "YOUR_ENTRA_CLIENT_ID";
    private static final String MICROSOFT_REDIRECT_URI =
            "com.codename1.samples.signin:/oauth2redirect";

    private static final String FACEBOOK_APP_ID =
            "YOUR_FB_APP_ID";
    private static final String FACEBOOK_REDIRECT_URI =
            "https://example.com/auth/facebook/callback";

    private static final String AUTH0_DOMAIN =
            "your-tenant.us.auth0.com";
    private static final String AUTH0_CLIENT_ID =
            "YOUR_AUTH0_CLIENT_ID";
    private static final String AUTH0_REDIRECT_URI =
            "com.codename1.samples.signin:/oauth2redirect";

    private static final String APPLE_SERVICE_ID =
            "com.codename1.samples.signin.web";
    private static final String APPLE_REDIRECT_URI =
            "https://example.com/auth/apple/callback";

    private static final String FIREBASE_API_KEY = "YOUR_FIREBASE_WEB_API_KEY";

    /** Generic OIDC issuer -- swap for your own (Keycloak, Okta, Cognito, ...). */
    private static final String GENERIC_OIDC_ISSUER = "https://accounts.google.com";

    // -------------------------------------------------------------------

    private Form current;
    private Resources theme;
    private TextArea output;

    public void init(Object context) {
        updateNetworkThreadCount(2);
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form f = new Form("Universal Sign-In", BoxLayout.y());

        f.add(new Label("Pick a provider:"));

        f.add(button("Sign in with Apple", new Runnable() {
            public void run() { doApple(); }
        }));
        f.add(button("Sign in with Google", new Runnable() {
            public void run() { doGoogle(); }
        }));
        f.add(button("Sign in with Microsoft", new Runnable() {
            public void run() { doMicrosoft(); }
        }));
        f.add(button("Sign in with Facebook", new Runnable() {
            public void run() { doFacebook(); }
        }));
        f.add(button("Sign in with Auth0", new Runnable() {
            public void run() { doAuth0(); }
        }));
        f.add(button("Sign in with Firebase (email/password)", new Runnable() {
            public void run() { doFirebase(); }
        }));
        f.add(button("Sign in with any OIDC issuer", new Runnable() {
            public void run() { doGenericOidc(); }
        }));
        f.add(button("Clear stored tokens", new Runnable() {
            public void run() { clear(); }
        }));

        output = new TextArea(8, 60);
        output.setEditable(false);
        f.add(new Label("Result:"));
        f.add(output);

        f.add(new Label("System browser native: "
                + (SystemBrowser.isNativeAvailable()
                        ? "yes (OS sheet)"
                        : "no (BrowserWindow fallback)")));

        f.show();
        current = f;
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    // -------------------------------------------------------------------

    private Button button(String text, final Runnable r) {
        Button b = new Button(text);
        b.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
                output("Running: " + text + "...");
                r.run();
            }
        });
        return b;
    }

    private void doApple() {
        AppleSignIn.getInstance()
                .withServiceId(APPLE_SERVICE_ID)
                .withRedirectUri(APPLE_REDIRECT_URI)
                .signIn("name email", new AppleSignInCallback() {
                    public void onSuccess(AppleSignInResult result) {
                        output("Apple OK\n"
                                + "  user: " + result.getUserId() + "\n"
                                + "  email: " + result.getEmail() + "\n"
                                + "  name: " + result.getFullName() + "\n"
                                + "  identityToken: " + truncate(result.getIdentityToken()));
                    }

                    public void onError(String error) {
                        output("Apple ERROR: " + error);
                    }

                    public void onCancel() {
                        output("Apple cancelled");
                    }
                });
    }

    private void doGoogle() {
        GoogleConnect.getInstance().signIn(
                GOOGLE_CLIENT_ID, GOOGLE_REDIRECT_URI,
                "openid", "email", "profile"
        ).ready(new SuccessCallback<OidcTokens>() {
            public void onSucess(OidcTokens t) {
                output("Google OK\n  email: " + t.getEmail()
                        + "\n  sub: " + t.getSubject()
                        + "\n  access_token: " + truncate(t.getAccessToken()));
            }
        }).except(errorTo("Google"));
    }

    private void doMicrosoft() {
        MicrosoftConnect.getInstance()
                .withTenant("common")
                .signIn(
                        MICROSOFT_CLIENT_ID, MICROSOFT_REDIRECT_URI,
                        "openid", "email", "profile", "User.Read"
                ).ready(new SuccessCallback<OidcTokens>() {
                    public void onSucess(OidcTokens t) {
                        output("Microsoft OK\n  email: " + t.getEmail()
                                + "\n  sub: " + t.getSubject()
                                + "\n  access_token: " + truncate(t.getAccessToken()));
                    }
                }).except(errorTo("Microsoft"));
    }

    private void doFacebook() {
        FacebookConnect.getInstance().signIn(
                FACEBOOK_APP_ID, FACEBOOK_REDIRECT_URI,
                "public_profile", "email"
        ).ready(new SuccessCallback<OidcTokens>() {
            public void onSucess(OidcTokens t) {
                output("Facebook OK\n  access_token: " + truncate(t.getAccessToken()));
            }
        }).except(errorTo("Facebook"));
    }

    private void doAuth0() {
        Auth0Connect.getInstance()
                .withDomain(AUTH0_DOMAIN)
                .signIn(
                        AUTH0_CLIENT_ID, AUTH0_REDIRECT_URI,
                        "openid", "email", "profile"
                ).ready(new SuccessCallback<OidcTokens>() {
                    public void onSucess(OidcTokens t) {
                        output("Auth0 OK\n  email: " + t.getEmail()
                                + "\n  id_token: " + truncate(t.getIdToken()));
                    }
                }).except(errorTo("Auth0"));
    }

    private void doFirebase() {
        ToastBar.showMessage("Enter email/password in a real app -- this demo uses sample creds",
                com.codename1.ui.FontImage.MATERIAL_INFO, 3000);
        FirebaseAuth.getInstance()
                .withApiKey(FIREBASE_API_KEY)
                .signInWithEmailAndPassword("demo@example.com", "password")
                .ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
                    public void onSucess(FirebaseAuth.FirebaseUser u) {
                        if (u == null) {
                            output("Firebase: no user (provide credentials)");
                            return;
                        }
                        output("Firebase OK\n  uid: " + u.getUid()
                                + "\n  email: " + u.getEmail()
                                + "\n  id_token: " + truncate(u.getIdToken()));
                    }
                }).except(errorTo("Firebase"));
    }

    private void doGenericOidc() {
        OidcClient.discover(GENERIC_OIDC_ISSUER).ready(new SuccessCallback<OidcClient>() {
            public void onSucess(OidcClient client) {
                client.setClientId(GOOGLE_CLIENT_ID)
                        .setRedirectUri(GOOGLE_REDIRECT_URI)
                        .setScopes("openid", "email", "profile");
                client.authorize().ready(new SuccessCallback<OidcTokens>() {
                    public void onSucess(OidcTokens t) {
                        output("Generic OIDC OK\n  issuer: " + GENERIC_OIDC_ISSUER
                                + "\n  email: " + t.getEmail()
                                + "\n  sub: " + t.getSubject());
                    }
                }).except(errorTo("Generic OIDC"));
            }
        }).except(errorTo("Generic OIDC discovery"));
    }

    private void clear() {
        AppleSignIn.getInstance().doLogout();
        GoogleConnect.getInstance().doLogout();
        MicrosoftConnect.getInstance().doLogout();
        FacebookConnect.getInstance().doLogout();
        Auth0Connect.getInstance().doLogout();
        FirebaseAuth.getInstance().signOut();
        output("Cleared all stored tokens");
    }

    private SuccessCallback<Throwable> errorTo(final String tag) {
        return new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                String reason = err.getMessage();
                if (err instanceof OidcException) {
                    OidcException oe = (OidcException) err;
                    reason = oe.getError() + ": " + oe.getErrorDescription();
                }
                output(tag + " ERROR: " + reason);
            }
        };
    }

    private void output(final String s) {
        if (CN.isEdt()) {
            output.setText(s);
        } else {
            CN.callSerially(new Runnable() {
                public void run() {
                    output.setText(s);
                }
            });
        }
    }

    private static String truncate(String token) {
        if (token == null) return "(none)";
        int len = token.length();
        if (len <= 32) return token;
        return token.substring(0, 16) + "..." + token.substring(len - 8);
    }
}
