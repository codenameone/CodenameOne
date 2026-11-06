import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// Exercises form fields, the Material 3 navigation bar, tables and the
/// layout helpers of the port.
class FormsLabScreen extends StatefulWidget {
  const FormsLabScreen({super.key});

  @override
  State<FormsLabScreen> createState() => _FormsLabScreenState();
}

enum Court { indoor, outdoor, clay }

class _FormsLabScreenState extends State<FormsLabScreen> {
  String? _level = 'Beginner';
  Court _court = Court.indoor;
  bool _newsletter = true;
  bool _bigBadge = false;
  bool _showMap = false;
  int _tab = 0;
  Set<Court> _courts = {Court.indoor};

  static const _levels = ['Beginner', 'Intermediate', 'Advanced'];

  @override
  Widget build(BuildContext context) {
    final members = NumberFormat.compact().format(12345);
    final fees = NumberFormat.compactSimpleCurrency().format(1234567);
    final when = DateFormat.yMMMEd().format(DateTime(2024, 3, 9));
    return Theme(
      data: ThemeData(navigationBarTheme: const NavigationBarThemeData(height: 72)),
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Forms lab'),
          flexibleSpace: const FlexibleSpaceBar(title: Text('')),
        ),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _tab,
          onDestinationSelected: (i) => setState(() => _tab = i),
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.event_outlined), label: 'Events'),
            NavigationDestination(icon: Icon(Icons.person_outline), label: 'Me'),
          ],
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('$members members, $fees raised, next: $when'),
            DropdownButton<String>(
              value: _level,
              isExpanded: true,
              items: [for (final l in _levels) DropdownMenuItem(value: l, child: Text(l))],
              onChanged: (v) => setState(() => _level = v),
            ),
            DropdownButtonHideUnderline(
              child: DropdownButton<Court>(
                value: _court,
                items: Court.values.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
                onChanged: (v) => setState(() => _court = v ?? _court),
              ),
            ),
            DropdownButtonFormField<String>(
              initialValue: _level,
              decoration: const InputDecoration(labelText: 'Level', helperText: 'Pick one'),
              items: [for (final l in _levels) DropdownMenuItem(value: l, child: Text(l))],
              onChanged: (v) => setState(() => _level = v),
            ),
            const InputDecorator(
              decoration: InputDecoration(labelText: 'Club', border: OutlineInputBorder()),
              child: Text('Riverside Racquets'),
            ),
            CheckboxListTile(
              value: _newsletter,
              title: const Text('Newsletter'),
              onChanged: (v) => setState(() => _newsletter = v ?? false),
            ),
            SegmentedButton<Court>(
              segments: const [
                ButtonSegment(value: Court.indoor, label: Text('Indoor')),
                ButtonSegment(value: Court.outdoor, label: Text('Outdoor')),
                ButtonSegment(value: Court.clay, label: Text('Clay')),
              ],
              selected: _courts,
              multiSelectionEnabled: true,
              onSelectionChanged: (s) => setState(() => _courts = s),
            ),
            Table(
              border: TableBorder.all(color: CupertinoColors.systemGrey4),
              columnWidths: const {0: FlexColumnWidth(2), 1: FlexColumnWidth(1)},
              defaultVerticalAlignment: TableCellVerticalAlignment.middle,
              children: const [
                TableRow(children: [Text('Court'), Text('Slots')]),
                TableRow(children: [Text('Centre'), TableCell(child: Text('4'))]),
              ],
            ),
            ListBody(children: const [Text('Rules'), Text('Bring water.')]),
            KeyedSubtree(key: const ValueKey('badge'), child: AnimatedScale(
              scale: _bigBadge ? 1.5 : 1,
              duration: const Duration(milliseconds: 200),
              child: IconButton(
                icon: const Icon(Icons.star),
                onPressed: () => setState(() => _bigBadge = !_bigBadge),
              ),
            )),
            AnimatedCrossFade(
              firstChild: TextButton(onPressed: () => setState(() => _showMap = true), child: const Text('Show map')),
              secondChild: Container(height: 80, color: CupertinoColors.systemGrey6, child: const Text('Map')),
              crossFadeState: _showMap ? CrossFadeState.showSecond : CrossFadeState.showFirst,
              duration: const Duration(milliseconds: 300),
            ),
            const ClipPath(child: Text('Clipped')),
            Text('Link', style: TextStyle(color: CupertinoColors.link)),
          ],
        ),
      ),
    );
  }
}
