---
title: Better Logging
slug: better-logging
url: /blog/better-logging/
original_url: https://www.codenameone.com/blog/better-logging.html
aliases:
- /blog/better-logging.html
date: '2017-03-06'
author: Shai Almog
---

![Header Image](/blog/better-logging/new-features-1.jpg)

I didn’t post much about new features in a while because we’ve been so busy with the bootcamp that we didn’t have as much time to write the posts or the actual functionality. But a few features/changes did slide in over the past couple of weeks as is pretty much inevitable.

There are a lot of small changes so I’ll divide them to avoid confusion.

### JSObject toString

Up until now if you got a callback or had obtained a [JSObject](/javadoc/com/codename1/javascript/JSObject/) and wanted to know what it contained e.g. via:
    
    
    Log.p("My JSObject is: " + myJSObject);

You would get the default `toString()` behavior which would give you no helpful information other than a unique object id. This is silly since JavaScript has a `toString()` method and what you probably want is to call that. So now the `toString()` method implicitly calls the JavaScript `toString()` method thus providing you with useful information for debugging.

### Crash Logs

`Log.e(exception)` prints the stack trace exception into the logs which you can view from the device using crash protection. This isn’t new functionality. However, when we launched Codename One we were very concerned with app size and tried to remove inter-dependencies so we didn’t use the `Log` class in many places.

As a result our code was littered with `printStackTrace()` calls which might make some on-device failures much harder to investigate especially for iOS. We replaced almost all of those legacy usages with proper `Log.e()` calls so it’s possible that some of the crash reports you will get in the future will include new information.

### Universal Windows Platform & Desktop Done Listener

I wasn’t even aware this didn’t work in UWP but it seems that the done listener wasn’t implemented there.

For those of you who don’t know, text field supports a special listener mode that allows you to bind a done listener such as:
    
    
    tf.setDoneListener(e -> done());

This happens when the user presses the done button in the virtual keyboard to perform the action. So you can use that to trigger an action immediately without requiring the user to press another button.

The other day Steve also added this to the JavaSE port which means you should be able to debug the done listener in the simulator too.

### Include Nulls

I mentioned this [pull request](https://github.com/codenameone/CodenameOne/pull/2051) from [Terry Wilkinson](https://github.com/twilkinson) in the Friday blog post but I didn’t go into details.

This pull request adds a mode to the `JSONParser` where null attributes will still be included in the `keySet` of the parse `Map`. So you should be able to do something like this:
    
    
    JSONParser p = new JSONParser();
    p.setIncludeNulls(true);
    Map<String,Object> m = p.parseJSON(reader);
    for(String key : m.keySet()) {
         Object value = m.get(key);
         // here value might be null where in the past a null value was just omitted
    }

Now why would you need that?

Lets say you display the set of attributes for a user to edit:
    
    
    Given Name: Shai
    Surname: Almog
    Age: null

I didn’t specify the age but in the old parser age would be removed so I wouldn’t know how to add it. This works OK if you know the keys in advance but obviously this isn’t as flexible.

### Localizable SignatureComponent:

Last but not least Steve added the ability to localize the signature component by setting some values in the resource bundle. You can learn more about resource bundles and localization [here](/how-do-i---localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app/).

You can use the following keys when localizing:

Table 1. Resource Bundle keys Key |  Default Value  
---|---  
`SignatureComponent.LeadText` |  Press to sign  
`SignatureComponent.DialogTitle` |  Sign Here  
`SignatureComponent.LeadText` |  Press to sign  
`SignatureComponent.SaveButtonLabel` |  Save  
`SignatureComponent.ResetButtonLabel |  Reset  
`SignatureComponent.CancelButtonLabel |  Cancel  
`SignatureComponent.ErrorDialog.SignatureRequired.Title` |  Signature Required  
`SignatureComponent.ErrorDialog.SignatureRequired.Body` |  Please draw your signature in the space provided.  
`SignatureComponent.ErrorDialog.OK` |  OK

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
