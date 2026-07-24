// Codename One Flutter runtime API stubs — "stubsFinal" category (new_gallery, Pass 8+).
//
// The last tail of brand-new concrete types (E0129 unresolved identifier, E0135
// unresolved constructor) that survived through Pass 8: the services input
// formatters, the dart:ui BlendMode enum, and a handful of widgets (FocusScope,
// ModalBarrier, ShapeBorderClipper and the flutter_staggered_grid_view
// MasonryGridView). Shapes mirror the real Flutter / package signatures.
//
// Conventions (see gallery_p7.dart header):
//  - positional constructor params -> Java constructor arguments
//  - named constructor params       -> void setter methods of the same name
//  - named constructors (X.name)    -> `external static X name(...)`
//  - instance getters               -> no-arg method calls
//  - callbacks / loosely-owned types are declared as `Object?`.

// ======================================================================
// services — text input formatting
// ======================================================================

// How `maxLength` is enforced on an editable text field — Flutter's
// `MaxLengthEnforcement` (text_field_demo passes `.none`).
@JavaName('com.codename1.flutter.services.MaxLengthEnforcement')
enum MaxLengthEnforcement { none, enforced, truncateAfterCompositionEnds }

// The base class every input formatter extends; new_gallery subclasses it
// (`_UsNumberTextInputFormatter extends TextInputFormatter`) and overrides
// `formatEditUpdate`. TextEditingValue is declared in gallery_p3_cascadeTypes.
@JavaName('com.codename1.flutter.services.TextInputFormatter')
class TextInputFormatter {
  external TextInputFormatter();
  external TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue);
}

// Filters the edited text against a pattern — Flutter's
// `FilteringTextInputFormatter`. Only `digitsOnly` is used by new_gallery; the
// allow/deny constructors and `singleLineFormatter` are declared for fidelity.
@JavaName('com.codename1.flutter.services.FilteringTextInputFormatter')
class FilteringTextInputFormatter extends TextInputFormatter {
  external FilteringTextInputFormatter(Object filterPattern, {bool allow, String replacementString});
  external static FilteringTextInputFormatter allow(Object filterPattern, {String replacementString});
  external static FilteringTextInputFormatter deny(Object filterPattern, {String replacementString});
  external static FilteringTextInputFormatter get digitsOnly;
  external static FilteringTextInputFormatter get singleLineFormatter;
}

// Truncates the edited text to a maximum length — Flutter's
// `LengthLimitingTextInputFormatter`.
@JavaName('com.codename1.flutter.services.LengthLimitingTextInputFormatter')
class LengthLimitingTextInputFormatter extends TextInputFormatter {
  external LengthLimitingTextInputFormatter(int? maxLength, {MaxLengthEnforcement? maxLengthEnforcement});
}

// ======================================================================
// dart:ui — painting
// ======================================================================

// The Porter-Duff / separable blend modes for `Canvas.drawVertices` and friends
// — dart:ui's `BlendMode`. new_gallery uses `BlendMode.color`; the full standard
// set is declared for fidelity.
@JavaName('com.codename1.flutter.BlendMode')
enum BlendMode {
  clear, src, dst, srcOver, dstOver, srcIn, dstIn, srcOut, dstOut, srcATop,
  dstATop, xor, plus, modulate, screen, overlay, darken, lighten, colorDodge,
  colorBurn, hardLight, softLight, difference, exclusion, multiply, hue,
  saturation, color, luminosity
}

// ======================================================================
// widgets
// ======================================================================

// A focus container that groups its subtree — Flutter's `FocusScope`.
@JavaName('com.codename1.flutter.widgets.FocusScopeNode')
class FocusScopeNode {
  external FocusScopeNode({String? debugLabel});
  external bool get hasFocus;
  external void requestFocus([Object? node]);
  external void unfocus({Object? disposition});
}

@JavaName('com.codename1.flutter.widgets.FocusScope')
class FocusScope extends Widget {
  external FocusScope({Key? key, FocusScopeNode? node, bool? autofocus, Object? onFocusChange, bool? canRequestFocus, bool? skipTraversal, Widget? child});
  external static FocusScopeNode of(BuildContext context);
}

// A full-screen barrier that optionally dismisses a route on tap — Flutter's
// `ModalBarrier` (pages/backdrop passes `dismissible: false`).
@JavaName('com.codename1.flutter.widgets.ModalBarrier')
class ModalBarrier extends Widget {
  external ModalBarrier({Key? key, Color? color, bool? dismissible, String? semanticsLabel, bool? barrierSemanticsDismissible, Object? onDismiss});
}

// Adapts a ShapeBorder to the CustomClipper protocol used by PhysicalShape —
// Flutter's `ShapeBorderClipper` (crane/backdrop clips its front layer).
@JavaName('com.codename1.flutter.widgets.ShapeBorderClipper')
class ShapeBorderClipper {
  external ShapeBorderClipper({Object shape, TextDirection? textDirection});
}

// A staggered, Pinterest-style grid — the flutter_staggered_grid_view package's
// `MasonryGridView`. crane/backdrop builds it via the `.count` constructor.
@JavaName('com.codename1.flutter.widgets.MasonryGridView')
class MasonryGridView extends Widget {
  external static MasonryGridView count({Key? key, String? restorationId, int crossAxisCount, double? mainAxisSpacing, double? crossAxisSpacing, int? itemCount, Object itemBuilder, Object? scrollDirection, bool? shrinkWrap, Object? physics, Object? padding, Object? controller});
}
