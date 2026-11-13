import 'dart:async';
import 'dart:collection';
import 'dart:convert';

enum Role { admin, user }
enum Level {
  low(1), high(5);
  final int weight;
  const Level(this.weight);
  String get tag => 'L$weight';
  int doubled() => weight * 2;
  int against(Level other) => weight - other.weight;
}

class Item {
  final Role role;
  final int n;
  Item(this.role, this.n);
  factory Item.fromJson(Map<String, dynamic> j) =>
      Item(Role.values.byName(j['role'] as String), j['n'] as int);
}

Future<void> runA(FutureOr<void> Function() f) async => await f();
Future<int> runB(FutureOr<int> Function() f) async => await f();

void main() async {
  final List raw = [1, 2];
  final items = raw.map((e) => Item(Role.admin, e as int)).toList();
  print(items is List<Item>);
  print(items.first.n.isEven);
  print(items.first.role.index);
  print(items.first.role.name);
  print(items.last.role);
  final more = raw.expand((e) => [Item(Role.user, e as int)]).toList();
  print(more.first.role.index + more.length);

  final parsed = (jsonDecode('[{"role":"user","n":4},{"role":"admin","n":5}]') as List)
      .map((e) => Item.fromJson(e))
      .toList();
  print(parsed.first.role.index);
  print(parsed.map((i) => i.role.name).join(','));

  dynamic d = Role.user;
  print(d.index);
  try {
    print(d.name);
  } on NoSuchMethodError {
    print('name is an extension');
  }
  print(d);
  print(d.toString());
  dynamic lv = Level.high;
  print(lv.weight);
  print(lv.tag);
  print(lv.doubled());
  print(lv.against(Level.low));
  print(lv.index);
  dynamic r0 = Role.admin;
  try {
    print(r0.compareTo(Role.user) < 0);
  } on NoSuchMethodError {
    print('plain enums are not Comparable');
  }

  print(Role.values.byName('user'));
  try {
    Role.values.byName('nobody');
  } on ArgumentError catch (e) {
    print('arg ${e.runtimeType}');
  }

  print(num.parse('42'));
  print(num.parse('2.5'));
  print(num.parse('42') is int);
  print(num.tryParse('x'));
  print(num.tryParse('1e3'));
  try {
    num.parse('q');
  } on FormatException catch (e) {
    print(e.message);
  }

  Object? x = 3;
  print(x.runtimeType != Null);
  Object? y;
  print(y.runtimeType == Null);
  print(x.runtimeType == Null);

  final sm = SplayTreeMap<String, int>.from({'b': 2, 'a': 1, 'c': 3});
  print(sm);
  print(sm.keys.first);
  final sm2 = SplayTreeMap<String, int>.of({'z': 1, 'y': 2});
  print(sm2.keys.toList());
  final sm3 = SplayTreeMap<int, String>.fromIterable([3, 1, 2], value: (e) => 'v$e');
  print(sm3);
  final ss = SplayTreeSet<int>.from([5, 1, 3]);
  print(ss);
  final ss2 = SplayTreeSet<int>.of([9, 7]);
  print(ss2.first);

  await runA(() => print('ranA'));
  await runA(() async => print('ranA2'));
  print(await runB(() => 7));
  print(await runB(() async => 8));

  try {
    int.parse('zz');
  } on FormatException catch (e) {
    print(e.message);
    print(e.source);
  }
  try {
    double.parse('abc');
  } on FormatException catch (e) {
    print(e.message);
    print(e.source);
  }
  try {
    Uri.parse('http://[bad');
  } on FormatException catch (e) {
    print(e.message);
  }
  try {
    jsonDecode('{bad');
  } on FormatException catch (e) {
    print(e.message);
  }
}
