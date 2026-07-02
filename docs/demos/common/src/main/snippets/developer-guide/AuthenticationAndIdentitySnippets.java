// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::authentication-and-identity-java-001[]
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.SuccessCallback;

OidcClient.discover("https://accounts.google.com").ready(new SuccessCallback<OidcClient>() {
    public void onSucess(OidcClient client) {
        client.setClientId("YOUR_CLIENT_ID")
              .setRedirectUri("com.example.app:/oauth2redirect")
              .setScopes("openid", "email", "profile");
        client.authorize().ready(new SuccessCallback<OidcTokens>() {
            public void onSucess(OidcTokens tokens) {
                // tokens.getAccessToken() -- bearer for API calls
                // tokens.getIdToken()      -- JWT with user identity claims
                // tokens.getEmail()        -- convenience accessor
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                System.out.println("Sign-in failed: " + err.getMessage());
            }
        });
    }
});
// end::authentication-and-identity-java-001[]

// tag::authentication-and-identity-java-002[]
client.refreshIfExpired(60).ready(new SuccessCallback<OidcTokens>() {
    public void onSucess(OidcTokens tokens) {
        if (tokens == null) {
            // No saved tokens, or they expired and we have no refresh token.
            client.authorize();
        } else {
            // Reuse `tokens.getAccessToken()` -- still valid (refreshed if needed).
        }
    }
});
// end::authentication-and-identity-java-002[]

// tag::authentication-and-identity-java-003[]
AppleSignIn.getInstance().signIn("name email", new AppleSignInCallback() {
    public void onSuccess(AppleSignInResult result) {
        // result.getIdentityToken()  -- JWT to verify on your server
        // result.getUserId()         -- stable opaque user id, use as PK
        // result.getEmail()          -- may be a real or relay address
    }
    public void onError(String err) { }
    public void onCancel()          { }
});
// end::authentication-and-identity-java-003[]

// tag::authentication-and-identity-java-004[]
AppleSignIn.getInstance()
    .withServiceId("com.example.appleweb")
    .withRedirectUri("https://example.com/auth/apple/callback")
    .withClientSecret(serverGeneratedJwt)
    .signIn("name email", callback);
// end::authentication-and-identity-java-004[]

// tag::authentication-and-identity-java-005[]
GoogleConnect.getInstance().signIn(
    "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
    "com.example.app:/oauth2redirect",
    "openid", "email", "profile"
).ready(new SuccessCallback<OidcTokens>() {
    public void onSucess(OidcTokens t) {
        String email = t.getEmail();
        String idToken = t.getIdToken();
    }
});
// end::authentication-and-identity-java-005[]

// tag::authentication-and-identity-java-006[]
FacebookConnect.getInstance().signIn(
    "FB_APP_ID",
    "com.example.app:/oauth2redirect",
    "public_profile", "email"
).ready(new SuccessCallback<OidcTokens>() {
    public void onSucess(OidcTokens t) {
        String accessToken = t.getAccessToken(); // ID token will be null
        // call https://graph.facebook.com/me with the access token for user data
    }
});
// end::authentication-and-identity-java-006[]

// tag::authentication-and-identity-java-007[]
MicrosoftConnect.getInstance()
    .withTenant("common")
    .signIn(
        "YOUR_ENTRA_CLIENT_ID",
        "com.example.app:/oauth2redirect",
        "openid", "email", "profile", "User.Read"
    ).ready(new SuccessCallback<OidcTokens>() {
        public void onSucess(OidcTokens t) {
            // t.getAccessToken() is a Microsoft Graph access token
        }
    });
// end::authentication-and-identity-java-007[]

// tag::authentication-and-identity-java-008[]
Auth0Connect.getInstance()
    .withDomain("dev-xyz.us.auth0.com")
    .withAudience("https://api.example.com")        // optional
    .signIn(
        "YOUR_AUTH0_CLIENT_ID",
        "com.example.app:/oauth2redirect",
        "openid", "email", "profile"
    ).ready(new SuccessCallback<OidcTokens>() {
        public void onSucess(OidcTokens t) {
            String idToken = t.getIdToken();
        }
    });
// end::authentication-and-identity-java-008[]

// tag::authentication-and-identity-java-009[]
FirebaseAuth.getInstance()
    .withApiKey("YOUR_FIREBASE_WEB_API_KEY")
    .signInWithEmailAndPassword("a@b.com", "hunter2")
    .ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
        public void onSucess(FirebaseAuth.FirebaseUser u) {
            String uid = u.getUid();
            String firebaseIdToken = u.getIdToken();
        }
    });
// end::authentication-and-identity-java-009[]

// tag::authentication-and-identity-java-010[]
GoogleConnect.getInstance().signIn(..., "openid", "email")
    .ready(new SuccessCallback<OidcTokens>() {
        public void onSucess(OidcTokens g) {
            FirebaseAuth.getInstance().signInWithIdpIdToken(g.getIdToken(), "google.com")
                .ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
                    public void onSucess(FirebaseAuth.FirebaseUser u) {
                        // Firebase user is now signed in.
                    }
                });
        }
    });
// end::authentication-and-identity-java-010[]

// tag::authentication-and-identity-java-011[]
FirebaseAuth fa = FirebaseAuth.getInstance().withApiKey(KEY);
if (!fa.isSignedIn()) {
    fa.refresh().ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
        public void onSucess(FirebaseAuth.FirebaseUser u) {
            // u is null if no refresh token was stored
        }
    });
}
// end::authentication-and-identity-java-011[]

// tag::authentication-and-identity-java-012[]
import com.codename1.io.webauthn.PublicKeyCredential;
import com.codename1.io.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.io.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.io.webauthn.WebAuthnClient;
import com.codename1.io.webauthn.WebAuthnException;
import com.codename1.util.SuccessCallback;

// Registration -- enroll a new passkey
String challengeJson = myServer.startPasskeyRegistration(currentUserId); // PublicKeyCredentialCreationOptionsJSON
PublicKeyCredentialCreationOptions opts =
        PublicKeyCredentialCreationOptions.fromJson(challengeJson);

WebAuthnClient.getInstance().create(opts)
        .ready(new SuccessCallback<PublicKeyCredential>() {
            public void onSucess(PublicKeyCredential cred) {
                myServer.finishPasskeyRegistration(cred.toJson()); // server verifies + persists
            }
        })
        .except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                if (err instanceof WebAuthnException) {
                    String code = ((WebAuthnException) err).getError();
                    // "NotAllowedError" -- user dismissed the sheet
                    // "not_supported"   -- platform lacks a passkey impl
                    // ... etc.
                }
            }
        });

// Sign-in -- assert with an existing passkey
String requestJson = myServer.startPasskeySignIn(); // PublicKeyCredentialRequestOptionsJSON
WebAuthnClient.getInstance()
        .get(PublicKeyCredentialRequestOptions.fromJson(requestJson))
        .ready(new SuccessCallback<PublicKeyCredential>() {
            public void onSucess(PublicKeyCredential cred) {
                myServer.finishPasskeySignIn(cred.toJson()); // server verifies signature
            }
        });
// end::authentication-and-identity-java-012[]

// tag::authentication-and-identity-java-013[]
// Sign in with an existing passkey
Auth0Connect.getInstance()
    .withDomain("dev-xyz.us.auth0.com")
    .signInWithPasskey(
        "YOUR_AUTH0_CLIENT_ID",
        "Username-Password-Authentication",   // connection / realm
        "openid", "email", "profile", "offline_access")
    .ready(new SuccessCallback<OidcTokens>() {
        public void onSucess(OidcTokens t) {
            String idToken = t.getIdToken();
        }
    });

// Enroll a brand-new passkey for a new account
Auth0Connect.getInstance()
    .withDomain("dev-xyz.us.auth0.com")
    .registerPasskey(
        "YOUR_AUTH0_CLIENT_ID",
        "Username-Password-Authentication",
        "alice@example.com",
        "Alice",
        "openid", "email", "profile")
    .ready(new SuccessCallback<OidcTokens>() {
        public void onSucess(OidcTokens tokens) {
            // user is signed in; passkey is enrolled for next time
        }
    });
// end::authentication-and-identity-java-013[]

// tag::authentication-and-identity-java-014[]
FirebaseAuth fa = FirebaseAuth.getInstance().withApiKey("YOUR_FIREBASE_WEB_API_KEY");

// Sign in with a passkey already on this device
fa.signInWithPasskey().ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
    public void onSucess(FirebaseAuth.FirebaseUser u) {
        String uid = u.getUid();
        String firebaseIdToken = u.getIdToken();
    }
});

// Enroll a new passkey for the currently signed-in user
fa.signInWithEmailAndPassword(email, password)
    .ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
        public void onSucess(FirebaseAuth.FirebaseUser u) {
            fa.registerPasskey("Alice's iPhone")
                .ready(new SuccessCallback<FirebaseAuth.FirebaseUser>() {
                    public void onSucess(FirebaseAuth.FirebaseUser enrolled) {
                        // Next launch can sign in with signInWithPasskey() alone
                    }
                });
        }
    });
// end::authentication-and-identity-java-014[]

// tag::authentication-and-identity-java-015[]
Oauth2 oauth = new Oauth2(
    "https://provider.example.com/oauth2/authorize",
    "CLIENT_ID",
    "https://example.com/callback",
    "openid email",
    "https://provider.example.com/oauth2/token",
    "CLIENT_SECRET");
oauth.showAuthentication(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof AccessToken) {
            AccessToken t = (AccessToken) e.getSource();
            // ...
        }
    }
});
// end::authentication-and-identity-java-015[]

// tag::authentication-and-identity-java-016[]
OidcConfiguration cfg = OidcConfiguration.newBuilder()
    .authorizationEndpoint("https://provider.example.com/oauth2/authorize")
    .tokenEndpoint("https://provider.example.com/oauth2/token")
    .build();
OidcClient client = OidcClient.create(cfg)
    .setClientId("CLIENT_ID")
    .setClientSecret("CLIENT_SECRET")
    .setRedirectUri("https://example.com/callback")
    .setScopes("openid", "email");
client.authorize().ready(new SuccessCallback<OidcTokens>() {
    public void onSucess(OidcTokens t) {
        AccessToken legacy = t.toAccessToken();  // for code that still expects AccessToken
    }
});
// end::authentication-and-identity-java-016[]

// tag::authentication-and-identity-java-017[]
OidcClient.discover("https://provider.example.com").ready(new SuccessCallback<OidcClient>() {
    public void onSucess(OidcClient client) {
        client.setClientId("CLIENT_ID")
              .setRedirectUri("https://example.com/callback")
              .setScopes("openid", "email");
        client.authorize().ready(/* ... */);
    }
});
// end::authentication-and-identity-java-017[]
