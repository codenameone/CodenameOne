// Codename One Flutter runtime API stubs -- delegate-driven layouts, persistent
// and overlap slivers, the draggable sheet, Autocomplete and the button, bar and
// slider themes (see gallery_p4_apitail.dart for the conventions: named params
// -> setters, named constructors -> static methods).

@JavaName('com.codename1.flutter.widgets.SingleChildLayoutDelegate')
abstract class SingleChildLayoutDelegate {
  external Size getSize(BoxConstraints constraints);
  external BoxConstraints getConstraintsForChild(BoxConstraints constraints);
  external Offset getPositionForChild(Size size, Size childSize);
  bool shouldRelayout(covariant SingleChildLayoutDelegate oldDelegate);
}

@JavaName('com.codename1.flutter.widgets.CustomSingleChildLayout')
class CustomSingleChildLayout extends Widget {
  external CustomSingleChildLayout({Key? key, required SingleChildLayoutDelegate delegate, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.SliverPersistentHeaderDelegate')
abstract class SliverPersistentHeaderDelegate {
  Widget build(BuildContext context, double shrinkOffset, bool overlapsContent);
  double get minExtent;
  double get maxExtent;
  bool shouldRebuild(covariant SliverPersistentHeaderDelegate oldDelegate);
}

@JavaName('com.codename1.flutter.widgets.SliverPersistentHeader')
class SliverPersistentHeader extends Widget {
  external SliverPersistentHeader({Key? key, required SliverPersistentHeaderDelegate delegate, bool? pinned, bool? floating});
}

@JavaName('com.codename1.flutter.widgets.SliverOverlapAbsorberHandle')
class SliverOverlapAbsorberHandle {
  external double? get layoutExtent;
  external double? get scrollExtent;
}

@JavaName('com.codename1.flutter.widgets.SliverOverlapAbsorber')
class SliverOverlapAbsorber extends Widget {
  external SliverOverlapAbsorber({Key? key, required SliverOverlapAbsorberHandle handle, Widget? sliver});
}

@JavaName('com.codename1.flutter.widgets.SliverOverlapInjector')
class SliverOverlapInjector extends Widget {
  external SliverOverlapInjector({Key? key, required SliverOverlapAbsorberHandle handle, Widget? sliver});
}

@JavaName('com.codename1.flutter.widgets.DraggableScrollableSheet')
class DraggableScrollableSheet extends Widget {
  external DraggableScrollableSheet({Key? key, double? initialChildSize, double? minChildSize, double? maxChildSize, bool? expand, bool? snap, required Widget Function(BuildContext, ScrollController) builder});
}

@JavaName('com.codename1.flutter.material.Autocomplete')
class Autocomplete<T extends Object> extends Widget {
  external Autocomplete({Key? key, required Iterable<T> Function(TextEditingValue) optionsBuilder, void Function(T)? onSelected, String Function(T)? displayStringForOption, double? optionsMaxHeight});
  external static String defaultStringForOption(Object option);
}

@JavaName('com.codename1.flutter.material.ElevatedButtonThemeData')
class ElevatedButtonThemeData {
  external ElevatedButtonThemeData({ButtonStyle? style});
  external ButtonStyle? get style;
}

@JavaName('com.codename1.flutter.material.TextButtonThemeData')
class TextButtonThemeData {
  external TextButtonThemeData({ButtonStyle? style});
  external ButtonStyle? get style;
}

@JavaName('com.codename1.flutter.material.OutlinedButtonThemeData')
class OutlinedButtonThemeData {
  external OutlinedButtonThemeData({ButtonStyle? style});
  external ButtonStyle? get style;
}

@JavaName('com.codename1.flutter.material.BottomNavigationBarThemeData')
class BottomNavigationBarThemeData {
  external BottomNavigationBarThemeData({Color? backgroundColor, Color? selectedItemColor, Color? unselectedItemColor, double? elevation});
  external Color? get backgroundColor;
  external Color? get selectedItemColor;
  external Color? get unselectedItemColor;
  external double? get elevation;
}

@JavaName('com.codename1.flutter.material.RoundSliderThumbShape')
class RoundSliderThumbShape {
  external RoundSliderThumbShape({double? enabledThumbRadius, double? disabledThumbRadius, double? elevation});
  external double get enabledThumbRadius;
  external double? get disabledThumbRadius;
}
