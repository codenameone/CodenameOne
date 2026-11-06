import 'dart:async';

class Part { final String sku; final int count; Part(this.sku, this.count); }
class Tool { final String sku; Tool(this.sku); }

class Box<T> {
  final List<T> items = [];
  Box();
  factory Box.of(Iterable<T> it) { final b = Box<T>(); for (final e in it) b.items.add(e); return b; }
  T get first => items.first;
}

void main() async {
  final things = <Object>[Part('p1', 2), Tool('t1'), Part('p2', 5), 'other'];
  print([for (var i in things) if (i is Part) ...[i.sku, i.count] else if (i is Tool) i.sku]);
  print({for (var i in things) if (i is Part) i.sku: i.count});

  final b = Box.of([1, 2, 3]);
  print(b.items); print(b.first + 1);
  final bs = Box.of(['a', 'b']);
  print(bs.first.toUpperCase());

  print(Iterable.generate(3, (i) => 'x$i').followedBy(['y']).toList());
  print(Iterable.generate(3, (i) => i * 2).map((v) => v + 1).toList());

  print(await Stream.fromIterable([3, 1, 2]).asyncMap((x) async => x * 10).expand((x) => [x, x + 1]).toList());
  print(await Stream.fromIterable([1, 2]).asyncMap((x) async { await Future.delayed(Duration.zero); return 'v$x'; }).map((s) => s.length).toList());

  const big = 9007199254740993;
  print(big.clamp(9007199254740992, 9007199254740994.0));
  print(9007199254740993.clamp(0, 9007199254740992.0));
  print(5.clamp(1, 3)); print(3.7.clamp(1, 3)); print(2.clamp(2.5, 4));
  print(9007199254740993 < 9007199254740992.0);
  print(9007199254740993 > 9007199254740992.0);
}
