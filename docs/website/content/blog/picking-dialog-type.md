---
title: Picking a Dialog Type
slug: picking-dialog-type
url: /blog/picking-dialog-type/
original_url: https://www.codenameone.com/blog/picking-dialog-type.html
aliases:
- /blog/picking-dialog-type.html
date: '2020-01-09'
author: Shai Almog
---

![Header Image](/blog/picking-dialog-type/picking-dialog-type.jpg)

The duality of `InteractionDialog` and `Dialog` is often confusing to the Codename One newcomer (and to some degree to veteran developers too). This is in part due to the multiple behavior differences that extend far beyond the "official" functionality difference. This has its roots in history that predated Codename One.  
In this post I’ll try to clarify the process of picking the "right one" and the tradeoffs involved.

A dialog can be modal or non-modal but it isn’t interactive like an `InteractionDialog`. The modal dialog blocks the EDT internally using `InvokeAndBlock` so the current thread stops until there’s a response from the dialog. This is convenient but has some edge case issues. E.g. the event that launched the dialog might trigger other events that would happen after the dialog was dismissed and cause odd behavior.

But that’s not the big thing in modality. Modality effectively means the form behind you "doesn’t exist". Everything that matters is the content of the dialog and until that is finished we don’t care about the form behind. This core idea meant that a `Dialog` effectively derives `Form` and as such it behaves exactly like showing another `Form`. In other words a `Dialog` IS A `FORM`. This effectively disables the current `Form`. What you see behind the dialog is a drawing of the previous `Form`, not the actual `Form`.

Text fields can pose a problem in this case. Because the way the dialog is positioned (effectively padded into place within its form using margin) the UI can’t be scrolled as text field requires when the virtual keyboard rises. Since people use dialogs in such scenarios we try to workaround most of these problems but sometimes it’s very hard e.g. if the dialog has a lot of top margin, the virtual keyboard is open and covering it. Or if the user rotates the screen at which point the margin positioning the dialog becomes invalid.

__ |  In `InteractionDialog` Some of these issues such as the margin to position also apply so it’s also a bit problematic for text input   
---|---  
  
`InteractionDialog` is a completely different beast that sprung out of a completely different use case. What if we want a `Dialog` such as a "color palette" that floats on top of the ui?

We can move it from one place to another but still interact with the underlying form. That’s the core use case for InteractionDialog. As such modality is no longer something we need so it was never baked into InteractionDialog although it technically could have been (but it doesn’t make sense to the core use case).

It’s implemented as a `Container` placed into the layered pane of the current `Form` so the `Form` around it is "real". Because the `Form` is "live", layout works better. The removal of modality makes some edge cases related to editing slightly better. There are still some inherent problems with `Dialog` positioning and rotation though. It also allows you to click outside of the `Dialog` while input is ongoing which might be a desirable/undesirable effect for your use case.

Overall I try to use dialogs only for very simple cases and avoid input in any `Dialog` when possible. If I use input I never use more than one field (e.g. no-username and password fields) so I won’t need to scroll. These things work badly for native UIs as well e.g. with the virtual keyboard obscuring the submit button etc. Since those behaviors are very hard to get right for all resolution/virtual keyboard scenarios.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
