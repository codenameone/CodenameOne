---
title: GET REPEATABLE BUILDS? BUILD AGAINST A CONSISTENT VERSION OF CODENAME ONE?
  USE THE VERSIONING FEATURE?
slug: how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature
url: /how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html
tags:
- pro
description: Versioning allows us to build against a point release and get stability/consistency
  over time
youtube_id: w7xvlw3rI6Y
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-4-1.jpg
---

{{< youtube "w7xvlw3rI6Y" >}} 

#### Transcript

In this short video I will try to explain versioned builds which is Codename One’s approach for repeatable builds. Before we begin, versioned builds are a pro feature that has extended functionality in the enterprise tier. I’ll talk more about that soon but first, what does versioned build actually mean?

With versioned builds we send a build to a specific Codename One point version. For instance, you can send a build to Codename One 3.7 and it will build against the exact version of Codename One that existed when 3.7 was released.

This allows you to avoid potential regressions due to frequent changes in the build server that might impact compatibility. It’s also useful for testing purposes, if your app suddenly fails you can use versioned build to see if this is due to a change in the Codename One servers.

As I mentioned before there is a difference between enterprise and pro subscriptions. For enterprise developers we support up to 18 months back. That means an enterprise user can build against a version released in the past 18 months which is typically 4 releases back.

The pro versions include 5 month support which typically maps to the last one or two releases.

You can enable versioned build by selecting the specific version in the Codename One Settings tool under the basics section.  
This opens the list of versions and you can pick the right one. You can use update client libs to update the simulator to that specific release as well.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
