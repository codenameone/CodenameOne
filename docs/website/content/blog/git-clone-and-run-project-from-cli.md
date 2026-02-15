---
title: Clone and Run Codename One Demos In Single Line of Code
slug: git-clone-and-run-project-from-cli
url: /blog/git-clone-and-run-project-from-cli/
original_url: https://www.codenameone.com/blog/git-clone-and-run-project-from-cli.html
aliases:
- /blog/git-clone-and-run-project-from-cli.html
date: '2017-11-27'
author: Steve Hannah
---

![Header Image](/blog/git-clone-and-run-project-from-cli/tip.jpg)

We have loads of demo Codename One apps hosted on Github, however cloning and running a project can be a little tricky because we generally don’t publish the dependent jar files (e.g. CodenameOne.jar) in the Github repository. This helps keep the repository lean, but it adds some steps to the process of cloning and running the project.

For use cases like this, you may want to try the [Codename One CLI tool](https://www.npmjs.com/package/codenameone-cli), as it provides many useful functions directly on the command line. In this post I’ll demonstrate how you can easily clone a Codename One project from Github and run it in the Codename One simulator using a single line of code.

Consider the [KitchenSink](https://github.com/codenameone/KitchenSink) demo. We can clone this repository using the following command
    
    
    $ cn1 git-clone https://github.com/codenameone/KitchenSink
    Cloning into 'KitchenSink'...
    Installing jars into KitchenSink...
    Downloading 11606508 bytes
    Download completed!
    Project ready at KitchenSink

**What just happened?**

The `cn1 git-clone` command is a thin wrapper around `git clone` (which implies that you need to have `git clone` in your PATH). Therefore you can pass it the same parameters as you pass to `git clone`. After cloning the repository, `cn1 git-clone` downloads the latest Codename One libs and adds them to the project so that it is ready to roll.

**Running the Demo**

Now that the project is cloned, we can run the demo in the Codename One simulator by running the “run” target of the project. E.g.
    
    
    $ cd KitchenSink
    $ ant run

### Combining it into a Single Line

If you are on Mac/Linux, it is easy to use the ‘&&’ operator to combine all this into a single line:
    
    
    $ cn1 git-clone https://github.com/codenameone/KitchenSink && cd KitchenSink && ant run

This clones it, changes to the KitchenSink directory, and runs it.

### Finding Existing Demos

You can also use the `cn1 list-demos` command to find existing Codename One demos on Github that you can clone. E.g.
    
    
    $ cn1 list-demos

This will produce a list of all of the repositories on Github that are tagged with both the “codenameone” topic, and the “demo” topic. The output will look like
    
    
    shannah/GeoVizDemo            : A demo app using the Codename One GeoViz Library
    codenameone/KitchenSink       : Rewrite of the kitchen sink demo to match design aesthetics of 2016
    ... etc..

You can filter the results by adding a parameter.
    
    
    $ cn1 list-demos "GeoViz"

This will only show demos that also match the GeoViz search. We have only just started tagging our demos so for now there aren’t very many listed there. But the list will grow as time goes on.

__ |  `cn1 list-demos` uses the Github search API, so you can use any filters that you would put into searches on the Github website.   
---|---  
  
Once we see a demo that we want to run, we can pass its full name to `cn1 git-clone`. E.g.
    
    
    $ cn1 git-clone shannah/GeoVizDemo

__ |  This demonstrates that git-clone allows you to omit the `<https://github.com>` from the repository name, and just provide the full repository name of the form `ownername/repositoryname`  
---|---  
  
### Adding Your Own Demos

Adding your own demos so that they will be included in the `cn1 list-demos` results is easy. If your project is already hosted on Github, you simply need to add the `codenameone` and `demo` topics to the repository.

__ |  When hosting a project on Github I recommend stripping out all of the jar files just as we do in our demos. You can do this by simply copying the following directives into your `.gitignore` file   
---|---  
  
The `.gitignore` contents from the KitchenSink repository
    
    
    *.jar
    nbproject/private/
    build/
    nbbuild/
    dist/
    lib/CodenameOne_SRC.zip
    *.p12
    *.mobileprovision

A shortcut would also be to use `cn1 git-init` instead of `git init` when you initialize the repository.

### Appendix: Installing the CLI Tool

Thus far, I’ve skipped the step of actually installing the CLI tool. It is distributed using `npm`, which is included when you install [NodeJS](https://nodejs.org/en/), which has a simple installer for Windows, Linux, and Mac.

**Installing Globally**

Installing globally on Windows (Requires Admin permissions)
    
    
    npm install -g codenameone-cli

Installing globally on Mac/Linux
    
    
    sudo npm install -g codenameone-cli

**Installing Locally**

If you don’t have admin permissions, or you just want to install it in the current directory, you can omit the `-g` flag. Then installation becomes

Installing locally
    
    
    npm install codenameone-cli

This will install the command at `./node_modules/.bin/cn1`

### Screencast

I’ve create a short screencast demonstrating the use of the `cn1 git-clone` command.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — November 29, 2017 at 3:36 am ([permalink](/blog/git-clone-and-run-project-from-cli/#comment-23689))

> Nick Koirala says:
>
> Nice one. Makes the demo projects much more accessible.
>



### **Mohammed Kamal** — November 12, 2019 at 2:56 pm ([permalink](/blog/git-clone-and-run-project-from-cli/#comment-24266))

> [Mohammed Kamal](https://lh3.googleusercontent.com/a-/AAuE7mCCX8URBU8WJDWqV22i4h7HBcx_AKz6hh_WX4-U) says:
>
> Steve, excellent, just done this now .. better late than never! Many thanks for that, saved me lots of times.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
