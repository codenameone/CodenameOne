import 'dart:async';
import 'dart:collection';

void main() async {
  final hm = HashMap<String, int>();
  hm['a'] = 1;
  print(hm['a']);
  final lhm = LinkedHashMap<String, int>();
  lhm['z'] = 1;
  lhm['y'] = 2;
  print(lhm.keys.toList());
  final hs = HashSet<int>();
  hs.add(3);
  print(hs.contains(3));
  final lhs = LinkedHashSet<String>();
  lhs.addAll(['b', 'a', 'b']);
  print(lhs.toList());
  final sts = SplayTreeSet<int>();
  sts.addAll([5, 1, 3]);
  print(sts.toList());
  final desc = SplayTreeSet<int>((x, y) => y.compareTo(x));
  desc.addAll([5, 1, 3]);
  print(desc.toList());
  final stm = SplayTreeMap<String, int>();
  stm['b'] = 2;
  stm['a'] = 1;
  print(stm.keys.toList());
  final q = Queue<int>();
  q.add(1);
  q.addFirst(0);
  q.addLast(2);
  print('${q.removeFirst()} ${q.removeLast()} ${q.length}');
  final lq = ListQueue<String>();
  lq.add('x');
  print(lq.first);
  final dq = DoubleLinkedQueue<int>();
  dq.add(7);
  print(dq.length);
  final um = UnmodifiableMapView<String, int>({'k': 1});
  print(um['k']);
  final ul = UnmodifiableListView<int>([1, 2]);
  print(ul.length);
  final List<Object> objs = ['s', 't'];
  final strs = objs.cast<String>();
  print(strs.first.length);
  final it = strs.iterator;
  while (it.moveNext()) {
    print(it.current.toUpperCase());
  }
  final f1 = await Future.sync(() => 5);
  print(f1);
  try {
    await Future.error(StateError('boom'));
  } on StateError catch (e) {
    print(e.message);
  }
  final f2 = await Future.microtask(() => 'mt');
  print(f2);
  final comp = Completer<String>.sync();
  comp.complete('done');
  print(await comp.future);
  Iterable<int?> xs = [1, null, 2];
  print(xs.where((x) => x != null).length);
  final ctl = StreamController<int>();
  final Future<List<Object?>> fut = ctl.stream.toList();
  ctl.add(1);
  ctl.add(2);
  await ctl.close();
  final got = await fut;
  print(got);
  List<int> nums = [1, 2, 3, 4];
  print(nums.reduce((a, b) => a + b));
}
