import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() {
  runApp(const ClubHubApp());
}

class ClubHubApp extends StatelessWidget {
  const ClubHubApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Club Hub',
      theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
      home: const HomeScreen(),
    );
  }
}
