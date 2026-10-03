// Codename One Flutter runtime API stubs — geometryPaint category (new_gallery, Pass 2).
//
// The dart:ui / painting value types: geometric primitives (Offset, Rect,
// RelativeRect, Radius), the low-level painting surface (Paint, Path, Canvas,
// Gradient, Shader) and the painting/decoration value objects (Border,
// BorderSide, BorderRadius, the ShapeBorder family, EdgeInsetsDirectional).
//
// These are signature-only declarations resolved by the Dart transpiler; each
// maps to a hand-written Java runtime class via @JavaName. They are the
// receiver types behind the bulk of new_gallery's `..cascade` and painter code.
//
// NOTE: Alignment, AlignmentDirectional, EdgeInsets, Size, Color, Colors,
// Decoration and BoxDecoration are declared elsewhere (flutter_material.dart /
// gallery_coreWidgets.dart) and are intentionally NOT redeclared here.
// EdgeInsets gains its shared supertype (EdgeInsetsGeometry) and `fromLTRB`
// factory in flutter_material.dart so this file can hang EdgeInsetsDirectional
// off it.

// --- enums ------------------------------------------------------------

@JavaName('com.codename1.flutter.PaintingStyle')
enum PaintingStyle { fill, stroke }

@JavaName('com.codename1.flutter.StrokeCap')
enum StrokeCap { butt, round, square }

@JavaName('com.codename1.flutter.StrokeJoin')
enum StrokeJoin { miter, round, bevel }

@JavaName('com.codename1.flutter.BorderStyle')
enum BorderStyle { none, solid }

@JavaName('com.codename1.flutter.TileMode')
enum TileMode { clamp, repeated, mirror, decal }

// --- dart:ui geometry -------------------------------------------------

@JavaName('com.codename1.flutter.Offset')
class Offset {
  external Offset(double dx, double dy);
  external static Offset get zero;
  external static Offset get infinite;
  external static Offset fromDirection(double direction, [double distance = 1.0]);
  external double get dx;
  external double get dy;
  external double get distance;
  external double get distanceSquared;
  external double get direction;
  external Offset scale(double scaleX, double scaleY);
  external Offset translate(double translateX, double translateY);
  external Offset operator +(Offset other);
  external Offset operator -(Offset other);
  external Offset operator *(double operand);
  external Offset operator /(double operand);
  external Rect operator &(Size other);
}

@JavaName('com.codename1.flutter.Rect')
class Rect {
  external static Rect fromLTWH(double left, double top, double width, double height);
  external static Rect fromLTRB(double left, double top, double right, double bottom);
  external static Rect fromCircle({Offset center, double radius});
  external static Rect fromCenter({Offset center, double width, double height});
  external static Rect fromPoints(Offset a, Offset b);
  external static Rect get zero;
  external static Rect get largest;
  external double get left;
  external double get top;
  external double get right;
  external double get bottom;
  external double get width;
  external double get height;
  external double get shortestSide;
  external double get longestSide;
  external bool get isEmpty;
  external bool get isFinite;
  external bool get hasNaN;
  external Offset get center;
  external Offset get topLeft;
  external Offset get topCenter;
  external Offset get topRight;
  external Offset get centerLeft;
  external Offset get centerRight;
  external Offset get bottomLeft;
  external Offset get bottomCenter;
  external Offset get bottomRight;
  external Size get size;
  external bool contains(Offset offset);
  external Rect translate(double translateX, double translateY);
  external Rect shift(Offset offset);
  external Rect inflate(double delta);
  external Rect deflate(double delta);
  external Rect intersect(Rect other);
  external Rect expandToInclude(Rect other);
  external bool overlaps(Rect other);
}

@JavaName('com.codename1.flutter.RelativeRect')
class RelativeRect {
  external static RelativeRect fromLTRB(double left, double top, double right, double bottom);
  external static RelativeRect fromRect(Rect rect, Rect container);
  external static RelativeRect fromSize(Rect rect, Size container);
  external static RelativeRect get fill;
  external double get left;
  external double get top;
  external double get right;
  external double get bottom;
  external Rect toRect(Rect container);
}

@JavaName('com.codename1.flutter.Radius')
class Radius {
  external static Radius circular(double radius);
  external static Radius elliptical(double x, double y);
  external static Radius get zero;
  external double get x;
  external double get y;
}

// --- painting surface -------------------------------------------------

@JavaName('com.codename1.flutter.Shader')
abstract class Shader {}

@JavaName('com.codename1.flutter.Gradient')
abstract class Gradient {
  external Shader createShader(Rect rect, {Object? textDirection});
}

@JavaName('com.codename1.flutter.LinearGradient')
class LinearGradient extends Gradient {
  external LinearGradient({Object? begin, Object? end, List<Color> colors, List<double>? stops, TileMode? tileMode, Object? transform});
}

@JavaName('com.codename1.flutter.RadialGradient')
class RadialGradient extends Gradient {
  external RadialGradient({Object? center, double radius = 0.5, List<Color> colors, List<double>? stops, TileMode? tileMode, Object? focal, double focalRadius = 0.0, Object? transform});
}

@JavaName('com.codename1.flutter.SweepGradient')
class SweepGradient extends Gradient {
  external SweepGradient({Object? center, double startAngle = 0.0, double endAngle = 6.283185307179586, List<Color> colors, List<double>? stops, TileMode? tileMode, Object? transform});
}

@JavaName('com.codename1.flutter.Paint')
class Paint {
  external Paint();
  external Color get color;
  external set color(Color v);
  external PaintingStyle get style;
  external set style(PaintingStyle v);
  external double get strokeWidth;
  external set strokeWidth(double v);
  external StrokeCap get strokeCap;
  external set strokeCap(StrokeCap v);
  external StrokeJoin get strokeJoin;
  external set strokeJoin(StrokeJoin v);
  external double get strokeMiterLimit;
  external set strokeMiterLimit(double v);
  external bool get isAntiAlias;
  external set isAntiAlias(bool v);
  external Shader? get shader;
  external set shader(Shader? v);
  external Object? get maskFilter;
  external set maskFilter(Object? v);
  external Object? get colorFilter;
  external set colorFilter(Object? v);
  external Object? get blendMode;
  external set blendMode(Object? v);
}

@JavaName('com.codename1.flutter.Path')
class Path {
  external Path();
  external void moveTo(double x, double y);
  external void lineTo(double x, double y);
  external void cubicTo(double x1, double y1, double x2, double y2, double x3, double y3);
  external void quadraticBezierTo(double x1, double y1, double x2, double y2);
  external void conicTo(double x1, double y1, double x2, double y2, double w);
  external void arcTo(Rect rect, double startAngle, double sweepAngle, bool forceMoveTo);
  external void arcToPoint(Offset arcEnd, {Radius radius, double rotation, bool largeArc, bool clockwise});
  external void relativeMoveTo(double dx, double dy);
  external void relativeLineTo(double dx, double dy);
  external void addRect(Rect rect);
  external void addOval(Rect oval);
  external void addRRect(RRect rrect);
  external void addPolygon(List<Offset> points, bool close);
  external void addPath(Path path, Offset offset);
  external void close();
  external void reset();
  external bool contains(Offset point);
  external Path shift(Offset offset);
}

@JavaName('com.codename1.flutter.RRect')
class RRect {
  external static RRect fromRectAndRadius(Rect rect, Radius radius);
  external static RRect fromLTRBR(double left, double top, double right, double bottom, Radius radius);
  external static RRect fromRectAndCorners(Rect rect, {Radius topLeft, Radius topRight, Radius bottomLeft, Radius bottomRight});
  // The rectangle that would remain after the corner radii are removed, and the
  // enclosing / enclosed straight rects — Flutter's `RRect.middleRect/outerRect/
  // innerRect`.
  external Rect get middleRect;
  external Rect get outerRect;
  external Rect get safeInnerRect;
  external Rect get wideMiddleRect;
  external Rect get tallMiddleRect;
  external double get left;
  external double get top;
  external double get right;
  external double get bottom;
  external double get width;
  external double get height;
  external Offset get center;
}

@JavaName('com.codename1.flutter.Canvas')
class Canvas {
  external void drawPath(Path path, Paint paint);
  external void drawRect(Rect rect, Paint paint);
  external void drawRRect(RRect rrect, Paint paint);
  external void drawCircle(Offset c, double radius, Paint paint);
  external void drawOval(Rect rect, Paint paint);
  external void drawLine(Offset p1, Offset p2, Paint paint);
  external void drawArc(Rect rect, double startAngle, double sweepAngle, bool useCenter, Paint paint);
  external void drawPoints(Object pointMode, List<Offset> points, Paint paint);
  external void drawColor(Color color, Object blendMode);
  external void drawShadow(Path path, Color color, double elevation, bool transparentOccluder);
  external void drawVertices(Object vertices, Object blendMode, Paint paint);
  external void drawImage(Object image, Offset offset, Paint paint);
  external void translate(double dx, double dy);
  external void scale(double sx, [double sy = 1.0]);
  external void rotate(double radians);
  external void skew(double sx, double sy);
  external void save();
  external void saveLayer(Rect? bounds, Paint paint);
  external void restore();
  external void clipRect(Rect rect);
  external void clipRRect(RRect rrect);
  external void clipPath(Path path);
}

// --- EdgeInsets (directional variant) ---------------------------------
// EdgeInsetsGeometry is the shared supertype of EdgeInsets (see
// flutter_material.dart) and EdgeInsetsDirectional. The directional variant
// extends EdgeInsets so it stays assignable to the `EdgeInsets`-typed padding
// parameters the widget stubs declare (a pragmatic superclass — text-direction
// resolution treats `start`/`end` as `left`/`right` under LTR).

@JavaName('com.codename1.flutter.EdgeInsetsGeometry')
abstract class EdgeInsetsGeometry {}

@JavaName('com.codename1.flutter.EdgeInsetsDirectional')
class EdgeInsetsDirectional extends EdgeInsets {
  external static EdgeInsetsDirectional all(double value);
  external static EdgeInsetsDirectional only({double start, double top, double end, double bottom});
  external static EdgeInsetsDirectional symmetric({double horizontal, double vertical});
  external static EdgeInsetsDirectional fromSTEB(double start, double top, double end, double bottom);
  external static EdgeInsetsDirectional get zero;
  external double get start;
  external double get end;
}

// --- border radii -----------------------------------------------------

@JavaName('com.codename1.flutter.BorderRadiusGeometry')
abstract class BorderRadiusGeometry {}

@JavaName('com.codename1.flutter.BorderRadius')
class BorderRadius extends BorderRadiusGeometry {
  external static BorderRadius all(Radius radius);
  external static BorderRadius circular(double radius);
  external static BorderRadius only({Radius topLeft = Radius.zero, Radius topRight = Radius.zero, Radius bottomLeft = Radius.zero, Radius bottomRight = Radius.zero});
  external static BorderRadius vertical({Radius top = Radius.zero, Radius bottom = Radius.zero});
  external static BorderRadius horizontal({Radius left = Radius.zero, Radius right = Radius.zero});
  external static BorderRadius get zero;
  // Linearly interpolates between two BorderRadius values — Flutter's
  // `BorderRadius.lerp(a, b, t)`. Returns null only when both inputs are null.
  external static BorderRadius? lerp(BorderRadius? a, BorderRadius? b, double t);
  external RRect toRRect(Rect rect);
}

@JavaName('com.codename1.flutter.BorderRadiusDirectional')
class BorderRadiusDirectional extends BorderRadiusGeometry {
  external static BorderRadiusDirectional all(Radius radius);
  external static BorderRadiusDirectional circular(double radius);
  external static BorderRadiusDirectional only({Radius topStart = Radius.zero, Radius topEnd = Radius.zero, Radius bottomStart = Radius.zero, Radius bottomEnd = Radius.zero});
  external static BorderRadiusDirectional vertical({Radius top = Radius.zero, Radius bottom = Radius.zero});
  external static BorderRadiusDirectional horizontal({Radius start = Radius.zero, Radius end = Radius.zero});
  external static BorderRadiusDirectional get zero;
}

// --- borders (ShapeBorder family) -------------------------------------

@JavaName('com.codename1.flutter.ShapeBorder')
abstract class ShapeBorder {}

@JavaName('com.codename1.flutter.OutlinedBorder')
abstract class OutlinedBorder extends ShapeBorder {}

@JavaName('com.codename1.flutter.InputBorder')
abstract class InputBorder extends ShapeBorder {
  // The "no border" sentinel — Flutter's `InputBorder.none`.
  external static InputBorder get none;
}

@JavaName('com.codename1.flutter.BoxBorder')
abstract class BoxBorder extends ShapeBorder {}

@JavaName('com.codename1.flutter.BorderSide')
class BorderSide {
  external BorderSide({Color? color, double width = 1.0, BorderStyle style = BorderStyle.solid, double? strokeAlign});
  // Where the stroke sits relative to the path: -1 inside, 0 centred, 1 outside.
  external static double get strokeAlignInside;
  external static double get strokeAlignCenter;
  external static double get strokeAlignOutside;
  external static BorderSide get none;
  // Linearly interpolates between two BorderSide values — Flutter's
  // `BorderSide.lerp(a, b, t)`.
  external static BorderSide lerp(BorderSide a, BorderSide b, double t);
  external Color get color;
  external double get width;
  external BorderStyle get style;
  // Builds a Paint stroking this side — Flutter's `BorderSide.toPaint()`.
  external Paint toPaint();
}

@JavaName('com.codename1.flutter.Border')
class Border extends BoxBorder {
  external Border({BorderSide top, BorderSide right, BorderSide bottom, BorderSide left});
  external static Border all({Color? color, double width = 1.0, BorderStyle style = BorderStyle.solid, double? strokeAlign});
  external static Border symmetric({BorderSide vertical, BorderSide horizontal});
  external BorderSide get top;
  external BorderSide get right;
  external BorderSide get bottom;
  external BorderSide get left;
}

@JavaName('com.codename1.flutter.RoundedRectangleBorder')
class RoundedRectangleBorder extends OutlinedBorder {
  external RoundedRectangleBorder({Object? borderRadius, BorderSide side});
}

@JavaName('com.codename1.flutter.StadiumBorder')
class StadiumBorder extends OutlinedBorder {
  external StadiumBorder({BorderSide side});
}

@JavaName('com.codename1.flutter.CircleBorder')
class CircleBorder extends OutlinedBorder {
  external CircleBorder({BorderSide side, double eccentricity = 0.0});
}

@JavaName('com.codename1.flutter.BeveledRectangleBorder')
class BeveledRectangleBorder extends OutlinedBorder {
  external BeveledRectangleBorder({Object? borderRadius, BorderSide side});
}

@JavaName('com.codename1.flutter.ContinuousRectangleBorder')
class ContinuousRectangleBorder extends OutlinedBorder {
  external ContinuousRectangleBorder({Object? borderRadius, BorderSide side});
}

@JavaName('com.codename1.flutter.OutlineInputBorder')
class OutlineInputBorder extends InputBorder {
  external OutlineInputBorder({BorderSide borderSide, Object? borderRadius, double gapPadding = 4.0});
  // Fields subclasses (e.g. shrine's CutCornersBorder) read off `this`/`super`.
  external BorderSide get borderSide;
  external BorderRadius get borderRadius;
  external double get gapPadding;
  // ShapeBorder interpolation hooks — Flutter's `lerpFrom` / `lerpTo`.
  external ShapeBorder? lerpFrom(ShapeBorder? a, double t);
  external ShapeBorder? lerpTo(ShapeBorder? b, double t);
}

@JavaName('com.codename1.flutter.UnderlineInputBorder')
class UnderlineInputBorder extends InputBorder {
  external UnderlineInputBorder({BorderSide borderSide, Object? borderRadius});
}
