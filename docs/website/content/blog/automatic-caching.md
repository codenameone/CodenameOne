---
title: Automatic Caching
slug: automatic-caching
url: /blog/automatic-caching/
original_url: https://www.codenameone.com/blog/automatic-caching.html
aliases:
- /blog/automatic-caching.html
date: '2016-12-14'
author: Shai Almog
---

![Header Image](/blog/automatic-caching/networking.jpg)

Caching server data locally is a huge part of the advantage a native app has over a web app. Normally this is  
non-trivial as it requires a delicate balance especially if you want to test the server resource for changes.

HTTP provides two ways to do that the [ETag](https://en.wikipedia.org/wiki/HTTP_ETag) and  
[Last-Modified](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified). While both are  
great they are non-trivial to use and by no definition seamless.

We just added an experimental feature to connection request that allows you to set the caching mode to one of  
4 states either globally or per connection request:

  * **OFF** is the default meaning no caching.

  * **SMART** means all get requests are cached intelligently and caching is “mostly” seamless

  * **MANUAL** means that the developer is responsible for the actual caching but the system will not do a request on a resource that’s already “fresh”

  * **OFFLINE** will fetch data from the cache and wont try to go to the server. It will generate a 404 error if data isn’t available

You can toggle these in the specific request by using `setCacheMode(CachingMode)` and set the global  
default using `setDefaultCacheMode(CachingMode)`.

__ |  Caching only applies to `GET` operations, it will not work for `POST` or other methods   
---|---  
  
There are several methods of interest to keep an eye for:
    
    
    protected InputStream getCachedData() throws IOException;
    protected void cacheUnmodified() throws IOException;
    public void purgeCache();
    public static void purgeCacheDirectory() throws IOException;

### getCachedData()

This returns the cached data. This is invoked to implement `readResponse(InputStream)` when running offline  
or when we detect that the local cache isn’t stale.

The smart mode implements this properly and will fetch the right data. However, the manual mode doesn’t  
store the data and relies on you to do so. In that case you need to return the data you stored at this point and must  
implement this method for manual mode.

### cacheUnmodified()

This is a callback that’s invoked to indicate a cache hit, meaning that we already have the data.

The default implementation still tries to call all the pieces for compatibility (e.g. `readResponse`).  
However, if this is unnecessary you can override that method with a custom implementation or even a blank  
implementation to block such a case.

### purgeCache & purgeCacheDirectory

These methods are pretty self explanatory. Notice one caveat though…​

When you download a file or a storage element we don’t cache them and rely on the file/storage element to  
be present and serve as “cache”. When purging we won’t delete a file or storage element you downloaded and  
thus these might remain.

However, we do remove the `ETag` and `Last-Modified` data so the files might get refreshed the next time around.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
