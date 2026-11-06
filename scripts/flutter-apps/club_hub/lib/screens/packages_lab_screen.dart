import 'package:confetti/confetti.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:simple_gesture_detector/simple_gesture_detector.dart';
import 'package:table_calendar/table_calendar.dart';

/// Uses one widget from each third-party UI package the app depends on.
class PackagesLabScreen extends StatefulWidget {
  const PackagesLabScreen({super.key});

  @override
  State<PackagesLabScreen> createState() => _PackagesLabScreenState();
}

class _PackagesLabScreenState extends State<PackagesLabScreen> {
  final _confetti = ConfettiController(duration: const Duration(seconds: 1));
  DateTime _focused = DateTime(2024, 3, 9);
  String _swipe = 'none';
  Box<int>? _box;

  @override
  void initState() {
    super.initState();
    Hive.initFlutter().then((_) => Hive.openBox<int>('visits')).then((b) {
      if (mounted) setState(() => _box = b);
    });
  }

  @override
  void dispose() {
    _confetti.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final box = _box;
    return Scaffold(
      appBar: AppBar(title: const Text('Packages lab')),
      body: ListView(
        children: [
          const MarkdownBody(data: '# Club rules\n\n*Be kind* and **book early**.'),
          ConfettiWidget(confettiController: _confetti, blastDirectionality: BlastDirectionality.explosive),
          IconButton(icon: const FaIcon(FontAwesomeIcons.trophy), onPressed: _confetti.play),
          TableCalendar<void>(
            firstDay: DateTime(2024, 1, 1),
            lastDay: DateTime(2024, 12, 31),
            focusedDay: _focused,
            onDaySelected: (selected, focused) => setState(() => _focused = focused),
          ),
          SimpleGestureDetector(
            onHorizontalSwipe: (direction) => setState(() => _swipe = direction.name),
            child: Container(height: 60, alignment: Alignment.center, child: Text('Swiped: $_swipe')),
          ),
          if (box != null)
            ValueListenableBuilder<Box<int>>(
              valueListenable: box.listenable(),
              builder: (context, b, _) => TextButton(
                onPressed: () => b.put('count', (b.get('count') ?? 0) + 1),
                child: Text('Visits: ${b.get('count') ?? 0}'),
              ),
            ),
        ],
      ),
    );
  }
}
