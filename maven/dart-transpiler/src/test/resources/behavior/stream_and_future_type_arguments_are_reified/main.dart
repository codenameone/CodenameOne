import 'dart:async';
abstract class Event {}
class Login extends Event { final String who; Login(this.who); }
class Logout extends Event {}
Stream<T> pass<T>(Stream<T> s) => s;
void main() async {
  final ctl = StreamController<Event>();
  final s = ctl.stream;
  print('streamIsEventStream ${s is Stream<Event>}');
  print('streamIsLoginStream ${s is Stream<Login>}');
  print('streamIsObjectStream ${s is Stream<Object>}');
  print('controllerIsLoginController ${ctl is StreamController<Login>}');
  print('controllerIsEventSink ${ctl is StreamSink<Event>}');
  final bc = StreamController<Login>.broadcast();
  print('broadcastIsEventStream ${bc.stream is Stream<Event>}');
  print('broadcastIsLogoutStream ${bc.stream is Stream<Logout>}');
  print('passedThroughGeneric ${pass(bc.stream) is Stream<Logout>}');
  Object fi = Future<int>.value(1);
  print('futureIntIsFutureString ${fi is Future<String>}');
  print('futureIntIsFutureNum ${fi is Future<num>}');
  print('futureIntIsFutureInt ${fi is Future<int>}');
  Object fs = Future<String>.value('x');
  print('futureStringIsFutureInt ${fs is Future<int>}');
  print('futureStringIsFutureString ${fs is Future<String>}');
  try { (fi as Future<String>).then(print); print('castOk'); } catch (e) { print('castFutureTypeError ${e is TypeError}'); }
  print('await ${await (fi as Future<int>)} ${await (fs as Future<String>)}');
  await ctl.close();
  await bc.close();
}
