import 'dart:async';
import 'dart:typed_data';

final _doneChunk = Uint8List.fromList([0, 13, 10]);
var counter = _startAt();
final words = 'alpha beta gamma'.split(' ');
final lengths = words.map((w) => w.length).toList();
final firstLength = lengths.first;
final stamp = Duration(seconds: 90).inMinutes;

int _startAt() => 40;

class Config {
  static final defaults = Map<String, int>.from({'retries': 3});
  static final name = 'cfg'.toUpperCase();
  static var hits = name.length;
  static final buffer = Uint8List(4);
}

void main() {
  print(_doneChunk.length);
  print(_doneChunk.sublist(1));
  counter += 2;
  print(counter.isEven);
  print(words.reversed.join(','));
  print(lengths.reduce((a, b) => a + b));
  print(firstLength + 1);
  print(stamp * 2);
  print(Config.defaults['retries']! + 1);
  print(Config.name.toLowerCase());
  Config.hits++;
  print(Config.hits);
  Config.buffer[1] = 7;
  print(Config.buffer.toList());
  // Nothing awaits these: the program must not end while they are pending.
  Timer(Duration(milliseconds: 40), () => print('late timer'));
  Future.delayed(Duration(milliseconds: 20), () => print('delayed future'));
  scheduleMicrotask(() => print('microtask'));
  print('main done');
}
