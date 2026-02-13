---
title: New Peer & GUI Builder Tree
slug: new-peer-gui-builder-tree
url: /blog/new-peer-gui-builder-tree/
original_url: https://www.codenameone.com/blog/new-peer-gui-builder-tree.html
aliases:
- /blog/new-peer-gui-builder-tree.html
date: '2016-09-07'
author: Shai Almog
---

![Header Image](/blog/new-peer-gui-builder-tree/generic-java-1.jpg)

If you relied on the `android.newPeer=false` build hint it will no longer be available starting with this update. When  
you build for 3.5 you will still get the old behavior if you define that hint but otherwise it will be ignored. This is a  
precursor step to merging the newPeer branch into the main branch. It’s an important step to help us move forward  
with one code base!

### GUI Builder UI Change

With the new update of the GUI builder we will move the tree tab into it’s own space below the tabs so it will  
look like this:

![New look for the GUI builder sidebar](/blog/new-peer-gui-builder-tree/gui-builder-tree.png)

Figure 1. New look for the GUI builder sidebar

After experimenting with this for a while we came to the conclusion that this is **far** more convenient than the  
previous arrangement.

A missing piece is the ability to drag into/within the tree, this is indeed something we would like to do but because  
the tree is physically within a `SideMenuBar` the task is non-trivial.

You can expect to see this change in the next plugin update, we are thinking of making such an update next week  
but these updates are more fluid than the standard Friday releases.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
