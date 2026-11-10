import 'dart:typed_data';

void main() {
  final u32 = Uint32List(4);
  u32[0] = 0xFFFFFFFF;
  u32[1] = -1;
  u32[2] = 7;
  print('u32 ${u32[0]} ${u32[1]} ${u32[2]} len=${u32.length} bytes=${u32.lengthInBytes} es=${u32.elementSizeInBytes}');
  final i8 = Int8List.fromList([127, 128, 255, -1]);
  print('i8 ${i8[0]} ${i8[1]} ${i8[2]} ${i8[3]}');
  final cl = Uint8ClampedList(3);
  cl[0] = 300;
  cl[1] = -5;
  cl[2] = 42;
  print('clamped ${cl[0]} ${cl[1]} ${cl[2]}');
  final i16 = Int16List(2);
  i16[0] = 40000;
  final u16 = Uint16List.fromList([65535, 65536]);
  print('16 ${i16[0]} ${u16[0]} ${u16[1]}');
  final i32 = Int32List(1);
  i32[0] = 0x80000000;
  final i64 = Int64List(1);
  i64[0] = -2;
  final u64 = Uint64List(2);
  u64[0] = 123456789012;
  print('32/64 ${i32[0]} ${i64[0]} ${u64[0]}');
  final f32 = Float32List(2);
  f32[0] = 1.1;
  final f64 = Float64List.fromList([2.5, -0.25]);
  print('floats ${f32[0] == 1.1} ${f32[0].toStringAsFixed(4)} ${f64[0]} ${f64[1]}');

  // views share the buffer
  final base = Uint8List(8);
  final asWords = Uint32List.view(base.buffer);
  asWords[1] = 0x01020304;
  print('view ${base[4]} ${base[5]} ${base[6]} ${base[7]} n=${asWords.length}');
  final part = Uint16List.view(base.buffer, 2, 2);
  part[0] = 0xABCD;
  print('partial ${base[2]} ${base[3]} off=${part.offsetInBytes} len=${part.length}');
  final viaBuf = base.buffer.asInt32List();
  print('asInt32List ${viaBuf.length} ${viaBuf[1]}');

  // list operations on typed lists
  final xs = Int32List(6);
  xs.setRange(0, 3, [9, 8, 7]);
  xs.fillRange(3, 5, -1);
  xs.setAll(5, [42]);
  print('ops $xs');
  final sub = xs.sublist(1, 4);
  sub[0] = 100;
  print('sub $sub ${xs[1]} ${sub.runtimeType}');
  List<int> asList = xs;
  print('asList ${asList.length} ${asList.first} ${asList.last}');
  try {
    asList.add(1);
  } on UnsupportedError {
    print('fixed length');
  }
  final bytes = Uint8List(5);
  bytes.setRange(1, 4, [1, 2, 3]);
  bytes.fillRange(4, 5, 9);
  print('u8 ops $bytes');

  // ByteData with endianness
  final bd = ByteData(16);
  bd.setFloat64(0, 3.75);
  bd.setFloat32(8, 0.5, Endian.little);
  bd.setUint16(12, 0x1234);
  bd.setUint16(14, 0x1234, Endian.little);
  print('bd ${bd.getFloat64(0)} ${bd.getFloat32(8, Endian.little)} ${bd.getUint8(12)} ${bd.getUint8(14)}');
  print('bd2 ${bd.getUint32(12)} ${bd.getInt16(12, Endian.little)} ${bd.getFloat64(0, Endian.big)}');
  final bv = ByteData.view(bd.buffer, 12, 4);
  print('bdview ${bv.lengthInBytes} ${bv.offsetInBytes} ${bv.getUint8(0)}');

  final bb = BytesBuilder();
  bb.add([1, 2]);
  bb.addByte(3);
  bb.add(Uint8List.fromList([4]));
  print('builder ${bb.length} ${bb.toBytes()}');
  final taken = bb.takeBytes();
  print('taken $taken ${bb.isEmpty}');
}
