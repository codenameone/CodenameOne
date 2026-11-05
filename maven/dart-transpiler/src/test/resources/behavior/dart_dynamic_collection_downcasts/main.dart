// A dynamic value assigned to a parameterized collection type is a checked downcast:
// Dart reifies type arguments, so a List<dynamic> is no List<int> even when every
// element is an int, and the failure is a TypeError.
import 'dart:async';

dynamic hide(Object? o) => o;

int sumInts(List<int> xs) {
  var s = 0;
  for (final x in xs) {
    s += x;
  }
  return s;
}

Future<void> main() async {
  final dynamic typedInts = <int>[1, 2, 3];
  List<int> a = typedInts;
  a.add(4);
  print(a);
  print(sumInts(typedInts));
  print(identical(a, typedInts));

  final dynamic inferred = hide([5, 6]);
  List<int> b = inferred;
  print(b.length);

  final dynamic untyped = hide(<dynamic>[1, 2]);
  try {
    List<int> c = untyped;
    print('accepted $c');
  } on TypeError {
    print('List<dynamic> is not List<int>');
  }

  final dynamic objs = hide(<Object>['a']);
  try {
    List<String> d = objs;
    print('accepted $d');
  } on TypeError {
    print('List<Object> is not List<String>');
  }

  final dynamic strs = hide(<String>['x', 'y']);
  List<String> e = strs;
  List<Object> e2 = strs;
  print('${e.join()} ${e2.length}');

  final dynamic doubles = hide(<double>[1.5]);
  List<double> f = doubles;
  print(f.first + 1);

  final dynamic m = hide(<String, int>{'a': 1});
  Map<String, int> g = m;
  print(g['a']);
  final dynamic dm = hide(<String, dynamic>{'a': 1});
  try {
    Map<String, int> h = dm;
    print('accepted $h');
  } on TypeError {
    print('Map<String, dynamic> is not Map<String, int>');
  }

  final dynamic fut = hide(Future<int>.value(7));
  Future<int> i = fut;
  print(await i);
  final dynamic dfut = hide(7);
  try {
    Future<int> j = dfut;
    print('accepted $j');
  } on TypeError {
    print('int is not Future<int>');
  }

  final dynamic st = hide(Stream<int>.fromIterable([1, 2]));
  Stream<int> k = st;
  print(await k.toList());
  final dynamic dst = hide(<int>[1, 2]);
  try {
    Stream<int> l = dst;
    print('accepted $l');
  } on TypeError {
    print('List<int> is not Stream<int>');
  }

  final dynamic notList = hide('nope');
  try {
    List<int> n = notList;
    print('accepted $n');
  } on TypeError {
    print('String is not List<int>');
  }
}
