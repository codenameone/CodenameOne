---
title: New Update Framework
slug: new-update-framework
url: /blog/new-update-framework/
original_url: https://www.codenameone.com/blog/new-update-framework.html
aliases:
- /blog/new-update-framework.html
date: '2018-02-14'
author: Shai Almog
---

![Header Image](/blog/new-update-framework/new-features-2.jpg)

When it comes to big changes this is pretty huge but surprisingly “subtle”. This weekend we’ll release a new plugin update that will completely replace the update process of Codename One and a week after that we will start nagging you to update your plugin so we can all be on the same page. This is a HUGE change as we didn’t change anything about the update process since 2012. But the cool thing about it is that you might not notice it…​

When we launched Codename One in 2012 we needed a way to ship updates and fixes faster than the plugin update system. So we built the client lib update system. Then we needed a way to update the designer tool (resource editor), the GUI builder & the skins…​ We also needed a system to update the builtin builder code (`CodeNameOneBuildClient.jar` so we built a tool for that too).

__ |  Notice the uppercase N in `CodeNameOneBuildClient.jar`. It’s so old even we weren’t sure how to write our own company name correctly…​   
---|---  
  
### Update Framework

A big piece of the change is the removal of code within the IDE plugins that tries to do the update for us. Once we remove that code the new [Update Framework](https://github.com/codenameone/UpdateCodenameOne) can effectively fetch up to date versions of the important jars and make sure everything is at the latest. It solves several problems in the old systems:

  * Download once – if you have multiple projects the library will only download once to the `.codenameone` directory. All the projects will update from local storage

  * Skins update automatically – this is hugely important. When we change a theme we need to update it in the skins and if you don’t update the skin you might see a difference between the simulator and the device

  * Update of settings/designer without IDE plugin update – The IDE plugin update process is slow and tedious. This way we can push out a bug fix for the GUI builder without going through the process of releasing a new plugin version

For the most part this framework should be seamless. You should no longer see the “downloading” message whenever we push an update after your build client is updated. Your system would just poll for a new version daily and update when new updates are available.

You can also use the usual method of Codename One Settings → Basic → Update Client Libs which will force an update check. Notice that the UI will look a bit different after this update.

### How does it Work?

You can see the full code [here](https://github.com/codenameone/UpdateCodenameOne) the gist of it is very simple. We create a jar called `UpdateCodenameOne.jar` under `~/.codenameone` (`~` represents the users home directory).

An update happens by running this tool with a path to a Codename One project e.g.:
    
    
    java -jar ~/.codenameone/UpdateCodenameOne.jar path_to_my_codenameone_project

E.g.:
    
    
    java -jar ~/.codenameone/UpdateCodenameOne.jar ~/dev/AccordionDemo
    Checking: JavaSE.jar
    Checking: CodeNameOneBuildClient.jar
    Checking: CLDC11.jar
    Checking: CodenameOne.jar
    Checking: CodenameOne_SRC.jar
    Checking: designer_1.jar
    Checking: guibuilder_1.jar
    Updating the file: /Users/shai/dev/AccordionDemo/JavaSE.jar
    Updating the file: /Users/shai/dev/AccordionDemo/CodeNameOneBuildClient.jar
    Updating the file: /Users/shai/dev/AccordionDemo/lib/CLDC11.jar
    Updating the file: /Users/shai/dev/AccordionDemo/lib/CodenameOne.jar
    Updated project files

Notice that no download happened since the files were up to date. You can also force a check against the server by adding the force argument as such:
    
    
    java -jar ~/.codenameone/UpdateCodenameOne.jar path_to_my_codenameone_project

The way this works under the hood is thought a `Versions.properties` within your directory that lists the versions of local files. That way we know what should be updated.

__ |  Exclude `Versions.properties` from Git   
---|---  
  
Under the `~/.codenameone` directory we have a more detailed `UpdateStatus.properties` file that includes versions of the locally downloaded files. Notice you can delete this file and it will be recreated as all the jars get downloaded over again.

### What isn’t Covered

You will notice 3 big things that aren’t covered in this unified framework:

  * We don’t update cn1libs – I’m not sure if this is something we would like to update automatically

  * Versioned builds – there is a lot of complexity in the versioned build system. This might be something we address in the future but for now I wanted to keep the framework simple.

  * Offline builds – Offline builds work through manual download and aren’t subjected to this framework

### Finally

I don’t expect a big change like this to go well without a hitch. So please accept our apologies for everything that probably will go wrong over the next couple of weeks as we tune this system. Once it will be in place we will deliver fixes and updates faster.

We won’t need to deal with as many IDE specific behaviors and we will be able to update the system itself moving forward.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — February 18, 2018 at 11:46 am ([permalink](https://www.codenameone.com/blog/new-update-framework.html#comment-23667))

> Francesco Galgani says:
>
> How do I know when a new update is released?  
> For example, yesterday you published an update and this morning you  
> published another: do I have to check manually the available updates every day or is there a  
> way to automate this checking?
>



### **Shai Almog** — February 19, 2018 at 5:57 am ([permalink](https://www.codenameone.com/blog/new-update-framework.html#comment-23788))

> Shai Almog says:
>
> You don’t know an update is released.  
> When a build is made in the IDE we run the update tool and it checks for you. It does this check once a day unless it’s running in “force” mode which happens when you do an update client libs. Normally, this should be seamless.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
