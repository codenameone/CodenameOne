import 'dart:async' as async;
import 'dart:collection' as col;

// Program classes sharing their names with runtime classes reached through a prefix.
class Queue {
  final String name;
  Queue(this.name);
  String describe() => 'own queue $name';
}

class Timer {
  String describe() => 'own timer';
}

class Wrapper {
  final col.Queue<int> inner;
  Wrapper(this.inner);
  void push(int v) => inner.addLast(v);
  int get size => inner.length;
}

col.Queue<String> makeQueue(List<String> items) {
  final col.Queue<String> q = col.Queue<String>();
  for (final s in items) {
    q.addFirst(s);
  }
  return q;
}

int drain(col.Queue<int> q) {
  var sum = 0;
  while (q.isNotEmpty) {
    sum += q.removeFirst();
  }
  return sum;
}

Future<void> main() async {
  final own = Queue('mine');
  print(own.describe());
  final w = Wrapper(col.Queue<int>());
  w.push(3);
  w.push(4);
  print(w.size);
  print(makeQueue(['a', 'b', 'c']).toList());
  final List<col.Queue<int>> many = [col.Queue<int>.from([1, 2]), col.Queue<int>.from([5])];
  print(many.map(drain).toList());
  Object o = col.Queue<int>();
  print(o is col.Queue);
  print(o is Timer);
  Object ot = async.Timer(Duration.zero, () {});
  print(ot is Timer);
  print(ot is async.Timer);
  print(Timer().describe());
  final done = async.Completer<String>();
  final async.Timer t = async.Timer(Duration.zero, () => done.complete('fired'));
  print(await done.future);
  print(t.isActive);
}
