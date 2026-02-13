---
title: 'TIP: Setup a Codename One Project from Git'
slug: tip-setup-codename-one-demo-from-git
url: /blog/tip-setup-codename-one-demo-from-git/
original_url: https://www.codenameone.com/blog/tip-setup-codename-one-demo-from-git.html
aliases:
- /blog/tip-setup-codename-one-demo-from-git.html
date: '2017-01-01'
author: Shai Almog
---

![Header Image](/blog/tip-setup-codename-one-demo-from-git/just-the-tip.jpg)

Opening a demo or sample code from GIT is relatively easy if you are an experienced Codename One developer but for a lot of newer developers for whom samples are often more crucial this can be challenging. One of our solutions was placing the demos in the Codename One new project menu but that’s probably not enough.

First before we begin with the step by step guide let’s explain what is happening…​

### Why Doesn’t a Project “Just Work”?

There are two major reasons:

  * We don’t include the required JAR files/cn1lib dependencies and some of the required empty folders

  * Projects are usually built for NetBeans so if you use a different IDE they won’t work

Both of these are easy to fix, but with this post I’d like to show you a trivial trick to do this that will work in NetBeans/Eclipse & IntelliJ/IDEA. The trick is to create a new project and copy the sources on top of it for everything to work…​

I’ll explain this thru an example by opening and running [AlphabetScroll](https://github.com/codenameone/AlphabetScroll) in the 3 IDE’s using these steps.

### Get the Main Class & Package Name

Open the [codenameone_setting.properties](https://github.com/codenameone/AlphabetScroll/blob/master/codenameone_settings.properties) for the project. Within this file find two values:

  * `codename1.mainName` which in this case is `Alphabet`

  * `codename1.packageName` which in this case is `com.codename1.demos.alphabetscroll`

### Create a new Project in the IDE

You can leave everything in the default settings, the only two exceptions are the package name and main class name that must match the main class & package you found in the `codenameone_settings.properties`.

![Create a new project for Alphabet in NetBeans](/blog/tip-setup-codename-one-demo-from-git/import-from-git-netbeans.png)

Figure 1. Create a new project for Alphabet in NetBeans

![Create a new project for Alphabet in Eclipse](/blog/tip-setup-codename-one-demo-from-git/import-from-git-eclipse.png)

Figure 2. Create a new project for Alphabet in Eclipse

![Create a new project for Alphabet in IntelliJ/IDEA](/blog/tip-setup-codename-one-demo-from-git/import-from-git-idea.png)

Figure 3. Create a new project for Alphabet in IntelliJ/IDEA

### Copy the Files

Open the project folder outside of the IDE, replace the `src` directory with the one from GIT and replace the `codenameone_settings.properties` you have with the one from GIT.

If the git project has a `native` directory copy it on top of yours.

If the lib directory within the git project has any cn1lib files copy them into your lib directory & use Refresh Libs in the right click menu under the Codename One menu.

### Run

You can now run a project from GIT regardless of the IDE you are using, notice that if your plugin is old you might need one additional step. Right click the project and select Codename One → Codname One Settings.

Under the Basics menu click the Update Project Libs button (bottom left). This will update the jars to the latest allowing the project to run.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **ThomasH99** — September 19, 2021 at 2:44 pm ([permalink](https://www.codenameone.com/blog/tip-setup-codename-one-demo-from-git.html#comment-24483))

> I just posted this comment under the post “TIP: Using Git for Codename One Projects” but I think I should have posted it here instead:  
> ———————  
> Hi, is this approach is still the right way to use Git for CN1 projects using maven? (I’m no expert on either so maybe this is a basic question 🙂)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-setup-codename-one-demo-from-git.html)


### **Shai Almog** — September 20, 2021 at 1:47 am ([permalink](https://www.codenameone.com/blog/tip-setup-codename-one-demo-from-git.html#comment-24488))

> Hi,  
> no. I think maven is actually a bit simpler. You can just gitignor the target directory.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-setup-codename-one-demo-from-git.html)


### **ThomasH99** — September 19, 2021 at 2:44 pm ([permalink](https://www.codenameone.com/blog/tip-setup-codename-one-demo-from-git.html#comment-24484))

> I just posted this comment under the post “TIP: Using Git for Codename One Projects” but I think I should have posted it here instead:  
> ———————  
> Hi, is this approach is still the right way to use Git for CN1 projects using maven? (I’m no expert on either so maybe this is a basic question 🙂)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-setup-codename-one-demo-from-git.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
