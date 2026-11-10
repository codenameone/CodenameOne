import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

/// Exercises the styling, theming, dialog and lifecycle APIs of the port.
class StyleLabScreen extends StatefulWidget {
  const StyleLabScreen({super.key});

  @override
  State<StyleLabScreen> createState() => _StyleLabScreenState();
}

class _StyleLabScreenState extends State<StyleLabScreen>
    with WidgetsBindingObserver {
  AppLifecycleState? _lastState;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    setState(() => _lastState = state);
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached ||
        state == AppLifecycleState.inactive ||
        state == AppLifecycleState.hidden) {
      debugPrint('backgrounded');
    } else if (state == AppLifecycleState.resumed) {
      debugPrint('resumed');
    }
  }

  String get _mode => kReleaseMode
      ? 'release'
      : kProfileMode
          ? 'profile'
          : kDebugMode
              ? 'debug'
              : 'unknown';

  Future<void> _openDialogs(BuildContext context) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      useRootNavigator: true,
      barrierColor: Colors.black54,
      barrierLabel: 'Dismiss',
      useSafeArea: true,
      builder: (ctx) => Dialog(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Text('Dialog', style: Theme.of(ctx).textTheme.titleMedium),
        ),
      ),
    );
    if (!context.mounted) return;
    await showAdaptiveDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog.adaptive(title: const Text('Adaptive')),
    );
    if (!context.mounted) return;
    await showGeneralDialog<void>(
      context: context,
      barrierDismissible: true,
      barrierLabel: 'General',
      pageBuilder: (ctx, a1, a2) => const Center(child: Text('General')),
    );
    if (!context.mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) => const SizedBox(height: 120, child: Text('Sheet')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final base = Colors.teal;
    final faded = base.withValues(alpha: 0.5);
    final tinted = faded.withValues(red: 0.2, green: 0.4, blue: 0.6);
    final channels = '${tinted.a.toStringAsFixed(2)} ${tinted.r.toStringAsFixed(2)} '
        '${tinted.g.toStringAsFixed(2)} ${tinted.b.toStringAsFixed(2)}';
    final swatch = <Color>[
      Colors.teal.shade100,
      Colors.teal.shade200,
      Colors.teal.shade300,
      Colors.teal.shade400,
      Colors.teal.shade500,
      Colors.teal.shade600,
      Colors.teal.shade700,
      Colors.teal.shade800,
      Colors.teal.shade900,
      Colors.teal.shade50,
      Colors.primaries.first,
    ];
    final tokens = <Color>[
      scheme.surfaceContainer,
      scheme.surfaceContainerLowest,
      scheme.surfaceContainerLow,
      scheme.surfaceContainerHigh,
      scheme.surfaceContainerHighest,
      scheme.surfaceDim,
      scheme.surfaceBright,
      scheme.onSurfaceVariant,
      scheme.outlineVariant,
      scheme.inverseSurface,
      scheme.onInverseSurface,
      scheme.inversePrimary,
      scheme.surfaceTint,
      scheme.scrim,
      scheme.shadow,
      scheme.tertiaryContainer,
      scheme.onTertiaryContainer,
    ];
    final padding = MediaQuery.paddingOf(context);
    final viewPadding = MediaQuery.viewPaddingOf(context);
    final insets = MediaQuery.viewInsetsOf(context);
    final size = MediaQuery.sizeOf(context);
    final scaler = MediaQuery.textScalerOf(context);
    final scaled = scaler.scale(14);
    final corner = Size.square(size.shortestSide / 4).bottomRight(Offset.zero);
    const side = BorderSide(color: Colors.teal, width: 2);
    final radius = BorderRadius.circular(8).copyWith(topLeft: Radius.zero);
    const baseStyle = TextStyle(fontSize: 14);
    final style = baseStyle.merge(const TextStyle(
      decoration: TextDecoration.underline,
      decorationColor: Colors.teal,
      decorationStyle: TextDecorationStyle.dashed,
      decorationThickness: 2,
      shadows: [Shadow(color: Colors.black26, offset: Offset(1, 1), blurRadius: 2)],
    ));
    final combined = TextDecoration.combine(
        [TextDecoration.underline, TextDecoration.lineThrough]);
    final span = TextSpan(
      text: 'Lab ',
      style: style,
      semanticsLabel: 'style lab',
      recognizer: TapGestureRecognizer()..onTap = () {},
      children: const [TextSpan(text: 'span')],
    );
    final spanInfo = '${span.text} ${span.children?.length} ${span.semanticsLabel} '
        '${span.style?.fontSize} ${span.recognizer != null}';
    final richText = Text.rich(span);
    final selectable = SelectableText.rich(span);
    final sameSpan = identical(richText.textSpan, span) && selectable.textSpan == span;
    final icon = Icons.more_horiz;
    final iconInfo = '${icon.codePoint} ${icon.fontFamily} ${icon.fontPackage} ${Icons.public.codePoint}';
    final buttonStyle = ElevatedButton.styleFrom(
      minimumSize: const Size(64, 40),
      maximumSize: const Size(400, 56),
      foregroundColor: scheme.onPrimary,
      backgroundColor: scheme.primary,
      disabledForegroundColor: scheme.onSurfaceVariant,
      disabledBackgroundColor: scheme.surfaceDim,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      shape: RoundedRectangleBorder(borderRadius: radius),
      textStyle: style,
      elevation: 2,
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      visualDensity: VisualDensity.compact,
    );

    return MediaQuery.withClampedTextScaling(
      minScaleFactor: 1,
      maxScaleFactor: 1.5,
      child: Scaffold(
        appBar: AppBar(title: Text('Style lab ($_mode, web: $kIsWeb)')),
        floatingActionButton: Row(mainAxisSize: MainAxisSize.min, children: [
          FloatingActionButton.small(onPressed: () {}, child: const Icon(Icons.add)),
          FloatingActionButton.large(onPressed: () {}, child: const Icon(Icons.public)),
          FloatingActionButton.extended(
              onPressed: () => _openDialogs(context),
              icon: const Icon(Icons.more_horiz),
              label: const Text('Dialogs')),
        ]),
        body: ListView(
          padding: EdgeInsets.only(bottom: padding.bottom + viewPadding.bottom + insets.bottom),
          children: [
            richText,
            selectable,
            Text('same span: $sameSpan'),
            Text(spanInfo, style: TextStyle(decoration: combined)),
            Text('$channels $iconInfo ${scaled.toStringAsFixed(1)} ${corner.dx}'),
            Text('last lifecycle: ${_lastState?.name ?? 'none'}'),
            const Center(child: CircularProgressIndicator.adaptive()),
            const LinearProgressIndicator(value: 0.4, minHeight: 6),
            ConstrainedBox(
              constraints: const BoxConstraints.tightFor(height: 48),
              child: Container(
                decoration: BoxDecoration(
                  color: scheme.surfaceContainerLowest,
                  border: Border.fromBorderSide(side),
                  borderRadius: radius,
                  boxShadow: const [
                    BoxShadow(color: Colors.black26, blurRadius: 6, spreadRadius: 1, offset: Offset(0, 2)),
                  ],
                ),
              ),
            ),
            Wrap(children: [
              for (final c in swatch) Container(width: 24, height: 24, color: c),
              for (final c in tokens) Container(width: 24, height: 24, color: c),
            ]),
            ElevatedButton(style: buttonStyle, onPressed: () {}, child: const Text('Styled')),
            FilledButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.public),
              label: const Text('Trailing icon'),
              iconAlignment: IconAlignment.end,
            ),
            Row(children: [
              IconButton.filled(onPressed: () {}, icon: const Icon(Icons.add)),
              IconButton.filledTonal(onPressed: () {}, icon: const Icon(Icons.add)),
              IconButton.outlined(onPressed: () {}, icon: const Icon(Icons.add)),
            ]),
            TextButton(
              onPressed: () {
                final messenger = ScaffoldMessenger.maybeOf(context);
                messenger?.clearSnackBars();
                messenger?.hideCurrentSnackBar();
                messenger?.removeCurrentSnackBar();
                messenger?.showSnackBar(const SnackBar(content: Text('Hi')));
              },
              child: const Text('Snack'),
            ),
          ],
        ),
      ),
    );
  }
}
