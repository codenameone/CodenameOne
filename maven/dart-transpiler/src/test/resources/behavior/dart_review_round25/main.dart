// Fields and top-level variables declared from an UNTYPED collection literal take
// the literal's type, as Dart infers it: List<int>, Set<String>, Map<String, int>,
// or the dynamic form when the elements disagree or the literal is empty. They
// used to be dynamic, so `xs.add(3)` -- valid Dart, which throws at runtime on a
// const -- did not compile at all.

const topList = [1, 2, 3];
const topSet = {'a', 'b'};
const topMap = {'a': 1, 'b': 2};
const topMixed = [1, 'two', 3.0];
final topGrowable = [10, 20];
var topEmptyMap = {};

class Holder {
  static const staticList = ['x', 'y'];
  static const staticSet = {1, 2};
  static const staticMap = {1: 'one'};
  static var staticGrowable = <String, int>{};
  static final staticNested = [
    [1, 2],
    [3]
  ];

  final instanceList = const [4, 5];
  final instanceSet = const {'p'};
  final instanceMap = const {'k': true};
  final instanceGrowable = [7];
  var instanceEmpty = [];
}

void attempt(String label, void Function() body) {
  try {
    body();
    print('$label: allowed');
  } on UnsupportedError {
    print('$label: UnsupportedError');
  }
}

void reads() {
  final h = Holder();
  print('${topList.length} ${topList[1]} ${topList.first + topList.last}');
  print('${topSet.length} ${topSet.contains('a')}');
  print('${topMap.length} ${topMap['b']}');
  print('${topMixed.length} ${topMixed[1]}');
  var sum = 0;
  for (final x in topList) {
    sum += x;
  }
  for (final x in Holder.staticSet) {
    sum += x;
  }
  for (final x in h.instanceList) {
    sum += x;
  }
  print(sum);
  for (final k in topMap.keys) {
    print('$k=${topMap[k]}');
  }
  print(Holder.staticList.join('|'));
  print(Holder.staticMap[1]);
  print(Holder.staticNested.length + Holder.staticNested[0].length);
  print('${h.instanceSet.length} ${h.instanceMap['k']}');
}

void writes() {
  final h = Holder();
  attempt('top const list add', () => topList.add(4));
  attempt('top const list []=', () => topList[0] = 9);
  attempt('top const set add', () => topSet.add('c'));
  attempt('top const map []=', () => topMap['c'] = 3);
  attempt('top mixed add', () => topMixed.add(true));
  attempt('static const list add', () => Holder.staticList.add('z'));
  attempt('static const set add', () => Holder.staticSet.add(3));
  attempt('static const map []=', () => Holder.staticMap[2] = 'two');
  attempt('instance const list add', () => h.instanceList.add(6));
  attempt('instance const set add', () => h.instanceSet.add('q'));
  attempt('instance const map []=', () => h.instanceMap['j'] = false);
  attempt('top final list add', () => topGrowable.add(30));
  attempt('top var map []=', () => topEmptyMap['any'] = 1);
  attempt('static var map []=', () => Holder.staticGrowable['n'] = 1);
  attempt('static nested inner add', () => Holder.staticNested[1].add(4));
  attempt('instance final list add', () => h.instanceGrowable.add(8));
  attempt('instance var list add', () => h.instanceEmpty.add('anything'));
  print(topGrowable);
  print(topEmptyMap);
  print(Holder.staticGrowable);
  print(Holder.staticNested);
  print('${h.instanceGrowable} ${h.instanceEmpty}');
}

void main() {
  reads();
  writes();
}
