import 'other.dart' if (dart.library.js_interop) 'other_web.dart';

abstract class Base<E> {
  int watch({dynamic key});
}

class Impl<E> implements Base<E> {
  final int n;
  Impl(this.n);
  @override
  int watch({dynamic key}) => n + (key == null ? 0 : 100);
}

class Holder<T, B extends Base<T>> {
  final B box;
  Holder(this.box);
  int go() => box.watch() + box.watch(key: 1);
}

abstract class Sub<E> implements Base<E> {}

class SubImpl extends Impl<int> implements Sub<int> {
  SubImpl(int n) : super(n);
}

int useSub(Sub<int> s) => s.watch();

abstract class Iface {
  void init(String? path);
}

class IfaceImpl implements Iface {
  final List<String> seen = [];
  @override
  void init(String? path) => seen.add(path ?? 'null');
}

extension IfaceX on Iface {
  void initTwice() {
    init('a');
    init('b');
  }
}

class Blk {
  final List<int> children;
  Blk(this.children);
}

List<int> firstKids(List<Blk> b) => b.first.children + b.last.children;
int singleLen(List<Blk> b) => b.single.children.length;

class W {}

class T1 extends W {
  String get span => 's1';
}

class T2 extends W {
  int get width => 7;
}

String? spanOf(W w) => switch (w) { T1() => w.span, T2() => 'w${w.width}', _ => null };

class Cfg {
  final int Function(int) builder;
  Cfg(this.builder);
}

class Owner {
  final Cfg widget;
  Owner(this.widget);
  int callIt() => widget.builder(2) + widget.builder.call(3);
}

MapEntry<String, int> entryOf(String k) => MapEntry(k, k.length);

Map<String, int> emptyMap() => {};
Map<String, int> fromEntries() => Map.fromEntries([MapEntry('x', 1), entryOf('abc')]);

Map<T, S> lastBy<S, T>(Iterable<S> values, T Function(S) key) =>
    {for (var element in values) key(element): element};

Map<String, int> ordinals(List<String> labels) {
  final m = {
    for (var i = 0; i < labels.length; i++)
      if (labels[i].isNotEmpty) 'fn-${labels[i]}': i,
  };
  return m;
}

abstract class Span {
  int get start;
  int get end;
}

abstract class SpanMixin implements Span {
  int get length => end - start;
}

class RealSpan extends SpanMixin {
  @override
  final int start;
  @override
  final int end;
  RealSpan(this.start, this.end);
}

class Pair {
  final String key;
  final int value;
  Pair(this.key, this.value);
}

Map<String, int> unwrap(Map<String, Pair> base) =>
    base.map((_, pair) => MapEntry(pair.key, pair.value * 2));

void main() {
  print(RealSpan(3, 10).length);
  print(unwrap({'a': Pair('x', 4)}));
  print(lastBy(['aa', 'b', 'cc'], (String s) => s.length));
  print(ordinals(['x', '', 'y']));
  print(Holder<int, Impl<int>>(Impl<int>(5)).go());
  print(useSub(SubImpl(3)));
  final i = IfaceImpl();
  i.initTwice();
  print(i.seen);
  print(firstKids([Blk([1, 2]), Blk([3])]));
  print(singleLen([Blk([4, 5, 6])]));
  print(spanOf(T1()));
  print(spanOf(T2()));
  print(spanOf(W()));
  print(Owner(Cfg((x) => x * 10)).callIt());
  final e = entryOf('hey');
  print('${e.key} ${e.value}');
  final m = emptyMap();
  m['a'] = 1;
  print(m);
  print(fromEntries());
  Map<String, List<int>> nested = {};
  nested.putIfAbsent('k', () => []).add(9);
  print(nested);
  print(kGreeting);
  print(twice(21));
}
