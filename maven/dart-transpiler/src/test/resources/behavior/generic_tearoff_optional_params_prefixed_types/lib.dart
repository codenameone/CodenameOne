class K {
  static String s<T>(T x, [T? y]) => '$T:$x:$y';
  String m<T>(T x, {T? y}) => '$T:$x:$y';
}
class Box<E> { final E v; Box(this.v); String toString() => 'Box($v)'; }
