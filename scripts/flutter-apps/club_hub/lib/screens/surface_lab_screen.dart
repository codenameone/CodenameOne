import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Pictures from files and memory with their builders, pinch to zoom, a box
/// that grows with its content, and the system-chrome and keyboard values.
class SurfaceLabScreen extends StatefulWidget {
  const SurfaceLabScreen({super.key});

  @override
  State<SurfaceLabScreen> createState() => _SurfaceLabScreenState();
}

class _SurfaceLabScreenState extends State<SurfaceLabScreen> {
  double _zoom = 1;
  double _baseZoom = 1;
  double _turn = 0;
  bool _expanded = false;
  int _swaps = 0;
  final TapGestureRecognizer _link = TapGestureRecognizer();

  @override
  void initState() {
    super.initState();
    _link.onTap = () => setState(() => _swaps++);
    SystemChrome.setSystemUIOverlayStyle(
        SystemUiOverlayStyle.light.copyWith(statusBarColor: Colors.indigo));
  }

  @override
  void dispose() {
    final GestureRecognizer recognizer = _link;
    recognizer.dispose();
    super.dispose();
  }

  Path _diamond() {
    final p = Path()
      ..moveTo(0, -1)
      ..lineTo(1, 0)
      ..lineTo(0, 1)
      ..lineTo(-1, 0)
      ..close();
    final m = Matrix4.identity()..scaleByDouble(20, 20, 1, 1);
    return p.transform(m.storage);
  }

  @override
  Widget build(BuildContext context) {
    final missing = File('/no/such/photo.png');
    return Scaffold(
      appBar: AppBar(title: const Text('Surface lab')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Image.file(missing, width: 80, height: 80),
          Image(
            image: FileImage(missing),
            width: 80,
            height: 80,
            errorBuilder: (context, error, stack) => const Icon(Icons.broken_image),
            loadingBuilder: (context, child, progress) =>
                progress == null ? child : const CircularProgressIndicator(),
            frameBuilder: (context, child, frame, sync) =>
                AnimatedOpacity(opacity: frame == null ? 0 : 1, duration: const Duration(milliseconds: 200), child: child),
          ),
          Image.memory(Uint8List(0), width: 40, height: 40,
              ),
          GestureDetector(
            onScaleStart: (d) => _baseZoom = _zoom,
            onScaleUpdate: (d) => setState(() {
              _zoom = (_baseZoom * d.scale).clamp(0.5, 4.0);
              _turn = d.rotation;
            }),
            onScaleEnd: (d) => setState(() => _baseZoom = _zoom),
            child: Transform.rotate(
              angle: _turn,
              child: Transform.scale(
                scale: _zoom,
                child: const FlutterLogo(size: 96),
              ),
            ),
          ),
          Text('zoom ${_zoom.toStringAsFixed(2)}', style: const TextStyle(fontSize: kDefaultFontSize)),
          TextButton(
            onPressed: () => setState(() => _expanded = !_expanded),
            child: const Text('Toggle size'),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
            child: Container(
              color: Colors.amber,
              height: _expanded ? 160 : 40,
            ),
          ),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            transitionBuilder: (child, animation) => ScaleTransition(scale: animation, child: child),
            layoutBuilder: (current, previous) => Stack(
              alignment: Alignment.center,
              children: [...previous, if (current != null) current],
            ),
            child: Text('swap $_swaps', key: ValueKey<int>(_swaps)),
          ),
          RichText(
            text: TextSpan(text: 'tap to swap', recognizer: _link,
                style: const TextStyle(color: Colors.blue)),
          ),
          const TextField(
            keyboardType: TextInputType.numberWithOptions(signed: true, decimal: true),
            decoration: InputDecoration(labelText: 'Amount'),
          ),
          CustomPaint(size: const Size(48, 48), painter: _PathPainter(_diamond())),
        ],
      ),
    );
  }
}

class _PathPainter extends CustomPainter {
  _PathPainter(this.path);

  final Path path;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.translate(size.width / 2, size.height / 2);
    canvas.drawPath(path, Paint()..color = Colors.teal);
  }

  @override
  bool shouldRepaint(_PathPainter oldDelegate) => oldDelegate.path != path;
}
