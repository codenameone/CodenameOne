---
title: 'TIP: Handle EDT Errors'
slug: tip-handle-edt-errors
url: /blog/tip-handle-edt-errors/
original_url: https://www.codenameone.com/blog/tip-handle-edt-errors.html
aliases:
- /blog/tip-handle-edt-errors.html
date: '2017-05-21'
author: Shai Almog
---

![Header Image](/blog/tip-handle-edt-errors/tip.jpg)

Tracking & logging errors is crucial for a stable application. There are several tools we offer in Codename One both in the seamless level (crash protection) and in the lower level inner workings of Codename One. I’ll try to explain both and how they interact.

When you create a new Codename One application you might notice this line of code:
    
    
    Log.bindCrashProtection(true);

This code binds the pro crash protection feature that will implicitly send you an email whenever the app crashes or has an exception. This email will include the stack trace and other output you wrote to the console using `Log.p()`.

The argument to that method indicates whether the error should be swallowed or kept. When set to true the error is consumed and the user will be unaware that an exception occurred, this can be problematic in some cases as failure might be hard to track.

### How does it Work?

The code for `bindCrashProtection` looks like this:
    
    
    public static void bindCrashProtection(final boolean consumeError) {
        if(Display.getInstance().isSimulator()) {
            return;
        }
        Display.getInstance().addEdtErrorHandler(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(consumeError) {
                    evt.consume();
                }
                p("Exception in " + Display.getInstance().getProperty("AppName", "app") + " version " + Display.getInstance().getProperty("AppVersion", "Unknown"));
                p("OS " + Display.getInstance().getPlatformName());
                p("Error " + evt.getSource());
                if(Display.getInstance().getCurrent() != null) {
                    p("Current Form " + Display.getInstance().getCurrent().getName());
                } else {
                    p("Before the first form!");
                }
                e((Throwable)evt.getSource());
                sendLog();
            }
        });
        crashBound = true;
    }

The first interesting thing you will notice above is: `addEdtErrorHandler` which is a listener on the `Display` class. It allows us to to receive an event when the EDT catches an exception. All exceptions on the EDT are caught, we try to recover from them.

The default behavior is to show an error dialog but if we consume the event that error dialog won’t show.

The second interesting piece of code is `sendLog();`, this method sends the log to the email of the user who built the app. This method will only work for a pro account. You can invoke that method manually and create custom versions of this error handling logic.

Notice that we also add implicit calls to send log if you get an exception in other threads if crash protection is bound, that means that some other unexpected crashes might be detected by such code.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
