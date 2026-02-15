---
title: Terse REST API
slug: terse-rest-api
url: /blog/terse-rest-api/
original_url: https://www.codenameone.com/blog/terse-rest-api.html
aliases:
- /blog/terse-rest-api.html
date: '2017-07-04'
author: Shai Almog
---

![Header Image](/blog/terse-rest-api/new-features-2.jpg)

When discussing the features Chen slipped in just in the last minute of 3.7 I mentioned there were two such features but only discussed the [desktop skin](/blog/desktop-skin.html). The second one is a more terse/logical API for calling REST web services using the builder pattern.

I actually discussed this briefly in the [how do i video for networking & web services](/how-do-i/how-do-i-use-http-sockets-webservices-websockets/) near the end. But that’s one of those details that might have easily been missed in that video…​

### Easy Approach to Rest

The important class of note is `Rest`. You can use it to define the HTTP method and start building based on that. So if I want to get a parsed JSON result from a URL I could do:
    
    
    Response<Map> jsonData = Rest.get(myUrl).getAsJsonMap();

For a lot of REST requests this will fail because we need to add an HTTP header indicating that we accept JSON results. We have a special case support for that:
    
    
    Response<Map> jsonData = Rest.get(myUrl).acceptJson().getAsJsonMap();

We can also do POST requests just as easily:
    
    
    Response<Map> jsonData = Rest.post(myUrl).body(bodyValueAsString).getAsJsonMap();

Notice the usage of post and the body builder method. There are MANY methods in the builder class that cover pretty much everything you would expect and then some when it comes to the needs of rest services.

I changed the code in the kitchen sink webservice sample to use this API. I was able to make it shorter and more readable without sacrificing anything.

### Future

There is a lot of additional work that we can put into this API and we’ll invest the time based on interest from you. It has an async API too which I didn’t mention because I think it needs some additional work and there are probably good ways to better map this into properties to make the code more fluent and easy.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **salah Alhaddabi** — July 6, 2017 at 9:24 pm ([permalink](/blog/terse-rest-api/#comment-22382))

> salah Alhaddabi says:
>
> Thanks a lot Shi. This is a big help for today’s mobile businesses.
>



### **Tim Gallagher** — July 24, 2017 at 8:08 pm ([permalink](/blog/terse-rest-api/#comment-23324))

> Tim Gallagher says:
>
> Just in time for me. Is this included in the the base library or do I need to install one of the extensions?
>



### **Shai Almog** — July 25, 2017 at 4:35 am ([permalink](/blog/terse-rest-api/#comment-23510))

> Shai Almog says:
>
> It’s in the library just import
>



### **Tim Gallagher** — July 27, 2017 at 6:30 pm ([permalink](/blog/terse-rest-api/#comment-23736))

> Tim Gallagher says:
>
> OK – I’ve looked through the REST api, and found that I need more control. I have to use Basic Authorization to log into a web service, which then supplies a token in the response header, but not a cookie, and at the same time some JSON with details of the login. Then I have to use that token on subsequent requests. I’ve looked at the ConnectionRequest class, which seems to be the best option for me, adding header params and overriding buildRequestBody() (based on these two SO items: [https://stackoverflow.com/q…](<https://stackoverflow.com/questions/39063909/how-to-post-json-to-a-rest-webservice-in-codenameone>) and [https://stackoverflow.com/q…](<https://stackoverflow.com/questions/40557730/codenameone-post-request-body>)) but I’m not sure which method, if any, I can override in order to get the header information, the token, in the response. What do you suggest?
>



### **Shai Almog** — July 28, 2017 at 5:24 am ([permalink](/blog/terse-rest-api/#comment-23625))

> Shai Almog says:
>
> In ConnectionRequest readHeaders() can be overridden to read the headers. I lost you a bit on the token.
>
> The new REST API should have methods to process response headers and cookies. Can you please file RFE’s to add those?
>
> Thanks.
>



### **Thomas** — March 20, 2018 at 12:35 am ([permalink](/blog/terse-rest-api/#comment-23968))

> Thomas says:
>
> This is nothing but the Rest object is currently missing the .patch(url) method. Would be nice if it could be add in the next release (Furthermore that the RequestBuilder(method, url) constructor is not public so you have to extend it if you want to create a custom one to handle the PATCH method for now….)
>
> NOTE: the PATCH method act as a post or put, so, aditionnaly to define it in the Rest object, in the createRequest() method of RequestBuilder you also need to change  
> req.setPost(method.equalsIgnoreCase(“POST”) || method.equalsIgnoreCase(“PUT”));  
> to  
> req.setPost(method.equalsIgnoreCase(“POST”) || method.equalsIgnoreCase(“PUT”) || method.equalsIgnoreCase(“PATCH”));  
> to handle it correctly
>



### **Shai Almog** — March 20, 2018 at 6:00 am ([permalink](/blog/terse-rest-api/#comment-23879))

> Shai Almog says:
>
> Thanks, can you file an issue?
>



### **Tafadzwa Moyo** — April 17, 2018 at 5:36 am ([permalink](/blog/terse-rest-api/#comment-23888))

> Tafadzwa Moyo says:
>
> does it work for https too?
>



### **Shai Almog** — April 18, 2018 at 5:19 am ([permalink](/blog/terse-rest-api/#comment-23837))

> Shai Almog says:
>
> Sure.
>



### **Francesco Galgani** — May 6, 2018 at 8:21 pm ([permalink](/blog/terse-rest-api/#comment-23813))

> Francesco Galgani says:
>
> In this post, you wrote:
>
> Map<string, object=””> jsonData = Rest.get(myUrl).getAsJsonMap(); but getAsJsonMap() returns a Response<map>.
>
> I suppose that you mean:  
> Response<map> jsonData = Rest.get(myUrl).acceptJson().getAsJsonMap();
>
> if (jsonData.getResponseCode() == 200) {  
> Map<string, object=””> response = jsonData.getResponseData();  
> }
>



### **Francesco Galgani** — May 6, 2018 at 8:22 pm ([permalink](/blog/terse-rest-api/#comment-23818))

> Francesco Galgani says:
>
> Disquis changed the code in my comment…
>



### **Shai Almog** — May 7, 2018 at 4:16 am ([permalink](/blog/terse-rest-api/#comment-23830))

> Shai Almog says:
>
> Thanks, I’ll fix that in the next site update
>



### **Max Amende** — January 8, 2019 at 2:31 pm ([permalink](/blog/terse-rest-api/#comment-24095))

> Max Amende says:
>
> Hi,
>
> I have a problem and I can not find the solution. I use:  
> `Response<map> jsonData = Rest.get(myUrl).acceptJson().getAsJsonMap();`  
> to get Data from a firestore database.  
> The query works and codename one should get:  
> `{  
> “name”: “”,  
> “fields”: {  
> “1”: {  
> “stringValue”: “Test One”  
> },  
> “2”: {  
> “stringValue”: “Test Two”  
> },  
> “3”: {  
> “stringValue”: “Test Three”  
> },  
> “4”: {  
> “integerValue”: “1234”  
> } },  
> “createTime”: “2019-01-07T21:10:00.621697Z”,  
> “updateTime”: “2019-01-08T13:09:05.737437Z”  
> }`
>
> But no matter how it try it I can not get the vales out of it.  
> I thought I should get the value with:  
> `  
> jsonData.getResponseData().get(“1”);  
> `  
> @codenameone:disqus Do you know where my thinking error is?
>



### **Shai Almog** — January 9, 2019 at 3:36 am ([permalink](/blog/terse-rest-api/#comment-24065))

> Shai Almog says:
>
> Shouldn’t it be:
>
> String value = (String)((Map)jsonData.getResponseData().get(“1”)).get(“stringValue”);
>
> Notice you can inspect the returned Map in the debugger…
>



### **Max Amende** — January 9, 2019 at 5:35 pm ([permalink](/blog/terse-rest-api/#comment-24008))

> Max Amende says:
>
> Found the mistake.  
> Thanks a lot for your help.
>
> Sorry
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
