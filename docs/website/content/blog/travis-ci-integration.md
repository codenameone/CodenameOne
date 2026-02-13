---
title: Travis CI Integration
slug: travis-ci-integration
url: /blog/travis-ci-integration/
original_url: https://www.codenameone.com/blog/travis-ci-integration.html
aliases:
- /blog/travis-ci-integration.html
date: '2018-01-08'
author: Steve Hannah
---

![Header Image](/blog/travis-ci-integration/tip.jpg)

We’ve just added support for Travis CI in your Codename One projects. Travis can be set up to automatically test your project (i.e. run unit tests) on a variety of different platforms every time you commit changes to github.

There is a [wiki page](https://github.com/codenameone/CodenameOne/wiki/Travis-CI-Integration) with full documentation of this feature, but the general idea and workflow are:

  1. Enable Travis CI for your project via Codename One settings

  2. Push your project (including .travis.yml and .travis directory, which are created for you when you enable Travis) to Github.

  3. Activate your Project On [Travis](https://travis-ci.org).

Then every time you commit changes to Github, travis will run your tests.

### Settings Panel

After you’ve activated Travis, the “Travis Settings” form will look like

![Travis select jobs form](/blog/travis-ci-integration/travis-select-jobs.png)

__ |  On-device continuous integration requires an Enterprise account. Other accounts will see the Android and iOS options disabled. But they can still enable JavaSE.   
---|---  
  
This is a list of the jobs that you can have travis run for you. If you only select “JavaSE”, then Travis will run your unit tests in the Codename One simulator. Android jobs are run on the appropriate Android emulator, and iOS jobs are run on the appropriate iOS simulator.

We will be adding more versions and platforms as time goes on.

For full details, see the [wiki page](https://github.com/codenameone/CodenameOne/wiki/Travis-CI-Integration).

Also check out this screencast where I demonstrate Travis integration on the old GeoViz demo.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
