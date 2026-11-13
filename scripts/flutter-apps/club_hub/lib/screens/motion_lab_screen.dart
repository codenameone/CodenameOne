import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Exercises every implicit animation, plus the text, icon and context APIs
/// that come with them.
class MotionLabScreen extends StatefulWidget {
  const MotionLabScreen({super.key});

  @override
  State<MotionLabScreen> createState() => _MotionLabScreenState();
}

class _MotionLabScreenState extends State<MotionLabScreen> {
  bool _on = false;
  int _ends = 0;

  static const _dur = Duration(milliseconds: 400);

  void _ended() => setState(() => _ends++);

  @override
  Widget build(BuildContext context) {
    final icons = IconTheme.of(context);
    final dynamicColor = CupertinoColors.systemBlue;
    final overlay = SystemUiOverlayStyle.dark.copyWith(statusBarColor: Colors.transparent);
    final theme = context.getInheritedWidgetOfExactType<IconTheme>();
    const span = TextSpan(text: 'span', semanticsLabel: 'label');
    final InlineSpan inline = span;
    return Scaffold(
      appBar: AppBar(title: Text('Motion lab ($_ends)')),
      floatingActionButton: FloatingActionButton(
        onPressed: () => setState(() => _on = !_on),
        child: const Icon(Icons.play_arrow),
      ),
      body: ListView(
        physics: const PageScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          AnimatedOpacity(
            opacity: _on ? 1 : 0.2,
            duration: _dur,
            curve: Curves.easeInOut,
            onEnd: _ended,
            child: const Text('fades'),
          ),
          AnimatedContainer(
            duration: _dur,
            width: _on ? 200 : 100,
            height: 40,
            decoration: BoxDecoration(
              color: _on ? Colors.teal : Colors.orange,
              borderRadius: BorderRadius.circular(_on ? 20 : 4),
            ),
          ),
          AnimatedPadding(
            padding: EdgeInsets.only(left: _on ? 48 : 0),
            duration: _dur,
            child: const Text('slides in'),
          ),
          AnimatedAlign(
            alignment: _on ? Alignment.centerRight : Alignment.centerLeft,
            duration: _dur,
            child: const FlutterLogo(),
          ),
          SizedBox(
            height: 60,
            child: Stack(children: [
              AnimatedPositioned(
                duration: _dur,
                left: _on ? 120 : 0,
                top: 10,
                child: const Icon(Icons.directions_run),
              ),
            ]),
          ),
          AnimatedRotation(
            turns: _on ? 0.5 : 0,
            duration: _dur,
            child: const Icon(Icons.refresh),
          ),
          AnimatedSlide(
            offset: _on ? const Offset(0.5, 0) : Offset.zero,
            duration: _dur,
            child: const Text('nudged'),
          ),
          AnimatedDefaultTextStyle(
            style: TextStyle(
              fontSize: _on ? 24 : 14,
              color: _on ? Colors.indigo : Colors.grey,
              fontVariations: const [FontVariation('wght', 600)],
              leadingDistribution: TextLeadingDistribution.even,
              fontFamilyFallback: const ['sans-serif'],
            ),
            duration: _dur,
            child: const Text('grows'),
          ),
          AnimatedSwitcher(
            duration: _dur,
            child: Text(_on ? 'on' : 'off', key: ValueKey<bool>(_on)),
          ),
          TweenAnimationBuilder<double>(
            tween: Tween<double>(begin: 0, end: _on ? 1 : 0),
            duration: _dur,
            builder: (context, value, child) =>
                LinearProgressIndicator(value: value),
          ),
          Text('icon weight ${icons.weight ?? 400}, fill ${icons.fill ?? 0}, '
              'grade ${icons.grade ?? 0}, opsz ${icons.opticalSize ?? 24}, '
              'shadows ${icons.shadows?.length ?? 0}, '
              'mirrors ${Icons.arrow_back.matchTextDirection}'),
          Text('dynamic ${dynamicColor.color} / ${dynamicColor.darkColor}, '
              'overlay $overlay, theme ${theme != null}, '
              'inline ${span.semanticsLabel} ${inline.style == null}, '
              'zero ${Size.zero.width}'),
        ],
      ),
    );
  }
}
