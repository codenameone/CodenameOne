import 'package:flutter/material.dart';

import 'style_lab_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Club Hub')),
      body: Center(
        child: TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const StyleLabScreen())),
          child: const Text('Welcome'),
        ),
      ),
    );
  }
}
