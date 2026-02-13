---
title: Rest API Error Handling
slug: rest-api-error-handling
url: /blog/rest-api-error-handling/
original_url: https://www.codenameone.com/blog/rest-api-error-handling.html
aliases:
- /blog/rest-api-error-handling.html
date: '2018-08-12'
author: Shai Almog
---

![Header Image](/blog/rest-api-error-handling/new-features-2.jpg)

We added a lot of features and fixed bugs over the past couple of months and I’ve been a bit lax on blogging. I’ll try to fix that as we approach the revised 5.0 release date. One of the big changes we added over the weekend (it will be in the builds on Friday), is a huge rework of the `Rest` API.

If you are unfamiliar with the `Rest` API check out this [older post](/blog/terse-rest-api.html). The `Rest` API had a few problems, the most pressing of these was the inconsistent error handling behavior. A failure of the `Rest` call didn’t always trigger error handling correctly and behaved inconsistently on reading the response of the error.

Another inherent problem which we ran into was processing errors differently. E.g. I have a JSON webservice but an error might return a String instead of JSON. With the existing API there was no way of handling this.

This mostly impacted the asynchronous calls but was a problem with the synchronous calls too. Another problem was the `SuccessCallback` interface used through the API. In itself it’s fine but the callback method is named: `onSucess` which somehow slipped through…​  
Unfortunately fixing interfaces is nearly impossible so we had to add a new interface [OnComplete](https://github.com/codenameone/CodenameOne/blob/master/CodenameOne/src/com/codename1/util/OnComplete.java). Like the [SuccessCallback](https://github.com/codenameone/CodenameOne/blob/master/CodenameOne/src/com/codename1/util/SuccessCallback.java) the interface is “lambda friendly” by featuring only one method.

### A Word About Deprecation

We use deprecation a lot to indicate the better API for usage. As a result we had to deprecate all the existing asynchronous calls in the `Rest` class and replace them with new API’s.

We won’t remove these deprecated API’s in the foreseeable future. The deprecation is there to show the better/newer API’s.

### Better Properties Integration

As part of this work we also squeezed in some work on [property business object support](/blog/properties-are-amazing.html). So if I want to make a JSON request to the server with a `User` `PropertyBusinessObject` I can do something like:
    
    
    User userPropertyObject = ...;
    ServerResultObject result = Rest.post(url).
       jsonContent().
       body(userPropertyObject).
       getAsProperties(ServerResultObject.class);

Notice that the result can also be parsed from JSON into a property business object seamlessly!

### Fetch API’s

The existing synchronous get API’s such as `getAsString()` didn’t change. We left the signature as is and they should work the same way.

However, the asynchronous API’s relied on the old `SuccessCallback` interface so we had to change the signature. If this was old Java 5 we could have just overloaded the method but this would have created a conflict for lambda calls so we decided to change the method name to a fetch prefix.

So the existing code:
    
    
    Rest.get(url).
       getAsString(c -> callback(c));

Would become:
    
    
    Rest.get(url).
       fetchAsString(c -> callback(c));

Since I used a lambda the fact that we replaced `SuccessCallback` with `OnComplete` becomes irrelevant.

All the versions of the methods that received `FailureCallback` instances are now deprecated as there were serious problems with error handling.

### Error Handling

The new error handling code is far more consistent and would work both for the synchronous and asynchronous calls. Notice that if you don’t define an error handler the behavior in the case of an error might be slightly inconsistent but should generally just return the error value in the standard response.

E.g.:
    
    
    Rest.get(url).
       jsonContent().
       onErrorCodeString(r -> error(r)).
       fetchAsJsonMap(c -> callback(c));

There are five new `onError` style methods:
    
    
    public RequestBuilder onErrorCodeBytes(ErrorCodeHandler<byte[]> err);
    public RequestBuilder onErrorCodeJSON(ErrorCodeHandler<Map> err);
    public RequestBuilder onErrorCode(ErrorCodeHandler<PropertyBusinessObject> err, Class errorClass);
    public RequestBuilder onErrorCodeString(ErrorCodeHandler<String> err);
    public RequestBuilder onError(ActionListener<NetworkEvent> error);

The first four methods are invoked for an error code from the server (value that isn’t 2xx or 3xx from the HTTP response). Notice that each type parses the result and can use a different parsing system from the default parsing logic. We even support parsing errors into a `PropertyBusinessObject` which can be of a type different from the one used in the main result. So you can define a `ServerError` business object and parse error result JSON directly into a type-safe object.

The `ErrorCodeHandler` interface is a simple lambda friendly interface that returns the `Result` object.

The last method `onError` is invoked in case of an exception during the connection. We separate a failure such as exceptions which occur due to connectivity or physical issues from a server error code failure. Normally, I would recommend ignoring `onError` and focusing on a global error handler using `NetworkManager` as exists in the [boilerplate code in a new project](/blog/new-default-code.html).

### Moving Forward

Let us know what you think of these changes and how we can further improve the syntax/utility of this API. I think there is a lot we can do to take it forward.

Two omissions in the API that I’ve run into over the years are:

  * Support for XML — this should be easy to add but there wasn’t much demand for it. If there is we can probably add XML support too

  * Image handling — we already have great Image download/caching API’s so we didn’t add them to this class. I’m not sure if this would be the right API to do image requests but it might

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
