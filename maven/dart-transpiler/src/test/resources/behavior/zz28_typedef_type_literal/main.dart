typedef Alias = Map<String, int>;
typedef IntFn = int Function(String);
typedef Pair<T> = Holder<T>;
typedef IntHolder = Holder<int>;
typedef Cb = void Function(int, String);
typedef Fn<T> = T Function(T);
class Holder<T> { final T v; Holder(this.v); }
class Plain {}
void main() {
  print(Alias);
  print(IntFn);
  print(IntHolder);
  print(Pair<String>);
  print(Holder);
  print(Holder<int>);
  print(Plain);
  print(Holder(1).runtimeType);
  print(Holder<num>(1).runtimeType);
  print(Holder<String>('a').runtimeType == Pair<String>);
  print(Alias == Map<String, int>);
  print(IntHolder == Holder<int>);
  Type t = Holder;
  print(t == Holder<dynamic>);
  var m = <Type, String>{Holder<int>: 'hi', Plain: 'p'};
  print(m[IntHolder]);
  print(Cb);
  print(Fn<int>);
  print(Pair);
  print(<Type>[Alias, Plain]);
}
