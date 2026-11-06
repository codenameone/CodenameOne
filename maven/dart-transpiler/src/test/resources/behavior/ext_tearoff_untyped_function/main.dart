import 'dart:async';

class Sink2 {
  final List<String> log = [];
}

extension SinkOps on Sink2 {
  void fail(Object error, StackTrace stack) => log.add('fail:$error');
  String label(Object v) => 'v=$v';
}

Object apply(Function f, Object a) => f(a);

Future<void> main() async {
  final s = Sink2();
  await Future<void>.error('boom').catchError(s.fail);
  print(s.log);
  print(apply(s.label, 7));
  final Function g = s.label;
  print(g('x'));
  final c = Completer<int>();
  c.future.then((v) => print('ok $v'), onError: s.fail);
  c.completeError('late');
  await Future<void>.delayed(Duration.zero);
  print(s.log);
}
