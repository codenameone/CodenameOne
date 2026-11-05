import 'package:flutter/material.dart';

/// Delegate-driven layouts, persistent slivers, themes read by widgets and the
/// span/text getters.
class LayoutLabScreen extends StatelessWidget {
  const LayoutLabScreen({super.key});

  static const List<String> _teams = <String>['Falcons', 'Foxes', 'Otters', 'Owls'];

  @override
  Widget build(BuildContext context) {
    final theme = ThemeData(
      elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(backgroundColor: Colors.teal)),
      outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(foregroundColor: Colors.indigo)),
      textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(foregroundColor: Colors.deepOrange)),
      bottomNavigationBarTheme:
          const BottomNavigationBarThemeData(selectedItemColor: Colors.teal),
    );
    final tabs = TabBar(tabs: const [Tab(text: 'Fixtures'), Tab(text: 'Table')]);
    final span = const TextSpan(text: 'Match ', children: [TextSpan(text: 'day')]);
    final rich = RichText(text: span, textScaler: const TextScaler.linear(1.0));
    final label = const Text('Kick-off', textScaler: TextScaler.linear(1.0));
    final m = Matrix4.identity();
    m.storage[12] = 8.0;
    final deco = BoxDecoration(borderRadius: BorderRadius.circular(6));
    final handleNote = Icons.sports_soccer.fontFamilyFallback == null ? 'none' : 'some';
    return Theme(
      data: theme,
      child: DefaultTabController(
        length: 2,
        child: Scaffold(
          appBar: AppBar(title: const Text('Layout'), bottom: tabs),
          body: NestedScrollView(
            headerSliverBuilder: (context, inner) => [
              SliverOverlapAbsorber(
                handle: NestedScrollView.sliverOverlapAbsorberHandleFor(context),
                sliver: SliverPersistentHeader(pinned: true, delegate: _BannerDelegate()),
              ),
            ],
            body: Builder(
              builder: (context) => CustomScrollView(slivers: [
                SliverOverlapInjector(
                    handle: NestedScrollView.sliverOverlapAbsorberHandleFor(context)),
                SliverToBoxAdapter(
                  child: Column(children: [
                    SizedBox(
                      height: 60,
                      child: CustomSingleChildLayout(
                          delegate: _CenterDelegate(), child: Container(decoration: deco, color: null)),
                    ),
                    Text('${(rich.text as TextSpan).children?.length} ${label.data} '
                        '${tabs.preferredSize.height} ${m.storage[12]} $handleNote '
                        '${deco.borderRadius != null}'),
                    Text.rich(span),
                    SelectableText('Notes', onSelectionChanged: (sel, cause) {}),
                    Text('x', style: const TextStyle(fontFeatures: [])),
                    SliderTheme(
                      data: SliderTheme.of(context).copyWith(
                          thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8)),
                      child: Slider(value: 0.4, onChanged: (_) {}),
                    ),
                    ElevatedButton(onPressed: () {}, child: const Text('Join')),
                    OutlinedButton(onPressed: () {}, child: const Text('Share')),
                    Autocomplete<String>(
                      optionsBuilder: (v) => _teams.where(
                          (t) => t.toLowerCase().startsWith(v.text.toLowerCase())),
                      onSelected: (t) => debugPrint('picked $t'),
                    ),
                    SizedBox(
                      height: 80,
                      child: ListView(scrollDirection: Axis.horizontal, children: [
                        for (final t in _teams) Padding(padding: const EdgeInsets.all(8), child: Text(t)),
                      ]),
                    ),
                    SizedBox(
                      height: 200,
                      child: DraggableScrollableSheet(
                        initialChildSize: 0.6,
                        builder: (context, controller) => ListView(
                            children: [for (final t in _teams) ListTile(title: Text(t))]),
                      ),
                    ),
                  ]),
                ),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}

class _BannerDelegate extends SliverPersistentHeaderDelegate {
  @override
  Widget build(BuildContext context, double shrinkOffset, bool overlapsContent) =>
      Container(color: Colors.teal, alignment: Alignment.center,
          child: Text('Season ${shrinkOffset.toInt()}'));

  @override
  double get minExtent => 40;

  @override
  double get maxExtent => 64;

  @override
  bool shouldRebuild(covariant SliverPersistentHeaderDelegate oldDelegate) => false;
}

class _CenterDelegate extends SingleChildLayoutDelegate {
  @override
  BoxConstraints getConstraintsForChild(BoxConstraints constraints) =>
      const BoxConstraints.tightFor(width: 40, height: 40);

  @override
  Offset getPositionForChild(Size size, Size childSize) =>
      Offset((size.width - childSize.width) / 2, (size.height - childSize.height) / 2);

  @override
  bool shouldRelayout(covariant SingleChildLayoutDelegate oldDelegate) => false;
}
