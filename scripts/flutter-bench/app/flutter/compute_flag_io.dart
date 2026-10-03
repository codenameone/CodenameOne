// Native builds: the harness asks for compute mode with an argument (Android's
// dart_entrypoint_args intent extra) or a marker file (desktop), the same file
// the Codename One app checks.
import 'dart:io';

bool computeRequested(List<String> args) {
  if (args.contains('compute')) {
    return true;
  }
  try {
    return File('/tmp/nat/BENCH_COMPUTE').existsSync();
  } catch (_) {
    return false;
  }
}
