// Codename One Dart runtime API stubs -- dart:typed_data.
//
// Backed by dart.typed_data (maven/dart-runtime). The integer lists extend
// dart.core.DartLongList and the float lists dart.core.DartDoubleList, so a
// typed list goes wherever a List<int>/List<double> goes; every list is little
// endian over a ByteBuffer that views and ByteData share.

@JavaName('dart.typed_data.Endian')
class Endian {
  external static Endian get big;
  external static Endian get little;
  external static Endian get host;
}

@JavaName('dart.typed_data.TypedDataList')
abstract class TypedDataList<E> implements List<E> {
  external int get length;
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
  external void setRange(int start, int end, Iterable<E> from, [int skipCount]);
  external void fillRange(int start, int end, [E? fillValue]);
}

@JavaName('dart.typed_data.Uint8List')
class Uint8List implements TypedDataList<int>, List<int> {
  external Uint8List(int length);
  external static Uint8List fromList(List<int> elements);
  external static Uint8List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external static Uint8List sublistView(Object data, [int start, int? end]);
  external int get length;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList();
  external Uint8List sublist(int start, [int? end]);
  external bool get isEmpty;
  external bool get isNotEmpty;
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
}

@JavaName('dart.typed_data.Int8List')
class Int8List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Int8List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Int8List fromList(List<int> elements);
  external static Int8List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Int8List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Uint8ClampedList')
class Uint8ClampedList implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Uint8ClampedList(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Uint8ClampedList fromList(List<int> elements);
  external static Uint8ClampedList view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Uint8ClampedList sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Int16List')
class Int16List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Int16List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Int16List fromList(List<int> elements);
  external static Int16List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Int16List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Uint16List')
class Uint16List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Uint16List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Uint16List fromList(List<int> elements);
  external static Uint16List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Uint16List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Int32List')
class Int32List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Int32List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Int32List fromList(List<int> elements);
  external static Int32List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Int32List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Uint32List')
class Uint32List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Uint32List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Uint32List fromList(List<int> elements);
  external static Uint32List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Uint32List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Int64List')
class Int64List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Int64List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Int64List fromList(List<int> elements);
  external static Int64List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Int64List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Uint64List')
class Uint64List implements TypedDataList<int>, List<int> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external int get first;
  external int get last;
  external int operator [](int index);
  external void operator []=(int index, int value);
  external List<int> toList({bool growable});
  external Set<int> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(int) action);
  external Iterable<T> map<T>(T Function(int) f);
  external Iterable<int> where(bool Function(int) test);
  external int reduce(int Function(int, int) combine);
  external T fold<T>(T initialValue, T Function(T, int) combine);
  external String join([String separator]);
  external bool any(bool Function(int) test);
  external bool every(bool Function(int) test);
  external Iterable<int> skip(int count);
  external Iterable<int> take(int count);
  external void setRange(int start, int end, Iterable<int> from, [int skipCount]);
  external void setAll(int index, Iterable<int> from);
  external void fillRange(int start, int end, [int? fillValue]);
  external Uint64List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Uint64List fromList(List<int> elements);
  external static Uint64List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Uint64List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Float32List')
class Float32List implements TypedDataList<double>, List<double> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external double get first;
  external double get last;
  external double operator [](int index);
  external void operator []=(int index, double value);
  external List<double> toList({bool growable});
  external Set<double> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(double) action);
  external Iterable<T> map<T>(T Function(double) f);
  external Iterable<double> where(bool Function(double) test);
  external double reduce(double Function(double, double) combine);
  external T fold<T>(T initialValue, T Function(T, double) combine);
  external String join([String separator]);
  external bool any(bool Function(double) test);
  external bool every(bool Function(double) test);
  external Iterable<double> skip(int count);
  external Iterable<double> take(int count);
  external void setRange(int start, int end, Iterable<double> from, [int skipCount]);
  external void setAll(int index, Iterable<double> from);
  external void fillRange(int start, int end, [double? fillValue]);
  external Float32List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Float32List fromList(List<double> elements);
  external static Float32List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Float32List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.Float64List')
class Float64List implements TypedDataList<double>, List<double> {
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external double get first;
  external double get last;
  external double operator [](int index);
  external void operator []=(int index, double value);
  external List<double> toList({bool growable});
  external Set<double> toSet();
  external bool contains(Object? element);
  external void forEach(void Function(double) action);
  external Iterable<T> map<T>(T Function(double) f);
  external Iterable<double> where(bool Function(double) test);
  external double reduce(double Function(double, double) combine);
  external T fold<T>(T initialValue, T Function(T, double) combine);
  external String join([String separator]);
  external bool any(bool Function(double) test);
  external bool every(bool Function(double) test);
  external Iterable<double> skip(int count);
  external Iterable<double> take(int count);
  external void setRange(int start, int end, Iterable<double> from, [int skipCount]);
  external void setAll(int index, Iterable<double> from);
  external void fillRange(int start, int end, [double? fillValue]);
  external Float64List(int length);
  external static int get BYTES_PER_ELEMENT;
  external static Float64List fromList(List<double> elements);
  external static Float64List view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external Float64List sublist(int start, [int? end]);
  external ByteBuffer get buffer;
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
}

@JavaName('dart.typed_data.ByteData')
class ByteData {
  external ByteData(int length);
  external static ByteData view(ByteBuffer buffer, [int offsetInBytes, int? length]);
  external static ByteData sublistView(Object data, [int start, int? end]);
  external int get lengthInBytes;
  external int get offsetInBytes;
  external int get elementSizeInBytes;
  external ByteBuffer get buffer;
  external int getUint8(int byteOffset);
  external void setUint8(int byteOffset, int value);
  external int getInt8(int byteOffset);
  external void setInt8(int byteOffset, int value);
  external int getUint16(int byteOffset, [Endian endian]);
  external void setUint16(int byteOffset, int value, [Endian endian]);
  external int getInt16(int byteOffset, [Endian endian]);
  external void setInt16(int byteOffset, int value, [Endian endian]);
  external int getUint32(int byteOffset, [Endian endian]);
  external void setUint32(int byteOffset, int value, [Endian endian]);
  external int getInt32(int byteOffset, [Endian endian]);
  external void setInt32(int byteOffset, int value, [Endian endian]);
  external int getUint64(int byteOffset, [Endian endian]);
  external void setUint64(int byteOffset, int value, [Endian endian]);
  external int getInt64(int byteOffset, [Endian endian]);
  external void setInt64(int byteOffset, int value, [Endian endian]);
  external double getFloat32(int byteOffset, [Endian endian]);
  external void setFloat32(int byteOffset, double value, [Endian endian]);
  external double getFloat64(int byteOffset, [Endian endian]);
  external void setFloat64(int byteOffset, double value, [Endian endian]);
}

@JavaName('dart.typed_data.ByteBuffer')
class ByteBuffer {
  external int get lengthInBytes;
  external Uint8List asUint8List([int offsetInBytes, int? length]);
  external ByteData asByteData([int offsetInBytes, int? length]);
  external Int8List asInt8List([int offsetInBytes, int? length]);
  external Uint8ClampedList asUint8ClampedList([int offsetInBytes, int? length]);
  external Int16List asInt16List([int offsetInBytes, int? length]);
  external Uint16List asUint16List([int offsetInBytes, int? length]);
  external Int32List asInt32List([int offsetInBytes, int? length]);
  external Uint32List asUint32List([int offsetInBytes, int? length]);
  external Int64List asInt64List([int offsetInBytes, int? length]);
  external Uint64List asUint64List([int offsetInBytes, int? length]);
  external Float32List asFloat32List([int offsetInBytes, int? length]);
  external Float64List asFloat64List([int offsetInBytes, int? length]);
}

@JavaName('dart.typed_data.BytesBuilder')
class BytesBuilder {
  external BytesBuilder({bool copy});
  external void add(List<int> bytes);
  external void addByte(int byte);
  external Uint8List takeBytes();
  external Uint8List toBytes();
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external void clear();
}
