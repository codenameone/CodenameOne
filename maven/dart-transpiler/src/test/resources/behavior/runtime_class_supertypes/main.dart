import 'dart:async';
import 'dart:collection';
import 'dart:io';

// implements a runtime class with no superclass of its own: extends it.
class CountingTimer implements Timer {
  final Timer _inner;
  int resets = 0;
  CountingTimer(Duration d, void Function() cb) : _inner = Timer(d, cb);
  @override
  bool get isActive => _inner.isActive;
  @override
  int get tick => _inner.tick;
  @override
  void cancel() => _inner.cancel();
}

class AppFailure implements Exception {
  final String why;
  AppFailure(this.why);
  String toString() => 'AppFailure: $why';
}

class BadInput implements FormatException {
  @override
  final String message;
  BadInput(this.message);
  @override
  dynamic get source => null;
  @override
  int? get offset => 3;
}

// implements a runtime class while extending a program class: the members stay.
class SocketFailure extends AppFailure implements SocketException {
  SocketFailure(String why) : super(why);
  @override
  String get message => 'socket: $why';
  @override
  InternetAddress? get address => null;
  @override
  OSError? get osError => null;
  @override
  int? get port => 8080;
}

abstract class Base {
  int? get size;
}

class Sized extends Base {
  @override
  int get size => 7;
}

class Ring<E, L extends List<E>> with ListMixin<E> {
  final L _table;
  final List<E> Function() _grow;
  Ring(this._table, this._grow);
  @override
  int get length => _table.length;
  @override
  set length(int n) => throw UnsupportedError('fixed');
  @override
  E operator [](int i) => _table[i];
  @override
  void operator []=(int i, E v) => _table[i] = v;
  L _same() => _table;
  List<E> both(bool g) {
    var l = g ? Grow<E>(_table.first) : _same();
    return l;
  }
  @override
  List<E> toList({bool growable = true}) {
    var list = growable ? _grow() : _same();
    return list;
  }

  @override
  void setRange(int start, int end, Iterable<E> iterable, [int skipCount = 0]) {
    print('own setRange $start $end');
    var src = iterable.skip(skipCount).toList();
    for (var i = start; i < end; i++) {
      _table[i] = src[i - start];
    }
  }
}

class Grow<E> with ListMixin<E> {
  final E only;
  Grow(this.only);
  @override
  int get length => 1;
  @override
  set length(int n) {}
  @override
  E operator [](int i) => only;
  @override
  void operator []=(int i, E v) {}
}

void main() {
  var fired = false;
  var t = CountingTimer(const Duration(hours: 1), () => fired = true);
  Timer asTimer = t;
  print(asTimer.isActive);
  t.cancel();
  print(t.isActive);
  print(fired);
  FormatException fe = BadInput('nope');
  print('${fe.message} ${fe.offset}');
  var s = SocketFailure('down');
  print('${s.message} ${s.port} ${s.address}');
  Base b = Sized();
  print(b.size);
  var r = Ring<int, List<int>>([1, 2, 3, 4], () => [9]);
  r.setRange(1, 3, [7, 8]);
  print(r.toList());
  print(r.toList(growable: false));
  print(r.both(true));
  print(r.both(false).length);
  List<int> asList = r;
  asList.setRange(0, 1, [5]);
  print(r.toList(growable: false));
}
