// cn1:package firebase_ui_oauth_apple
// package:firebase_ui_auth, firebase_ui_oauth, firebase_ui_oauth_google and
// firebase_ui_oauth_apple: SignInScreen and ProfileScreen built from the runtime's
// Material widgets, over firebase_auth. EmailAuthProvider is firebase_auth's own class,
// which also stands for firebase_ui_auth's (an app imports one and hides the other).

@JavaName('com.codename1.flutter.plugins.firebaseui.AuthAction')
enum AuthAction { signIn, signUp, link, none }

@JavaName('com.codename1.flutter.plugins.firebaseui.AuthState')
class AuthState {
}

@JavaName('com.codename1.flutter.plugins.firebaseui.SignedIn')
class SignedIn extends AuthState {
  external User? get user;
}

@JavaName('com.codename1.flutter.plugins.firebaseui.UserCreated')
class UserCreated extends AuthState {
  external UserCredential get credential;
}

@JavaName('com.codename1.flutter.plugins.firebaseui.AuthFailed')
class AuthFailed extends AuthState {
  external Exception get exception;
}

@JavaName('com.codename1.flutter.plugins.firebaseui.AuthStateChangeAction')
class AuthStateChangeAction<T extends AuthState> {
  external AuthStateChangeAction(void Function(BuildContext context, T state) callback);
}

@JavaName('com.codename1.flutter.plugins.firebaseui.SignedOutAction')
class SignedOutAction {
  external SignedOutAction(void Function(BuildContext context) callback);
}

@JavaName('com.codename1.flutter.plugins.firebaseui.FirebaseUIAuth')
class FirebaseUIAuth {
  external static void configureProviders(List<Object> configs, {FirebaseApp? app});
  external static List<Object> providersFor(FirebaseApp app);
}

@JavaName('com.codename1.flutter.plugins.firebaseui.SignInScreen')
class SignInScreen extends StatefulWidget {
  @Unapplied('sideBuilder, breakpoint, styles, headerMaxExtent, oauthButtonVariant, showPasswordVisibilityToggle, email')
  external SignInScreen({Key? key, List<Object>? providers, List<Object> actions, FirebaseAuth? auth,
      Widget Function(BuildContext context, BoxConstraints constraints, double shrinkOffset)? headerBuilder,
      Widget Function(BuildContext context, AuthAction action)? subtitleBuilder,
      Widget Function(BuildContext context, AuthAction action)? footerBuilder,
      Widget Function(BuildContext context, BoxConstraints constraints)? sideBuilder,
      bool showAuthActionSwitch, AuthAction? initialAuthAction, double breakpoint, Object? styles,
      double? headerMaxExtent, Object? oauthButtonVariant, bool showPasswordVisibilityToggle, String? email,
      bool? resizeToAvoidBottomInset});
}

@JavaName('com.codename1.flutter.plugins.firebaseui.ProfileScreen')
class ProfileScreen extends StatelessWidget {
  @Unapplied('providers, avatar, avatarSize, showDeleteConfirmationDialog')
  external ProfileScreen({Key? key, List<Object> actions, List<Widget> children, PreferredSizeWidget? appBar,
      FirebaseAuth? auth, List<Object>? providers, Widget? avatar, double? avatarSize,
      bool showDeleteConfirmationDialog});
}

@JavaName('com.codename1.flutter.plugins.firebaseui.OAuthProviderButton')
class OAuthProviderButton extends StatelessWidget {
  @Unapplied('action, variant')
  external OAuthProviderButton({Key? key, required Object provider, FirebaseAuth? auth, AuthAction? action,
      Object? variant});
}

@JavaName('com.codename1.flutter.plugins.firebaseuigoogle.GoogleProvider')
class GoogleProvider {
  @Unapplied('redirectUri, iOSPreferPlist')
  external GoogleProvider({required String clientId, String? redirectUri, List<String> scopes,
      bool iOSPreferPlist});
  external String get providerId;
}

@JavaName('com.codename1.flutter.plugins.firebaseuiapple.AppleProvider')
class AppleProvider {
  external AppleProvider({Object? scopes});
  external String get providerId;
}
