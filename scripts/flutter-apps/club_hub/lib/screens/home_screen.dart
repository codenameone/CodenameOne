import 'package:flutter/material.dart';

import 'effects_lab_screen.dart';
import 'motion_lab_screen.dart';
import 'forms_lab_screen.dart';
import 'packages_lab_screen.dart';
import 'style_lab_screen.dart';
import 'surface_lab_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Club Hub')),
      persistentFooterButtons: [
        TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const SurfaceLabScreen())),
          child: const Text('Surface'),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const MotionLabScreen())),
          child: const Text('Motion'),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const EffectsLabScreen())),
          child: const Text('Effects'),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const FormsLabScreen())),
          child: const Text('Forms'),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const PackagesLabScreen())),
          child: const Text('Packages'),
        ),
      ],
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
