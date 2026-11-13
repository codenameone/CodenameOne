// Calls through mixin-typed receivers whose override sets box the Java return:
// methods, getters and mixin fields, compared with == and identical, used in
// arithmetic and as map keys.

abstract class Source {
  num next(int step);
  Object get flag;
  num get level;
}

mixin Ticker {
  int base = 1000000;
  int next(int step) => base + step;
  bool get flag => base > 10;
}

mixin Leveled {
  int level = 2000000;
}

class Clock with Ticker, Leveled implements Source {}

class Other with Ticker implements Source {
  @override
  int get level => 2000000;
}

void main() {
  Ticker a = Clock();
  Ticker b = Other();
  Leveled l1 = Clock();
  Leveled l2 = Clock();
  print('${a.next(5) == b.next(5)} ${identical(a.next(5), b.next(5))}');
  print('${a.next(5) + b.next(5)} ${a.next(5) - b.next(6)} ${a.next(1) * 2} ${a.next(4) ~/ 3}');
  print('${a.flag == b.flag} ${identical(a.flag, b.flag)} ${a.flag && b.flag}');
  print('${a.base == b.base} ${identical(a.base, b.base)} ${a.base + b.base}');
  print('${l1.level == l2.level} ${identical(l1.level, l2.level)} ${l1.level - l2.level}');
  a.base = 3000000;
  l1.level += 1;
  print('${a.base} ${a.next(0)} ${l1.level} ${l1.level > l2.level}');
  final m = <int, String>{};
  m[a.next(1)] = 'x';
  m[(Clock() as Ticker).next(3000001 - 1000000)] = 'y';
  m[l2.level] = 'z';
  m[(Other() as Source).level as int] = 'w';
  print('${m.length} ${m[3000001]} ${m[2000000]}');
  final s = <Object>{a.flag, b.flag, a.next(0), (Clock() as Ticker).next(2000000)};
  print('${s.length}');
  Source src = Clock();
  print('${src.next(1)} ${src.flag} ${src.level}');
}
