// A program class whose simple name is also a runtime (dart:io / dart:async)
// class name, reached through every position a class name can be written in,
// including a conditional prefixed import whose target re-exports a const
// tear-off of a static method.
import 'socket.dart';
import 'timer.dart' as t;

void main() {
  // constructor call, static call, static field, type annotation
  WebSocket a = WebSocket('a');
  print(a.describe());
  print(WebSocket.count);
  // conditional import + const tear-off
  WebSocket b = WebSocket.open('b');
  print(b.describe());
  // tear-off of a static method, stored and called
  WebSocket Function(String) f = WebSocket.make;
  print(f('c').describe());
  // const constructor and const instance
  const k = WebSocket.konst('k');
  print(k.url);
  // casts and type tests
  Object o = b;
  print(o is WebSocket);
  print((o as WebSocket).url);
  print(o is! WebSocket);
  // generics
  final list = <WebSocket>[a, b];
  print(list.map((s) => s.url).join(','));
  List<WebSocket> typed = list.whereType<WebSocket>().toList();
  print(typed.length);
  // a second collision: dart:async Timer, through a prefix
  final tm = t.Timer(3);
  print(tm.ticks);
  print(t.Timer.zero().ticks);
  print(t.Timer.periodicName);
  // a future of the program class
  WebSocket.connect('d').then((s) => print('connected ${s.url}'));
}
