import 'package:emoji_picker_flutter/emoji_picker_flutter.dart';

String pickerConfigHeight() {
  final c = Config(height: 256);
  return 'picker ${c is Object}';
}
