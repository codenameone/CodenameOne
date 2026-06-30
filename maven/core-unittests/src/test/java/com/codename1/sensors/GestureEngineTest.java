package com.codename1.sensors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the platform independent gesture recognizer. The engine has no framework dependency
 * so it can be driven directly with synthetic accelerometer samples.
 */
class GestureEngineTest {
  private static final double G = MotionSensorManager.STANDARD_GRAVITY;

  private boolean fired(List<GestureEvent> events, int type) {
    for (GestureEvent e : events) {
      if (e.getType() == type) {
        return true;
      }
    }
    return false;
  }

  @Test
  void detectsShake() {
    GestureEngine engine = new GestureEngine();
    boolean shaken = false;
    long t = 0;
    // Alternate a strong sideways jolt with a rest sample every 50ms. Each rising edge counts as
    // one jolt; three within the window fire a shake.
    for (int i = 0; i < 12; i++) {
      float ax = (i % 2 == 0) ? 25f : 0f;
      List<GestureEvent> evt = engine.onSample(ax, 0, (float) G, 0, 0, (float) G, t);
      if (fired(evt, GestureEvent.TYPE_SHAKE)) {
        shaken = true;
      }
      t += 50;
    }
    assertTrue(shaken, "expected a shake to be detected");
  }

  @Test
  void quietDeviceDoesNotShake() {
    GestureEngine engine = new GestureEngine();
    boolean shaken = false;
    long t = 0;
    for (int i = 0; i < 40; i++) {
      float ax = (i % 2 == 0) ? 0.5f : -0.5f;
      List<GestureEvent> evt = engine.onSample(ax, 0, (float) G, 0, 0, (float) G, t);
      if (fired(evt, GestureEvent.TYPE_SHAKE)) {
        shaken = true;
      }
      t += 50;
    }
    assertFalse(shaken, "a still device must not register a shake");
  }

  @Test
  void detectsFlipFaceDown() {
    GestureEngine engine = new GestureEngine();
    long t = 0;
    for (int i = 0; i < 10; i++) {
      engine.onSample(0, 0, (float) G, 0, 0, (float) G, t);
      t += 50;
    }
    boolean flipped = false;
    for (int i = 0; i < 12; i++) {
      List<GestureEvent> evt = engine.onSample(0, 0, (float) -G, 0, 0, (float) -G, t);
      if (fired(evt, GestureEvent.TYPE_FLIP_FACE_DOWN)) {
        flipped = true;
      }
      t += 50;
    }
    assertTrue(flipped, "expected a face-down flip");
  }

  @Test
  void detectsTiltRight() {
    GestureEngine engine = new GestureEngine();
    long t = 0;
    for (int i = 0; i < 5; i++) {
      engine.onSample(0, 0, (float) G, 0, 0, (float) G, t);
      t += 50;
    }
    double roll = 0.9; // radians, beyond the tilt threshold
    float gx = (float) (G * Math.sin(roll));
    float gz = (float) (G * Math.cos(roll));
    boolean tilted = false;
    for (int i = 0; i < 5; i++) {
      List<GestureEvent> evt = engine.onSample(gx, 0, gz, gx, 0, gz, t);
      if (fired(evt, GestureEvent.TYPE_TILT_RIGHT)) {
        tilted = true;
      }
      t += 50;
    }
    assertTrue(tilted, "expected a tilt-right gesture");
  }

  @Test
  void detectsFreeFall() {
    GestureEngine engine = new GestureEngine();
    long t = 0;
    for (int i = 0; i < 5; i++) {
      engine.onSample(0, 0, (float) G, 0, 0, (float) G, t);
      t += 50;
    }
    boolean falling = false;
    for (int i = 0; i < 6; i++) {
      List<GestureEvent> evt = engine.onSample(0, 0, 0, 0, 0, (float) G, t);
      if (fired(evt, GestureEvent.TYPE_FREE_FALL)) {
        falling = true;
      }
      t += 50;
    }
    assertTrue(falling, "expected a free-fall gesture");
  }

  @Test
  void detectsPickUp() {
    GestureEngine engine = new GestureEngine();
    long t = 0;
    for (int i = 0; i < 12; i++) {
      engine.onSample(0, 0, (float) G, 0, 0, (float) G, t);
      t += 50;
    }
    boolean pickedUp = false;
    for (int i = 0; i < 6; i++) {
      List<GestureEvent> evt = engine.onSample(4f, 0, (float) (G + 4f), 0, 0, (float) G, t);
      if (fired(evt, GestureEvent.TYPE_PICK_UP)) {
        pickedUp = true;
      }
      t += 50;
    }
    assertTrue(pickedUp, "expected a pick-up gesture");
  }
}
