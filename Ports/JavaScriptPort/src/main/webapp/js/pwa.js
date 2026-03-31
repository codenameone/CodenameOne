UpUp.start({
    'content-url': 'offline.html',
    'assets': [
        '/css/bootstrap.min.css', 
        '/css/bootstrap-theme.min.css', 
        '/style.css', 
        '/js/push.js?v={APP_VERSION}',  
        '/js/jquery.min.js?v={APP_VERSION}', 
        '/js/fontmetrics.js?v={APP_VERSION}',
        '/js/manup.js',
        '/teavm/classes.js?v={APP_VERSION}',
        '/js/bootstrap.min.js',
        '/js/upup.min.js',
        '/progress.gif',
        '/icon.png'
        /* UPUP_ASSETS_LIST */

    ],
    'service-worker-url': 'sw.js',
    'cache-version' : '{APP_VERSION}'
});