// dio headless: there is no network, so every request fails as a connectionError
// -- which is exactly an app's offline path -- and the interceptor chain runs as
// dio runs it: request hooks in order, then the error hooks, and an error hook can
// recover with a response. Options merge from the instance down to the request.
import 'package:dio/dio.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(this.log);
  final List<String> log;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    options.headers['Authorization'] = 'Bearer t0k3n';
    log.add('auth ${options.method} ${options.uri}');
    handler.next(options);
  }
}

Future<void> main() async {
  final log = <String>[];
  final dio = Dio(BaseOptions(
    baseUrl: 'https://example.com/api',
    connectTimeout: const Duration(seconds: 5),
    headers: {'Accept': 'application/json'},
  ));
  dio.interceptors.add(AuthInterceptor(log));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) {
      log.add('headers ${options.headers['Authorization']} ${options.headers['Accept']}');
      handler.next(options);
    },
    onError: (e, handler) {
      log.add('error ${e.type.name}');
      handler.next(e);
    },
  ));

  try {
    await dio.get<List<dynamic>>('/pets', queryParameters: {'species': 'dog', 'page': 2});
    print('unexpected success');
  } on DioException catch (e) {
    print('caught ${e.type} ${e.requestOptions.path}');
  }
  for (final l in log) {
    print(l);
  }

  // An error interceptor that answers from a cache instead.
  final cached = Dio();
  cached.interceptors.add(InterceptorsWrapper(onError: (e, handler) {
    handler.resolve(Response(requestOptions: e.requestOptions, data: {'cached': true}, statusCode: 200));
  }));
  final r = await cached.post<Map<String, dynamic>>('https://example.com/x', data: {'a': 1});
  print('recovered ${r.statusCode} ${r.data} ${r.requestOptions.method}');

  // A request interceptor that answers without sending.
  final mocked = Dio();
  mocked.interceptors.add(InterceptorsWrapper(onRequest: (o, handler) {
    handler.resolve(Response(requestOptions: o, data: 'mock ${o.path}', statusCode: 201));
  }));
  final m = await mocked.get<String>('/hello');
  print('${m.statusCode} ${m.data}');

  final token = CancelToken();
  token.cancel('bye');
  try {
    await Dio().get('https://example.com', cancelToken: token);
  } on DioError catch (e) {
    print('legacy name catches ${e.type.name}');
  }
}
