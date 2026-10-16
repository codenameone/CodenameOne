// Compute mode: the VM workloads from vm/benchmarks, instead of the gallery.
//
// common_workloads.dart is vm/benchmarks/dart/common_workloads.dart, copied in
// by prepare.sh: a port of com.bench.CommonWorkloads that reproduces Java's
// 32-bit wrapping, unsigned shift and String.hashCode, so both apps do the same
// work and print the same checksum. The Codename One app runs the Java source
// with the identical repetition scheme (Bench.java).
import 'common_workloads.dart' if (dart.library.js_interop) 'common_workloads_web.dart';

typedef _Work = int Function();

/// Warm-up passes before timing, then the best of the timed passes.
const int _warmup = 2;
const int _measured = 5;

final Map<String, _Work> _workloads = <String, _Work>{
  'intArithmetic': intArithmetic,
  'longArithmetic': longArithmetic,
  'mathTranscendental': mathTranscendental,
  'arraySequential': arraySequential,
  'arrayRandom': arrayRandom,
  'objectAllocation': objectAllocation,
  'valueEscape': valueEscape,
  'hashMapChurn': hashMapChurn,
  'stringBuilding': stringBuilding,
  'recursion': recursion,
  'quicksortBench': quicksortBench,
};

/// Runs every workload and prints one `BENCH:COMPUTE` line each, then
/// `BENCH:COMPUTE-DONE`. Yields between workloads so the platform's own event
/// loop keeps running.
Future<void> runCompute() async {
  for (final MapEntry<String, _Work> w in _workloads.entries) {
    int checksum = 0;
    try {
      checksum = w.value();
    } on UnsupportedError {
      // A workload this platform cannot run (64-bit integers on the web) is left
      // out, and the report shows it as not measured.
      continue;
    }
    for (int i = 1; i < _warmup; i++) {
      checksum = w.value();
    }
    int best = -1;
    for (int i = 0; i < _measured; i++) {
      final Stopwatch sw = Stopwatch()..start();
      checksum = w.value();
      sw.stop();
      final int ms = sw.elapsedMilliseconds;
      if (best < 0 || ms < best) {
        best = ms;
      }
    }
    // ignore: avoid_print
    print('BENCH:COMPUTE name=${w.key} checksum=$checksum ms=$best');
    await Future<void>.delayed(Duration.zero);
  }
  // ignore: avoid_print
  print('BENCH:COMPUTE-DONE');
}
