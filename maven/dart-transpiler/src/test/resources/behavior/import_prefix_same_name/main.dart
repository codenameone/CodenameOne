import 'base_channel.dart' as base;
import 'wrapped_channel.dart';

void main() {
  print(Channel.connect('a').describe());
  final base.Channel raw = connector.connect('b');
  print(raw.describe());
  final base.Channel viaConst = defaultOpener('c');
  print(viaConst.describe());
  final List<base.Channel> list = [base.Channel('d')];
  print(list.first.describe());
  print(Channel(base.Channel('e')).describe());
}
