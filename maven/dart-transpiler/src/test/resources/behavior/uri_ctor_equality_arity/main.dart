abstract class Same<T> {
  bool equals(T a, T b);
  int hash(T a);
}

class Caseless implements Same<String> {
  @override
  bool equals(String a, String b) => a.toLowerCase() == b.toLowerCase();
  @override
  int hash(String a) => a.length;
}

class Pairwise<T> {
  final Same<T> inner;
  Pairwise(this.inner);
  bool all(List<T> xs, List<T> ys) {
    for (var i = 0; i < xs.length; i++) {
      if (!inner.equals(xs[i], ys[i])) return false;
    }
    return true;
  }
}

class Box {
  final String label;
  Box(this.label);
  @override
  String toString() => 'Box($label)';
}

class Runner {
  final List<String> log = [];
  void push(Box b) => log.add(b.label);
}

void fire(void Function() cb) => cb();

void main() {
  var p = Pairwise<String>(Caseless());
  print(p.all(['A', 'b'], ['a', 'B']));
  print(p.all(['A'], ['c']));
  Same<String> s = Caseless();
  print(s.equals('X', 'x'));
  print(s == s);

  print(Uri(scheme: 'https', host: 'example.com', path: '/a b', query: 'q=1'));
  print(Uri(scheme: 'file', pathSegments: ['tmp', 'x y']));
  print(Uri(pathSegments: ['a', 'b']));
  print(Uri());
  print(Uri(scheme: 'http', host: 'h', port: 80, pathSegments: ['p']));
  print(Uri(scheme: 'http', host: 'h', port: 8080, queryParameters: {'k': 'v w', 'z': '1'}, fragment: 'f'));
  var u = Uri(scheme: 'https', host: 'h.io', pathSegments: ['s', 'websocket']);
  print(u.path);
  print(u.pathSegments.length);

  var src = [1, 2];
  try {
    throw ConcurrentModificationError(src);
  } on ConcurrentModificationError catch (e) {
    print(e.modifiedObject == src);
  }
  print(ConcurrentModificationError());

  var r = Runner();
  var name = 'one';
  fire(() => r.push(Box(name.isEmpty ? 'none' : name)..toString()));
  print(r.log);
}
