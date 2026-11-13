import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

Future<void> main() async {
  // typed-data views share storage with their buffer, both ways
  final whole = Uint8List(8);
  for (var i = 0; i < 8; i++) {
    whole[i] = i;
  }
  final part = Uint8List.view(whole.buffer, 2, 3);
  part[0] = 99;
  whole[3] = 77;
  print('view ${part.length} ${part.offsetInBytes} $part ${whole[2]}');
  final sv = Uint8List.sublistView(whole, 5, 7);
  sv[1] = 55;
  print('sublistView $sv ${whole[6]}');
  final bd = ByteData.view(whole.buffer, 4, 2);
  bd.setUint8(0, 44);
  print('byteData ${whole[4]} ${bd.lengthInBytes}');
  final bd2 = ByteData.sublistView(whole, 1, 3);
  whole[1] = 11;
  print('byteData2 ${bd2.getUint8(0)} ${bd2.lengthInBytes}');
  final asList = whole.buffer.asUint8List(6, 2);
  asList[1] = 66;
  print('asUint8List $asList ${whole[7]}');
  print('sublist copies ${(whole.sublist(0, 2)..[0] = 9)[0]} ${whole[0]}');

  // asFuture and decodeStream
  final end = await Stream.fromIterable([1, 2]).listen(null).asFuture('end');
  print('asFuture $end');
  final text = await utf8.decodeStream(Stream.fromIterable([
    [104, 105],
    [33]
  ]));
  print('decodeStream $text');

  // ByteConversionSink and the Utf8 converter constructors
  List<int>? got;
  final sink = ByteConversionSink.withCallback((bytes) => got = bytes);
  sink.add([1, 2]);
  sink.addSlice([3, 4, 5, 6], 1, 3, false);
  sink.close();
  print('byteSink $got');
  print('utf8 ctors ${const Utf8Encoder().convert('ok')} ${const Utf8Decoder(allowMalformed: true).convert([104, 0xff])}');

  // StreamIterator
  final it = StreamIterator<String>(Stream<String>.fromIterable(['a', 'b']));
  final seen = <String>[];
  while (await it.moveNext()) {
    seen.add(it.current);
  }
  print('iterator $seen');

  // Uri members
  final base = Uri.parse('http://a/b/c/d;p?q');
  print('resolve ${base.resolve('g')} ${base.resolve('../g')} ${base.resolve('/g')} ${base.resolve('?y')} ${base.resolve('g#s')} ${base.resolve('../../../g')}');
  print('scheme ${base.hasScheme} ${base.isScheme('HTTP')} ${base.isScheme('https')} ${Uri.parse('x/y').hasScheme} ${base.data}');
  print('filePath ${Uri.parse('file:///tmp/a%20b.txt').toFilePath()}');

  // Symbol
  print('symbol ${Symbol('a')} ${Symbol('a') == Symbol('a')} ${Symbol('a') == Symbol('b')} ${Symbol('a').hashCode == Symbol('a').hashCode}');

  // named groups
  final m = RegExp(r'(?<year>\d{4})-(?<month>\d\d)(x)?').firstMatch('on 2024-05 ok')!;
  print('named ${m.namedGroup('year')} ${m.namedGroup('month')} ${m.groupNames.toList()} ${m.group(3)} ${m.groupCount}');
  try {
    m.namedGroup('day');
  } on ArgumentError {
    print('named missing ArgumentError');
  }
}
