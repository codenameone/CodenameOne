---
title: Accelerometer & Code Freeze
slug: accelerometer-code-freeze
url: /blog/accelerometer-code-freeze/
original_url: https://www.codenameone.com/blog/accelerometer-code-freeze.html
aliases:
- /blog/accelerometer-code-freeze.html
date: '2015-04-12'
author: Shai Almog
---

![Header Image](/blog/accelerometer-code-freeze/sensors.jpg)

Devices have sensors such as accelerometer, GPS and up until now our support for them was relatively basic.  
Chen recently introduced a [cn1lib](https://github.com/chen-fishbein/sensors-codenameone)  
that includes support for various types of sensors on the device. Its really simple to use: 
    
    
    SensorsManager sensor = SensorsManager.getSenorsManager(SensorsManager.TYPE_ACCELEROMETER);
    if (sensor != null) {
        sensor.registerListener(new SensorListener() {
            public void onSensorChanged(long timeStamp, float x, float y, float z) {
                //do your stuff here...
            }
        });
    }

Check it out if you need access to such features. 

### Code Freeze

We will be entering code freeze later today which should allow us to gear up towards the 3.0 release of Codename One  
in two weeks. All commits will be made against an issue and always with a peer review. Releasing 3.0 will allow  
us to improve versioned builds and cleanup our issue tracker. We already cleared more than 100 issues in the past  
week in an effort to make this a high quality polished release.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
