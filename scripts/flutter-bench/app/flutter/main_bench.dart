// Benchmark entry point: the gallery, plus one printed marker.
//
// Kept OUT of lib/main.dart so the app the goldens render and the app the
// Codename One side transpiles stay byte-identical to the Flutter SDK's own
// copy. This file only wraps it.
//
// The marker is printed on the first painted frame. The benchmark's cold-start
// figure is the wall time from launching the process to this line appearing,
// measured from outside so neither runtime is trusted for its own clock; the
// Codename One build prints the identical marker from its own wrapper.
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:gallery/main.dart';

import 'bench_compute.dart';
import 'compute_flag_io.dart' if (dart.library.js_interop) 'compute_flag_web.dart';

int _frames = 0;
// Set when FIRSTCONTENT is announced; the next FrameTiming to arrive belongs to
// a frame at or after the content frame, and carries its raster timings.
bool _contentAnnounced = false;
bool _rasterAnnounced = false;
int _buildUs = 0;
int _rasterUs = 0;

late final Stopwatch _benchClock;

void main(List<String> args) {
  // Compute mode runs the VM workloads instead of the gallery; see
  // bench_compute.dart. The same binary: both apps carry the workloads.
  if (computeRequested(args)) {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(const SizedBox.shrink());
    runCompute();
    return;
  }
  final Stopwatch clock = Stopwatch()..start();
  _benchClock = clock;
  GoogleFonts.config.allowRuntimeFetching = false;
  WidgetsFlutterBinding.ensureInitialized();
  // Flutter's FIRST frame is a warm-up frame: runApp schedules it before the
  // root widget is attached, so it paints an essentially empty tree (26
  // elements, 3 render objects) in ~10ms. Reporting that as "time to first
  // frame" and comparing it against a runtime that paints its built UI is not
  // a comparison at all.
  //
  // So both are reported: FIRSTFRAME for the warm-up, and FIRSTCONTENT for the
  // first frame after the element tree stops growing — the point at which the
  // user is actually looking at the app.
  bool announcedFirst = false;
  int previous = -1;
  int stable = 0;
  void watch(Duration _) {
    final int n = _countElements();
    if (!announcedFirst) {
      announcedFirst = true;
      // ignore: avoid_print
      print('BENCH:FIRSTFRAME after=${clock.elapsedMilliseconds}ms '
          'elements=$n renderObjects=${_countRenderObjects()}');
    }
    if (n == previous) {
      stable++;
    } else {
      stable = 0;
      previous = n;
    }
    if (stable >= 2) {
      // ignore: avoid_print
      print('BENCH:FIRSTCONTENT after=${clock.elapsedMilliseconds}ms '
          'elements=$n renderObjects=${_countRenderObjects()}');
      _contentAnnounced = true;
      // Stage split, so the comparison is not just a single number. build is
      // widget build + layout on the UI thread; raster is paint and GPU
      // submission on the raster thread. Codename One reports the same split as
      // mount/show and paint.
      // addTimingsCallback delivers a frame's timing AFTER the frame, and
      // asynchronously, so the frames that built the tree have not been
      // reported yet at this point. Wait before reporting the split.
      Timer(const Duration(milliseconds: 600), () {
        // ignore: avoid_print
        print('BENCH:STAGES frames=$_frames buildMs=${_buildUs ~/ 1000} '
            'rasterMs=${_rasterUs ~/ 1000} '
            'buildUs=$_buildUs rasterUs=$_rasterUs');
      });
      return;
    }
    SchedulerBinding.instance.addPostFrameCallback(watch);
    SchedulerBinding.instance.scheduleFrame();
  }

  SchedulerBinding.instance.addTimingsCallback((List<FrameTiming> timings) {
    for (final FrameTiming t in timings) {
      _frames++;
      _buildUs += t.buildDuration.inMicroseconds;
      _rasterUs += t.rasterDuration.inMicroseconds;
      // Codename One's own marker fires when the GPU has finished presenting,
      // so FIRSTCONTENT -- a UI-thread callback that runs BEFORE raster -- is
      // not the same event and comparing the two charges one runtime for
      // rasterising its first screen and not the other.
      //
      // This reports the moment the content frame's timings are in hand, which
      // is after its raster completed. addTimingsCallback is delivered
      // asynchronously, so it is an UPPER bound: the true present-complete time
      // lies between FIRSTCONTENT and this.
      if (_contentAnnounced && !_rasterAnnounced) {
        _rasterAnnounced = true;
        // ignore: avoid_print
        print('BENCH:RASTERDONE after=${_benchClock.elapsedMilliseconds}ms '
            'rasterMs=${t.rasterDuration.inMicroseconds ~/ 1000}');
      }
    }
  });
  SchedulerBinding.instance.addPostFrameCallback(watch);
  runApp(const GalleryApp());
}

/// How many elements exist once the first frame is on screen.
///
/// The point of comparison is not "how fast is each runtime" but "is each one
/// doing the same work". A first frame that built a tenth of the tree is not a
/// faster first frame, and comparing against it would be measuring nothing.
int _countElements() {
  int n = 0;
  void visit(Element e) {
    n++;
    e.visitChildren(visit);
  }

  final Element? root = WidgetsBinding.instance.rootElement;
  if (root != null) {
    visit(root);
  }
  return n;
}

int _countRenderObjects() {
  int n = 0;
  void visit(RenderObject r) {
    n++;
    r.visitChildren(visit);
  }

  final RenderObject? root = WidgetsBinding.instance.rootElement?.renderObject;
  if (root != null) {
    visit(root);
  }
  return n;
}
