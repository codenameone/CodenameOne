class Vec { final num x, y; const Vec(this.x, this.y); static const zero = Vec(0, 0); }
class Tag { final String n; final List<int> v; const Tag(this.n, this.v); }
const k = Vec(1, 2);
void main() {
  print(identical(Vec.zero, const Vec(0, 0)));
  print(identical(k, const Vec(1, 2)));
  print(identical(const Vec(1, 2), const Vec(2, 1)));
  print(identical(Vec(0, 0), Vec(0, 0)));
  print(identical(const [1, 2], const [1, 2]));
  print(const [1, 2] == const [1, 2]);
  print(const [1, 2] == const [2, 1]);
  print(identical(const {'a': 1}, const {'a': 1}));
  print(identical(const Tag('t', [1]), const Tag('t', [1])));
  print(identical(const Tag('t', [1]), const Tag('t', [2])));
  print(identical(const <int>{1, 2}, const <int>{1, 2}));
}
