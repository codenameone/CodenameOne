import 'dart:collection' as col;
import 'dart:io' as io;
import 'other_lib.dart' as other;
import 'iface_lib.dart';

// LocalQueue implements a Queue whose name collides with both prefixed imports.
class LocalQueue implements Queue {
  final col.Queue<int> inner;
  LocalQueue(this.inner);
  String describe() => 'local Queue ${inner.length}';

  static Future<LocalQueue> open(List<int> xs) async {
    final col.Queue<int> q;
    try {
      q = await later(col.Queue<int>.from(xs));
    } catch (e) {
      rethrow;
    }
    return LocalQueue(q);
  }
}

Future<T> later<T>(T v) => Future<T>.delayed(Duration.zero, () => v);

Future<String> runtimeQueue() async {
  col.Queue<int> q;
  try {
    q = await later(col.Queue<int>.from([1, 2, 3]));
    q.addFirst(0);
  } catch (e) {
    return 'caught $e';
  } finally {
    print('finally runtime');
  }
  return 'runtime ${q.toList()} first=${q.first}';
}

Future<String> programType() async {
  other.Queue o;
  try {
    o = await later(other.Queue('a'));
    final other.Queue p = await later(other.Queue('b'));
    return '${o.describe()} ${p.describe()}';
  } finally {
    print('finally program');
  }
}

class NamedFile implements File {
  final io.File file;
  NamedFile._(this.file);
  String get label => 'file ${file.path}';

  static Future<NamedFile> open(String path) async {
    final io.File f;
    try {
      f = await later(io.File(path));
    } catch (e) {
      rethrow;
    }
    return NamedFile._(f);
  }
}

class TempDir implements Directory {
  final bool fresh;
  TempDir._(this.fresh);

  static Future<TempDir> make({String? prefix}) async {
    final io.Directory d;
    try {
      d = await io.Directory.systemTemp.createTemp(prefix ?? 'cps');
    } catch (e) {
      rethrow;
    }
    final ok = d.existsSync();
    d.deleteSync();
    return TempDir._(ok && !d.existsSync());
  }
}

Future<void> main() async {
  print(await runtimeQueue());
  print(await programType());
  Queue lq = await LocalQueue.open([4, 5]);
  print(lq.describe());
  File nf = await NamedFile.open('notes.txt');
  print(nf.label);
  Directory td = await TempDir.make(prefix: 'cps_case');
  print('temp ${td.fresh}');
}
