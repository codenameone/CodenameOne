import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// Exercises the implicit animations, shaped clips, shader masks, blur filters
/// and the Material picker dialogs.
class EffectsLabScreen extends StatefulWidget {
  const EffectsLabScreen({super.key});

  @override
  State<EffectsLabScreen> createState() => _EffectsLabScreenState();
}

class _TicketClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final path = Path();
    path.moveTo(0, 0);
    path.lineTo(size.width, 0);
    path.lineTo(size.width, size.height * 0.7);
    path.lineTo(size.width / 2, size.height);
    path.lineTo(0, size.height * 0.7);
    path.close();
    return path;
  }

  @override
  bool shouldReclip(covariant CustomClipper<Path> oldClipper) => false;
}

class _EffectsLabScreenState extends State<EffectsLabScreen> {
  bool _big = false;
  bool _second = false;
  String _picked = 'nothing picked';

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 1),
      helpText: 'Match day',
    );
    if (!mounted) return;
    setState(() => _picked = date == null ? 'cancelled' : 'date ${date.year}-${date.month}-${date.day}');
  }

  Future<void> _pickTime() async {
    final time = await showTimePicker(context: context, initialTime: const TimeOfDay(hour: 18, minute: 30));
    if (!mounted) return;
    setState(() => _picked = time == null ? 'cancelled' : 'time ${time.hour}:${time.minute}');
  }

  Future<void> _pickRange() async {
    final now = DateTime.now();
    final range = await showDateRangePicker(
      context: context,
      firstDate: now,
      lastDate: DateTime(now.year + 1),
      saveText: 'Book',
    );
    if (!mounted) return;
    setState(() => _picked = range == null ? 'cancelled' : 'range of ${range.duration.inDays} days');
  }

  @override
  Widget build(BuildContext context) {
    final corners = BorderRadius.circular(12).copyWith(topLeft: const Radius.circular(2));
    return Scaffold(
      appBar: AppBar(title: const Text('Effects')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Corner radius ${corners.topLeft.x} / ${corners.bottomRight.x}'),
          GestureDetector(
            onTap: () => setState(() => _big = !_big),
            child: AnimatedScale(
              scale: _big ? 1.4 : 1.0,
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeOut,
              child: const Icon(Icons.sports_tennis, size: 48),
            ),
          ),
          TextButton(
            onPressed: () => setState(() => _second = !_second),
            child: const Text('Cross fade'),
          ),
          AnimatedCrossFade(
            firstChild: const Text('Singles'),
            secondChild: const Text('Doubles'),
            crossFadeState: _second ? CrossFadeState.showSecond : CrossFadeState.showFirst,
            duration: const Duration(milliseconds: 250),
          ),
          ClipPath(
            clipper: _TicketClipper(),
            child: Container(height: 80, color: Colors.teal),
          ),
          ShaderMask(
            shaderCallback: (bounds) => const LinearGradient(
              colors: [Colors.purple, Colors.orange],
            ).createShader(bounds),
            blendMode: BlendMode.srcIn,
            child: const Text('Club Hub', style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
          ),
          ImageFiltered(
            imageFilter: ui.ImageFilter.blur(sigmaX: 2, sigmaY: 2),
            child: const Text('Spoiler: the final score'),
          ),
          Stack(
            children: [
              Container(height: 60, color: Colors.amber),
              Positioned.fill(
                child: BackdropFilter(
                  filter: ui.ImageFilter.blur(sigmaX: 4, sigmaY: 4),
                  child: const Center(child: Text('Frosted')),
                ),
              ),
            ],
          ),
          Text(_picked),
          Row(
            children: [
              TextButton(onPressed: _pickDate, child: const Text('Date')),
              TextButton(onPressed: _pickTime, child: const Text('Time')),
              TextButton(onPressed: _pickRange, child: const Text('Range')),
            ],
          ),
        ],
      ),
    );
  }
}
