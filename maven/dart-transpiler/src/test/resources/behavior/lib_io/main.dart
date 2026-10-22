import 'dart:async';
import 'dart:convert';
import 'dart:io';

Future<void> main() async {
  final dir = Directory('/data/app');
  await dir.create(recursive: true);
  print(dir.existsSync());
  final f = File('${dir.path}/notes.txt');
  await f.writeAsString('line one\nline two');
  print(await f.readAsString());
  await f.writeAsString('\nline three', mode: FileMode.append);
  final lines = await f.readAsLines();
  print(lines.length);
  print(await f.length());
  File('${dir.path}/b.bin').writeAsBytesSync([1, 2, 3]);
  print(File('${dir.path}/b.bin').readAsBytesSync().length);
  final names = dir.listSync().map((e) => e.path.split('/').last).toList()..sort();
  print(names.join(','));
  await File('${dir.path}/b.bin').delete();
  print(File('${dir.path}/b.bin').existsSync());
  try {
    await File('/nope/x.txt').readAsString();
  } on FileSystemException catch (e) {
    print('fs error');
  }
  print(Platform.operatingSystem.isNotEmpty);
  print(Platform.isIOS || Platform.isAndroid || Platform.isLinux || Platform.isMacOS || Platform.isWindows);
  print(HttpHeaders.contentTypeHeader);
  print(HttpStatus.notFound);
  print(ContentType.json.mimeType);
  print(ContentType.parse('text/html; charset=utf-8').charset);

  final s = Stream.fromIterable([1, 2, 3, 4]);
  print(await s.where((x) => x.isEven).toList());
  print(await Stream.fromIterable(['a', 'b']).join('-'));
  final text = await Stream.fromIterable([utf8.encode('he'), utf8.encode('llo\nworld')])
      .transform(utf8.decoder)
      .transform(const LineSplitter())
      .toList();
  print(text);
  print(await Stream.fromIterable([1, 2, 3]).fold(0, (a, b) => a + b));
  print(await Stream.fromIterable([5, 6]).first);
  try {
    await HttpClient().getUrl(Uri.parse('http://example.com')).then((r) => r.close());
  } on SocketException {
    print('offline');
  }
}
