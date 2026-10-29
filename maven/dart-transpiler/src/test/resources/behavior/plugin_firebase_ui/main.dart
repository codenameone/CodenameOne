// FirebaseUI's screens and providers, headless: they are made, configured and carry
// their provider ids; App Check's AppleProvider enum shares its name with FirebaseUI's
// Apple button; each library sees the one its imports name.
import 'fui_app_check.dart';
import 'package:firebase_auth/firebase_auth.dart' hide EmailAuthProvider;
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_ui_auth/firebase_ui_auth.dart';
import 'package:firebase_ui_oauth_apple/firebase_ui_oauth_apple.dart';
import 'package:firebase_ui_oauth_google/firebase_ui_oauth_google.dart';
import 'package:flutter/material.dart';

Future<void> main() async {
  await Firebase.initializeApp(
      options: const FirebaseOptions(apiKey: 'k', appId: 'a', messagingSenderId: 'm', projectId: 'p'));
  final providers = <Object>[EmailAuthProvider(), GoogleProvider(clientId: 'id.apps.googleusercontent.com'),
      AppleProvider()];
  FirebaseUIAuth.configureProviders(providers);
  print(FirebaseUIAuth.providersFor(Firebase.app()).length);
  print('${(providers[1] as GoogleProvider).providerId} ${(providers[2] as AppleProvider).providerId}');
  print(appCheckProviderName());
  final screen = SignInScreen(
    providers: providers,
    actions: [
      AuthStateChangeAction<SignedIn>((context, state) => print('signed in ${state.user?.uid}')),
      AuthStateChangeAction<UserCreated>((context, state) => print('created')),
      SignedOutAction((context) => print('out')),
    ],
    headerBuilder: (context, constraints, shrink) => const Text('header'),
  );
  print(screen is StatefulWidget);
  final profile = ProfileScreen(actions: [SignedOutAction((context) {})], children: const [Text('x')]);
  print(profile is StatelessWidget);
  print(OAuthProviderButton(provider: providers[1]) is Widget);
}
