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
import 'dart:ui' show FramePhase;
import 'package:flutter/foundation.dart' show kIsWeb;
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
// When the callback that first saw the final tree ran, on the WALL clock, so
// the content frame's FrameTiming can be picked out.
//
// The content frame is the FIRST frame that built the final tree, not the one
// that announces it: FIRSTCONTENT waits for two more frames with the same
// element count to prove the tree has stopped growing, and those two repaint
// nothing -- their raster measured 0-1ms against 10-11ms for the frame that drew
// the gallery. Timing to the announcing frame charged Flutter for two frames of
// waiting that a user does not see.
//
// Found by time, not by number: PlatformDispatcher.frameData.frameNumber is not
// kept current through the start-up frames (the warm-up, the 59-element and the
// gallery frame all read 1 on macOS). And by WALL time, the one clock FrameTiming
// shares with Dart everywhere (FramePhase.rasterFinishWallTime against
// DateTime.now). Its monotonic phases matched dart:developer's Timeline on macOS
// but not on Windows, where no frame could be matched and Flutter's start-up
// went unmeasured.
int _contentSeenWallUs = -1;
// rasterFinishWallTime and raster duration of every frame reported, in
// microseconds. Kept because the content frame's timing can arrive in a batch
// BEFORE the frame that announces content has run.
final List<List<int>> _frameTimes = <List<int>>[];
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
      _contentSeenWallUs = DateTime.now().microsecondsSinceEpoch;
    }
    if (stable >= 2) {
      // ignore: avoid_print
      print('BENCH:FIRSTCONTENT after=${clock.elapsedMilliseconds}ms '
          'elements=$n renderObjects=${_countRenderObjects()}');
      _contentAnnounced = true;
      _announcePresented();
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
      //
      // Off the web it is EXACT instead. The timings carry the wall-clock moment
      // the content frame's raster finished, so
      // the line says how long ago that was and the harness subtracts it: the
      // result is the content frame on screen -- the event Codename One's marker
      // reports -- without the ~1s the engine batches timing reports for in a
      // release build, which is what made the old upper bound useless.
      if (kIsWeb) {
        if (_contentAnnounced && !_rasterAnnounced) {
          _rasterAnnounced = true;
          // ignore: avoid_print
          print('BENCH:RASTERDONE after=${_benchClock.elapsedMilliseconds}ms '
              'rasterMs=${t.rasterDuration.inMicroseconds ~/ 1000}');
        }
      } else {
        _frameTimes.add(<int>[
          t.timestampInMicroseconds(FramePhase.rasterFinishWallTime),
          t.rasterDuration.inMicroseconds,
        ]);
      }
    }
    _announcePresented();
  });
  SchedulerBinding.instance.addPostFrameCallback(watch);
  runApp(const GalleryApp());
}

/// Prints RASTERDONE for the content frame once both halves are known: that the
/// tree has settled, and the timing of the frame that first drew it.
void _announcePresented() {
  if (kIsWeb || _rasterAnnounced || !_contentAnnounced) {
    return;
  }
  // The content frame's raster finishes after the callback that saw its tree,
  // which runs at the end of its build; the previous frame's has already
  // finished. So it is the earliest raster finish at or after that callback.
  // Timings arrive in order, so the first batch holding such a frame holds it.
  List<int>? raster;
  for (final List<int> f in _frameTimes) {
    if (f[0] >= _contentSeenWallUs && (raster == null || f[0] < raster[0])) {
      raster = f;
    }
  }
  if (raster == null) {
    return;
  }
  _rasterAnnounced = true;
  final int agoUs = DateTime.now().microsecondsSinceEpoch - raster[0];
  // ignore: avoid_print
  print('BENCH:RASTERDONE after=${_benchClock.elapsedMilliseconds}ms '
      'rasterMs=${raster[1] ~/ 1000} presentedAgoUs=$agoUs');
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
