---
title: Accordion Control & Xcode Migration Update
slug: accordion-control-xcode-migration-update
url: /blog/accordion-control-xcode-migration-update/
original_url: https://www.codenameone.com/blog/accordion-control-xcode-migration-update.html
aliases:
- /blog/accordion-control-xcode-migration-update.html
date: '2016-08-31'
author: Shai Almog
---

![Header Image](/blog/accordion-control-xcode-migration-update/accordion-post.png)

In the coming update we have a new API to expand/collapse an `Accordion` component programmatically similar to the `Tree` component.

To achieve this we introduced three new API’s to the `Accordion` class:
    
    
    /**
     * Returns the body component of the currently expanded accordion element or null if none is expanded
     * @return a component
     */
    public Component getCurrentlyExpanded();
    
    /**
     * Expands the accordion with the given "body"
     * @param body the body component of the accordion to expand
     */
    public void expand(Component body);
    
    /**
     * Closes the accordion with the given "body"
     * @param body the body component of the accordion to close
     */
    public void collapse(Component body);

All of these methods get/return the body of the accordion which is probably the best way to identify an accordion node.

### Xcode Migration

The migration to the new xcode servers is going well so far. We did experience some minor issues the biggest was the need to use `https` has kicked in as a result of the newer tooling.

Pretty much everything seems to be in order and we migrated other servers to pick up the load so builds should be back to their typical build times. Before that builds might have been slower but once the migration is complete we’ll have more servers than we had when we started off making builds even faster.

Right now we still have the `iphone_old` build target running, we plan to remove it by 3.6 (due December) so if you depend on it we suggest you keep us posted!

Notice that once it’s removed we won’t be able to restore it as it depends on a legacy version of Mac OS X no longer sold.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
