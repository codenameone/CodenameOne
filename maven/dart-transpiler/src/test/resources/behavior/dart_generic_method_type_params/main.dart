// Regression for a transpiler bug: a generic stub method whose type argument is
// inferred from its closure (the `Stream<R> map<R>(R convert(T event))` shape --
// see dart_async_stream.dart) bound to an untyped local emitted the method's OWN,
// still-unresolved type parameter as part of the declared Java type
// (`Stream<R> s = ...;`), which javac rejects: nothing in the generated file
// declares R. Covers that pattern bound to a local and then used -- with no
// witness at all, an explicit `<R>` witness, and a closure whose own return type
// is concrete without one -- plus (as a regression check, since these already
// erase to a concrete/dynamic element through a dedicated intrinsic rather than
// the generic stub-return path this bug lives in) the same local-binding idiom on
// Iterable/List/Future/fold/expand.
import 'dart:async';

class Person {
  final String name;
  Person(this.name);
}

void main() {
  final controller = StreamController<int>.broadcast(sync: true);

  // No witness, and the closure's own body (`x` is untyped) is itself only
  // dynamically typed -- R is neither spelled out nor inferable to anything
  // more specific, so this is the erase-to-dynamic/Object fallback. Must still
  // compile and run: before the fix the declared local type was the literal,
  // unbound `Stream<R>`.
  final results = <String>[];
  final doubled = controller.stream.map((x) => x * 2);
  doubled.listen((v) => results.add('doubled:$v'));

  // Explicit <int> witness on the same closure shape: the call site pins R
  // itself, rather than leaving it to inference.
  final pinnedResults = <String>[];
  final pinned = controller.stream.map<int>((x) => x * 2);
  pinned.listen((v) => pinnedResults.add('pinned:$v'));

  // No witness, but the closure body (a string interpolation) is concretely
  // String-typed independent of x -- R is inferred from it rather than erased.
  final tagResults = <String>[];
  final tags = controller.stream.map((x) => 'tag-$x');
  tags.listen((v) => tagResults.add(v));

  for (final v in [1, 2, 3]) {
    controller.add(v);
  }
  print('doubled: $results');
  print('pinned: $pinnedResults');
  print('tags: $tagResults');

  // Iterable<R> map<R>(...) bound to an untyped local -- already safe via a
  // dedicated intrinsic rather than this stub-return path; kept here as a
  // regression check for the same local-binding idiom.
  final people = [Person('Ann'), Person('Bo')];
  var names = people.map((p) => p.name);
  print('iterable-map: ${names.toList()}');

  // List's map, a different static receiver type through the same intrinsic.
  final nums = <int>[1, 2, 3];
  final squares = nums.map((n) => n * n);
  print('list-map: ${squares.toList()}');

  // Future<R> then<R>(...) bound to an untyped local, then used.
  final later = Future.value(10).then((v) => v + 5);
  later.then((v) => print('future-then: $v'));

  // Iterable.fold / Iterable.expand bound to untyped locals.
  final total = nums.fold(0, (sum, n) => sum + n);
  print('fold: $total');
  final expanded = nums.expand((n) => [n, n]);
  print('expand: ${expanded.toList()}');
}
