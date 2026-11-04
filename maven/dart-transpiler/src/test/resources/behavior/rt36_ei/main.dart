T gen<T>(T x) => x;
class G<T> { final T v; G(this.v); String toString() => 'G<$T>($v)'; }
void main() {
  final Object a = gen<dynamic>;
  final Object b = gen;
  print(a == b);
  print(gen<dynamic> == gen<dynamic>);
  print(gen<Object> == gen<Object?>);
  final Object c = G<Object>.new;
  final Object d = G.new;
  print(c == d);
  print(G<Object>.new == G<Object>.new);
  print(G<dynamic>.new == G<Object?>.new);
  print((a as dynamic)(1));
  print((c as dynamic)(2));
  print((d as dynamic)(3));
}
