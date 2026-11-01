// Codename One Dart runtime API stubs -- dart:convert.
//
// Backed by dart.convert (maven/dart-runtime). Same conventions as
// dart_async_stream.dart: named parameters of methods are passed in their
// declared order, instance getters are no-arg method calls.
//
// jsonEncode calls toJson() on an object it cannot encode directly; the
// transpiler makes every class that declares a zero-argument toJson()
// implement dart.convert.JsonEncodable so that needs no reflection.
//
// The text and byte codecs share one Java class, dart.convert.ByteCodec, so
// their encode/decode are typed dynamic here: utf8.encode takes a String and
// returns a Uint8List, base64.encode the reverse.

@JavaName('dart.convert.JsonCodec')
class JsonCodec extends Codec<Object?, String> {
  external JsonCodec({Object? reviver, Object? toEncodable});
  external dynamic decode(String source, {Object? reviver});
  external String encode(Object? value, {Object? toEncodable});
  external JsonEncoder get encoder;
  external JsonDecoder get decoder;
}

@JavaName('dart.convert.JsonEncoder')
class JsonEncoder {
  external JsonEncoder([Object? toEncodable]);
  external static JsonEncoder withIndent(String? indent, [Object? toEncodable]);
  external String convert(Object? object);
}

@JavaName('dart.convert.JsonDecoder')
class JsonDecoder {
  external JsonDecoder([Object? reviver]);
  external dynamic convert(String input);
}

@JavaName('dart.convert.JsonUnsupportedObjectError')
class JsonUnsupportedObjectError {
  external Object? get unsupportedObject;
}

// Dart's Codec<S, T>. Only the byte codecs implement it (dart.convert.Codec); its
// converters are the codec itself, which picks the direction by input type.
@JavaName('dart.convert.Codec')
abstract class Codec<S, T> {
  external dynamic encode(dynamic input);
  external dynamic decode(dynamic encoded);
  external CodecConverter get encoder;
  external CodecConverter get decoder;
}

// What a Codec's encoder and decoder answer (the Java side's common type).
@JavaName('dart.convert.CodecConverter')
abstract class CodecConverter {
  external dynamic convert(dynamic input);
}

@JavaName('dart.convert.ByteCodec')
abstract class Encoding extends Codec<String, List<int>> {
  external String get name;
  external dynamic encode(dynamic input);
  external dynamic decode(dynamic encoded, {bool? allowMalformed});
  external Encoding get encoder;
  external Encoding get decoder;
  external dynamic convert(dynamic input);
  external static Encoding? getByName(String? name);
  external Future<String> decodeStream(Stream<List<int>> byteStream);
  external Stream bind(Stream stream);
}

@JavaName('dart.convert.Converter')
abstract class Converter<S, T> {
  external Converter();
  external T convert(S input);
  external dynamic startChunkedConversion(dynamic sink);
  external Stream<T> bind(Stream<S> stream);
  external Converter<S, R> fuse<R>(Converter<T, R> other);
}

@JavaName('dart.convert.ByteConversionSink')
abstract class ByteConversionSink {
  external ByteConversionSink();
  external static ByteConversionSink withCallback(void Function(List<int>) callback);
  external void add(List<int> chunk);
  external void addSlice(List<int> chunk, int start, int end, bool isLast);
  external void close();
}

@JavaName('dart.convert.ByteConversionSinkBase')
abstract class ByteConversionSinkBase extends ByteConversionSink {
  external ByteConversionSinkBase();
}

@JavaName('dart.convert.Utf8Encoder')
class Utf8Encoder extends Converter<String, List<int>> {
  external Utf8Encoder();
}

@JavaName('dart.convert.Utf8Decoder')
class Utf8Decoder extends Converter<List<int>, String> {
  external Utf8Decoder({bool allowMalformed});
}

@JavaName('dart.convert.HtmlEscape')
class HtmlEscape {
  external HtmlEscape([HtmlEscapeMode? mode]);
  external String convert(String text);
}

@JavaName('dart.convert.HtmlEscapeMode')
class HtmlEscapeMode {
  external HtmlEscapeMode({String name = 'custom', bool escapeLtGt = false, bool escapeQuot = false,
      bool escapeApos = false, bool escapeSlash = false});
  external static HtmlEscapeMode get unknown;
  external static HtmlEscapeMode get attribute;
  external static HtmlEscapeMode get sqAttribute;
  external static HtmlEscapeMode get element;
}

@JavaName('dart.convert.Convert.htmlEscape')
HtmlEscape htmlEscape = HtmlEscape();

@JavaName('dart.convert.ByteCodec')
abstract class Utf8Codec extends Encoding {
}

@JavaName('dart.convert.ByteCodec')
abstract class Base64Codec extends Encoding {
}

@JavaName('dart.convert.LineSplitter')
class LineSplitter {
  external LineSplitter();
  external static List<String> split(String lines);
  external List<String> convert(String data);
}

@JavaName('dart.convert.Convert.json')
JsonCodec json = JsonCodec();
@JavaName('dart.convert.Convert.utf8')
Utf8Codec utf8 = Utf8Codec();
@JavaName('dart.convert.Convert.ascii')
Encoding ascii = Encoding();
@JavaName('dart.convert.Convert.latin1')
Encoding latin1 = Encoding();
@JavaName('dart.convert.Convert.base64')
Base64Codec base64 = Base64Codec();
@JavaName('dart.convert.Convert.base64Url')
Base64Codec base64Url = Base64Codec();

@JavaName('dart.convert.Convert.jsonDecode')
external dynamic jsonDecode(String source, {Object? reviver});
@JavaName('dart.convert.Convert.jsonEncode')
external String jsonEncode(Object? object, {Object? toEncodable});
@JavaName('dart.convert.Convert.base64Encode')
external String base64Encode(dynamic bytes);
@JavaName('dart.convert.Convert.base64UrlEncode')
external String base64UrlEncode(dynamic bytes);
@JavaName('dart.convert.Convert.base64Decode')
external Uint8List base64Decode(String source);
