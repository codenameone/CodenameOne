// package:flutter_svg over Image.createSVG where the port renders SVG; elsewhere
// (and headless) the picture is a sized empty box. See SvgPicture.java.

@JavaName('com.codename1.flutter.plugins.svg.ColorFilter')
class ColorFilter {
  external static ColorFilter mode(Color color, BlendMode blendMode);
}

@JavaName('com.codename1.flutter.plugins.svg.SvgPicture')
class SvgPicture extends Widget {
  external static SvgPicture asset(String assetName, {Key? key, double? width, double? height,
      BoxFit fit, dynamic alignment, String? package, ColorFilter? colorFilter,
      String? semanticsLabel, Widget Function(BuildContext context)? placeholderBuilder,
      bool matchTextDirection, bool excludeFromSemantics, dynamic clipBehavior, dynamic theme});
  external static SvgPicture network(String url, {Key? key, double? width, double? height,
      BoxFit fit, dynamic alignment, Map<String, String>? headers, ColorFilter? colorFilter,
      String? semanticsLabel, Widget Function(BuildContext context)? placeholderBuilder,
      bool matchTextDirection, bool excludeFromSemantics, dynamic clipBehavior, dynamic theme});
  external static SvgPicture string(String string, {Key? key, double? width, double? height,
      BoxFit fit, dynamic alignment, ColorFilter? colorFilter,
      String? semanticsLabel, Widget Function(BuildContext context)? placeholderBuilder,
      bool matchTextDirection, bool excludeFromSemantics, dynamic clipBehavior, dynamic theme});
  external static SvgPicture file(File file, {Key? key, double? width, double? height,
      BoxFit fit, dynamic alignment, ColorFilter? colorFilter,
      String? semanticsLabel, Widget Function(BuildContext context)? placeholderBuilder,
      bool matchTextDirection, bool excludeFromSemantics, dynamic clipBehavior, dynamic theme});
  external static SvgPicture memory(Uint8List bytes, {Key? key, double? width, double? height,
      BoxFit fit, dynamic alignment, ColorFilter? colorFilter,
      String? semanticsLabel, Widget Function(BuildContext context)? placeholderBuilder,
      bool matchTextDirection, bool excludeFromSemantics, dynamic clipBehavior, dynamic theme});
  external double? get width;
  external double? get height;
}
