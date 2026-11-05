library shelf;

import 'dart:convert';

part 'shelf_reader.dart';

class ListReader extends Reader {
  @override
  String readText(List<int> bytes, {Converter<List<int>, String> decoder = Reader.textDecoder}) =>
      decoder.convert(bytes);
  @override
  String readRaw(List<int> bytes, [int? limit, Converter<List<int>, String> decoder = Reader.plainDecoder]) =>
      decoder.convert(limit == null ? bytes : bytes.sublist(0, limit));
}

String register<T>(T sample) {
  if (T == dynamic || T == Object) {
    return 'loose';
  }
  return 'typed $T';
}

class Slot<T> {
  bool get isOpen => T == dynamic;
  String kind() => T == dynamic ? 'any' : '$T';
}

Type typeOfDynamic() => dynamic;

void main() {
  final r = ListReader();
  print(r.readText([104, 105]));
  print(r.tag());
  print(r.tag('x'));
  print(Slot<dynamic>().isOpen);
  print(Slot<int>().isOpen);
  print(Slot<String>().kind());
  print(Slot<dynamic>().kind());
  print(typeOfDynamic() == dynamic);
  print(r.readRaw([111, 107, 33], 2));
  final Reader base = r;
  print(base.readRaw([111, 107, 33]));
  print(base.readText([121]));
  print(base.tag());
  print(register<dynamic>(1));
  print(register<Object>(1));
  print(register<int>(1));
}
