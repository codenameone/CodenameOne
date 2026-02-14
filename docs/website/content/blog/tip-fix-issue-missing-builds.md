---
title: 'TIP: Fix Issue with Missing Builds'
slug: tip-fix-issue-missing-builds
url: /blog/tip-fix-issue-missing-builds/
original_url: https://www.codenameone.com/blog/tip-fix-issue-missing-builds.html
aliases:
- /blog/tip-fix-issue-missing-builds.html
date: '2018-10-01'
author: Shai Almog
---

![Header Image](/blog/tip-fix-issue-missing-builds/tip.jpg)

A while back we announced the migration to the [new build cloud](https://www.codenameone.com/blog/new-build-cloud.html). The migration worked very smoothly and mostly seamlessly but there was one caveat: client libraries must be up to date. This is a confusing point so hopefully this long overdue post will clarify it.

The core of the problem is `CodeNameOneBuildClient.jar`. It’s a relatively simple jar with a few ant tasks that performs a lot of “under the hood” services such as sending the build to the cloud. It’s shipped within the IDE plugin and old versions of the IDE plugins would replace it automatically. We now update it via the [update framework](https://www.codenameone.com/blog/new-update-framework.html) which is better but might cause a few issues.

Generally the issues can be expressed either via a build that doesn’t appear. You might get an error that a build is already in the queue and once we remove app engine entirely you’ll get a connection error.

To fix this you need to do the following:

  * Update your plugin to the latest version, make sure that other team members don’t use an old plugin either

  * Run Update Project Libs which you can do by right clicking the project and selecting: Codename One → Codename One Settings → Basic → Update Project Libs

__ |  You need to Update Project Libs for every project if you have more than one   
---|---  
  
Notice that new projects should be fine.

### If this Didn’t Work

The problem is that these two steps might fail. Here are things you need to look at:

  * Make sure the `Versions.properties` and the jars in the projects aren’t under source control. They should be excluded from it as we update them dynamically

  * In your home directory there is a directory named `.codenameone` make sure it doesn’t contain an `UpdateStatus.lock` file. If it does you can delete it assuming it’s been there for a while

When in doubt you can delete `Versions.properties` and Update Project Libs again. This should work but if that doesn’t do it you can go with the “nuclear option” and delete the `.codenameone` directory and `Versions.properties`. After that do an Update Project Libs.

If this still doesn’t work let us know via the chat. Ideally try to run the [update framework](https://www.codenameone.com/blog/new-update-framework.html) from command line to figure out what went wrong.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Andrew** — May 7, 2020 at 5:08 am ([permalink](https://www.codenameone.com/blog/tip-fix-issue-missing-builds.html#comment-21401))

> Andrew says:
>
> Hi there,  
> I hope this finds you well. I am hoping for some pointers regarding the above “Tips”. My home directory has “UpdateStatus.properties” and “UpdateCodenameOne.jar” are these ok as is? Also, How would I go about making sure the Versions.properties aren’t under source control? Finally… how would I go about making sure the Versions.properties aren’t under source control? I am using OSX with Netbeans  
> Thank you for your time, Codename One is a great tool!  
> Be Safe!


### **Shai Almog** — May 8, 2020 at 4:53 am ([permalink](https://www.codenameone.com/blog/tip-fix-issue-missing-builds.html#comment-21403))

> Shai Almog says:
>
> If the version file contains CodenameOneJar=124 or newer then you’re good to go  
> Are you using source control such as git?  
> If so they should be under .gitignore  
> If you aren’t using source control then they aren’t under source control

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
