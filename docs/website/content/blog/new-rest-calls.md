---
title: New Rest Calls
slug: new-rest-calls
url: /blog/new-rest-calls/
original_url: https://www.codenameone.com/blog/new-rest-calls.html
aliases:
- /blog/new-rest-calls.html
date: '2018-02-07'
author: Shai Almog
---

![Header Image](/blog/new-rest-calls/new-features-1.jpg)

I really like the [newer Rest API’s](/blog/terse-rest-api.html) Chen added a while back. They are ultimately far more convenient than the `ConnectionRequest` API’s for most day to day development. In fact I used them almost exclusively in the [Uber clone app](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java). As a result of that I added a few useful enhancements and capabilities.

If you are not familiar with the API it works roughly like this:
    
    
    Map<String, Object> jsonData = Rest.
                get(myUrl).
                acceptJson().
                getAsJsonMap();

This will request using an HTTP GET method from the url. It will set the accepts header to JSON and parse the response.

### Content Type

An important ommission was the content type setting, you could use the `header()` value to achieve that but content type is an important header and deserves its own method:
    
    
    Map<String, Object> jsonData = Rest.
                post(myUrl).
                contentType("application/json").
                body(bodyContentAsJSON).
                acceptJson().
                getAsJsonMap();

In this case we submit JSON data in a post request with the `application/json` content type.

### jsonContent

Notice that there is duplicate data in the sample above where both `acceptJson` and the `contentType` effectively communicate the same idea of using JSON for the protocol (albeit the two sides of the protocol). Since this is a common case it can use the shorthand:
    
    
    Map<String, Object> jsonData = Rest.
                post(myUrl).
                jsonContent().
                body(bodyContentAsJSON).
                getAsJsonMap();

### Asynchronous Callbacks

We always had asynchronous callbacks in the API but in the past they were delivered via the interface `Callback` which has 2 methods. That means we couldn’t leverage the shorthand lambda notation when implementing simple requests. We added two new permutations to the method and removed the `Async` suffix for them to avoid potential collision.

E.g. in the past I would have had to write something like this:
    
    
    Map<String, Object> jsonData = Rest.
                post(myUrl).
                jsonContent().
                body(bodyContentAsJSON).
                getAsJsonMapAsync(new Callback<Response<String>>() {
                    @Override
                    public void onSucess(Response<String> value) {
                        // code here...
                    }
    
                    @Override
                    public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                        // code here...
                    }
            });

We can now use a terse approach like this:
    
    
    Map<String, Object> jsonData = Rest.
                post(myUrl).
                jsonContent().
                body(bodyContentAsJSON).
                getAsJsonMap(value -> {
                        // code here...
                });

You will notice I somewhat glazed over the error handling which will go to the global error handler in this case. You can avoid that by overriding the error handler separately in the second argument to this method (which is optional).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — February 10, 2018 at 5:13 pm ([permalink](/blog/new-rest-calls/#comment-23657))

> Chibuike Mba says:
>
> Nice one Shai.
>
> Can I parse the server response directly to my custom object instead of Map. Or do I have to retrieve the data from the Map object and then iterate to populate my own custom objects?
>
> Have been using my CustomConnectionRequest object to achieve my aim though, but the API code above looks more concise.
>



### **Shai Almog** — February 11, 2018 at 7:30 am ([permalink](/blog/new-rest-calls/#comment-23816))

> Shai Almog says:
>
> Thanks!  
> Not directly. We have a get version that returns a byte array so the data will be read into a byte array and you would be able to parse it after.
>



### **Mohamed Selim** — May 1, 2018 at 2:11 am ([permalink](/blog/new-rest-calls/#comment-23855))

> Mohamed Selim says:
>
> Hello Mr Shai,  
> I’m a student and i’m working in project, could help me please i didn’t found the link of API Message Codename One and i didn’t know how to put web service “reponses by JSON” to use it in the form at my application in Codename One? Best Regards.
>



### **Shai Almog** — May 1, 2018 at 4:29 am ([permalink](/blog/new-rest-calls/#comment-23889))

> Shai Almog says:
>
> Hi,  
> You can search the JavaDoc for any API you can’t find [https://www.codenameone.com…](<https://www.codenameone.com/javadoc/>)  
> JSON is returned as a Java Map object in this API, you can just use the Map API to get JSON values. You can stop with a debugger and inspect the object to see what’s in it.
>



### **Mohamed Selim** — May 1, 2018 at 1:20 pm ([permalink](/blog/new-rest-calls/#comment-23974))

> Mohamed Selim says:
>
> Thank you sir, i didn’t know to use the webservice “CRUD” values from symfony to put it in CodenameOne is there any document have a good exemple for it?
>



### **Shai Almog** — May 2, 2018 at 2:28 pm ([permalink](/blog/new-rest-calls/#comment-24327))

> Shai Almog says:
>
> Sorry I’ve never worked with PHP so I have no experience with that and can’t really help. However, Steve wrote about xatafaces and CRUD operations to mysql here: [https://www.codenameone.com…](</blog/connecting-to-a-mysql-database/>)
>



### **Tafadzwa Moyo** — February 27, 2019 at 12:01 pm ([permalink](/blog/new-rest-calls/#comment-23979))

> Tafadzwa Moyo says:
>
> am getting an authentication error 401 after implementing the code below. Where am i getting it wrong
>
> ` Map<string, object=””> shiftTimes = (Map<string, object=””>) Rest.get(new UrlManager().getShiftTimes())  
> .basicAuth(urlManager.getAuthname(), manager.getVerifyDriverCode() + id)  
> .acceptJson().getAsJsonMap().getResponseData(); `
>



### **Shai Almog** — February 28, 2019 at 3:25 am ([permalink](/blog/new-rest-calls/#comment-23985))

> Shai Almog says:
>
> That means the web site didn’t accept your basic auth challenge or didn’t allow you to access that particular page. You can use the network monitor which sometimes provides insight into the error message and also into what didn’t work in the request.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
