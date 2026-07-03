---
title: "Motion, Stylus, Trackpads And Foldables In The Core"
slug: motion-input-form-factors
url: /blog/motion-input-form-factors/
date: '2026-07-05'
author: Shai Almog
description: "Two new core APIs expose the hardware modern apps actually see: motion sensors and gestures on one side, rich pointer detail, wheel events, stylus input, trackpads and foldable posture on the other."
feed_html: '<img src="https://www.codenameone.com/blog/motion-input-form-factors.jpg" alt="Motion, Stylus, Trackpads And Foldables" /> Motion sensors, gestures, pointer detail, wheel events, stylus input, trackpads and foldable posture are now core Codename One APIs.'
series: ["release-2026-07-03"]
---

![Motion, Stylus, Trackpads And Foldables In The Core](/blog/motion-input-form-factors.jpg)

For a long time, mobile meant a touch screen and a few platform-specific escape hatches. That is no longer enough. A Codename One app can run on phones, tablets, desktops, watches, TVs, browsers, foldables, touch laptops, external displays, cars, and devices with pens, mice, and trackpads.

[PR #5310](https://github.com/codenameone/CodenameOne/pull/5310) and [PR #5309](https://github.com/codenameone/CodenameOne/pull/5309) make that hardware visible through core APIs instead of cn1libs and platform branches.

## Motion Sensors And Gestures

The new `com.codename1.sensors` package exposes accelerometer, gyroscope and magnetometer readings. It also derives gravity, linear acceleration and orientation in the core when the platform does not provide them directly.

```java
MotionSensorManager sensors = MotionSensorManager.getInstance();
MotionSensor accel = sensors.getSensor(MotionSensorManager.TYPE_ACCELEROMETER);

if (accel != null) {
    MotionSensorListener listener = new MotionSensorListener() {
        public void motionReceived(MotionEvent evt) {
            label.setText(evt.getX() + ", " + evt.getY() + ", " + evt.getZ());
            label.getParent().revalidate();
        }
    };
    accel.addListener(listener);
}
```

The manager itself is always available. Individual sensors return `null` when the hardware is unsupported, so the app can keep one code path with a small capability check.

Gestures are built on top of the sensor stream in the core:

```java
MotionSensorManager.getInstance().addGestureListener(
        GestureEvent.TYPE_SHAKE,
        new GestureListener() {
            public void gestureDetected(GestureEvent evt) {
                Dialog.show("Shaken", "You shook the device", "OK", null);
            }
        });
```

The recognized set is practical rather than exotic: shake, flip face down, flip face up, tilt left, tilt right, tilt forward, tilt backward, pick up and free fall. Because recognition lives in `GestureEngine`, every port that exposes an accelerometer gets the same behavior.

Sampling is reference counted. The hardware powers on while listeners exist and powers down when they are removed. On iOS you still need `ios.NSMotionUsageDescription` with a user-facing reason for motion access.

## Rich Pointer Detail

Classic pointer callbacks tell you where a press happened. Modern input asks more questions: was it a mouse, a finger, a stylus, or an eraser? Which button? What pressure? Was the pen tilted? Was the pointer hovering?

`PointerEvent` is the snapshot attached to pointer action events:

```java
myComponent.addPointerPressedListener(e -> {
    PointerEvent pe = e.getPointerEvent();
    if (pe.isSecondaryButton()) {
        showContextMenu(pe.getX(), pe.getY());
    }
    drawPreview(pe.getX(), pe.getY(), pe.getPressure());
});
```

You can also poll the current dispatch state through `CN`:

```java
int button = CN.getPointerButton();
float pressure = CN.getPointerPressure();
boolean pen = CN.isStylusPointer();
PointerEvent current = CN.getCurrentPointerEvent();
```

On plain touch screens these values have safe defaults: primary button, pressure `1.0`, touch or unknown pointer type. Existing apps keep working.

## Wheel, Trackpad And Context Menus

`WheelEvent` is now the cross-platform scroll-gesture event. It handles mouse wheels, precision trackpads, horizontal scrolling, iOS/iPadOS pointer devices and the Apple Watch Digital Crown through the same path.

```java
canvas.addMouseWheelListener(e -> {
    WheelEvent w = (WheelEvent)e;
    if (w.isControlDown()) {
        zoom(w.getDeltaY());
        w.consume();
    }
});
```

Consuming the event suppresses the default scroll. That is the difference between "wheel scrolls the container" and "control plus wheel zooms a design surface."

For context menus, use the higher-level listener:

```java
label.addContextMenuListener(e -> {
    e.consume();
    showMyContextMenu(e.getX(), e.getY());
});
```

It fires for right click, stylus barrel button and long press, which is exactly the kind of cross-platform behavior app code should not have to rediscover.

Trackpad magnify and rotate gestures route to component methods:

```java
Container photo = new Container() {
    protected boolean pinch(float scale) {
        zoom(scale);
        return true;
    }

    protected boolean rotation(float radians) {
        rotate(radians);
        return true;
    }
};
```

## Stylus And Foldables

Stylus support exposes pressure, tilt, contact size and eraser type. A drawing surface can listen only to stylus events:

```java
drawingArea.addStylusListener(e -> {
    PointerEvent pe = e.getPointerEvent();
    float width = 1f + pe.getPressure() * 9f;
    if (pe.isEraser()) {
        erase(pe.getX(), pe.getY());
    } else {
        drawPoint(pe.getX(), pe.getY(), width);
    }
});
```

Foldables use `DevicePosture`:

```java
DevicePosture posture = CN.getDevicePosture();
if (posture.isFoldable() && posture.isTableTop()) {
    layoutForTableTop(posture.getFoldBounds(null));
}

CN.addPostureListener(e -> relayoutForPosture(CN.getDevicePosture()));
```

On Android, foldable support is opt-in with `android.foldableSupport=true`, so apps that do not need the AndroidX window library do not carry it. The simulator can generate posture changes for testing.

## Wrapping Up

Input used to mean key codes, pointer coordinates, and maybe a scroll wheel. That model is too small now. A modern app has to reason about pointer type, pressure, hover, buttons, tilt, posture, trackpads, sensor sampling, and gestures that may or may not exist on the current device.

The new APIs do not make that complexity disappear. They put it behind capability checks and default-safe events, so the same component can handle a finger on a phone, a stylus on a tablet, a mouse on desktop, and a foldable hinge without turning every screen into platform-specific code.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
