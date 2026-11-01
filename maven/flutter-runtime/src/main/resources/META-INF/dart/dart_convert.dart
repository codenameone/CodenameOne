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
// The text and byte codecs share one Java base class, dart.convert.ByteCodec,
// whose encode/decode are typed dynamic (an Encoding value may be either kind).
// utf8, ascii and latin1 are dart.convert.TextCodec and base64/base64Url
// dart.convert.Base64Codec, which narrow encode/decode/encoder/decoder to the
// types Dart gives them, so a caller handing the result to a List<int> or a
// String compiles without a cast.

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

// Dart's Codec<S, T> (dart.convert.Codec). Its encoder and decoder are Converters, so
// a codec written in Dart overrides them with its own Converter subclasses; the byte
// codecs are their own converters and pick the direction by input type.
@JavaName('dart.convert.Codec')
abstract class Codec<S, T> {
  external dynamic encode(dynamic input);
  external dynamic decode(dynamic encoded);
  external Converter<S, T> get encoder;
  external Converter<T, S> get decoder;
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

@JavaName('dart.convert.BytesConverter')
abstract class CodecBytesConverter extends Encoding {
  external Uint8List convert(dynamic input);
}

@JavaName('dart.convert.TextConverter')
abstract class CodecTextConverter extends Encoding {
  external String convert(dynamic input);
}

@JavaName('dart.convert.TextCodec')
abstract class Utf8Codec extends Encoding {
  external Uint8List encode(String input);
  external String decode(List<int> encoded, {bool? allowMalformed});
  external CodecBytesConverter get encoder;
  external CodecTextConverter get decoder;
}

@JavaName('dart.convert.TextCodec')
abstract class AsciiCodec extends Utf8Codec {
}

@JavaName('dart.convert.TextCodec')
abstract class Latin1Codec extends Utf8Codec {
}

@JavaName('dart.convert.Base64Codec')
abstract class Base64Codec extends Encoding {
  external String encode(List<int> input);
  external Uint8List decode(String encoded);
  external CodecTextConverter get encoder;
  external CodecBytesConverter get decoder;
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
AsciiCodec ascii = AsciiCodec();
@JavaName('dart.convert.Convert.latin1')
Latin1Codec latin1 = Latin1Codec();
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
