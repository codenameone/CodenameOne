// A function typedef (`Cb` in p.dart) and a same-named value alias in a prefixed import
// (`q.Cb` = int), next to a Flutter import: each name resolves in its own library's scope.
// Expected output is hand-written (the plain Dart SDK cannot run a Flutter import); every
// line is what the code below computes.
import 'package:flutter/material.dart';
import 'p.dart';
import 'q.dart' as q;
void main() {
  Cb c = (s) => print('got $s');
  callIt(c);
  q.Cb n = 21;
  print(q.twice(n));
  print(n.isEven);
  Object o = c;
  print(o is Cb);
  print(o is q.Cb);
  Object i = 5;
  print(i is q.Cb);
  print(q.Cb);
}
