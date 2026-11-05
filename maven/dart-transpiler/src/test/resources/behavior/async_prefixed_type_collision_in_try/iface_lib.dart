abstract class Queue {
  String describe();
}

abstract class File {
  String get label;
}

abstract class Directory {
  bool get fresh;
}
