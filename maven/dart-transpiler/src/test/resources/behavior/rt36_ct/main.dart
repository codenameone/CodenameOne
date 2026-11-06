class Box<T> {
  final T v;
  const Box(this.v);
  const Box.named(this.v);
  factory Box.f(T v) => Box(v);
  factory Box.redir(T v) = Box<T>.named;
  Box.gen(T v) : this(v);
  String toString() => 'Box<$T>($v)';
}
class Num<T extends num> {
  final T v;
  Num(this.v);
  factory Num.of(T v) = Num;
  String toString() => 'Num<$T>($v)';
}
void main() {
  for (final t in [Box.new, Box.named, Box.f, Box.redir, Box.gen]) {
    dynamic d = t;
    print(d(1));
    print(Function.apply(t, ['s']));
  }
  Box<int> Function(int) a = Box.f;
  Box<String> Function(String) b = Box.redir;
  print('${a(1)} ${b('x')}');
  dynamic n = Num.of;
  print(n(2.5));
  print(Function.apply(Num.new, [3]));
  print(identical(Box.new, Box.new));
  print(Box<int>.new == Box<int>.new);
  print(Box.redir == Box.redir);
  const c = Box(1);
  print(c);
  List<Box<int>> l = [1, 2].map(Box.f).toList();
  print(l);
  print([1, 2].map(Box.redir).toList());
}
