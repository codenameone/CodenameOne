---
title: New Push Servers
slug: new-push-servers
url: /blog/new-push-servers/
original_url: https://www.codenameone.com/blog/new-push-servers.html
aliases:
- /blog/new-push-servers.html
date: '2015-09-01'
author: Shai Almog
---

![Header Image](/blog/new-push-servers/push-megaphone.png)

We are starting the complete overhaul of our push implementation that will allow us to deliver improved push  
related fixes/features and provide more reliability to the push service. When we designed our push offering  
initially it was focused around the limitations of Google App Engine which we are finally phasing out. The new  
servers are no longer constrained by this can scale far more easily and efficiently for all requirements. 

However, as part of this we decided to separate the push functionality into two very different capabilities:  
push & device registration.  
Currently only push is supported and so the feature of pushing to all devices is effectively unsupported at the moment.  
However, once the device registration API is exposed it will allow you to perform many tasks that were often  
requested such as the ability to push to a cross section of devices (e.g. users in a given city). 

The original push API mixed device tracking and the core push functionality in a single API which meant we  
had scaling issues when dealing with large volumes of push due to database issues. So the new API discards  
the old device id structure which is just a numeric key into our database. With the new API we have a new  
device key which includes the native OS push key as part of its structure e.g. `cn1-ios-nativedevicekey`  
instead of `999999`. 

Assuming you store device ids in your server code you can easily convert them to the new device ids, your server  
code can check if a device id starts with `cn1-` and if not convert the old numeric id to the new ID  
using this request (assuming 999999 is the device id): 
    
    
    https://codename-one.appspot.com/token?m=id&i=999999

The response to that will be something like this: 
    
    
    {"key":"cn1-ios-nativedevicecode"}

The response to that will be something or this: 
    
    
    {"error":"Unsupported device type"}

To verify that a push is being sent by your account and associate the push quotas correctly the new API  
requires a push token. You can see your push token in the developer console at the bottom of the account  
settings tab. If it doesn’t appear logout and login again. 

The new API is roughly identical to the old API with two major exceptions: 

  1. We now need to replace usage of `Push.getDeviceKey()` with `Push.getPushKey()`.  
We thought about keeping the exact same API but eventually decided that creating a separate API will simplify  
migration and allow you to conduct it at your own pace. 
  2. All push methods now require the push token as their first argument. The old methods will push to the old push servers  
and the new identical methods that accept a token go to the new servers. 

To send a push directly to the new servers you can use very similar code to the old Java SE code we provided just  
changing the URL, adding the token and removing some of the unnecessary arguments. Send the push to the  
URL `https://push.codenameone.com/push/push` which accepts the following arguments: 

  * **token** – your developer token to identify the account sending the push
  * **device** – one or more device keys to send the push to. You can send push to up to 500 devices with a single push.
  * **type** – the message type identical to the old set of supported types in the old push servers
  * **body** – the body of the message
  * **auth** – the Google push auth key
  * **production** – true/false whether to push to production or sandbox environment in iOS
  * **certPassword** – password for the push certificate in iOS push
  * **cert** – http or https URL containing the push certificate for an iOS push

E.g. we can send push to the new servers using something like this from Java SE/EE: 
    
    
    URLConnection connection = new URL("https://push.codenameone.com/push/push").openConnection();
    connection.setDoOutput(true); 
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    String query = "token=TOKEN&device=" + deviceId1 + 
                        "&device=" + deviceId2 + "&device=" + deviceId3 +
                        "&type=1&auth=GOOGLE_AUTHKEY&certPassword=URL_ENCODED_CERTIFICATE_PASSWORD&" +
                        "cert=URL_ENCODED_LINK_TO_YOUR_P12_FILE&body=" + URLEncoder.encode(MESSAGE_BODY, "UTF-8") +
                        "&production=false"; 
    try (OutputStream output = connection.getOutputStream()) {
        output.write(query.getBytes("UTF-8"));
    }
    int c = connection.getResponseCode();
    ... read response and parse JSON
    

Unlike the previous API which was completely asynchronous and decoupled the new API is mostly synchronous  
so we return JSON that you should inspect for results and to maintain your device list. E.g. if there is an error  
that isn’t fatal such as quota exceeded etc. you will get an error message like this: 
    
    
    {"error":"Error message"}

A normal response though, will be an array with results: 
    
    
    [
        {"id"="deviceId","status"="error","message"="Invalid Device ID"},
        {"id"="cn1-gcm-nativegcmkey","status"="updateId" newId="cn1-gcm-newgcmkey"},
        {"id"="cn1-gcm-okgcmkey","status"="OK"},
        {"id"="cn1-gcm-errorkey","status"="error" message="Server error message"},
        {"id"="cn1-ios-iphonekey","status"="inactive" },
    ]

There are several things to notice in the responses above: 

  * If the response contains `status=updateId` it means that the GCM server wants you to update the device id to  
a new device id. You should do that in the database and avoid sending pushes to the old key.
  * iOS doesn’t acknowledge device receipt but it does send a `status=inactive` result which  
you should use to remove the device from the list of devices.

**Update:** It seems that APNS (Apple’s push service) returns uppercase key results. So you need  
to query the database in a case insensitive way. 

#### Moving Forward

Right now the legacy push system still sends to its own push destination. We will redirect the old push servers  
to the new servers which will effectively mean that every push sent to the old push servers will go to the new  
servers and it should allow us to keep them up longer. However, we’ll eventually retire them as we will retire  
the entire App Engine infrastructure so you should try to migrate at your own pace instead of rushing thru  
it at the last moment. 

We are aware that for some corporate requirements push is strategically important so we are working on the ability  
to integrate with 3rd party push providers such as Urban Airship to provide scale for higher push volumes and  
builtin features for cross section/analytics.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jaanus Hansen** — September 2, 2015 at 9:01 pm ([permalink](/blog/new-push-servers/#comment-24181))

> Jaanus Hansen says:
>
> A stupid question – why we just can’t use Apple’s and Google’s push services? Why is it better to have one more constant connection up to use push services from CN1?
>



### **Shai Almog** — September 3, 2015 at 3:39 am ([permalink](/blog/new-push-servers/#comment-22420))

> Shai Almog says:
>
> Its only 1 instead of 3 (RIM too) and potentially more as we move forward (e.g. Amazon etc.).  
> APNS sucks, its really hard to work with and isn’t just a simple webservice.
>



### **Carlos** — September 10, 2015 at 5:56 pm ([permalink](/blog/new-push-servers/#comment-22162))

> Carlos says:
>
> You say:  
> device – one or more device keys to send the push to. You can send push to up to 500 devices with a single push.
>
> How can I do this with more than one device if I use cn1 ConnectionRequest instead of Java SE URLConnection?  
> Can I pass the argument just like this?  
> “&device=” + deviceId1 + “&device=” + deviceId2 + “&device=” + deviceId3
>



### **Shai Almog** — September 11, 2015 at 3:58 am ([permalink](/blog/new-push-servers/#comment-22357))

> Shai Almog says:
>
> Yes. Notice that in the JavaSE code listed above I did just that.
>



### **Wim Bervoets** — September 15, 2015 at 7:51 am ([permalink](/blog/new-push-servers/#comment-21546))

> Wim Bervoets says:
>
> When will the old Push infrastructure stop working?
>
> Wim
>



### **Shai Almog** — September 15, 2015 at 1:38 pm ([permalink](/blog/new-push-servers/#comment-22316))

> Shai Almog says:
>
> We didn’t schedule this. We are looking at it as a multi-stage process:  
> 1 – make sure the new push infrastructure works and is robust  
> 2 – confirm this by making all of the old API push calls effectively redirect to the new servers (this will be seamless to guys using the old push servers but will demonstrate to us that the new push servers can handle the load).  
> 3 – Start migrating old API users and evaluate the transition process for everyone using the API.
>
> We’re at stage 1 and haven’t scheduled stage 2 at this time. We’ll probably leave the compatibility layer running as long as we can but we have to migrate off of app engine eventually and we’ll try to do it when everyone finished the migration.
>
> So you have plenty of time right now and I doubt we’ll shut them down in the next 6 months. I’ll consider it a success if we can make the entire migration in one year.
>



### **Gerben Kegel** — February 16, 2016 at 11:51 am ([permalink](/blog/new-push-servers/#comment-22582))

> Gerben Kegel says:
>
> Is there a reason why the response is not (always) valid JSON?
>



### **Shai Almog** — February 17, 2016 at 3:33 am ([permalink](/blog/new-push-servers/#comment-22349))

> Shai Almog says:
>
> Bug. Can you give a specific example? We’ll fix those cases.
>



### **Gerben Kegel** — February 17, 2016 at 9:17 am ([permalink](/blog/new-push-servers/#comment-22334))

> Gerben Kegel says:
>
> `[  
> {“id”=”deviceId”,”status”=”error”,”message”=”Invalid Device ID”},  
> {“id”=”cn1-gcm-nativegcmkey”,”status”=”updateId” newId=”cn1-gcm-newgcmkey”},  
> {“id”=”cn1-gcm-okgcmkey”,”status”=”OK”},  
> {“id”=”cn1-gcm-errorkey”,”status”=”error” message=”Server error message”},  
> {“id”=”cn1-ios-iphonekey”,”status”=”inactive” },  
> ]`
>
> You use = instead of : and not always doublequote the key (newId and message)
>



### **Shai Almog** — February 18, 2016 at 3:27 am ([permalink](/blog/new-push-servers/#comment-22245))

> Shai Almog says:
>
> Thanks, this should be fixed now.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
