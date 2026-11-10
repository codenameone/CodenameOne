import 'dart:convert';

import 'package:async/async.dart';
import 'package:collection/collection.dart';
import 'package:crypto/crypto.dart';
import 'package:hive/hive.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:markdown/markdown.dart' as md;
import 'package:path/path.dart' as p;
import 'package:source_span/source_span.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:stream_channel/stream_channel.dart';
import 'package:string_scanner/string_scanner.dart';
import 'package:typed_data/typed_data.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

/// Touches one API of every foundational package so each is transpiled.
class PackageUse {
  final AsyncMemoizer<int> memo = AsyncMemoizer<int>();
  final QueueList<int> queue = QueueList<int>();
  final Uint8Buffer buffer = Uint8Buffer();

  String digest(String s) => sha256.convert(utf8.encode(s)).toString();

  String html(String markdown) => md.markdownToHtml(markdown);

  String join(String a, String b) => p.join(a, b);

  MediaType type(String s) => MediaType.parse(s);

  SourceFile source(String text) => SourceFile.fromString(text);

  String firstWord(String s) {
    final scanner = StringScanner(s);
    scanner.scan(RegExp(r'\w+'));
    return scanner.lastMatch?[0] ?? '';
  }

  int? maxOf(List<int> xs) => xs.maxOrNull;

  Future<Box<String>> openNotes() => Hive.openBox<String>('notes');

  Future<String> fetch(Uri uri) => http.read(uri);

  StompClient chat(String url) =>
      StompClient(config: StompConfig(url: url, onConnect: (frame) {}));

  WebSocketChannel socket(Uri uri) => WebSocketChannel.connect(uri);

  StreamChannelController<String> pipe() => StreamChannelController<String>();
}
