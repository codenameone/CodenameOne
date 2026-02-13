---
title: USE PUSH NOTIFICATION SEND SERVER PUSH MESSAGES?
slug: how-do-i-use-push-notification-send-server-push-messages
url: /how-do-i/how-do-i-use-push-notification-send-server-push-messages/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-push-notification-send-server-push-messages.html
tags:
- pro
description: Codename One unifies the push architecture for the various platforms
  under a single API
youtube_id: 8wzBpEp81Kc
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-6-1.jpg
---

{{< youtube "8wzBpEp81Kc" >}} 

#### Transcript

In this video I’ll cover some core concepts of push notification. This is a HUGE subject so I suggest you check out the developer guide for further reading as I can only scratch the surface.

iOS didn’t support background tasks and polling was a big battery drain. Push was offered as a workaround. It allowed developers to notify the user that something happened and even change a numeric badge on an icon.

Push is initiated by your server to notify a device that something happened

This is mostly a matter of user interface like a message or a visual badge. You can send a hidden push and you can send a hidden payload within the visual message but the core of push notification is a message to the end user

On Android hidden push is popular and works properly, on iOS it doesn’t. So with Codename One we aligned with the way iOS works to avoid confusion. Push shouldn’t be used as a network protocol, it isn’t designed for that

One of the main problems with that is that the user can disable push and some of the devices that don’t include play services don’t even have push from Google. This means you can’t rely on push to be there when you need it

As I mentioned before each OS implements push differently

To solve this we created our own push servers which use a simple webservice to send push. That works for almost all OS’s and push targets available with a single webservice.

You still need to register with all the providers and get credentials since our servers delegate to the vendor servers to perform the actual native push.

We’ll start with Google by going to <https://developers.google.com/mobile/add> where we need to be logged in with our google account. Here start by clicking pick a platform for which we select Android. While GCM has some support for iOS it is not native support and we use the native iOS push servers directly instead.

Now we need to paste the package of the application so Android can properly identify the right caller. You are prompted to create the package if the app doesn’t exist yet. Notice the term app in this case refers to the server side logical application in googles cloud not to the client side Android native application…  
After we finish this we can press choose and configure services this opens a prompt after a “short” wait…

In the final stage we can activate cloud messaging this provides us with two important keys one a long string and the other numeric…

You need to keep both values for later as we will need them to send and receive push but first we need the value from the sender ID

We need to launch the Codename One Settings application and go to build hint.

Here we can add the `gcm.sender_id` build hint and place in the sender id that we got from Google. Once we do that the Android portion of registration is done, we’ll still need some of these values in the code but we finished the configuration portion for Android

In the iOS side we need to run the certificate wizard because push needs special certificates of its own. But first you need to make sure you are logged in with a pro account or higher within the preferences UI, otherwise you won’t be prompted for push details

Once we login to the Apple developer account we can move to the next step

We pick or add devices, I tinted the device list a bit for privacy and then we move to the next stage

This is a very important and confusing concept in the certificate wizard… If you don’t have a certificate or it’s the first time around you won’t see this dialog but if you already have a certificate and it’s working for you should normally answer NO!  
That’s important as revoking the certificate would mean it would stop working for your other apps and if you have more than one Codename One app on the same account it’s probably not what you want.  
If you already have a P12 certificate for iOS you need to reference it from this project. A provisioning profile will be generated with reference to the existing certificate so it’s important to get all of these pieces right.

Assuming this is the case you will probably get prompted again for the debug certificate as iOS has two certificates for debug and production. Everything we said about the production certificate beforehand applies here in exactly the same way.

We now get this form, naturally I chose not to generate the certificates so it says so on top but the important piece here is the enable push checkbox that we must activate. If this checkbox isn’t here then you aren’t logged in with a pro account!  
Once we finish and press save the certificates should be generated, we should also receive an email with instructions and URLs explaining how we should integrate push.  
The email should contain URL’s in the cloud for 2 push certificates which are also generated to your local filesystem under iOS certs. These URL’s are important when we need to send the push message.

Going back to the app we can see the main class implements PushCallback. This is sometimes confusing to developers so let me be completely clear. The push callback interface MUST be implemented in the main class. The main class is the one with the init(Object) & start() methods. It must be in that class with no exception as it represents the object lifecycle which is the underlying OS concept push binds to.

Let’s move to the most important method from that interface the push(String) method which is invoked when we receive a push. This method won’t be invoked whenever push is sent from a server as devices don’t work that way. If the app is in the background and the user didn’t click the push message this method won’t be invoked.

These are the two other callbacks for push. registeredForPush is normally very important. The push key value will only be available after this method is invoked. Normally in this method we send the push key value to a server so it can send push messages to this device.  
A common pitfall with this method is usage of the device id argument. This isn’t the push key but rather the OS native id. To get the right push key you need to invoke `Push.getPushKey()`.  
The second method is invoked when there was a registration error, notice that it isn’t always invoked and sometimes it isn’t called for a device where push is unavailable. Another important thing to highlight is that you shouldn’t show an error message directly here but rather log it to show later. This method might be invoked too early in the application loading process and that might be a problem

That covered the process of receiving a push message, let’s move on to sending. From a mobile device we can use the Push class to send a message. This isn’t as common so I won’t discuss it here although this is very useful for debugging

In the server we can use a POST call to our servers to send a push. I suggest reading the specification in the developer guide as there is too much nuance to highlight in a short video

This is a simple push request sample in standard JavaSE or JavaEE that sends a push message to the server. It doesn’t parse the response which is a crucial thing to do as the JSON response data can include a lot of important information.  
If you look at the code you will see it’s a standard http post request that passes the information needed for google’s and apple’s push services.

You might have noticed the type value for push mentioned before. I’ll only talk about the 3 most important push types but there are quite a few other types and I recommend checking the developer guide to go thru the list

The first and most common push type is push type 0 or 1. This is the push type you see visually where an app that isn’t running posts a notice like “we haven’t seen you in a while” or “you have a new message”. This message will be seen visually on the device and if the user taps it you will get a call to push(String). You will also get a call in push(String) when the app is running and a push is received, this is true for all of these push types so the difference in behavior is when an app isn’t running.

Invisible push won’t work when the app isn’t running on iOS but will work on Android. This is a conceptual issue. For iOS push is a visual concept whereas for Android it’s a communication protocol. In that sense we recommend people don’t use push as a networking protocol, it isn’t very good as a networking protocol and is useful mostly for signaling.

Type 3 is here to workaround the issues in type 2 and type 1. With type 1 we might not have the data we need to understand the content of the push. Type 3 includes two strings separated by a semicolon one for the visual message and one for the data payload. The push method will be invoked with both of these in sequence one after the other.

Push is painful as there are so many stumbling points. Here are a few tips to get you around them. First try sending push from the simulator. The simulator can’t receive push but it can send it with the Push API. You will be able to inspect the network traffic and see exactly what happened when sending a push request.

Check the JSON results from the server, they often contain crucial error messages that you can use to understand what went wrong

If you used the URL’s we sent with the certificate wizard that should work. But if you host the P12 files for push on your own then you need to make sure the file is directly accessible and not a link to a download site.

Production and sandbox modes on iOS are painful and it’s hard to test on the same device. This is true for push, in app purchase and other features. It takes Apple up to 24 hours to reassign a device from development to production and visa versa.

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
