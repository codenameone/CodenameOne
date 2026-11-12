// Codename One Flutter runtime API stubs -- enums, services and widgets an
// ordinary production app (a social/marketplace app with ~750 files) reached
// that no earlier pass declared. Conventions as in gallery_p4_apitail.dart.

@JavaName('com.codename1.flutter.material.ListTileControlAffinity')
enum ListTileControlAffinity { leading, trailing, platform }

@JavaName('com.codename1.flutter.VerticalDirection')
enum VerticalDirection { up, down }

@JavaName('com.codename1.flutter.material.StretchMode')
enum StretchMode { zoomBackground, blurBackground, fadeTitle }

@JavaName('com.codename1.flutter.material.CollapseMode')
enum CollapseMode { parallax, pin, none }

@JavaName('com.codename1.flutter.widgets.OverflowBarAlignment')
enum OverflowBarAlignment { start, end, center }

@JavaName('com.codename1.flutter.widgets.ScrollViewKeyboardDismissBehavior')
enum ScrollViewKeyboardDismissBehavior { manual, onDrag }

@JavaName('com.codename1.flutter.widgets.OptionsViewOpenDirection')
enum OptionsViewOpenDirection { up, down }

@JavaName('com.codename1.flutter.services.DeviceOrientation')
enum DeviceOrientation { portraitUp, landscapeLeft, portraitDown, landscapeRight }

// Short device vibrations.
@JavaName('com.codename1.flutter.services.HapticFeedback')
abstract class HapticFeedback {
  external static Future<void> vibrate();
  external static Future<void> lightImpact();
  external static Future<void> mediumImpact();
  external static Future<void> heavyImpact();
  external static Future<void> selectionClick();
}

// pop() sends the app to the background.
@JavaName('com.codename1.flutter.services.SystemNavigator')
abstract class SystemNavigator {
  external static Future<void> pop({bool? animated});
}

@JavaName('com.codename1.flutter.widgets.Scrollable')
abstract class Scrollable {
  external static Future<void> ensureVisible(BuildContext context,
      {double? alignment, Duration? duration, Curve? curve, Object? alignmentPolicy});
}

// Dependents rebuild whenever `notifier` notifies.
@JavaName('com.codename1.flutter.widgets.InheritedNotifier')
class InheritedNotifier<T extends Listenable> extends InheritedWidget {
  external InheritedNotifier({Key? key, T? notifier, required Widget child});
  external T? get notifier;
}

@JavaName('com.codename1.flutter.widgets.ListenableBuilder')
class ListenableBuilder extends Widget {
  external ListenableBuilder({Key? key, required Listenable listenable, required TransitionBuilder builder, Widget? child});
}

// A widget inline in a run of text. Text lays such a span tree out as a wrap of
// words and widgets (WidgetSpan.flow), since the text painter draws glyphs only.
@JavaName('com.codename1.flutter.widgets.WidgetSpan')
class WidgetSpan extends TextSpan {
  external WidgetSpan({required Widget child, Object? alignment, Object? baseline, TextStyle? style});
  external Widget get child;
}

// Flutter 3.32's RadioGroup: the group value and change handler its Radio
// descendants read when they are not given their own.
@JavaName('com.codename1.flutter.material.RadioGroup')
class RadioGroup<T> extends Widget {
  external RadioGroup({Key? key, T? groupValue, void Function(T?)? onChanged, required Widget child});
}
