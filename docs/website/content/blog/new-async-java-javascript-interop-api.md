---
title: New Async Java-Javascript Interop API
slug: new-async-java-javascript-interop-api
url: /blog/new-async-java-javascript-interop-api/
original_url: https://www.codenameone.com/blog/new-async-java-javascript-interop-api.html
aliases:
- /blog/new-async-java-javascript-interop-api.html
date: '2018-02-13'
author: Steve Hannah
---

![Header Image](/blog/new-async-java-javascript-interop-api/html5-banner.jpg)

We recently introduced a new API for interacting with Javascript in Codename One. This new API is part of the [BrowserComponent](/javadoc/com/codename1/ui/BrowserComponent/) class, and effectively replaces the [com.codename1.javascript package](/javadoc/com/codename1/javascript/package-summary/), which is now deprecated.

## So what was wrong with the old API?

The old API provided a synchronous wrapper around an inherently asynchronous process, and made extensive use of [invokeAndBlock()](/javadoc/com/codename1/ui/Display/#invokeAndBlock-java.lang.Runnable-) underneath the covers. This resulted in a very nice API with high-level abstractions that played nicely with a synchronous programming model, but it came with a price-tag in terms of performance, complexity, and predictability. Let’s take a simple example, getting a reference to the “window” object:
    
    
    JSObject window = ctx.get("window");

This code looks harmless enough, but this is actually quite expensive. It issues a command to the `BrowserComponent`, and uses [invokeAndBlock()](/javadoc/com/codename1/ui/Display/#invokeAndBlock-java.lang.Runnable-) to wait for the command to go through and send back a response. [invokeAndBlock()](/javadoc/com/codename1/ui/Display/#invokeAndBlock-java.lang.Runnable-) is a magical tool that allows you to “block” without blocking the EDT, but it has its costs, and shouldn’t be overused. Most of the Codename One APIs that use `invokeAndBlock()` indicate this in their name. E.g. `Component.animateLayoutAndWait()`. This gives you the expectation that this call could take some time, and helps to alert you to the underlying cost.

The problem with the `ctx.get("window")` call is that it looks the same as a call to `Map.get(key)`. There’s no indication that this call is expensive and could take time. One call like this probably isn’t a big deal, but it doesn’t take long before you have dozens or even hundreds of calls like this littered throughout your codebase, and they can be hard to pick out.

## The New API

The new API fully embraces the asynchronous nature of Javascript. It uses callbacks instead of return values, and provides convenience wrappers with the appropriate “AndWait()” naming convention to allow for synchronous usage. Let’s look at a simple example:

__ |  In all of the sample code below, you can assume that variables named `bc` represent an instance of [BrowserComponent](/javadoc/com/codename1/ui/BrowserComponent/).   
---|---  
      
    
    bc.execute(
        "callback.onSuccess(3+4)",
        res -> Log.p("The result was "+res.getInt())
    );

This code should output “The result was 7” to the console. It is fully asynchronous, so you can include this code anywhere without worrying about it “bogging down” your code. The full signature of this form of the [execute()](/javadoc/com/codename1/ui/BrowserComponent/#execute-java.lang.String-com.codename1.util.SuccessCallback-) method is:
    
    
    public void execute(String js, SuccessCallback<JSRef> callback)

The first parameter is just a javascript expression. This javascript **MUST** call either `callback.onSuccess(result)` or `callback.onError(message, errCode)` at some point in order for your callback to be called.

The second parameter is your callback that is executed from the javascript side, when `callback.onSuccess(res)` is called. The callback takes a single parameter of type [JSRef](/javadoc/com/codename1/ui/BrowserComponent.JSRef/) which is a generic wrapper around a javascript variable. JSRef has accessors to retrieve the value as some of the primitive types. E.g. `getBoolean()`, `getDouble()`, `getInt()`, `toString()`, and it provides some introspection via the `getType()` method.

__ |  It is worth noting that the callback method can only take a single parameter. If you need to pass multiple parameters, you may consider including them in a single string which you parse in your callback.   
---|---  
  
## Synchronous Wrappers

As mentioned above, the new API also provides an `executeAndWait()` wrapper for `execute()` that will work synchronously. It, as its name suggests, uses `invokeAndBlock` under the hood so as not to block the EDT while it is waiting.

E.g.
    
    
    JSRef res = bc.executeAndWait("callback.onSuccess(3+4)");
    Log.p("The result was "+res.Int());

Prints “The result was 7”.

__ |  When using the `andWait()` variant, it is **extremely** important that your Javascript calls your callback method at some point – otherwise it will block **indefinitely**. We provide variants of executeAndWait() that include a timeout in case you want to hedge against this possibility.   
---|---  
  
## Multi-use Callbacks

The callbacks you pass to `execute()` and `executeAndWait()` are single-use callbacks. You can’t, for example, store the `callback` variable on the javascript side for later use (e.g. to respond to a button click event). If you need a “multi-use” callback, you should use the `addJSCallback()` method instead. Its usage looks identical to `execute()`, the only difference is that the callback will life on after its first use. E.g. Consider the following code:
    
    
    bc.execute(
        "$('#somebutton').click(function(){callback.onSuccess('Button was clicked')})",
        res -> Log.p(res.toString())
    );

The above example, assumes that jQuery is loaded in the webpage that we are interacting with, and we are adding a click handler to a button with ID “somebutton”. The click handler calls our callback.

If you run this example, the first time the button is clicked, you’ll see “Button was clicked” printed to the console as expected. However, the 2nd time, you’ll just get an exception. This is because the callback passed to `execute()` is only single-use.

We need to modify this code to use the `addJSCallback()` method as follows:
    
    
    bc.addJSCallback(
        "$('#somebutton').click(function(){callback.onSuccess('Button was clicked')})",
        res -> Log.p(res.toString())
    );

Now it will work no matter how many times the button is clicked.

## Passing Parameters to Javascript

In many cases, the javascript expressions that you execute will include parameters from your java code. Properly escaping these parameters is tricky at worst, and annoying at best. E.g. If you’re passing a string, you need to make sure that it escapes quotes and new lines properly or it will cause the javascript to have a syntax error. Luckily we provide variants of `execute()` and [addJSCallback()](/javadoc/com/codename1/ui/BrowserComponent/#addJSCallback-java.lang.String-com.codename1.util.SuccessCallback-) that allow you to pass your parameters and have them automatically escaped.

For example, suppose we want to pass a string with text to set in a textarea within the webpage. We can do something like:
    
    
    bc.execute(
        "jQuery('#bio').text(${0}); jQuery('#age').text(${1})",
        new Object[]{
           "A multi-linen string with "quotes"",
           27
        }
    );

The gist is that you embed placeholders in the javascript expression that are replaced by the corresponding entry in an array of parameters. The `${0}` placeholder is replaced by the first item in the parameters array, the `${1}` placeholder is replaced by the 2nd, and so on.

## Proxy Objects

The new API also includes a [JSProxy](/javadoc/com/codename1/ui/BrowserComponent.JSProxy/) class that encapsulates a Javascript object simplify the getting and setting of properties on Javascript objects – and the calling of their methods. It provides essentially three core methods, along with several variants of each to allow for async or synchronous usages, parameters, and timeouts.

E.g. We might want to create a proxy for the [window.location](https://developer.mozilla.org/en-US/docs/Web/API/Window/location) object so that we can access its properties more easily from Java.
    
    
    JSProxy location = bc.createJSProxy("window.location");

Then we can retrieve its properties using the `get()` method:
    
    
    location.get("href", res -> Log.p("location.href="+res));

Or synchronously:
    
    
    JSRef href = location.getAndWait("href");
    Log.p("location.href="+href);

We can also set its properties:
    
    
    location.set("href", "http://www.google.com");

And call its methods:
    
    
    location.call("replace", new Object[]{"http://www.google.com"},
        res -> Log.p("Return value was "+res)
    );
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **RWang** — February 18, 2018 at 2:41 pm ([permalink](/blog/new-async-java-javascript-interop-api/#comment-23554))

> RWang says:
>
> With the recent update and deprecation of the com.codename1.javascript package (specifically the JSFunction and JSObject classes), I’d like to know if there are any way to represent a JavaScript Object or functions? For example when I’d like to use the [JSProxy.call](<http://JSProxy.call)(>…) with a js function as argument, using a String representation doesn’t work as of now, so are there any alternatives?
>



### **shannah78** — February 19, 2018 at 1:38 pm ([permalink](/blog/new-async-java-javascript-interop-api/#comment-23795))

> shannah78 says:
>
> I have just added support for this in Git. [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/commit/1d6aa547da7297d7caaff2640d18052f37fedb48>)  
> This will be available in next server update on Friday.  
> Just wrap the Javascript literal expression in a JSExpression object. E.g.  
> [myProxy.call](<http://myProxy.call)(>“myMethod”, new Object[]{“a string”, new JSExpression(“a.javascript.expression()”)}, res->{…})
>



### **ZombieLover** — March 14, 2018 at 4:35 pm ([permalink](/blog/new-async-java-javascript-interop-api/#comment-23618))

> ZombieLover says:
>
> Love this thanks Steve
>



### **ZombieLover** — March 15, 2018 at 9:09 am ([permalink](/blog/new-async-java-javascript-interop-api/#comment-23945))

> ZombieLover says:
>
> For anyone trying to run a jquery script using addJSCallback… you may have to do it after the document has loaded. Otherwise you might be running it before jQuery has been loaded. Example:  
> browser.addWebEventListener(“onLoad”, new ActionListener() {  
> public void actionPerformed(ActionEvent evt) {  
> browser.addJSCallback(  
> “$(‘#someId’).click(function(){callback.onSuccess(‘Button was clicked’)})”,  
> res -> System.out.print(res.toString())  
> );  
> }  
> });
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
