int n = 0;
int next() { n++; return n; }

class G {
  final List<int> v = [0, 0];
  int operator [](int i) => v[i];
  void operator []=(int i, int x) { v[i] = x; }
}

class V {
  final int x;
  const V(this.x);
  V operator -() => V(-x);
  V operator ~() => V(x * 10);
  V operator +(V o) => V(x + o.x);
}

void main() {
  var g = G();
  int y = n + (g[next()] = 5);
  print('$y ${g.v}');

  var v = const V(2);
  print((-v).x);
  print((~v).x);
  print((-(v + v)).x);

  T id<T>(T t) => t;
  print(id<int>(3) + 1);
  print(id('s').length);

}
