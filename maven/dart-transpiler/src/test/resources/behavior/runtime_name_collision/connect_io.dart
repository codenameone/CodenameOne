import 'dart:async' as async;
import 'socket.dart';

class IOSocket extends WebSocket {
  IOSocket(String url) : super('io:' + url);
  static WebSocket open(String url) => IOSocket(url);
  static Future<WebSocket> connect(String url) async {
    // the runtime class of the same simple name, through a dart: prefix
    final c = async.Completer<WebSocket>();
    async.Timer.run(() => c.complete(IOSocket(url)));
    return c.future;
  }
}

const open = IOSocket.open;
const connect = IOSocket.connect;
