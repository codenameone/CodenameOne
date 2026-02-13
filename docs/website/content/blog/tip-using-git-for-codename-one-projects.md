---
title: 'TIP: Using Git for Codename One Projects'
slug: tip-using-git-for-codename-one-projects
url: /blog/tip-using-git-for-codename-one-projects/
original_url: https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html
aliases:
- /blog/tip-using-git-for-codename-one-projects.html
date: '2017-07-02'
author: Shai Almog
---

![Header Image](/blog/tip-using-git-for-codename-one-projects/tip.jpg)

I wrote in the past about [importing a Codename One demo from github](/blog/tip-setup-codename-one-demo-from-git.html) but I left the whole working with git as an open subject. Mostly because I considered it a bit out of scope for the discussion. I have a section in the free online course about the [anatomy of a Codename One application](https://codenameone.teachable.com/courses/java-for-mobile-devices-introducing-codename-one/lectures/2925056) which I completely forgot to upload until I wrote this!

If you want to understand more, I strongly suggest watching that as it explains a lot of the “why” and “how” logic of Codename One project structures. It explains the purpose of every file you see in the project…​

### The Right Gitignore

We spent yesterday migrating our internal SVN repository (yes we still used SVN internally) to git. The reason we didn’t do it before was historic. We have a 15gb repository for Codename One that includes a lot of history and junk. Git doesn’t work well with large repositories and splitting up the whole thing effectively means discarding history.

We made some attempts at migrating in the past but the scripts would generally fail after 2-3 days of continuous work…​ So we split up the repositories to smaller ones and made a clean break. While doing that we wrote a lot of gitignore files and this made me think a bit about the whole thing. I think there is a lot of nuance in setting up the right gitignore file and there are a few interesting tradeoffs we should consider.

When we first started committing to git we used something like this for netbeans projects:
    
    
    *.jar
    nbproject/private/
    build/
    dist/
    lib/CodenameOne_SRC.zip

Removing the jars, build, private folder etc. makes a lot of sense but there are a few nuances that are missing here…​

#### cn1lib’s

You will notice we excluded the jars which are stored under lib and we exclude the Codename One source zip. But I didn’t exclude cn1libs…​ That was an omission since the original project we committed didn’t have cn1libs. But should we commit a binary file to git?

I don’t know. Generally git isn’t very good with binaries but cn1libs make sense. In another project that did have a cn1lib I did this:
    
    
    *.jar
    nbproject/private/
    build/
    dist/
    lib/CodenameOne_SRC.zip
    lib/impl/
    native/internal_tmp/

The important lines are `lib/impl/` and `native/internal_tmp/`. Technically cn1libs are just zips. When you do a refresh libs they unzip into the right directories under `lib/impl` and `native/internal_tmp`. By excluding these directories we can remove duplicates that can result in conflicts.

I’m not sure if committing the cn1libs is a good choice. It’s something i’m still conflicted about.

#### Resource Files

I’m generally in favor of committing the res file and it’s committed in the git ignore files above. The res file is at risk of corruption and in that case having a history we can refer to matters a lot.

But the resource file is a bit of a problematic file. As a binary file if we have a team working with it the conflicts can be a major blocker. This was far worse with the old GUI builder, that was one of the big motivations of moving into the new GUI builder which works better for teams.

Still, if you want to keep an eye of every change in the resource file you can switch on the File → XML Team Mode which should be on by default. This mode creates a file hierarchy under the `res` directory to match the res file you opened. E.g. if you have a file named `src/theme.res` it will create a matching `res/theme.xml` and also nest all the images and resources you use in the res directory.

That’s very useful as you can edit the files directly and keep track of every file in git. However, this has two big drawbacks:

  * It’s flaky – while this mode works it never reached the stability of the regular res file mode

  * It conflicts – the simulator/device are oblivious to this mode. So if you fetch an update you also need to update the res file and you might still have conflicts related to that file

Ultimately both of these issues shouldn’t be a deal breaker. Even though this mode is a bit flaky it’s better than the alternative as you can literally “see” the content of the resource file. You can easily revert and reapply your changes to the res file when merging from git, it’s tedious but again not a deal breaker.

#### Eclipse Version

Building on the gitignore we have for NetBeans the eclipse version should look like this:
    
    
    .DS_Store
    *.jar
    build/
    dist/
    lib/impl/
    native/internal_tmp/
    .metadata
    bin/
    tmp/
    *.tmp
    *.bak
    *.swp
    *.zip
    *~.nib
    local.properties
    .settings/
    .loadpath
    .recommenders
    .externalToolBuilders/
    *.launch
    *.pydevproject
    .cproject
    .factorypath
    .buildpath
    .project
    .classpath

#### IntelliJ/IDEA
    
    
    .DS_Store
    *.jar
    build/
    dist/
    lib/impl/
    native/internal_tmp/
    *.zip
    .idea/**/workspace.xml
    .idea/**/tasks.xml
    .idea/dictionaries
    .idea/**/dataSources/
    .idea/**/dataSources.ids
    .idea/**/dataSources.xml
    .idea/**/dataSources.local.xml
    .idea/**/sqlDataSources.xml
    .idea/**/dynamic.xml
    .idea/**/uiDesigner.xml
    .idea/**/gradle.xml
    .idea/**/libraries
    *.iws
    /out/
    atlassian-ide-plugin.xml

### Finally

This is by no means the final version of this. These sort of files tend to change over time as more functionality (e.g. Kotlin support) makes its way into Codename One or the respective IDE’s. This can also be impacted by your OS or IDE plugin. Notice I excluded `.DS_Store` which is a file Mac OS sometimes creates but I didn’t exclude `Thumbs.db` because I don’t use Windows as much.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Martin Grajcar** — November 4, 2018 at 3:11 pm ([permalink](https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html#comment-24007))

> Martin Grajcar says:
>
> My project build just failed in `<copy todir=”bin”><fileset dir=”override”/></copy>` because of the missing `override` directory. This was my fault as I deleted all empty directories, but git doesn’t track them, so build of a cloned project will fail for the very same reason.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-using-git-for-codename-one-projects.html)


### **Shai Almog** — November 5, 2018 at 5:26 am ([permalink](https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html#comment-23901))

> Shai Almog says:
>
> Is it possible your build.xml is out of date?  
> build.xml should implicitly create that directory if it’s missing when you send a build and when you run if you use netbeans.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-using-git-for-codename-one-projects.html)


### **Martin Grajcar** — November 5, 2018 at 3:19 pm ([permalink](https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html#comment-23972))

> Martin Grajcar says:
>
> It may be out of date, but at most by a week as on Oct 29, I created a new project. I edited it manually, because of [https://www.codenameone.com…](<https://www.codenameone.com/blog/tip-using-lombok-other-tools.html>).
>
> There are many `<mkdir dir=”override”/>` there, but there’s none before the copy in the jar target. So I added it now.
>
> I’m using Eclipse. I wonder, how to get updates to build.xml, in case you do some important changes in the future (overwriting my edited file is no problem because of git).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-using-git-for-codename-one-projects.html)


### **Shai Almog** — November 6, 2018 at 6:55 am ([permalink](https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html#comment-23883))

> Shai Almog says:
>
> build.xml is embedded in Codename One Settings so when you save changes there it offers to override your current version. Please file an issue on this and we’ll try to add that mkdir before that as well.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-using-git-for-codename-one-projects.html)


### **ThomasH99** — September 19, 2021 at 2:30 pm ([permalink](https://www.codenameone.com/blog/tip-using-git-for-codename-one-projects.html#comment-24482))

> ThomasH99 says:
>
> Hi, is this approach is still the right way to use Git for CN1 projects using maven? (I’m no expert on either so maybe this is a basic question 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-using-git-for-codename-one-projects.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
