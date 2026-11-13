import 'connect_stub.dart'
    if (dart.library.io) 'connect_io.dart' as connector;

class WebSocket {
  static int count = 0;
  final String url;
  WebSocket(this.url) {
    count++;
  }
  const WebSocket.konst(this.url);
  factory WebSocket.open(String url) => connector.open(url);
  static WebSocket make(String url) => WebSocket(url);
  static Future<WebSocket> connect(String url) => connector.connect(url);
  String describe() => 'WebSocket($url)';
}
