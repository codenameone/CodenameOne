// Codename One Flutter runtime API stubs — "animPaintPhysicsTheme" category
// (new_gallery, Pass 9).
//
// Brand-new types that survived Pass 7/8 with no @JavaName mapping, so the
// emitter left their references unqualified (no import) and javac failed with
// "cannot find symbol". Each names a REAL Flutter (painting / widgets /
// semantics) type; the Java implementations live under
// com.codename1.flutter.{painting,widgets,semantics}. Only NEW types live here;
// members of pre-existing types are added in place on their owning stub.
//
// Conventions follow gallery_p7.dart:
//  - positional constructor params -> Java constructor arguments
//  - named constructor params       -> void setter methods of the same name
//  - instance getters               -> no-arg method calls
//  - callbacks / types owned elsewhere are declared loosely as `Object?`.

// ======================================================================
// painting — BoxPainter
// ======================================================================

// The object a Decoration produces to paint itself — Flutter's `BoxPainter`.
// A decoration returns one from `createBoxPainter`; the tab-indicator and Rally
// pie-chart decorations subclass it and override `paint(canvas, offset,
// configuration)`.
@JavaName('com.codename1.flutter.painting.BoxPainter')
abstract class BoxPainter {
  void paint(Canvas canvas, Offset offset, ImageConfiguration configuration);
  external void dispose();
}

// ======================================================================
// widgets — OverlayRoute
// ======================================================================

// A Route that inserts OverlayEntry objects into the navigator's Overlay —
// Flutter's `OverlayRoute`. new_gallery's TwoPanePageRoute extends it and
// overrides `createOverlayEntries()`.
@JavaName('com.codename1.flutter.widgets.OverlayRoute')
class OverlayRoute<T> extends Route<T> {
  external OverlayRoute();
  external Iterable<OverlayEntry> createOverlayEntries();
}

// ======================================================================
// semantics — SemanticsBuilderCallback
// ======================================================================

// The signature of a CustomPainter's `semanticsBuilder` — Flutter's
// `SemanticsBuilderCallback` typedef, `List<CustomPainterSemantics> Function(
// Size size)`. Declared as a single-method type so transpiled painters that
// override `semanticsBuilder` bind the lambda against a Java functional
// interface. The Rally line chart returns one to expose per-day balances to
// screen readers.
@JavaName('com.codename1.flutter.semantics.SemanticsBuilderCallback')
class SemanticsBuilderCallback {
  external List<CustomPainterSemantics> call(Size size);
}
