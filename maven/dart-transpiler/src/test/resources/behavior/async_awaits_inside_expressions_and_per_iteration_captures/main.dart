import 'dart:async';

int counter = 0;

Future<int> val(String tag, int v) async {
  print('eval $tag');
  await null;
  return v;
}

Future<bool> flag(String tag, bool v) async {
  print('flag $tag');
  return v;
}

Future<String?> maybe(String tag, String? v) async {
  print('maybe $tag');
  return v;
}

int side(String tag, int v) {
  print('side $tag');
  return v;
}

int add3(int a, int b, int c) => a + b + c;

class Box {
  int n = 0;
  List<int> items = [];
  String? label;
  Box add(int v) {
    items.add(v);
    return this;
  }

  int sum(int a, {int b = 0}) => n + a + b;
}

Box? nullBox() => null;

FutureOr<int> futureOrMethod(int x) async {
  final y = await val('fo', x);
  return y * 2;
}

Future<void> orderAndForms() async {
  print(add3(side('a', 1), await val('b', 2), side('c', 3)));
  print(side('l', 10) + await val('r', 5) * side('m', 2));
  final box = Box()..n = 7;
  print(box.sum(await val('arg', 1), b: await val('named', 2)));
  print([side('e0', 0), await val('e1', 1), side('e2', 2)]);
  print({'k${side('k', 1)}': await val('v', 2)});
  print({await val('s1', 1), side('s2', 2)});
  print('interp ${side('i1', 1)} ${await val('i2', 2)} ${side('i3', 3)}');
  final b2 = Box()
    ..add(await val('c1', 1))
    ..add(side('c2', 2))
    ..n = await val('c3', 3);
  print('${b2.items} ${b2.n}');
  print((await val('recv', 4)).toString().length);
}

Future<void> shortCircuit() async {
  print(false && await flag('and', true));
  print(true && await flag('and2', true));
  print(true || await flag('or', false));
  print(false || await flag('or2', true));
  String? s = 'have';
  print(s ?? await maybe('nn', 'no'));
  s = null;
  print(s ?? await maybe('nn2', 'yes'));
  print(side('c', 1) > 0 ? await val('then', 1) : await val('else', 2));
  print(side('c2', 0) > 0 ? await val('then2', 1) : await val('else2', 2));
  Box? nb = nullBox();
  print(nb?.sum(await val('skipped', 1)));
  nb = Box();
  print(nb?.sum(await val('called', 1)));
  final x = (await flag('cond', true)) ? 'yes' : 'no';
  print(x);
}

Future<void> compound() async {
  final list = [1, 2, 3];
  var i = 1;
  list[i] += await val('rhs', 10);
  print(list);
  final box = Box();
  box.n += await val('n', 5);
  box.label ??= await maybe('label', 'set');
  box.label ??= await maybe('label2', 'unused');
  print('${box.n} ${box.label}');
  var total = 1;
  total *= await val('mul', 6);
  print(total);
  counter += await val('top', 3);
  print(counter);
}

Future<void> conditions() async {
  if (await flag('if', true)) {
    print('if body');
  }
  var n = 0;
  while (await val('w$n', n) < 2) {
    n++;
  }
  print('while $n');
  var d = 0;
  do {
    d++;
    if (d == 1) continue;
  } while (await val('d$d', d) < 3);
  print('do $d');
  for (var k = 0; await val('f$k', k) < 2; k++) {
    print('for $k');
  }
  for (final e in await Future.value([4, 5])) {
    print('in $e');
  }
  switch (await val('sw', 2)) {
    case 1:
      print('one');
    case 2:
      print('two');
  }
}

Future<void> captures() async {
  final fs = <String Function()>[];
  for (var i = 0; i < 3; i++) {
    await null;
    fs.add(() => 'for $i');
    await null;
  }
  for (final e in ['a', 'b']) {
    await null;
    fs.add(() => 'in $e');
  }
  var w = 0;
  while (w < 2) {
    final local = w * 10;
    await null;
    fs.add(() => 'while $local');
    w++;
  }
  final muts = <void Function()>[];
  for (var j = 0; j < 2; j++) {
    var count = j;
    muts.add(() => count += 100);
    await null;
    muts.add(() => print('count $count'));
  }
  for (final f in fs) {
    print(f());
  }
  for (final m in muts) {
    m();
  }
}

Future<void> formerFallbacks() async {
  print(await futureOrMethod(4));
  final k = await val('lab', 3);
  outer:
  switch (k) {
    case 3:
      print('three before');
      if (await flag('leave', true)) break outer;
      print('never');
    default:
      print('default');
  }
  print('after labeled switch');
}

Future<void> main() async {
  await orderAndForms();
  await shortCircuit();
  await compound();
  await conditions();
  await captures();
  await formerFallbacks();
}
