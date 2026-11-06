import 'dart:async';

// whereType<T> inside a generic class that implements a generic interface.
class Gatherer<T> {
  final List<Object?> _values = [];
  void add(Future<T> f) {
    f.then((v) {
      _values.add(v);
      print('only: ${_values.whereType<T>().toList()}');
    });
  }
  void addRaw(Object? o) => _values.add(o);
  void close() {
    print('close: ${_values.whereType<T>().toList()}');
  }
}

// Explicit extension application, public and private (static member).
extension Sorting<T> on Iterable<T> {
  bool ascending(int Function(T, T) cmp) {
    T? prev;
    var first = true;
    for (final e in this) {
      if (!first && cmp(prev as T, e) > 0) return false;
      prev = e;
      first = false;
    }
    return true;
  }
}

class Bag<T> {
  final List<T> items;
  Bag(this.items);
  bool check(int Function(T, T) cmp) => Sorting(items).ascending(cmp);
}

extension _Fmt on String {
  static String bracket(String s, int i) => '[$s-$i]';
  String twice() => this + this;
}

// A spread of a nullable map is a map, not a set.
String headersOf({Map<String, String>? extra}) {
  final headers = {...?extra};
  if (!headers.containsKey('id')) {
    headers['id'] = 'sub-0';
  }
  return headers.toString();
}

// A generic typedef called through a field.
typedef Builder<T> = String Function(T value, int depth);
class Holder<T> {
  final Builder<T> builder;
  final T value;
  Holder(this.builder, this.value);
  String run() => builder(value, 2);
}

void main() async {
  final g = Gatherer<int>();
  g.addRaw('x');
  g.addRaw(3);
  g.add(Future.value(4));
  await Future.delayed(Duration.zero);
  g.close();

  print(Bag([1, 2, 3]).check((a, b) => a - b));
  print(Bag([3, 1]).check((a, b) => a - b));
  print(_Fmt.bracket('a', 1));
  print(_Fmt('ab').twice());

  print(headersOf());
  print(headersOf(extra: {'id': 'z', 'k': 'v'}));


  final h = Holder<int>((v, d) => 'v=$v d=$d', 7);
  print(h.run());
  final hs = Holder<String>((v, d) => v * d, 'ab');
  print(hs.run());
}
