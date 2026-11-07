// Benchmark gauge in Flutter: same workload as slint/qt -- 800x480, background
// ring, sweeping arc, inner disc, needle and a bold 28px percentage, redrawn
// every frame (vsync paced, 60 fps cap on flutter-pi). Prints the CN1BENCH lines.
import 'dart:io';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

int cpuTicks() {
  final s = File('/proc/self/stat').readAsStringSync();
  final f = s.substring(s.lastIndexOf(')') + 2).split(' ');
  return int.parse(f[11]) + int.parse(f[12]);
}

int rssKb() => int.parse(File('/proc/self/statm').readAsStringSync().split(' ')[1]) * 4;

void main() {
  runApp(const Directionality(textDirection: TextDirection.ltr, child: Bench()));
}

class Bench extends StatefulWidget {
  const Bench({super.key});
  @override
  State<Bench> createState() => _BenchState();
}

class _BenchState extends State<Bench> with SingleTickerProviderStateMixin {
  late final Ticker ticker;
  double angle = 0;
  int frames = 0;
  final sw = Stopwatch()..start();
  late final int cpu0 = cpuTicks();
  final secs = int.tryParse(Platform.environment['CN1_BENCH_SECONDS'] ?? '') ?? 10;
  bool done = false;

  @override
  void initState() {
    super.initState();
    cpu0;
    SchedulerBinding.instance.addPostFrameCallback((_) {
      stdout.writeln('CN1BENCH first_frame_epoch_ms=${DateTime.now().millisecondsSinceEpoch}');
    });
    ticker = createTicker((_) {
      if (done) return;
      frames++;
      if (sw.elapsedMilliseconds >= secs * 1000) {
        done = true;
        final ms = sw.elapsedMilliseconds;
        stdout.writeln('CN1BENCH fps=${frames * 1000 ~/ ms} frames=$frames rss_kb=${rssKb()} '
            'cpu_pct=${(cpuTicks() - cpu0) * 1000 ~/ ms}');
        exit(0);
      }
      setState(() => angle += 0.04);
    })..start();
  }

  @override
  Widget build(BuildContext context) =>
      CustomPaint(size: const Size(800, 480), painter: GaugePainter(angle));
}

class GaugePainter extends CustomPainter {
  GaugePainter(this.angle);
  final double angle;
  @override
  void paint(Canvas c, Size size) {
    const cx = 400.0, cy = 240.0, r = 230.0;
    c.drawRect(Offset.zero & const Size(800, 480), Paint()..color = const Color(0xFF101820));
    c.drawCircle(const Offset(cx, cy), r, Paint()..color = const Color(0xFF2A3A4A));
    final sweep = ((math.sin(angle) + 1) * 135).toInt();
    final a0 = 225 * math.pi / 180, a1 = (225 - sweep) * math.pi / 180;
    final rect = Rect.fromCircle(center: const Offset(cx, cy), radius: r);
    c.drawPath(
        Path()
          ..moveTo(cx, cy)
          ..arcTo(rect, -a0, a0 - a1, false)
          ..close(),
        Paint()..color = const Color(0xFF33AAFF));
    c.drawCircle(const Offset(cx, cy), 206, Paint()..color = const Color(0xFF101820));
    c.drawLine(const Offset(cx, cy), Offset(cx + 200 * math.cos(a1), cy - 200 * math.sin(a1)),
        Paint()..color = const Color(0xFFFFFFFF)..strokeWidth = 1);
    final tp = TextPainter(
        text: TextSpan(text: '${sweep * 100 ~/ 270}%',
            style: const TextStyle(color: Color(0xFFFFFFFF), fontSize: 28, fontWeight: FontWeight.w700)),
        textDirection: TextDirection.ltr, textAlign: TextAlign.center)
      ..layout(minWidth: 800, maxWidth: 800);
    c.save();
    c.translate(0, cy + 76 - 28);
    tp.paint(c, Offset((800 - tp.width) / 2, 0));
    c.restore();
  }

  @override
  bool shouldRepaint(GaugePainter old) => old.angle != angle;
}
