import 'dart:io';
import 'dart:typed_data';

String describe(SocketException e) =>
    '${e.message}|${e.osError?.message}|${e.osError?.errorCode}|${e.address}|${e.port}';

int lengthOf(TypedData data) => data.lengthInBytes;

Uint8List bytesOf(List<int> input) {
  if (input is Uint8List) return input;
  if (input case TypedData data) {
    return Uint8List.view(data.buffer);
  }
  return Uint8List.fromList(input);
}

void main() {
  // dart:io failures
  var plain = SocketException('refused');
  print(describe(plain));
  var os = OSError('Connection reset', 54);
  print('${os.message} ${os.errorCode}');
  var full = SocketException('lost', osError: os, port: 8080);
  print(describe(full));
  print(full.osError!.errorCode + 1);
  try {
    throw full;
  } on IOException catch (e) {
    print('caught ${e is SocketException}');
  }

  // Int32x4
  var a = Int32x4(1, 2, 3, 4);
  var b = Int32x4(8, 4, 2, 1);
  print(a);
  print('${a.x} ${a.y} ${a.z} ${a.w}');
  var o = a | b;
  print('${o.x} ${o.y} ${o.z} ${o.w}');
  var s = a + b;
  print('${s.x} ${s.w}');
  var d = a - b;
  print('${d.x} ${d.w}');
  var r = a.shuffle(Int32x4.wzyx);
  print('${r.x} ${r.y} ${r.z} ${r.w}');
  var m = a.shuffleMix(b, Int32x4.xyxy);
  print('${m.x} ${m.y} ${m.z} ${m.w}');
  print(Int32x4.xxxx + Int32x4.wwww * 0 + Int32x4.wzyx);
  var flags = Int32x4.bool(true, false, true, false);
  print('${flags.flagX} ${flags.flagY} ${flags.signMask}');
  print(Int32x4(-1, 0, -5, 7).signMask);
  print(a.withZ(30).z);
  var neg = Int32x4(0x7fffffff, 0, 0, 0) + Int32x4(1, 0, 0, 0);
  print(neg.x);

  // Float32x4
  var f = Float32x4(1.5, -2.0, 3.25, 4.0);
  print(f);
  var g = Float32x4.splat(2.0);
  var p = f * g;
  print('${p.x} ${p.y} ${p.z} ${p.w}');
  var q = f / g;
  print('${q.x} ${q.y}');
  print(f.abs().y);
  print(f.scale(0.5).w);
  print((-f).x);
  var lt = f.lessThan(g);
  print('${lt.flagX} ${lt.flagY} ${lt.flagZ} ${lt.signMask}');
  print(lt.select(Float32x4.splat(1.0), Float32x4.zero()).z);
  print(Float32x4(0.1, 0, 0, 0).x);
  print(f.clamp(Float32x4.splat(0.0), Float32x4.splat(3.0)));
  print(f.signMask);
  print(f.shuffle(Float32x4.yxwz).x);
  print(Float32x4(4.0, 9.0, 16.0, 25.0).sqrt());
  print(f.max(g).y);
  var bits = Int32x4.fromFloat32x4Bits(Float32x4(1.0, 0, 0, 0));
  print(bits.x);
  print(Float32x4.fromInt32x4Bits(bits).x);

  // Float64x2
  var h = Float64x2(1.25, -3.5);
  print(h);
  var k = h + Float64x2.splat(1.0);
  print('${k.x} ${k.y}');
  print(h.abs().y);
  print((h * Float64x2(2.0, 2.0)).y);
  print(h.signMask);
  print(h.withY(9.0));
  print(Float64x2.fromFloat32x4(f));

  // SIMD lists over shared bytes
  var il = Int32x4List(2);
  print('${il.length} ${il.lengthInBytes} ${il.elementSizeInBytes}');
  print(il[1].z);
  il[1] = Int32x4(5, 6, 7, 8);
  print(il[1].z);
  var raw = il.buffer.asInt32List();
  print(raw);
  raw[0] = 42;
  print(il[0].x);
  var fl = Float32x4List(1);
  fl[0] = Float32x4(1.0, 2.0, 3.0, 4.0);
  print(fl.buffer.asFloat32List());
  var dl = Float64x2List.fromList([Float64x2(1.0, 2.0), Float64x2(3.0, 4.0)]);
  print(dl.buffer.asFloat64List());
  print(dl.sublist(1)[0].y);
  print(Int32x4List.bytesPerElement);

  // ByteBuffer views share storage
  var u8 = Uint8List(8);
  var bb = u8.buffer;
  var u16 = bb.asUint16List();
  u16[0] = 0x0102;
  print('${u8[0]} ${u8[1]}');
  var u32 = bb.asUint32List(4, 1);
  u32[0] = 0xffffffff;
  print(u8);
  var bd = ByteData.view(bb);
  print(bd.getUint16(0, Endian.big));
  var f64 = Float64List(1);
  f64[0] = 1.0;
  print(f64.buffer.asUint8List());
  print(lengthOf(u16));
  print(lengthOf(bd));
  print(bytesOf(Int16List.fromList([1, 256])));
  print(bytesOf([3, 4]));
  TypedDataList<int> tl = Int8List(0);
  print('${tl.isEmpty} ${tl.isNotEmpty}');

  // Set algebra
  var s1 = {1, 2, 3, 4};
  var s2 = {3, 4, 5};
  print(s1.union(s2));
  print(s1.intersection(s2));
  print(s1.difference(s2));
  print(s1.containsAll([1, 4]));
  var s3 = s1.toSet();
  s3.removeWhere((e) => e.isEven);
  print(s3);
  s3.retainAll([1, 9]);
  print(s3);
  print(s1);
}
