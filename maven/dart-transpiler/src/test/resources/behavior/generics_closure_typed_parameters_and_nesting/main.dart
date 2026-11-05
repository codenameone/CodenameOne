T first<T>(List<T> xs) => xs.first;
S apply<S, T>(S Function(T) f, T x) => f(x);
void main() {
  var gl = <T>(List<T> xs) { var c = xs; c.add(xs.first); return c; };
  var r = gl<int>([1]);
  print(r.runtimeType);
  var mk = <T>() => <T, List<T>>{};
  print(mk<int>().runtimeType);
  var cast = <T>(Object o) => o is T;
  print(cast<int>(1)); print(cast<String>(1));
  var conv = <T>(dynamic o) => o as T;
  try { conv<String>(1); print('no'); } catch (e) { print('TE ${e is TypeError}'); }
  print(apply<String, int>((i) => 'n$i', 4));
  int Function(List<int>) fi = first;
  print(fi([9]));
  var nested = <T>(T x) => <U>(U y) => '$T $U';
  print(nested<int>(1)<String>('s'));
  print(nested(1.0)(true));
  Function f = cast;
  print(f.runtimeType);
}
