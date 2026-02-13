---
title: Property Of The Client
slug: property-of-the-client
url: /blog/property-of-the-client/
original_url: https://www.codenameone.com/blog/property-of-the-client.html
aliases:
- /blog/property-of-the-client.html
date: '2013-10-13'
author: Shai Almog
---

![Header Image](/blog/property-of-the-client/property-of-the-client-1.png)

  
  
  
  
![Notifications](/blog/property-of-the-client/property-of-the-client-1.png)  
  
  
  

One of the really useful features in Codename One that many developers are just unaware of is the client properties trick. Every component in Codename One has a Map (Hashtable) associated with it containing arbitrary objects you can store in association with that component. 

  
  
To use this feature you can just invoke putClientProperty(propertyName, value); then getClientProperty(propertyName) to extract the property.

  
When faced with this feature developers are often a bit stumped, why would I need this?  
  
  
  
Why not just store a variable?  

  
This is an immensely useful  
  
feature that allows you to decouple your UI code from the business logic and store state information directly in the component. This is especially useful in a GUI builder application where you might not want to litter the state machine with variables. We use these methods extensively in Codename One itself e.g. the GridBagLayout stores constraint information right into the component state using the putClientProperty method. Tree indicates whether a node is expanded or folded right in the component itself, it also allows the tree to have a reverse lookup reference from the Component to the state node (so it “knows” the object that formed a specific component). 

On a different subject altogether Fabrício Carvalho Cabeça of  
[  
Pumpop  
](http://www.codenameone.com/featured-pumpop.html)  
contributed a patch that allows ImageViewer to be non-cyclic. Hopefully people will find this useful. I’m not sure if it made it to the update we released last night though.

  
We also added some basic support for showing the native OS hints when entering edit mode in iOS and Android. This won’t necessarily give you the best UX since the native hints might differ from ours which is why this feature is off by default. You can enable this by defining  
  
nativeHintBool=true in your theme constants.

  
I’ve also made some changes to the notification API in Android allowing you to test whether notifications are supported (they only work for Android right now) and also allowing the passage of additional arguments, dismissal etc. You now have an isNotificationSupported() method in Display which will be true only on Android (at least for now). You should also use the new version of  
  
[  
notifyStatusBar  
](/javadoc/com/codename1/ui/Display.html#notifyStatusBar\(java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean,%20boolean,%20java.util.Hashtable\))  
which differs both in its return value (which can be used to  
[  
dismiss the notification  
](/javadoc/com/codename1/ui/Display.html#dismissNotification\(java.lang.Object\))  
) and also allows you to pass more arguments.  
  
  
Currently supported arguments include:  
  
  
persist=Boolean.TRUE – this will cause the notification to persist and won’t allow users to close it. You should then explicitly call dismiss to remove it.  
  
  
id=Integer(…) – a number indicating the unique id for a notification, if you want to show multiple notifications from the same app you will need to give them each a unique id.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
