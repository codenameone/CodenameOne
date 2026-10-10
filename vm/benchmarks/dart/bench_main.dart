// Head-to-head runner: prints "name checksum ms" per workload, one line each,
// in the same shape the Java harness prints so the two can be diffed directly.
//
// Checksums are the contract. A Dart checksum that differs from the Java one
// means the port is not running the same computation and the timing is
// meaningless -- compare the checksum columns before believing any ratio.
import 'common_workloads.dart';

typedef Work = int Function();

void main(List<String> args) {
  final int rounds = args.isEmpty ? 1 : int.parse(args[0]);
  final Map<String, Work> work = <String, Work>{
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
  final Map<String, int> best = <String, int>{};
  final Map<String, int> sums = <String, int>{};
  for (int r = 0; r < rounds; r++) {
    work.forEach((String name, Work fn) {
      final Stopwatch sw = Stopwatch()..start();
      final int checksum = fn();
      sw.stop();
      final int ms = sw.elapsedMilliseconds;
      sums[name] = checksum;
      if (!best.containsKey(name) || ms < best[name]!) best[name] = ms;
    });
  }
  work.forEach((String name, Work _) {
    print('$name ${sums[name]} ${best[name]}');
  });
}
