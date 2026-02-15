---
title: Tips for the Javascript port of Codename One apps
slug: tips-for-the-javascript-port-of-codename-one-apps
url: /blog/tips-for-the-javascript-port-of-codename-one-apps/
original_url: https://www.codenameone.com/blog/tips-for-the-javascript-port-of-codename-one-apps.html
aliases:
- /blog/tips-for-the-javascript-port-of-codename-one-apps.html
date: '2021-05-31'
author: Steve Hannah
description: This blog post contain tips for working with the Javascript port of Codename
  One apps.
---

This blog post contain tips for working with the Javascript port of Codename One apps.

### Sending Messages to Outside Webpage

## Problem

You want to send a message from your Codename One app to the webpage that contains it.

## Solution

You can use CN.postMessage(), in Codename One to send the message. The message will be dispatched to Javascript event listeners in the outside webpage that register to receive cn1outbox events.

## Sending a message from Codename One to the outside webpage:

```java
				
					// The Java side
MessageEvent message = new MessageEvent(
    null,        // event source... we'll leave it null
    "Hello",     // The message to deliver
    0            // Optional message code.
);
//Dispatch the message
CN.postMessage(message);
				
			
```

## Receiving messages from Codename One in outside webpage:

```javascript
				
					// The javascript side
window.addEventListener('cn1outbox', function(evt) {
    var message = evt.detail;
    var code = evt.code;
    ...
});
				
			
```

## Discussion

The CN.postMessage() method allows you to send a message to the native platform. When deploying as a Javascript app, these messages are converted to custom DOM events and dispatched on the window object. The event name is “cn1outbox”, so you can receive events like this from the “javascript” side by registering an event listener for these types of events on the window object.

### Receiving Messages from the Outside Webpage

## Problem

You want to send messages from Javascript (i.e. the page containing the app) to the Codename One app.

## Solution

From Javascript, you can dispatch a custom event named ‘cn1inbox’ on the window object. You can receive these events in Codename One using the CN.addMessageListener() method.

## Sending Message from Javascript that can be received inside Codename One app:

```javascript
				
					// The javascript side
var message = new CustomEvent('cn1inbox', {detail: 'Hello', code: 0});
window.dispatchEvent(message);
				
			
```

## Receiving Event in Codename One

```java
				
					// The Java side
CN.addMessageListener(evt->{
    String message = evt.getMessage();
    int code = evt.getCode();
    ...
});
				
			
```

## Discussion

The CN.addMessageListener() and CN.removeMessageListener() methods allow you to register listeners to receive messages from the native platform. When the app is deployed as a Javascript app, the webpage can target these listeners using a custom DOM event named ‘cn1inbox’. The Codename One app will receive all events of this type, and dispatch them to the listeners that were registered using CN.addMessageListener().

### Notify Webpage When App is Started

## Problem

You want to notify the outside page when the app is finished loading. If the webpage needs to communicate with the app, it is very helpful to know when the app is ready.

## Solution

Register a DOM event listener on the window object for the aftercn1start event.

```javascript
				
					window.addEventListener('aftercn1start', function(evt) {
   console.log("The Codename One app has started...");
   ...
});
				
			
```

## Discussion

Codename One broadcasts its lifecycle events as DOM events so that the webpage can stay synchronized with its status. The following events are currently supported:

## Table 4. Supported DOM events

| Event | Description |
| --- | --- |
| beforecn1init | Fired before the init() method is run. |
| aftercn1init | Fired after the init() method is run. |
| beforecn1start | Fired before the start() method is run. |
| aftercn1start | Fired after the start() method is run. |

## NOTE:

> Currently The stop() and destroy() lifecycle methods are not used in the Javascript port, as there doesn’t seem to be a logical place to fire them. This may change in the future.

In addition to these DOM events, you can also check window.cn1Initialized and window.cn1Started for true to see if the init() and start() methods have already run.

### Deploying as a "Headless" Javascript App

## Problem

You want to deploy your app inside a webpage “headlessly”. i.e. You don’t want the user to see the app. This might be useful if you just want to use your app as a javascript library.

## Solution

Embed the app inside a 1-pixel iframe.

```html
				
					

   ... 
  
    
    .. Rest of webpage..
  

				
			
```

## NOTE:

> By trial and error, we have determined that displaying the iframe with 1px width and height is the best solution. Using display:none causes the browser to not load the iframe at all. Positioning the iframe outside the viewport, causes some APIs to be turned off (e.g. microphone).

## Discussion

If you are deploying your app as a headless app, then you are likely expecting to be able to communicate between the webpage and your app. You will also need to be notified of lifecycle events in your app so you know when it has finished loading. Be aware of CORS (cross-origin-resource-sharing) browser policies if the page containing the <iframe> is loaded from a different domain than your app.

## CORS Checklist

If the app (inside the iframe) is hosted at a different domain than the parent page (the page with the <iframe> tag), then you need to jump through some hoops to get things working.
  
  
1. Make sure that you are not sending the [X-Frame-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options) response header with your app. This header prevents your page from being displayed inside an iframe. Many web hosts add this header automatically.
  
  
2. If you want to use features like “camera” and “microphone”, you’ll need to add the “allow” attribute to your iframe tag. e.g. <iframe allow=”camera;microphone” …​/>. For more information about this attribute, see [This article](https://sites.google.com/a/chromium.org/dev/Home/chromium-security/deprecating-permissions-in-cross-origin-iframes).
  
  
3. If you need to communicate between the parent window and the iframe document (i.e. the window with your app, you’ll need to use [Window.postMessage()](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage). You can access the iframe’s “window” object using myIframe.contentWindow.

### Playing Audio in a Headless App

## Problem

In some cases Codename One apps may be deployed as “headless” apps. This can be achieved by simply embedding the app inside an iframe and positioning the iframe outside the main view port (e.g. x=-1000, y=-1000). If you are deploying the app this way, you may run into cases where the app requires user interaction.
For example, if you try to play audio in the app, and you are running on iOS, then the app may require some user interaction in order for Safari to allow the audio. Codename One apps deal with this situation by prompting the user to play the audio. However, if the app is off screen, the user won’t see this prompt, so the audio will just not play.

## NOTE:

> The user will only be prompted for the ****first**** audio clip that the app tries to play. Subsequent clips can be played unimpeded.

## Solution

Codename One broadcasts a custom DOM event named “cn1userprompt” when a prompt is displayed that the user needs to interact with. You can register an event listener in the outside webpage to listen for this event, and display the iframe in such cases.

The “cn1userpromptresponse” custom DOM event will be dispatched after the user has finished the interaction.

```javascript
				
					myIframe.contentWindow.addEventListener('cn1userprompt', function(evt) {
    // The app requires user interaction..  display the iframe
});

myIframe.contentWindow.addEventListener('cn1userpromptresponse', function(evt) {
    // The user has finished their interaction... you can hide the iframe
});
				
			
```

### Displaying Custom Prompt to Play Audio

## Background

On some browsers (e.g. Safari), your app can only play audio as a direct response to user interaction. e.g. The user needs to actually click on the screen to initiate audio play. This is only required for the first audio clip that your app plays. If the app is ever denied permission to play an audio clip by the browser, it will display a prompt to the user saying “Audio Ready”, with a “Play Now” button. When the user presses that button, the audio will begin to play.

## Problem

You want to customize the dialog prompt that is displayed to ask the user for permission to play audio.

## Solution

Register a message listener using CN.addMessageListener(), and call isPromptForAudioPlayer() on the received MessageEvent object to see if it is a prompt to play audio.
  
  
If isPromptForAudioPlayer() returns true, then you can consume() the event to signal that you’ll be displaying a custom dialog, and then you can display your own dialog as shown in the example below.
  
  
When the user has accepted or rejected the permission prompt, you must call the complete() method on the promise that you obtain using the getPromptPromise() method.
  
  
complete(true) indicates that the user decided to play the audio. complete(false) indicates that the user decided not to play the audio.

## Example:

```java
				
					CN.addMessageListener(evt->{
    if (evt.isPromptForAudioPlayer()) { (1)
        System.out.println("Received a prompt for the audio player... audio is ready");
        // This is a prompt that is shown when there is audio ready to play
        // but the user needs to interact.  This is javascript-only to get around
        // restrictions that only allow audio in direct response to user interaction

        // We should display some kind of UI to let the user know that the audio is ready
        // and they need to press a button to play it.
        evt.consume(); (2)
        CN.callSerially(()-> { (3)
            MessageEvent.PromptPromise res = evt.getPromptPromise(); (4)
            if (Dialog.show("Audio Ready", "The audio is ready.", "Play", "Cancel")) {
                res.complete(true); (5)
            } else {
                res.complete(false); (6)
            }
            return;
        });
        return;

    }
});
				
			
```

1. isPromptForAudioPlayer() tells us that this event is a prompt to play audio.
  
  
2. Important: You must call evt.consume() to let Codename One know that you are going to handle this prompt. Otherwise, the default permission prompt will still be shown.
  
  
3. Because we are using a modal dialog which will block the event dispatch, we wrap the dialog in callSerially() so this event dispatch won’t be blocked. This is not absolutely necessary, but it will make it easier to follow the app’s logic, as these prompts are designed to by asynchronous.
  
  
4. Obtain the PromptPromise from the event which we will use to convey the user’s response back to the app. YOU MUST call the complete() on this promise no matter what, or the app will lock up.
  
  
5. If the user elected to “Play” the audio, then call res.complete(true) on the promise.
  
  
6. If the user elected not to play the audio, then call res.complete(false) on the promise.

## TIP:

> You can also use the isPromptForAudioRecorder() method to detect a request for the audio recorder prompt.

## Discussion

In this example we used a modal dialog to prompt the user, but you can use any UI mechanism you like for prompting the user. A Sheet, an interaction dialog, or a separate Form. You just need to remember to call complete() on the promise after the user has made their choice. If you forget to call complete() it could lock up the app.

## Important

> Calling complete(true) directly without actually displaying a dialog to the user won’t work. It is the "click" that satisfies the browsers "media engagement index" restrictions so that it will allow the app to play audio. The user can click anywhere in the app; but they need to click. If you call complete(true) without the user clicking, then the app will try to play the audio and just fail.

## Further Reading:

[Chrome Autoplay Policy (2017-09)](https://developers.google.com/web/updates/2017/09/autoplay-policy-changes)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Francesco Galgani** — June 6, 2021 at 1:03 pm ([permalink](/blog/tips-for-the-javascript-port-of-codename-one-apps/#comment-24465))

> Francesco Galgani says:
>
> This is just a theoretical curiosity of mine: in which real use cases could a Codename One project like «”Headless” Javascript App» be useful? Could you give some concrete examples?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
