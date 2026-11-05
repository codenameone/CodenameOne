import 'dart:async';

class Holder<T> { final T? v; Holder([this.v]); }
class Pair<A, B> { }
class Late<T> { }
class Late2<T> { }

typedef F = int Function(String a, {int? b, required String c});
typedef G = int Function([int]);
typedef H = int Function(String a, [int? b, double c]);
typedef K = void Function({int x, String? y});
typedef F2 = int Function(String, {required String c, int? b});

Type typeOf<T>() => T;
int fn(String a, {int? b, required String c}) => 0;
int opt([int x = 0]) => x;

void main() {
  // Raw generic class literals before any instance or other reference.
  print(Late);
  print(Late2<int>);
  print(Late == typeOf<Late<dynamic>>());
  print(Holder);
  print(Pair);
  print(Holder<int>);
  // Raw core generic literals.
  print(List);
  print(Map);
  print(Set);
  print(Iterable);
  print(Future);
  print(Stream);
  print(List == typeOf<List<dynamic>>());
  print(Map == typeOf<Map<dynamic, dynamic>>());
  print(Set == typeOf<Set<dynamic>>());
  print(Future == typeOf<Future<dynamic>>());
  print(List == typeOf<List<int>>());
  print(<dynamic>[].runtimeType == List);
  print([1, 2].runtimeType);
  print(Holder(1).runtimeType);
  // Function typedefs with named and optional parameters.
  print(F);
  print(G);
  print(H);
  print(K);
  print(F == F2);
  print(F == G);
  print(G == typeOf<int Function([int])>());
  print(typeOf<int Function(String a, {int? b, required String c})>() == F);
  print(typeOf<int Function(String, {int? b, String c})>() == F);
  print(typeOf<int Function(String, [int?, double])>() == H);
  print(typeOf<int Function(String, int?, double)>() == H);
  print(fn.runtimeType == F);
  print(F == fn.runtimeType);
  print(opt.runtimeType == G);
  print(fn is F);
  print(opt is G);
  print(opt is F);
  print(<F>[fn].runtimeType);
  print(<G?>[].runtimeType);
  print({F: 1}[F2]);
  print(Holder<dynamic>().runtimeType == Holder);
}
