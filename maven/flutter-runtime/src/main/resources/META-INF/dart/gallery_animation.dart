// Codename One Flutter runtime API stubs — ANIMATION category.
//
// Signature-only declarations for the animation engine (AnimationController,
// Animation/Animatable, Tween family, Curves, and the transition/animated
// widgets) as seen from Dart. Each @JavaName points at the hand-written Java
// runtime under com.codename1.flutter.animation. Parsed with the transpiler's
// own Dart front end; conventions match flutter_material.dart.

// --- ticker providers (framework mixins) ------------------------------

@JavaName('com.codename1.flutter.animation.TickerProvider')
abstract class TickerProvider {}

@JavaName('com.codename1.flutter.animation.SingleTickerProviderStateMixin')
mixin SingleTickerProviderStateMixin {}

@JavaName('com.codename1.flutter.animation.TickerProviderStateMixin')
mixin TickerProviderStateMixin {}

// --- status -----------------------------------------------------------

@JavaName('com.codename1.flutter.animation.AnimationStatus')
enum AnimationStatus { dismissed, forward, reverse, completed }

// Flutter exposes these as getters on the AnimationStatus enum (settings.dart /
// shrine app.dart read `status.isDismissed` / `status.isAnimating`). The
// transpiler drops enhanced-enum bodies, so they are supplied here as an
// extension, which member resolution consults for enum receivers. Backed by the
// static helpers on AnimationStatusExtensions.
@JavaName('com.codename1.flutter.animation.AnimationStatusExtensions')
extension AnimationStatusExtensions on AnimationStatus {
  external bool get isDismissed;
  external bool get isCompleted;
  external bool get isAnimating;
  external bool get isForwardOrCompleted;
}

// How an AnimationController behaves when animations are disabled — Flutter's
// `AnimationBehavior` (progress_indicator_demo passes it to the controller).
@JavaName('com.codename1.flutter.animation.AnimationBehavior')
enum AnimationBehavior { normal, preserve }

// --- animation / animatable core --------------------------------------

@JavaName('com.codename1.flutter.animation.Animation')
abstract class Animation<T> {
  external double get value;
  external AnimationStatus get status;
  external bool get isCompleted;
  external bool get isDismissed;
  external bool get isAnimating;
  external bool get isForwardOrCompleted;
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void addStatusListener(AnimationStatusListener listener);
  external void removeStatusListener(AnimationStatusListener listener);
  external Animation drive(Animatable child);
}

@JavaName('com.codename1.flutter.animation.Animatable')
abstract class Animatable<T> {
  external T transform(double t);
  external T evaluate(Animation<double> animation);
  external Animation<T> animate(Animation<double> parent);
  external Animatable<T> chain(Animatable<double> parent);
}

@JavaName('com.codename1.flutter.animation.AlwaysStoppedAnimation')
class AlwaysStoppedAnimation<T> extends Animation<T> {
  external AlwaysStoppedAnimation(T value);
}

// --- controller -------------------------------------------------------

@JavaName('com.codename1.flutter.animation.AnimationController')
class AnimationController extends Animation<double> {
  external AnimationController({Duration? duration, Duration? reverseDuration, double? value, double? lowerBound, double? upperBound, TickerProvider? vsync, String? debugLabel, AnimationBehavior? animationBehavior});
  external double get value;
  external set value(double v);
  // Drives the controller with a spring toward its bound at the given velocity —
  // Flutter's `AnimationController.fling`.
  external Future<void> fling({double? velocity, Object? springDescription, AnimationBehavior? animationBehavior});
  external Duration? get duration;
  external set duration(Duration? v);
  external Animation<double> get view;
  external Future<void> forward({double? from});
  external Future<void> reverse({double? from});
  external Future<void> animateTo(double target, {Duration? duration, Curve? curve});
  external Future<void> animateBack(double target, {Duration? duration, Curve? curve});
  external void repeat({double? min, double? max, bool? reverse, Duration? period});
  external void stop({bool? canceled});
  external void reset();
  external void dispose();
}

// --- curves -----------------------------------------------------------

@JavaName('com.codename1.flutter.animation.Curve')
abstract class Curve {
  external double transform(double t);
  external Curve get flipped;
}

@JavaName('com.codename1.flutter.animation.Cubic')
class Cubic extends Curve {
  external Cubic(double a, double b, double c, double d);
}

@JavaName('com.codename1.flutter.animation.Interval')
class Interval extends Curve {
  external Interval(double begin, double end, {Curve? curve});
}

@JavaName('com.codename1.flutter.animation.Curves')
abstract class Curves {
  external static Curve get linear;
  external static Curve get decelerate;
  external static Curve get ease;
  external static Curve get easeIn;
  external static Curve get easeOut;
  external static Curve get easeInOut;
  external static Curve get easeInOutCubic;
  external static Curve get easeInCubic;
  external static Curve get easeOutCubic;
  external static Curve get easeInSine;
  external static Curve get easeOutSine;
  external static Curve get easeInOutSine;
  external static Curve get fastOutSlowIn;
  external static Curve get slowMiddle;
  external static Curve get bounceIn;
  external static Curve get bounceOut;
  external static Curve get bounceInOut;
  external static Curve get elasticIn;
  external static Curve get elasticOut;
  external static Curve get fastLinearToSlowEaseIn;
}

@JavaName('com.codename1.flutter.animation.CurvedAnimation')
class CurvedAnimation extends Animation<double> {
  external CurvedAnimation({Animation<double> parent, Curve curve, Curve? reverseCurve});
}

// --- tweens -----------------------------------------------------------

@JavaName('com.codename1.flutter.animation.Tween')
class Tween<T> extends Animatable<T> {
  external Tween({T? begin, T? end});
  external T? get begin;
  external T? get end;
  external T lerp(double t);
}

@JavaName('com.codename1.flutter.animation.CurveTween')
class CurveTween extends Animatable<double> {
  external CurveTween({Curve curve});
}

@JavaName('com.codename1.flutter.animation.ColorTween')
class ColorTween extends Tween<Color> {
  external ColorTween({Color? begin, Color? end});
}

@JavaName('com.codename1.flutter.animation.IntTween')
class IntTween extends Tween<int> {
  external IntTween({int? begin, int? end});
}

@JavaName('com.codename1.flutter.animation.BorderRadiusTween')
class BorderRadiusTween extends Tween<Object> {
  external BorderRadiusTween({Object? begin, Object? end});
}

@JavaName('com.codename1.flutter.animation.EdgeInsetsGeometryTween')
class EdgeInsetsGeometryTween extends Tween<Object> {
  external EdgeInsetsGeometryTween({Object? begin, Object? end});
}

@JavaName('com.codename1.flutter.animation.Matrix4Tween')
class Matrix4Tween extends Tween<Object> {
  external Matrix4Tween({Object? begin, Object? end});
}

@JavaName('com.codename1.flutter.animation.RelativeRectTween')
class RelativeRectTween extends Tween<Object> {
  external RelativeRectTween({Object? begin, Object? end});
}

@JavaName('com.codename1.flutter.animation.TweenSequenceItem')
class TweenSequenceItem<T> {
  external TweenSequenceItem({Animatable<T> tween, double weight});
}

@JavaName('com.codename1.flutter.animation.TweenSequence')
class TweenSequence<T> extends Animatable<T> {
  external TweenSequence(List<TweenSequenceItem<T>> items);
}

// --- AnimatedWidget base ----------------------------------------------

// The base for widgets driven by a Listenable (usually an Animation) — Flutter's
// `AnimatedWidget`. new_gallery's shrine _BackdropTitle extends it and reads the
// inherited `listenable` (cast back to Animation<double>) in build().
@JavaName('com.codename1.flutter.animation.AnimatedWidget')
class AnimatedWidget extends Widget {
  external AnimatedWidget({Key? key, Listenable? listenable});
  external Listenable get listenable;
}

// --- transition widgets -----------------------------------------------

@JavaName('com.codename1.flutter.animation.FadeTransition')
class FadeTransition extends Widget {
  external FadeTransition({Key? key, Animation<double> opacity, Widget? child});
}

@JavaName('com.codename1.flutter.animation.ScaleTransition')
class ScaleTransition extends Widget {
  external ScaleTransition({Key? key, Animation<double> scale, Alignment? alignment, Widget? child});
}

@JavaName('com.codename1.flutter.animation.SlideTransition')
class SlideTransition extends Widget {
  external SlideTransition({Key? key, Animation<Object> position, Widget? child});
}

@JavaName('com.codename1.flutter.animation.RotationTransition')
class RotationTransition extends Widget {
  external RotationTransition({Key? key, Animation<double> turns, Alignment? alignment, Widget? child});
}

@JavaName('com.codename1.flutter.animation.PositionedTransition')
class PositionedTransition extends Widget {
  external PositionedTransition({Key? key, Animation<Object> rect, Widget? child});
}

// --- animated (implicit) widgets --------------------------------------

@JavaName('com.codename1.flutter.animation.AnimatedBuilder')
class AnimatedBuilder extends Widget {
  external AnimatedBuilder({Key? key, Animation<Object> animation, TransitionBuilder builder, Widget? child});
}

@JavaName('com.codename1.flutter.animation.AnimatedContainer')
class AnimatedContainer extends Widget {
  external AnimatedContainer({Key? key, Duration duration, Curve? curve, double? width, double? height, Color? color, EdgeInsets? padding, EdgeInsets? margin, Alignment? alignment, Object? decoration, Widget? child});
}

@JavaName('com.codename1.flutter.animation.AnimatedPadding')
class AnimatedPadding extends Widget {
  external AnimatedPadding({Key? key, EdgeInsets padding, Duration duration, Curve? curve, Widget? child});
}

@JavaName('com.codename1.flutter.animation.AnimatedSize')
class AnimatedSize extends Widget {
  external AnimatedSize({Key? key, Duration duration, Curve? curve, Alignment? alignment, Widget? child});
}

@JavaName('com.codename1.flutter.animation.AnimatedSwitcher')
class AnimatedSwitcher extends Widget {
  external AnimatedSwitcher({Key? key, Duration duration, Duration? reverseDuration, Curve? switchInCurve, Curve? switchOutCurve, Widget? child});
}

@JavaName('com.codename1.flutter.animation.AnimatedOpacity')
class AnimatedOpacity extends Widget {
  external AnimatedOpacity({Key? key, double opacity, Duration duration, Curve? curve, Widget? child});
}
