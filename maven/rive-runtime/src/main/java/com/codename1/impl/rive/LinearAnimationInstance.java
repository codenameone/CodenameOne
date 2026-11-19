/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// The playback state of a `LinearAnimation` on an artboard.
public class LinearAnimationInstance {
    private final LinearAnimation animation;
    private final Artboard artboard;
    private float time;
    private int direction = 1;
    private float spilledTime;
    private boolean didLoop;
    private float totalTime;
    private float lastTotalTime;

    public LinearAnimationInstance(LinearAnimation animation, Artboard artboard) {
        this.animation = animation;
        this.artboard = artboard;
        this.time = animation.startTime();
    }

    public LinearAnimation getAnimation() {
        return animation;
    }

    public float getTime() {
        return time;
    }

    /// Seeks to `value` seconds and resets the direction to forward.
    public void setTime(float value) {
        if (Float.floatToIntBits(time) == Float.floatToIntBits(value)) {
            return;
        }
        time = value;
        float diff = totalTime - lastTotalTime;
        float start = (animation.isEnableWorkArea() ? (float) animation.getWorkStart() : 0.0f) * animation.getFps();
        totalTime = value - start;
        lastTotalTime = totalTime - diff;
        direction = 1;
    }

    /// Restarts from the beginning.
    public void reset() {
        time = animation.startTime();
        direction = 1;
        totalTime = 0;
        lastTotalTime = 0;
        didLoop = false;
        spilledTime = 0;
    }

    public boolean didLoop() {
        return didLoop;
    }

    public float getSpilledTime() {
        return spilledTime;
    }

    /// The total time before the latest advance.
    public float getLastTotalTime() {
        return lastTotalTime;
    }

    public float getTotalTime() {
        return totalTime;
    }

    /// The current direction, 1 or -1 (ping-pong flips it).
    public int getDirection() {
        return direction;
    }

    private float directedSpeed() {
        return direction * animation.getSpeed();
    }

    private boolean keepGoing(float multiplier) {
        float ds = directedSpeed() * multiplier;
        return animation.getLoopValue() != LinearAnimation.LOOP_ONE_SHOT
                || (ds > 0 && time < animation.endSeconds())
                || (ds < 0 && time > animation.startSeconds());
    }

    /// True once a one-shot animation reached its end.
    public boolean isFinished() {
        return !keepGoing(1.0f);
    }

    /// Advances by `elapsedSeconds`; returns whether playback continues.
    public boolean advance(float elapsedSeconds) {
        float deltaSeconds = elapsedSeconds * animation.getSpeed() * direction;
        spilledTime = 0.0f;
        if (deltaSeconds == 0) {
            didLoop = false;
            return false;
        }
        lastTotalTime = totalTime;
        totalTime += Math.abs(deltaSeconds);
        boolean killSpilledTime = !keepGoing(elapsedSeconds);
        time += deltaSeconds;

        float fps = animation.getFps() <= 0 ? 60.0f : (float) animation.getFps();
        float frames = time * fps;
        float start = animation.isEnableWorkArea() ? (float) animation.getWorkStart() : 0.0f;
        float end = animation.isEnableWorkArea() ? (float) animation.getWorkEnd() : (float) animation.getDuration();
        float range = end - start;
        boolean looped = false;
        int dir = deltaSeconds < 0 ? -1 : 1;
        switch (animation.getLoopValue()) {
            case LinearAnimation.LOOP_ONE_SHOT:
                if (dir == 1 && frames > end) {
                    spilledTime = (frames - end) / (deltaSeconds * fps) * elapsedSeconds;
                    frames = end;
                    time = frames / fps;
                    looped = true;
                } else if (dir == -1 && frames < start) {
                    spilledTime = (start - frames) / Math.abs(deltaSeconds * fps) * elapsedSeconds;
                    frames = start;
                    time = frames / fps;
                    looped = true;
                }
                break;
            case LinearAnimation.LOOP_LOOP:
                if (range <= 0) {
                    break;
                }
                if (dir == 1 && frames >= end) {
                    float remainder = fmod(frames - start, range);
                    spilledTime = remainder / (deltaSeconds * fps) * elapsedSeconds;
                    frames = start + remainder;
                    time = frames / fps;
                    looped = true;
                } else if (dir == -1 && frames <= start) {
                    float remainder = Math.abs(fmod(start - frames, range));
                    spilledTime = Math.abs(remainder / (deltaSeconds * fps)) * elapsedSeconds;
                    frames = end - remainder;
                    time = frames / fps;
                    looped = true;
                }
                break;
            case LinearAnimation.LOOP_PING_PONG:
                if (range <= 0) {
                    break;
                }
                if (Float.isNaN(frames) || Float.isInfinite(frames)) {
                    // A non-finite advance (huge elapsed time or speed) cannot be
                    // reflected; settle on the edge in the current direction.
                    frames = dir == 1 ? end : start;
                    time = frames / fps;
                    looped = true;
                    break;
                }
                if (frames > end + 2 * range || frames < start - 2 * range) {
                    // Fold a large overshoot by whole round trips (2 * range) first,
                    // keeping the parity, so the reflection loop below runs at most
                    // a couple of times however far the advance went.
                    float over = dir == 1 ? frames - end : start - frames;
                    float folded = fmodDouble(over, 2 * range);
                    frames = dir == 1 ? end + folded : start - folded;
                }
                for (int guard = 0; guard < 8; guard++) {
                    if (dir == 1 && frames >= end) {
                        spilledTime = (frames - end) / fps;
                        frames = end + (end - frames);
                    } else if (dir == -1 && frames < start) {
                        spilledTime = (start - frames) / fps;
                        frames = start + (start - frames);
                    } else {
                        break;
                    }
                    time = frames / fps;
                    direction *= -1;
                    dir *= -1;
                    looped = true;
                }
                break;
            default:
                break;
        }
        if (killSpilledTime) {
            spilledTime = 0;
        }
        didLoop = looped;
        return keepGoing(elapsedSeconds);
    }

    private static float fmodDouble(float a, float b) {
        double r = (double) a - (double) b * Math.floor((double) a / (double) b);
        if (r < 0 || r >= b) {
            r = 0;
        }
        return (float) r;
    }

    private static float fmod(float a, float b) {
        return a - b * (float) (int) (a / b);
    }

    /// Applies the animation at the current time.
    public void apply(float mix) {
        animation.apply(artboard, time, mix);
    }
}
