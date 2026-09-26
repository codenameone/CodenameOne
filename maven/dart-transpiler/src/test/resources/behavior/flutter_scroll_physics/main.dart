// How a scrollable resists being dragged past its edge, and how far a fling carries.
//
// This is the "tensile" behaviour: Codename One has its own overscroll model, Flutter has
// BouncingScrollPhysics, and the two do not agree. Rather than tune ours by feel, this
// measures Flutter's directly - the rubber band is a published, deterministic function of
// (how far you are already past the edge, how far you just dragged, how big the viewport
// is), so it can be sampled exactly.
//
// Run on BOTH implementations:
//   real Flutter  - benchcn1/tools/dart-reference.sh (flutter test, writes expect.txt)
//   this runtime  - BehaviorTest transpiles it, runs it on the JVM, diffs the output
import 'package:flutter/widgets.dart';

const double kMin = 0.0;
const double kMax = 1000.0;
const double kViewport = 800.0;

ScrollMetrics metricsAt(double pixels) => FixedScrollMetrics(
  minScrollExtent: kMin,
  maxScrollExtent: kMax,
  pixels: pixels,
  viewportDimension: kViewport,
  axisDirection: AxisDirection.down,
  devicePixelRatio: 3.0,
);

/// Reported as an integer so double FORMATTING cannot be mistaken for a difference in
/// the physics. Three decimals is far below anything a finger could feel.
int fixed(double v) => (v * 1000).round();

void main() {
  final BouncingScrollPhysics physics = const BouncingScrollPhysics();

  // 1. Inside the range the drag is passed through untouched - one pixel of finger is
  //    one pixel of content, and any resistance here would feel like drag.
  for (final double p in <double>[0.0, 1.0, 500.0, 999.0, 1000.0]) {
    print('INSIDE ${fixed(p)} ${fixed(physics.applyPhysicsToUserOffset(metricsAt(p), 10.0))}');
  }

  // 2. Past the top edge. NOTE the sign convention: Flutter applies the result as
  //    `pixels -= offset`, so a POSITIVE offset pushes further out and a negative one
  //    eases back. The labels below are deliberately neutral about direction; what
  //    matters is that resistance grows with how far out you already are, and that curve
  //    is the whole feel of the rubber band.
  for (int over = 0; over <= 300; over += 15) {
    final ScrollMetrics m = metricsAt(kMin - over.toDouble());
    print('EDGE_TOP $over ${fixed(physics.applyPhysicsToUserOffset(m, -10.0))}');
  }

  // 3. ...and past the bottom edge, where the same two branches swap over.
  for (int over = 0; over <= 300; over += 15) {
    final ScrollMetrics m = metricsAt(kMax + over.toDouble());
    print('EDGE_BOTTOM $over ${fixed(physics.applyPhysicsToUserOffset(m, 10.0))}');
  }

  // 4. The two directions are NOT symmetric: from the same overscroll, dragging one way
  //    resists differently from dragging the other, because Flutter recomputes the
  //    friction from where the drag ENDS rather than where it starts. Which sign is
  //    which is left to the numbers - the point is that they differ, and by how much.
  for (int over = 30; over <= 300; over += 30) {
    final ScrollMetrics m = metricsAt(kMin - over.toDouble());
    final int neg = fixed(physics.applyPhysicsToUserOffset(m, -10.0));
    final int pos = fixed(physics.applyPhysicsToUserOffset(m, 10.0));
    print('EASE $over $neg $pos');
  }

  // 5. Drag size matters too: resistance is not a constant factor, so a big drag from
  //    the same position is not just a small one scaled up.
  for (final double delta in <double>[1.0, 5.0, 20.0, 80.0, 200.0]) {
    final ScrollMetrics m = metricsAt(kMin - 100.0);
    print('DELTA ${fixed(delta)} ${fixed(physics.applyPhysicsToUserOffset(m, -delta))}');
  }

  // 6. Whether a fling is even allowed to start, and the minimum it must beat.
  print('TOLERANCE ${fixed(physics.minFlingVelocity)} ${fixed(physics.maxFlingVelocity)}'
      ' ${fixed(physics.minFlingDistance)}');
  print('OUTOFRANGE ${physics.shouldAcceptUserOffset(metricsAt(500.0))}'
      ' ${physics.shouldAcceptUserOffset(metricsAt(-50.0))}');
}
