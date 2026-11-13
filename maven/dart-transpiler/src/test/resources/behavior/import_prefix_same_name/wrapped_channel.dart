import 'base_channel.dart' as base;

typedef Opener = base.Channel Function(String url);

const Opener defaultOpener = base.Channel.open;

class Channel {
  final base.Channel inner;
  Channel(this.inner);

  static Channel connect(String url, {Opener opener = defaultOpener}) {
    final base.Channel c = opener(url);
    return Channel(c);
  }

  String describe() => 'wrapped(${inner.describe()})';
}

class Connector {
  final Opener connect;
  const Connector(this.connect);
}

const connector = Connector(base.Channel.open);
