import 'dart:async';

class Box {
  final String label;
  final List<String> log = <String>[];
  Box(this.label);
  void put(String v) => log.add('$label:$v');
  String tag(String v) => '$label<$v>';
  void fail(Object e, [StackTrace? s]) => log.add('$label fail $e');
}

class Shelf {
  Box box;
  Shelf(this.box);
}

class Room {
  Shelf shelf;
  Room(this.shelf);
}

Future<void> main() async {
  final first = Box('first');
  final second = Box('second');
  final room = Room(Shelf(first));
  final f = Future<String>.value('a');
  final pending = f.then(room.shelf.box.put);
  room.shelf.box = second;
  await pending;
  final c = Completer<void>();
  final onErr = c.future.catchError(room.shelf.box.fail);
  room.shelf = Shelf(first);
  c.completeError('boom');
  await onErr;
  var local = first;
  final g = Future<String>.value('b').then(local.tag);
  local = second;
  print(await g);
  print(first.log);
  print(second.log);
}
