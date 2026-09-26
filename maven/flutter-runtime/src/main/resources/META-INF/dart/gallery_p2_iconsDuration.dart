// Codename One Flutter runtime API stubs — "iconsDuration" category (new_gallery, Pass 2).
//
// dart:core Duration member surface. The Duration() constructor and the
// `Duration` type itself are emitted intrinsically by the transpiler
// (Duration.of(...) / dart.core.Duration), so those paths win regardless of
// this stub. Declaring Duration as a stub class here is what lets member
// getters (inMilliseconds, inSeconds, ...) and the static `Duration.zero`
// getter resolve against the hand-written dart.core.Duration Java runtime.
//
// Conventions: named ctor params -> canonical positional order; instance
// getters -> no-arg method calls; static getters -> static field access.
//
// The remaining iconsDuration deliverables are wired directly into the
// transpiler/runtime rather than stubs: the full Icons.* constant set
// (flutter_material.dart Icons stub + com.codename1.flutter.Icons), and the
// numeric / List-factory / Iterable-helper intrinsics in JavaEmitter backed
// by dart.runtime.DartRuntime and dart.core.Dart*List.

@JavaName('dart.core.Duration')
class Duration {
  external Duration({int days, int hours, int minutes, int seconds, int milliseconds, int microseconds});
  external static Duration get zero;
  external int get inDays;
  external int get inHours;
  external int get inMinutes;
  external int get inSeconds;
  external int get inMilliseconds;
  external int get inMicroseconds;
  external bool get isNegative;
  external Duration abs();
  external int compareTo(Duration other);
}
