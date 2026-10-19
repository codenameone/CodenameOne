// package:firebase_core, firebase_auth, firebase_analytics and firebase_messaging.
// Auth talks to the Identity Toolkit REST API with the apiKey from
// Firebase.initializeApp (email/password and anonymous accounts). Analytics logs,
// and forwards screen views to AnalyticsService. Messaging reads the device push key
// and delivers foreground pushes through onMessage.

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseOptions')
class FirebaseOptions {
  external FirebaseOptions({required String apiKey, required String appId, required String messagingSenderId,
      required String projectId, String? authDomain, String? databaseURL, String? storageBucket,
      String? measurementId, String? trackingId, String? deepLinkURLScheme, String? androidClientId,
      String? iosClientId, String? iosBundleId, String? appGroupId});
  external String get apiKey;
  external String get appId;
  external String get messagingSenderId;
  external String get projectId;
  external String? get authDomain;
  external String? get storageBucket;
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseApp')
class FirebaseApp {
  external String get name;
  external FirebaseOptions get options;
  external bool get isAutomaticDataCollectionEnabled;
}

@JavaName('com.codename1.flutter.plugins.firebase.Firebase')
class Firebase {
  external static List<FirebaseApp> get apps;
  external static Future<FirebaseApp> initializeApp({String? name, FirebaseOptions? options});
  external static FirebaseApp app([String name]);
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseException')
class FirebaseException implements Exception {
  external String get plugin;
  external String get code;
  external String? get message;
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseAuthException')
class FirebaseAuthException extends FirebaseException {
}

@JavaName('com.codename1.flutter.plugins.firebase.User')
class User {
  external String get uid;
  external String? get email;
  external String? get displayName;
  external String? get photoURL;
  external String? get phoneNumber;
  external bool get isAnonymous;
  external bool get emailVerified;
  external String? get refreshToken;
  external Future<String?> getIdToken([bool forceRefresh]);
  external Future<void> updateDisplayName(String? displayName);
  external Future<void> updatePhotoURL(String? photoURL);
  external Future<void> reload();
  external Future<void> delete();
  external Future<void> sendEmailVerification([Object? actionCodeSettings]);
}

@JavaName('com.codename1.flutter.plugins.firebase.UserCredential')
class UserCredential {
  external User? get user;
  external Object? get additionalUserInfo;
  external Object? get credential;
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseAuth')
class FirebaseAuth {
  external static FirebaseAuth get instance;
  external static FirebaseAuth instanceFor({required FirebaseApp app, Object? persistence});
  external User? get currentUser;
  external Stream<User?> authStateChanges();
  external Stream<User?> idTokenChanges();
  external Stream<User?> userChanges();
  external Future<UserCredential> signInWithEmailAndPassword({required String email, required String password});
  external Future<UserCredential> createUserWithEmailAndPassword({required String email,
      required String password});
  external Future<UserCredential> signInAnonymously();
  external Future<UserCredential> signInWithCredential(Object credential);
  external Future<UserCredential> signInWithProvider(Object provider);
  external Future<void> signOut();
  external Future<void> sendPasswordResetEmail({required String email, Object? actionCodeSettings});
  external Future<void> setLanguageCode(String? languageCode);
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseAnalytics')
class FirebaseAnalytics {
  external static FirebaseAnalytics get instance;
  external Future<void> logEvent({required String name, Map<String, Object>? parameters, Object? callOptions});
  external Future<void> logScreenView({String? screenClass, String? screenName, Map<String, Object>? parameters,
      Object? callOptions});
  external Future<void> logLogin({String? loginMethod, Map<String, Object>? parameters, Object? callOptions});
  external Future<void> logSignUp({required String signUpMethod, Map<String, Object>? parameters,
      Object? callOptions});
  external Future<void> logAppOpen({Map<String, Object>? parameters, Object? callOptions});
  external Future<void> setUserId({String? id, Object? callOptions});
  external Future<void> setUserProperty({required String name, required String? value, Object? callOptions});
  external Future<void> setAnalyticsCollectionEnabled(bool enabled);
  external Future<void> setCurrentScreen({required String? screenName, String screenClassOverride,
      Object? callOptions});
}

@JavaName('com.codename1.flutter.plugins.firebase.AuthorizationStatus')
enum AuthorizationStatus { authorized, denied, notDetermined, provisional }

@JavaName('com.codename1.flutter.plugins.firebase.NotificationSettings')
class NotificationSettings {
  external AuthorizationStatus get authorizationStatus;
}

@JavaName('com.codename1.flutter.plugins.firebase.RemoteNotification')
class RemoteNotification {
  external String? get title;
  external String? get body;
}

@JavaName('com.codename1.flutter.plugins.firebase.RemoteMessage')
class RemoteMessage {
  external String? get messageId;
  external Map<String, dynamic> get data;
  external RemoteNotification? get notification;
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseMessaging')
class FirebaseMessaging {
  external static FirebaseMessaging get instance;
  external static Stream<RemoteMessage> get onMessage;
  external static Stream<RemoteMessage> get onMessageOpenedApp;
  external static void onBackgroundMessage(Future<void> Function(RemoteMessage message) handler);
  external Future<String?> getToken({String? vapidKey});
  external Future<String?> getAPNSToken();
  external Stream<String> get onTokenRefresh;
  external Future<void> deleteToken();
  external Future<NotificationSettings> requestPermission({bool alert, bool announcement, bool badge,
      bool carPlay, bool criticalAlert, bool provisional, bool sound});
  external Future<NotificationSettings> getNotificationSettings();
  external Future<RemoteMessage?> getInitialMessage();
  external Future<void> subscribeToTopic(String topic);
  external Future<void> unsubscribeFromTopic(String topic);
  external Future<void> setForegroundNotificationPresentationOptions({bool alert, bool badge, bool sound});
  external Future<void> setAutoInitEnabled(bool enabled);
}
