part of 'main.dart';

abstract class Reader {
  static const Utf8Decoder textDecoder = Utf8Decoder();
  static const String fallback = '-';
  static const plainDecoder = Utf8Decoder();

  String readRaw(List<int> bytes, [int? limit, Converter<List<int>, String> decoder = plainDecoder]);

  String readText(List<int> bytes, {Converter<List<int>, String> decoder = textDecoder});
  String tag([String mark = fallback]) => '<$mark>';
}

