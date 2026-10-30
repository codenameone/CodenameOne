// package:cached_network_image, fetching the bytes itself so the placeholder, progress and error
// builders run.

@JavaName('com.codename1.flutter.plugins.cachednetworkimage.CachedNetworkImage')
class CachedNetworkImage extends Widget {
  external CachedNetworkImage({Key? key, required String imageUrl, double? width, double? height,
      BoxFit? fit, Widget Function(BuildContext context, String url)? placeholder,
      Widget Function(BuildContext context, String url, Object error)? errorWidget,
      Widget Function(BuildContext context, String url, DownloadProgress progress)? progressIndicatorBuilder,
      Map<String, String>? httpHeaders, int? memCacheWidth, int? memCacheHeight,
      Duration? fadeInDuration, String? cacheKey});
  external Widget buildOnce(BuildContext? context);
  external Widget buildLoading(BuildContext? context, int downloaded, int? total);
  external void seedCache(Uint8List data);
}

@JavaName('com.codename1.flutter.plugins.cachednetworkimage.DownloadProgress')
class DownloadProgress {
  external DownloadProgress(String originalUrl, int? totalSize, int downloaded);
  external String get originalUrl;
  external int? get totalSize;
  external int get downloaded;
  external double? get progress;
}

@JavaName('com.codename1.flutter.NetworkImage')
class CachedNetworkImageProvider extends ImageProvider {
  external CachedNetworkImageProvider(String url, {double? scale, Object? headers});
}
