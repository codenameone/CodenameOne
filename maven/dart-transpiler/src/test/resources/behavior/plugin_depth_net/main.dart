// Deeper network plugin packs, headless: dio's interceptor order, error types,
// CancelToken, FormData, download and progress paths; url_launcher's LaunchMode;
// share_plus's ShareParams; image_picker with size options and no display.
import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

Future<void> main() async {
  try { await run(); } catch (e) { print('ERR $e'); }
}

Future<void> run() async {
  final dio = Dio(BaseOptions(connectTimeout: const Duration(seconds: 2), headers: {'X-A': '1'}));
  final log = <String>[];
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (o, h) { log.add('req1'); h.next(o); },
    onError: (e, h) { log.add('err1 ${e.type}'); h.next(e); },
  ));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (o, h) { log.add('req2'); h.next(o); },
    onError: (e, h) { log.add('err2'); h.next(e); },
  ));
  try {
    await dio.get('http://127.0.0.1:9/x');
  } on DioException catch (e) {
    print('conn ${e.type} ${e.response?.statusCode}');
  }
  print(log.join(','));

  final token = CancelToken();
  token.cancel('bye');
  try {
    await dio.get('http://127.0.0.1:9/y', cancelToken: token);
  } on DioException catch (e) {
    print('cancel ${e.type} ${CancelToken.isCancel(e)}');
  }

  var sent = 0;
  try {
    await dio.post('http://127.0.0.1:9/z', data: {'a': 1}, onSendProgress: (c, t) { sent++; });
  } on DioException catch (e) {
    print('post ${e.type} progress=$sent');
  }
  try {
    await dio.download('http://127.0.0.1:9/f', '/tmp/never', onReceiveProgress: (c, t) {});
  } on DioException catch (e) {
    print('download ${e.type}');
  }

  final form = FormData.fromMap({'name': 'x', 'file': MultipartFile.fromBytes([1, 2, 3], filename: 'a.bin')});
  print('form ${form.fields.length} ${form.files.length}');
  print('types ${ResponseType.json} ${ResponseType.plain} ${ResponseType.bytes}');
  print('kinds ${DioExceptionType.receiveTimeout} ${DioExceptionType.sendTimeout} ${DioExceptionType.badResponse}');

  print('modes ${LaunchMode.values.length} ${LaunchMode.inAppBrowserView.name}');
  print('canLaunch ${await canLaunchUrl(Uri.parse('tel:123'))}');
  print('launch ${await launchUrl(Uri.parse('https://example.com'), mode: LaunchMode.inAppWebView)}');

  final r = await SharePlus.instance.share(ShareParams(text: 'hi', subject: 's'));
  print('share ${r.status}');

  final picked = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 100, imageQuality: 50);
  print('picked $picked');
  final many = await ImagePicker().pickMultiImage(maxWidth: 10);
  print('many ${many.length}');
}
