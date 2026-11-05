import 'dart:collection';

// A mixin that forbids every length change, applied over two kinds of list:
// one forwarding to a wrapped list, one built on ListMixin with no length setter.
abstract mixin class FixedLengthGuard<E> implements List<E> {
  static Never _refuse() {
    throw UnsupportedError('fixed length');
  }

  @override
  set length(int newLength) => _refuse();

  // bool, not List's void: Dart lets a value-returning member override a void one.
  @override
  bool add(E value) => _refuse();

  @override
  void addAll(Iterable<E> iterable) => _refuse();

  @override
  E removeLast() => _refuse();

  @override
  void clear() => _refuse();
}

class ForwardingList<E> extends ListBase<E> {
  final List<E> _inner;
  ForwardingList(this._inner);

  @override
  int get length => _inner.length;

  @override
  set length(int newLength) {
    _inner.length = newLength;
  }

  @override
  E operator [](int index) => _inner[index];

  @override
  void operator []=(int index, E value) {
    _inner[index] = value;
  }

  @override
  void add(E value) {
    _inner.add(value);
  }
}

class FixedForwardingList<E> extends ForwardingList<E> with FixedLengthGuard<E> {
  FixedForwardingList(super.inner);
}

abstract class FlagList with ListMixin<bool> {
  final List<bool> _bits;
  FlagList(int count) : _bits = List<bool>.filled(count, false, growable: true);

  @override
  int get length => _bits.length;

  @override
  bool operator [](int index) => _bits[index];

  @override
  void operator []=(int index, bool value) {
    _bits[index] = value;
  }
}

class GrowableFlagList extends FlagList {
  GrowableFlagList(super.count);

  @override
  set length(int newLength) {
    while (_bits.length < newLength) {
      _bits.add(false);
    }
    while (_bits.length > newLength) {
      _bits.removeLast();
    }
  }
}

class FixedFlagList extends FlagList with FixedLengthGuard<bool> {
  FixedFlagList(super.count);
}

String attempt(void Function() f) {
  try {
    f();
    return 'ok';
  } on UnsupportedError catch (e) {
    return 'refused: ${e.message}';
  }
}

void main() {
  // String.runes: code points, surrogate pairs decoded as one, lone surrogates kept.
  var plain = 'abc';
  print(plain.runes);
  print(plain.runes.length);
  var emoji = 'a\u{1F600}b';
  print(emoji.length);
  print(emoji.codeUnits);
  print(emoji.runes.toList());
  print(emoji.runes.length);
  print(emoji.runes.first);
  print(emoji.runes.last);
  print(emoji.runes.map((r) => r.toRadixString(16)).join(' '));
  print(String.fromCharCodes(emoji.runes));
  var lone = 'x\uD800y';
  print(lone.runes.toList());
  print(''.runes.isEmpty);
  var sum = 0;
  for (final r in '\u{10000}\u{10FFFF}'.runes) {
    sum += r;
  }
  print(sum);
  Iterable<int> asIterable = 'hi'.runes;
  print(asIterable.contains(105));

  // A fixed-length mixin over a forwarding list: reads go through, growth is refused.
  var backing = <int>[1, 2, 3];
  var fixed = FixedForwardingList<int>(backing);
  fixed[0] = 10;
  print(fixed);
  print(backing);
  print(attempt(() => fixed.add(4)));
  print(attempt(() => fixed.length = 1));
  print(attempt(() => fixed.clear()));
  print(fixed.length);
  var open = ForwardingList<int>(backing);
  open.add(4);
  print(backing);

  // The same mixin over a ListMixin list that declares no length setter.
  var flags = FixedFlagList(3);
  flags[1] = true;
  print(flags);
  print(attempt(() => flags.length = 5));
  print(attempt(() => flags.add(true)));
  print(attempt(() => flags.removeLast()));
  print(flags.length);
  var growable = GrowableFlagList(2);
  growable.length = 4;
  growable[3] = true;
  print(growable);
}
