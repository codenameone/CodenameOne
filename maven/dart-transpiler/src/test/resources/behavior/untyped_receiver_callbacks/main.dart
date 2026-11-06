import 'dart:async';
import 'dart:io';

abstract class Pipe<T> {
  Stream<T> get stream;
}

class ListPipe implements Pipe<dynamic> {
  final StreamController<dynamic> c = StreamController<dynamic>();
  @override
  Stream<dynamic> get stream => c.stream;
}

class Router {
  Pipe<dynamic>? _inner;
  StreamSubscription<dynamic>? _sub;
  final List<String> seen = [];
  final StreamController<Object> errors = StreamController<Object>();
  Router(Pipe<dynamic> inner) : _inner = inner;

  void start(Completer<void> done) {
    _sub = _inner!.stream.cast<List>().listen((message) {
      var id = (message[0] as num).toInt();
      if (message.length > 1) {
        seen.add('$id:${message[1]}');
      } else {
        seen.add('$id:closed');
      }
    }, onDone: () => done.complete(), onError: errors.sink.addError);
  }
}

void main() async {
  var p = ListPipe();
  var r = Router(p);
  var done = Completer<void>();
  r.start(done);
  p.c.add([1, 'a']);
  p.c.add([2]);
  p.c.add([3, 'b']);
  r.errors.stream.listen((e) {}, onError: (Object e) => print('error $e'));
  p.c.addError('boom');
  await p.c.close();
  await done.future;
  print(r.seen.join(','));
  print(r._sub != null);

  // HttpHeaders cannot be built without a socket here; what is held is that a
  // closure handed to its forEach compiles to a callable callback.
  print(collect(null).length);
}

Map<String, List<String>> collect(HttpHeaders? h) {
  var out = <String, List<String>>{};
  h?.forEach((key, values) {
    out[key] = values;
  });
  return out;
}
