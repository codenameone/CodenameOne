---
title: Default Validation Emblem
slug: default-validation-emblem
url: /blog/default-validation-emblem/
original_url: https://www.codenameone.com/blog/default-validation-emblem.html
aliases:
- /blog/default-validation-emblem.html
date: '2016-12-05'
author: Shai Almog
---

![Header Image](/blog/default-validation-emblem/validation-emblem.png)

The validation framework makes it easy to verify input quickly and effectively. Up until now you had to define  
an emblem in order to create an error icon and if you didn’t you had to define an “Invalid” UIID for every entry.  
This exists by default for text fields and other types but is still a big hassle just to check that we have a valid  
email…​

The main reason for this is that when we introduced the validation framework we hadn’t yet integrated the  
material icons into Codename One, this was remedied and starting with the next update we’ll have a default  
emblem. Notice that if you replace it manually your emblem will still be used…​

However, we also changed the default behavior as a result. In the past we defaulted to `HighlightMode.UIID`  
which makes a lot of sense when you don’t have an emblem. This default no longer makes sense and so  
we now have `HighlightMode.EMBLEM` as the default.

So if your code relies on the default behavior of the validator this will no longer behave in the same way. The  
workaround is actually really simple, just add the call:
    
    
    myValidator.setValidationFailureHighlightMode(Validator.HighlightMode.UIID);

To force the same behavior as before.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
