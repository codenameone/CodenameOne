String gen<T>(T x, [T? y]) => '$T:$x:$y';
String gen2<T>(T x) => '$T:$x';
class Box<E> { final E v; Box(this.v); String toString() => 'Box($v)'; }
class Outer<A> { Inner<A, List<A>> mk() => Inner<A, List<A>>(); }
class Inner<X, Y> {}
String tn<T>() => '$T';
