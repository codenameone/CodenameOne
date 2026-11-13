abstract class Req<T> {
  bool step(List<T> src);
}

abstract class _Gather<T> implements Req<T> {
  final _items = <T>[];
  final int _wanted;
  _Gather(this._wanted);
  List<T> get items => _items;
}

class _TakeSome<T> extends _Gather<T> {
  _TakeSome(super.wanted);
  @override
  bool step(List<T> src) {
    while (_items.length < _wanted && src.isNotEmpty) {
      _items.add(src.removeAt(0));
    }
    return _items.length == _wanted;
  }
}

abstract class Flags {
  int _count;
  List<int> _bits;
  Flags._(this._bits, this._count);
  int get length => _count;
  bool operator [](int i) => _bits[i] != 0;
  void operator []=(int i, bool v) => _bits[i] = v ? 1 : 0;
}

class _FixedFlags extends Flags {
  _FixedFlags(int n) : super._(List<int>.filled(n, 0, growable: true), n);
  int ones() {
    var c = 0;
    for (var i = 0; i < _count; i++) {
      if (_bits[i] != 0) c++;
    }
    return c;
  }

  void shrink() {
    _count = _count - 1;
    _bits.removeLast();
  }
}

void main() {
  final t = _TakeSome<int>(2);
  final src = [5, 6, 7];
  print(t.step(src));
  print(t.items);
  final f = _FixedFlags(4);
  f[1] = true;
  f[3] = true;
  print(f.ones());
  print([for (var i = 0; i < f.length; i++) f[i]]);
  f.shrink();
  print(f.length);
  print([for (var i = 0; i < f.length; i++) f[i]]);
}
