---
title: Building Codename One From Source – Maven Edition
slug: building-codename-one-from-source-maven-edition
url: /blog/building-codename-one-from-source-maven-edition/
original_url: https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html
aliases:
- /blog/building-codename-one-from-source-maven-edition.html
date: '2021-04-21'
author: Steve Hannah
description: Learn how to build Codename One from source and use this "local" version
  in your Codename One projects.
---

Learn how to build Codename One from source and use this "local" version in your Codename One projects.

## Tip

> This post is targeted at experienced Codename One users. If you haven’t built an app with Codename One before, I recommend you start with this [Getting Started tutorial](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) (for Java) or [this tutorial](https://shannah.github.io/cn1app-archetype-kotlin-template/getting-started.html) (for Kotlin).

One of the benefits of [moving to Maven](/blog/moving-to-maven/) is improved project hygiene. It is now trivial to build Codename One from source.

In this video, I show you how to build Codename One from source and use this “local” version in your Codename One projects.

### TLDW (Too Long Didn’t Watch)

Here’s the gist of what happens in the video.

1. Clone the [Codename One repository](https://github.com/codenameone/CodenameOne), then run mvn install in the maven subdirectory:

```bash			
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne/maven
mvn install			
```

This will take a few minutes, but at the end of the tunnel you should see “SUCCESS” as shown below:

**NOTE**: Due to wordpress issues the images in this blogpost were lost.

2. Clone the [cn1-maven-archetypes repository](https://github.com/shannah/cn1-maven-archetypes), then run mvn install in its root directory:

```bash			
git clone https://github.com/shannah/cn1-maven-archetypes
cd cn1-maven-archetypes
mvn install				
```

This will take another minute or so, but at the end of the tunnel you should see “SUCCESS”:

...

After completing these steps, Codename One will be installed in the local maven repository. A key point I make in this video is the version number of the sources that I checked out of Github. If you are cloning the project from the master branch, then the version will usually be a SNAPSHOT version. E.g. 7.0.21-SNAPSHOT. This is a Maven convention. Release versions will not have the **-SNAPSHOT** suffix.
  
  
In the video, you can see that the version number is “7.0.21-SNAPSHOT”.

## Using the Local Version in Your Application Project

Now that Codename One is installed in your local Maven repo, you can use that version in your application instead of the release version.

I demonstrate this in the video by creating a new project with the [Codename One initializr](https://start.codenameone.com/).

...

## Tip

> Check out my [Video tutorial on Codename One initializr](https://sjhannah.medium.com/preview-online-tool-to-generate-ios-android-app-starter-project-c9f27c47850b) if you haven’t seen it yet.

After downloading and extracting the project, I open its pom.xml file and and look for the <cn1.version> and <cn1.plugin.version> properties:

...

I then change these to point to the version that I installed into my local maven repository: 7.0.21-SNAPSHOT.

...

### Why Build From Source?

Because you can, and because it is the first step toward taking control of your own destiny. It gives you early access to features that may not be available on Maven Central, and it also enables you to make your own changes, and potentially contribute them to the Codename One core.

### Getting Started

If you haven’t built an app yet, it’s easy to get started. Just go to [Codename One initializr](https://start.codenameone.com/) and press “Download”. You could be up and running in only a few minutes.

If you want to dig deeper into Codename One’s Maven support, check out the [Codename One Maven Developers Guide](https://shannah.github.io/codenameone-maven-manual/).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Dave Dyer** — July 20, 2023 at 10:07 pm ([permalink](/blog/building-codename-one-from-source-maven-edition/#comment-24563))

> Dave Dyer says:
>
> something is out of date. in codenameone\maven, mvn install
>
> [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project  
> java-runtime: Compilation failure: Compilation failure:  
> [ERROR] Source option 5 is no longer supported. Use 7 or later.
>



### **Dave Dyer** — July 21, 2023 at 7:35 pm ([permalink](/blog/building-codename-one-from-source-maven-edition/#comment-24564))

> Dave Dyer says:
>
> This is somehow related to the java version in use. I fixed this by  
> downgrading my default java from java-16 to and older jdk, jdk-1.8
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
