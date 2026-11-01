// package:google_sign_in (the version 6 API), over GoogleConnect's OIDC sign-in in the
// system browser. signInSilently answers this run's account only.

@JavaName('com.codename1.flutter.plugins.googlesignin.GoogleSignIn')
class GoogleSignIn {
  external GoogleSignIn({List<String> scopes, Object? signInOption, String? hostedDomain, String? clientId,
      String? serverClientId, bool forceCodeForRefreshToken});
  external List<String> get scopes;
  external GoogleSignInAccount? get currentUser;
  external Stream<GoogleSignInAccount?> get onCurrentUserChanged;
  external Future<GoogleSignInAccount?> signIn();
  external Future<GoogleSignInAccount?> signInSilently({bool suppressErrors, bool reAuthenticate});
  external Future<bool> isSignedIn();
  external Future<GoogleSignInAccount?> signOut();
  external Future<GoogleSignInAccount?> disconnect();
  external Future<bool> requestScopes(List<String> scopes);
  external Future<bool> canAccessScopes(List<String> scopes, {String? accessToken});
}

@JavaName('com.codename1.flutter.plugins.googlesignin.GoogleSignInAccount')
class GoogleSignInAccount {
  external String get id;
  external String get email;
  external String? get displayName;
  external String? get photoUrl;
  external String? get serverAuthCode;
  external Future<GoogleSignInAuthentication> get authentication;
  external Future<Map<String, String>> get authHeaders;
  external Future<void> clearAuthCache();
}

@JavaName('com.codename1.flutter.plugins.googlesignin.GoogleSignInAuthentication')
class GoogleSignInAuthentication {
  external String? get idToken;
  external String? get accessToken;
  external String? get serverAuthCode;
}
