class Box<T> { final T v; Box(this.v); Box<R> map<R>(R Function(T) f) => Box<R>(f(v)); }
void main() {
  Map<String, Iterable<int>> mk = {'z': [8]};
  print(mk['z']!.first);
  print(Box<int>(3).map((x) => 'n$x').v.length);
}
