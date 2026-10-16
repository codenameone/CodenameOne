// Two Set review findings:
//
// `set.removeAll(iterable)` and `set.retainAll(iterable)` had no case in the
// collection method table (keyed on receiver type + method name), so both
// fell through to the generic "cannot resolve method" error, E0137, even
// though DartSet.removeAll(Iterable) already existed in the runtime --
// retainAll did not exist there at all.
//
// `identitySet.toSet()` answered a plain, equality-based DartSet instead of
// another identity set: the real SDK's `Set.toSet()`/`union`/`intersection`/
// `difference` all copy into a set of the SAME kind as the receiver (an
// identity set stays an identity set), which matters because an identity
// set's whole point is to keep two separately built but `==`-equal elements
// apart.

class K {
  final String v;
  K(this.v);

  @override
  bool operator ==(Object other) => other is K && other.v == v;

  @override
  int get hashCode => Object.hashAll([v]);

  @override
  String toString() => 'K($v)';
}

void removeAllAndRetainAllOnAnEqualitySet() {
  // Equal-but-not-identical arguments: an equality set matches by `==`.
  final eq = <K>{K('a'), K('b'), K('c')};
  eq.removeAll([K('b')]);
  print('eq after removeAll(equal-not-identical): ${eq.length}');
  print('eq contains a: ${eq.contains(K('a'))}');
  print('eq contains b: ${eq.contains(K('b'))}');

  final eq2 = <K>{K('a'), K('b'), K('c')};
  eq2.retainAll([K('a'), K('c')]);
  print('eq2 after retainAll(equal-not-identical): ${eq2.length}');
  print('eq2 contains a: ${eq2.contains(K('a'))}');
  print('eq2 contains b: ${eq2.contains(K('b'))}');
  print('eq2 contains c: ${eq2.contains(K('c'))}');
}

void removeAllAndRetainAllOnAnIdentitySet() {
  final k1 = K('a');
  final k1b = K('a'); // equal to k1, but a different object
  final k2 = K('b');
  final k3 = K('c');

  final id = Set<K>.identity();
  id.addAll([k1, k2, k3]);
  id.removeAll([k1b]); // equal but not identical: nothing is removed
  print('id after removeAll(equal-not-identical): ${id.length}');
  id.removeAll([k1]); // identical: k1 is removed
  print('id after removeAll(identical): ${id.length}');

  final id2 = Set<K>.identity();
  id2.addAll([k1, k2, k3]);
  id2.retainAll([k1b, k2]); // k1 drops (k1b is not identical to it), k2 stays, k3 drops
  print('id2 after retainAll(identity semantics): ${id2.length}');
  print('id2 contains k2: ${id2.contains(k2)}');
  print('id2 contains k1: ${id2.contains(k1)}');

  // A removeAll argument at least as large as the receiver: on the real SDK's
  // _HashSet this forces a different internal path than a one-element argument
  // does, and that path must stay identity-based too.
  final id3 = Set<K>.identity();
  id3.addAll([k1, k2]);
  id3.removeAll([k1b, K('x'), K('y')]);
  print('id3 after large-argument removeAll(equal-not-identical): ${id3.length}');
  print('id3 contains k1: ${id3.contains(k1)}');
}

void toSetOnAnIdentitySetStaysAnIdentitySet() {
  final k1 = K('a');
  final k1b = K('a'); // equal to k1, but a different object

  final id = Set<K>.identity();
  id.add(k1);
  final copy = id.toSet();
  // If the copy stayed identity-kind, k1b is a NEW element (not identical to
  // k1) and this add succeeds; an equality-kind copy would reject it as a
  // duplicate of k1.
  final added = copy.add(k1b);
  print('copy.add(equal-not-identical) succeeded: $added');
  print('copy length: ${copy.length}');
  print('copy contains k1 and k1b separately: ${copy.contains(k1) && copy.contains(k1b)}');
}

void main() {
  removeAllAndRetainAllOnAnEqualitySet();
  removeAllAndRetainAllOnAnIdentitySet();
  toSetOnAnIdentitySetStaysAnIdentitySet();
}
