---
title: Clearable Text Field
slug: clearable-text-field
url: /blog/clearable-text-field/
original_url: https://www.codenameone.com/blog/clearable-text-field.html
aliases:
- /blog/clearable-text-field.html
date: '2017-06-05'
author: Shai Almog
---

![Header Image](/blog/clearable-text-field/new-features-6.jpg)

A common request over the past couple of years has been to add a text field that supports a clear button in the end, we used to have a common answer on how this can be implemented but we didn’t have an actual implementation builtin despite this being a relatively common request.

At first I thought this is something we should implement natively but it turns out that this doesn’t exist natively in Android so we just implemented this as a wrapper to the TextField e.g. replace this:
    
    
    cnt.add(myTextField);

With this:
    
    
    cnt.add(ClearableTextField.wrap(myTextField));

You can also specify the size of the clear icon if you wish. This is technically just a `Container` with the text field style and a button to clear the text at the edge.

### Global Context Update

After posting about [the new CN class](/blog/static-global-context.html) last week we added a lot of new features into it. The API now supports capabilities from `FileSystemStorage` & `Storage` both of which should allow easier storage.

We are still a bit conflicted about some of the more elaborate API’s such as database, contacts etc.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
