import 'dart:async';

import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'screens/notes_screen.dart';
import 'screens/sign_in_screen.dart';
import 'services/notifier.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: const FirebaseOptions(
      apiKey: 'demo-api-key',
      appId: '1:1234:android:abcd',
      messagingSenderId: '1234',
      projectId: 'field-notes-demo',
    ),
  );
  await Notifier.instance.init();
  runApp(const FieldNotesApp());
}

class FieldNotesApp extends StatelessWidget {
  const FieldNotesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Field Notes',
      theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
      home: StreamBuilder<User?>(
        stream: FirebaseAuth.instance.authStateChanges(),
        builder: (context, snapshot) {
          final user = snapshot.data;
          if (user == null) {
            return const SignInScreen();
          }
          FirebaseAnalytics.instance.logScreenView(screenName: 'notes');
          return NotesScreen(user: user);
        },
      ),
    );
  }
}
