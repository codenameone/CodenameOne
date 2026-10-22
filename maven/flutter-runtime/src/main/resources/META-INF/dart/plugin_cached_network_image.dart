// package:cached_network_image, over Image.network, which already caches what it
// downloads.

@JavaName('com.codename1.flutter.plugins.cachednetworkimage.CachedNetworkImage')
class CachedNetworkImage extends Widget {
  external CachedNetworkImage({Key? key, required String imageUrl, double? width, double? height,
      BoxFit? fit, Widget Function(BuildContext context, String url)? placeholder,
      Widget Function(BuildContext context, String url, Object error)? errorWidget,
      Map<String, String>? httpHeaders, int? memCacheWidth, int? memCacheHeight,
      Duration? fadeInDuration, String? cacheKey});
}

@JavaName('com.codename1.flutter.NetworkImage')
class CachedNetworkImageProvider extends ImageProvider {
  external CachedNetworkImageProvider(String url, {double? scale, Object? headers});
}
