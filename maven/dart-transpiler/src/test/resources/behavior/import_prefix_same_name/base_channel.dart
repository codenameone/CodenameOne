class Channel {
  final String name;
  Channel(this.name);
  static Channel open(String url) => Channel('base:$url');
  String describe() => 'base channel $name';
}
