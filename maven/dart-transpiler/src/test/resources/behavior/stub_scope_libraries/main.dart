// A stub type is scoped to the libraries that import its package: main and settings
// use the application's own Config, picker uses emoji_picker's, in whichever order
// the libraries are emitted.
import 'settings.dart';
import 'zz_picker.dart';

void main() {
  print(Config('app').name);
  print(pickerConfigHeight());
  print(describeSettings());
}
