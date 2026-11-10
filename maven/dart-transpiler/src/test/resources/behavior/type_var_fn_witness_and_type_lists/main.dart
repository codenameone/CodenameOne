// A function type naming a type variable is reified with the bound type in its
// signature, and an untyped literal of type literals is a List<Type> / Set<Type>.
List<T Function(T)> mk<T>() => [];
Set<Map<T, int>> ms<T>() => {};

class Holder<T> {
  List<T Function(T)> fns() => [];
  List<void Function(T?)> opt() => [];
}

void main() {
  var a = mk<int>();
  var b = mk<String>();
  print(a.runtimeType);
  print(b.runtimeType);
  print(a is List<int Function(int)>);
  print(a is List<String Function(String)>);
  print(b is List<String Function(String)>);
  print(b is List<int Function(int)>);
  print(a.runtimeType == b.runtimeType);
  print(a.runtimeType == mk<int>().runtimeType);
  print(ms<int>().runtimeType);
  print(ms<String>() is Set<Map<int, int>>);
  var h = Holder<double>().fns();
  print(h.runtimeType);
  print(h is List<double Function(double)>);
  print(h is List<int Function(int)>);
  print(Holder<bool>().opt().runtimeType);
  print([int, String].runtimeType);
  print({int, String}.runtimeType);
  var l = [int, String];
  print(l is List<Type>);
  print(l is List<Object>);
  List<Object> lo = [1];
  print(lo is List<Type>);
  print({int} is Set<Type>);
  print([int, null].runtimeType);
  var pats = ['a', RegExp('b')];
  print(pats.runtimeType);
  print(pats is List<Pattern>);
  print(pats is List<String>);
  print([1, 'a'].runtimeType);
  print([Uri.parse('x'), Uri.parse('y')].runtimeType);
}
