// The motion of the gallery's home carousel, as a function you can measure.
//
// Every card is wrapped in a Transform.scale whose scale comes from the card's
// distance from the centred page. That expression IS the carousel's feel: if the two
// implementations disagree anywhere along it, cards grow and shrink differently as they
// pass the centre, which is exactly the kind of thing that reads as "they shift oddly"
// and is almost impossible to judge by eye.
//
// The body of cardScale is lifted verbatim from _CarouselState.builder in
// pages/home.dart, so this measures the real thing rather than a paraphrase of it.
//
// Run on BOTH implementations:
//   real Flutter  - benchcn1/tools/dart-reference.sh (flutter test, writes expect.txt)
//   this runtime  - BehaviorTest transpiles it, runs it on the JVM, diffs the output
import 'package:flutter/material.dart';

/// The scale a card gets when it sits [pageDelta] pages from the centre.
double cardScale(double pageDelta) {
  double value = pageDelta;
  // .3 is an approximation of the curve used in the design.
  value = (1 - (value.abs() * .3)).clamp(0, 1).toDouble();
  value = Curves.easeOut.transform(value);
  return value;
}

/// Reported as an integer so a difference in double FORMATTING cannot be mistaken for a
/// difference in the curve. Five digits is far finer than a pixel: the card is 296dp
/// wide, so 1e-5 of scale is about three thousandths of a pixel.
int fixed(double v) => (v * 100000).round();

void main() {
  // A sweep across four pages either side of centre, which covers the whole visible
  // range and both clamped tails.
  for (int i = -40; i <= 40; i++) {
    final double delta = i / 10.0;
    print('SCALE $i ${fixed(cardScale(delta))}');
  }

  // The exact points the eye actually notices: the centred card, its neighbours, and
  // where the curve reaches its floor.
  for (final double d in <double>[0.0, 0.25, 0.5, 1.0, 2.0, 3.3, 10.0]) {
    print('POINT $d ${fixed(cardScale(d))}');
  }

  // The curve must be symmetric about the centre - a card approaching from the left has
  // to grow exactly as one leaving to the right shrinks, or the carousel breathes.
  bool symmetric = true;
  for (int i = 1; i <= 40; i++) {
    if (fixed(cardScale(i / 10.0)) != fixed(cardScale(-i / 10.0))) {
      symmetric = false;
    }
  }
  print('SYMMETRIC $symmetric');

  // ...and monotonic: moving further from the centre may never make a card BIGGER.
  bool monotonic = true;
  for (int i = 0; i < 40; i++) {
    if (fixed(cardScale((i + 1) / 10.0)) > fixed(cardScale(i / 10.0))) {
      monotonic = false;
    }
  }
  print('MONOTONIC $monotonic');

  // The OTHER motion on this screen: the staggered entrance. Each category item slides
  // up from 60px of top padding over its own slice of one controller, via
  // Interval(delay, delay + .4, curve: Curves.ease) driving a Tween. Two items with
  // different delays are sampled together because the stagger is the point - if the
  // slices do not line up the same way, the page assembles itself unevenly.
  for (final double delay in <double>[0.0, 0.2]) {
    final Interval interval = Interval(0.0 + delay, 0.400 + delay, curve: Curves.ease);
    final Tween<double> pad = Tween<double>(begin: 60.0, end: 0.0);
    for (int i = 0; i <= 20; i++) {
      final double t = i / 20.0;
      print('ENTRANCE $delay $i ${fixed(pad.transform(interval.transform(t)))}');
    }
  }

  // And the horizontal one the carousel itself rides in on.
  final Interval slide = Interval(0.0, 0.400, curve: Curves.ease);
  final Tween<double> start = Tween<double>(begin: 32.0, end: 0.0);
  for (int i = 0; i <= 20; i++) {
    print('SLIDE $i ${fixed(start.transform(slide.transform(i / 20.0)))}');
  }
}
