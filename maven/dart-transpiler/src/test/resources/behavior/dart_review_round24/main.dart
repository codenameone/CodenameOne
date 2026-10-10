// Const collection literals are unmodifiable -- written `const [...]`, and inside a
// const context, where Dart makes them const implicitly: a const variable's
// initializer, a const literal's elements, a const constructor's arguments.
// (-0.0).toStringAsFixed keeps the sign. A RegExp group index past 32 bits is a
// RangeError, not group 0. Future callbacks run synchronously in this runtime's
// blocking-await model (see Future.onComplete): the order below is the CURRENT
// behaviour, pinned on purpose, and differs from Dart, which prints sync first.

const topList = <int>[1, 2];
const topMap = <String, int>{'a': 1};
const topIntMap = <int, int>{1: 2};

class Holder {
  static const List<String> names = ['x', 'y'];
  final List<int> values;
  const Holder(this.values);
}

void attempt(String label, void Function() body) {
  try {
    body();
    print('$label: allowed');
  } on UnsupportedError {
    print('$label: UnsupportedError');
  }
}

void constCollections() {
  const local = <String>['a'];
  const nested = <List<int>>[
    [1]
  ];
  const holder = Holder([7]);
  final mutable = [1];
  attempt('top list add', () => topList.add(3));
  attempt('top list set', () => topList[0] = 9);
  attempt('top map put', () => topMap['b'] = 2);
  attempt('top int map put', () => topIntMap[3] = 4);
  attempt('local add', () => local.add('b'));
  attempt('nested inner add', () => nested[0].add(2));
  attempt('static field add', () => Holder.names.add('z'));
  attempt('ctor arg add', () => holder.values.add(8));
  attempt('explicit const list', () => const [1, 2].add(3));
  attempt('explicit const set', () => const {1, 2}.add(3));
  attempt('explicit const map remove', () => const {'k': 1}.remove('k'));
  attempt('const sort', () => const [3, 1].sort());
  attempt('mutable add', () => mutable.add(2));
  print(topList);
  print(topMap);
  print(topIntMap);
  print(local.length + nested[0].first + holder.values.first);
  print(mutable);
}

void negativeZero() {
  print((-0.0).toStringAsFixed(2));
  print((-0.0).toStringAsFixed(0));
  print((0.0).toStringAsFixed(2));
  print((-0.001).toStringAsFixed(1));
}

void groups() {
  final m = RegExp(r'(b)').firstMatch('abc')!;
  print(m.group(1));
  for (final i in [1 << 32, -1, 2]) {
    try {
      print(m.group(i));
    } on RangeError {
      print('group $i: RangeError');
    }
  }
}

void futureOrder() {
  final events = <String>[];
  Future.value(1).then((_) => events.add('future'));
  events.add('sync');
  print(events);
}

void main() {
  constCollections();
  negativeZero();
  groups();
  futureOrder();
}
