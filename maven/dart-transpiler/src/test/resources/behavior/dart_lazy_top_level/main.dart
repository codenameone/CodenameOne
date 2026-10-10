// Top-level variables initialise LAZILY in Dart, on first read — so a variable
// may be written ABOVE the ones its initialiser depends on, and Shrine's theme
// is written exactly that way:
//
//   final ThemeData shrineTheme = _buildShrineTheme();   // reads the scheme
//   final ColorScheme _shrineColorScheme = ColorScheme(...);
//
// Emitted as plain Java static fields, in textual order, the first one reads
// null and the whole library dies in its static initialiser.

class Palette {
  Palette(this.name);
  final String name;
  String describe() => 'palette:$name';
}

// Declared FIRST, depends on two things declared after it.
final String summary = _describe();
final Palette palette = Palette(paletteName);
const String paletteName = 'shrine';

String _describe() => '${palette.describe()} via $paletteName';

// A mutable top-level variable still assigns.
int visits = 0;

void main() {
  print(summary);
  print(palette.describe());
  visits = visits + 2;
  visits++;
  print('visits $visits');
}
