// A Flutter class name (`Key`) that a library this one does not import aliases with a
// typedef: the name resolves to the Flutter class the library sees, never to a typedef of
// a library outside its imports. Expected output is hand-written (the plain Dart SDK
// cannot run a Flutter import); every line is what the code below computes.
import 'package:flutter/material.dart';
import 'y.dart';
void main() {
  Key k = const ValueKey('a');
  print(k == const ValueKey('a'));
  print(k is ValueKey);
  final List<Key> ks = [k, UniqueKey()];
  print(ks.length);
  print(viaY('four'));
}
