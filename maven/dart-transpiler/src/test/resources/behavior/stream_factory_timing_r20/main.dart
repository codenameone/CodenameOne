import 'dart:async';

class FailingIterator implements Iterator<int> {
  int _n = 0;
  @override
  int get current => _n;
  @override
  bool moveNext() {
    _n++;
    if (_n > 1) throw 'iterBoom';
    return true;
  }
}

class FailingIterable extends Iterable<int> {
  @override
  Iterator<int> get iterator => FailingIterator();
}

void main() {
  scheduleMicrotask(() => print('m1'));
  Stream<int>.fromIterable([1, 2, 3]).listen((v) => print('it $v'),
      onDone: () => print('it done'));
  Stream<int>.error('bad').listen((_) {},
      onError: (e) => print('err $e'), onDone: () => print('err done'));
  scheduleMicrotask(() => print('m2'));
  Stream<String>.value('v').listen((v) => print('value $v'),
      onDone: () => print('value done'));
  Stream<int>.empty().listen((_) {}, onDone: () => print('empty done'));
  Stream<int>.fromIterable(FailingIterable()).listen((v) => print('f $v'),
      onError: (e) => print('f err $e'), onDone: () => print('f done'));
  Future(() {
    print('event');
    final s = Stream<int>.fromIterable([10, 20]);
    s.listen((v) => print('a $v'));
    s.listen((v) => print('b $v'));
    late StreamSubscription<int> sub;
    sub = Stream<int>.fromIterable([7, 8, 9]).listen((v) {
      print('p $v');
      if (v == 7) {
        sub.pause();
        scheduleMicrotask(() {
          print('resume');
          sub.resume();
        });
      }
    }, onDone: () => print('p done'));
    scheduleMicrotask(() => print('m3'));
  });
}
