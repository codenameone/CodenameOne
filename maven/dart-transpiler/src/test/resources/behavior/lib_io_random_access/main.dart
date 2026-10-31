// RandomAccessFile the way Hive's storage backend drives it: an append-only
// frame log read back by offset, a compaction that truncates, and locks.
import 'dart:io';
import 'dart:typed_data';

Future<void> main() async {
  final dir = await Directory.systemTemp.createTemp('raf');
  final file = File('${dir.path}/box.hive');

  final w = await file.open(mode: FileMode.append);
  await w.lock();
  await w.writeFrom([1, 2, 3, 4]);
  await w.writeFrom(Uint8List.fromList([9, 8, 7, 6, 5]), 1, 4);
  await w.flush();
  print('after flush ${await file.length()} ${await w.position()}');
  await w.writeByte(42);
  await w.unlock();
  await w.close();
  print('stored ${await file.readAsBytes()}');

  final r = await file.open();
  await r.setPosition(2);
  print('read ${await r.read(3)}');
  final buf = List<int>.filled(4, 0);
  print('into ${await r.readInto(buf, 1)} $buf');
  print('eof ${await r.readByte()} len ${await r.length()}');
  try {
    await r.writeFrom([1]);
  } on FileSystemException catch (e) {
    print('read only: ${e.message.startsWith('Cannot write')}');
  }
  await r.close();

  final c = file.openSync(mode: FileMode.write);
  print('emptied ${c.lengthSync()}');
  c.writeFromSync([5, 5, 5, 5, 5]);
  c.setPositionSync(1);
  c.writeByteSync(0);
  c.truncateSync(3);
  c.closeSync();
  print('compacted ${file.readAsBytesSync()}');

  try {
    await File('${dir.path}/missing').open();
  } on FileSystemException catch (e) {
    print('missing: ${e.path!.endsWith('missing')}');
  }
  try {
    r.lengthSync();
  } on FileSystemException catch (e) {
    print('closed: ${e.message}');
  }
}
