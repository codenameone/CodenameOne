// firebase_auth credentials and providers, and google_sign_in, headless: the
// credentials carry what they were built from, and every interactive sign-in fails
// with the plugin's own error rather than hanging.
import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';

Future<void> main() async {
  final email = EmailAuthProvider.credential(email: 'a@b.c', password: 'pw');
  print('${email.providerId} ${email.signInMethod}');
  final google = GoogleAuthProvider.credential(idToken: 'gid', accessToken: 'gat');
  print('${google.providerId} ${google.idToken} ${google.accessToken} ${google.rawNonce}');
  final apple = OAuthProvider('apple.com').credential(idToken: 'aid', rawNonce: 'raw');
  print('${apple.providerId} ${apple.idToken} ${apple.rawNonce} ${apple.accessToken}');
  final viaApple = AppleAuthProvider.credentialWithIDToken('tok', 'n', null);
  print('${viaApple.signInMethod} ${viaApple.rawNonce}');
  print(GoogleAuthProvider.PROVIDER_ID + ' ' + AppleAuthProvider.PROVIDER_ID + ' ' + EmailAuthProvider.PROVIDER_ID);
  final provider = GoogleAuthProvider()..addScope('drive');
  print('${provider.providerId} ${provider.scopes}');

  try {
    await FirebaseAuth.instance.signInWithProvider(OAuthProvider('microsoft.com'));
  } on FirebaseAuthException catch (e) {
    print('oauth ${e.code}');
  }
  try {
    await FirebaseAuth.instance.signInWithProvider(AppleAuthProvider());
  } on FirebaseAuthException catch (e) {
    print('apple ${e.code}');
  }
  try {
    await FirebaseAuth.instance.signInWithCredential(OAuthProvider('x.com').credential());
  } on FirebaseAuthException catch (e) {
    print('empty ${e.code}');
  }

  final signIn = GoogleSignIn(scopes: ['drive'], clientId: '123-abc.apps.googleusercontent.com');
  print(signIn.scopes);
  print('${signIn.currentUser} ${await signIn.isSignedIn()} ${await signIn.signInSilently()}');
  try {
    await signIn.signIn();
  } on PlatformException catch (e) {
    print('google ${e.code}');
  }
  await signIn.signOut();
  print('out ${signIn.currentUser}');
}
