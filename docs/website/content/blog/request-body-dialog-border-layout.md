---
title: Request Body, Dialog & Border Layout
slug: request-body-dialog-border-layout
url: /blog/request-body-dialog-border-layout/
original_url: https://www.codenameone.com/blog/request-body-dialog-border-layout.html
aliases:
- /blog/request-body-dialog-border-layout.html
date: '2016-11-30'
author: Shai Almog
---

![Header Image](/blog/request-body-dialog-border-layout/new-features-1.jpg)

Subclassing isn’t bad but it becomes tedious especially if it’s just there to implement something trivial. One of  
the pain points we had with the `ConnectionRequest` API’s is the submission body wasn’t as convenient as it  
should be.

E.g. if my web service accepts JSON in the post argument I have to write something like this:
    
    
    ConnectionRequest r = new ConnectionRequest(myURL, true) {
        protected void buildRequestBody(OutputStream os) throws IOException {
            os.write(myJSON.getBytes("UTF-8"));
        }
    };

This works great but does it really require subclassing?

So we added this:
    
    
    ConnectionRequest r = new ConnectionRequest(myURL, true); {
    r.setRequestBody(myJSON);

Notice that you must use post and if you have an argument (or try to add one later) it will fail as you can’t have both…​

### Dialog Layouts

A while back we fixed `Form` to have a constructor with a layout manager which reduces one line of code but  
is also a slight optimization as it saves an allocation of the default `FlowLayout` that would be there in the  
first place.

We now added two constructors to `Dialog` as well that allow you to create a `Dialog` with a layout manager and  
a layout manager + title.

### Border Layout Improvements

I don’t like the syntax of border layout, the constraint argument makes it overly tedious to work with for  
most cases but it is one of the best layouts as it’s remarkably consistent and predictable.

To make this slightly easier we added two new capabilities, first we added two new enclose methods:
    
    
    public static Container centerEastWest(Component center, Component east, Component west)
    public static Container centerAbsoluteEastWest(Component center, Component east, Component west)

These allow us to shorten the common use case of wrapping a component in a border layout, e.g. up until now  
if I wanted to add two components to a border layout one in the center and one in the west I had to do this:
    
    
    Container c = BorderLayout.center(cmp1).
                add(BorderLayout.WEST, cmp2);

Now I can do this which is a bit better:
    
    
    Container c = BorderLayout.centerEastWest(cmp1, null, cmp2);

Another common problem we had in the past is this, see if you can spot the error here:
    
    
    public class MyClass extends Form {
        public MyClass() {
            super(new BorderLayout());
            add(CENTER, new Label("Hello World"));
        }
    }

Those of you who spotted this might think this won’t compile but it will…​ `BorderLayout` expects one of it’s constants  
the string `BorderLayout.CENTER` but in this case we used `Component.CENTER` which is an `int` we  
derive thru `Form`.

While this is a “mistake” in theory it occurred to me that there is no “real” mistake here. The programmers  
intention is clear, why force the usage of a specific constant?

So we added code into `BorderLayout` that will detect this case and do what you expect for the other constants  
too (`BOTTOM`, `LEFT`, `TOP`, `RIGHT`). I would still recommend using `BorderLayout` as it is slightly more efficient  
(no autoboxing allocation)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
