---
title: When You Cannot Afford to Block the EDT
slug: invoke-without-blocking
url: /blog/invoke-without-blocking/
original_url: https://www.codenameone.com/blog/invoke-without-blocking.html
aliases:
- /blog/invoke-without-blocking.html
date: '2019-11-28'
author: Steve Hannah
---

![Header Image](/blog/invoke-without-blocking/new-features-3.jpg)

We recently added support for disabling `invokeAndBlock()` for certain sections of code. The syntax is:
    
    
    CN.invokeWithoutBlocking(()->{
          // This code is not allowed to call invokeAndBlock()
    });

If any attempt is made to execute `invokeAndBlock()` inside that block of code, it will throw a `BlockingDisallowedException`.

This can be useful for ensuring that your UI will be responsive, especially in cases where you’re building a new form to show to the user, and any delay will be noticed by the user.

For example:
    
    
    Button btn = new Button("Show Details");
    btn.addActionListener(evt->{
        DetailsForm form = new DetailsForm();
        form.show();
    });

The intention is that the user presses the “Show Details” button, and they are shown some sort of details form. But what if, somewhere in the construction of the `DetailsForm`, there is a call to `invokeAndBlock()`? A common reason for this is if a network call is performed during the building of the form. For example:
    
    
    DetailsModel model = loadDetailsFromServer();  // A synchronous call to the server
    nameField.setText(model.getName());
    ...

That `loadDetailsFromTheServer()` is a synchronous call to the server, so it uses `invokeAndBlock()` to be able to “block” control flow without actually blocking the EDT. It’s great that it doesn’t block the EDT, but it still blocks our ability to deliver the details form that the user is expecting. The form won’t be built until after the network request complete, then the user will experience a short delay before transitioning to the new form.

A better way is to build and show the `DetailsForm`, perform an asynchronous network request, and then update the details form with the data received when the network request completes. E.g.
    
    
    loadDetailsFromServerAsync().ready(model->{
        nameField.setText(model.getName());
        revalidateWithAnimationSafety();
    });

An easy fix in this simple case. But in the real world apps are much more complex. Calls to `invokeAndBlock()` may be nested deep within your app, or its libraries, so it may not be easy to ensure that the `DetailsForm` can be created without blocking.

This is where `invokeWithoutBlocking()` comes in handy. Let’s wrap our form creation to make sure that it doesn’t get bogged down by a slow network request.
    
    
    Button btn = new Button("Show Details");
    btn.addActionListener(evt->{
        CN.invokeWithoutBlocking(()->{
            DetailsForm form = new DetailsForm();
            form.show();
        });
    });

This is guaranteed to show the form instantly, or it will throw a `BlockingDisallowedException`. This may be useful, especially during development, to help you hunt down those pesky network requests to get them out of your way. You can also catch these exceptions in order to provide an alternate path in the case that you can’t build the form without blocking.
    
    
    Button btn = new Button("Show Details");
    btn.addActionListener(evt->{
        try {
            CN.invokeWithoutBlocking(()->{
                DetailsForm form = new DetailsForm();
                form.show();
            });
        } catch (BlockingDisallowedException ex) {
    
             showAlternateForm();
        }
    });

### Background: When to Block the EDT

Synchronous programming is easier to write and understand than asynchronous programming. You can follow the flow of the code line by line, knowing that line n is run after line n-1, and before line n+1. The problem with synchronous programming is that it causes problems for long-running tasks on threads that can never block. Threads like the event dispatch thread (EDT), which is where most application code runs since it is the only thread that is authorized to interact with the UI. Codename One’s `invokeAndBlock()` method is a nifty tool which allows you to “block” the current event dispatch, without blocking any other event dispatches. Other events and callSerially blocks will continue to be processed while this event is blocked.

This allows us to do cool things like:
    
    
    MyData data = loadSomeDataFromTheServer();
    updateUIWithMyData(data);

This can be run directly on the EDT without blocking it because events will continue to be delivered and processed while the single event dispatch that is running this code is waiting for the data to be retrieved from the server. In most cases, this is much more elegant than callbacks, promises, or workers. It is easier to write, and easier to understand.

However, there is a cost to using `invokeAndBlock()`. It still blocks the current dispatch, which can be problematic in certain contexts. E.g. What if we block the pointer-press dispatch. This might cause components to receive the corresponding pointer-release before the pointer-press – which is counter-intuitive.

I generally try to avoid using `invokeAndBlock()` directly inside event handlers like action events or pointer events, because you could be getting in the way of other event handlers that are relying on the order events. E.g., Instead of:
    
    
    button.addActionListener(evt->{
        doSomethingWithBlockingCode();
    });

Prefer:
    
    
    button.addActionListener(evt->{
        CN.callSerially(evt->{
            doSomethingWithBlockingCode();
        });
    });

This way it will only block the contents of that particular callSerially() dispatch. The action event dispatch (which is probably running inside a pointer pressed or pointer release dispatch) can continue to be delivered to other listeners unimpeded.

I also try to avoid blocking while constructing user interface components to show the user. Usually when I create a form, I want to create it **now** to show the user instantly.

You can also use callSerially() in this case to prevent the blocking.

E.g.
    
    
    class MyForm extends Form {
    
       public MyForm() {
            ...
            myLabel.setText("Some placeholder");
            callSerially(()->{
                MyData data = loadMyDataFromServer();
                myLabel.setText(data.getSomeString());
                revalidateWithAnimationSafefy();
            }
        }
    }

### Conclusion

`invokeAndBlock()` is a great tool for simplifying control flow, but in some circumstances, using it can negatively impact the user experience. Two places where `invokeAndBlock()` should be avoided is during the construction of UI forms to display to the user, and directly inside an event handler. In some cases you can mitigate the cost of `invokeAndBlock()` by simply wrapping it in a `callSerially()` (e.g. inside an event handler) so that it doesn’t block the current event. When constructing a UI component like a form, you can use `invokeWithoutBlocking()` to provide guarantees that your code doesn’t block, and be sure that the user will be shown your form without delay.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — December 4, 2019 at 8:52 pm ([permalink](https://www.codenameone.com/blog/invoke-without-blocking.html#comment-24275))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> can I used this code to avoid stopping my infinite rotating image animation while I paint others components in the screen?


### **Steve Hannah** — December 5, 2019 at 6:37 pm ([permalink](https://www.codenameone.com/blog/invoke-without-blocking.html#comment-24274))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> Are you referring to the InfiniteProgress.showInfiniteBlocking() method, or your own custom animation that you’ve made? In either case it probably wouldn’t be affected by this.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
