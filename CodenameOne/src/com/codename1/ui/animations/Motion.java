/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.animations;

/**
 * Abstracts the notion of physical motion over time from a numeric location to
 * another. This class can be subclassed to implement any motion equation for
 * appropriate physics effects.
 * <p>This class relies on the System.currentTimeMillis() method to provide
 * transitions between coordinates. The motion can be subclassed to provide every
 * type of motion feel from parabolic motion to spline and linear motion. The default
 * implementation provides a simple algorithm giving the feel of acceleration and
 * deceleration.
 *
 * @author Shai Almog
 */
public class Motion {
    private static boolean slowMotion;
    // package protected for the resource editor
    static final int LINEAR = 0;
    static final int SPLINE = 1;

    /**
     * Allows debugging motion behavior by slowing motions down 50 fold, doesn't apply to friction motion
     * @return the slowMotion
     */
    public static boolean isSlowMotion() {
        return slowMotion;
    }

    /**
     * Allows debugging motion behavior by slowing motions down 50 fold, doesn't apply to friction motion
     * @param aSlowMotion the slowMotion to set
     */
    public static void setSlowMotion(boolean aSlowMotion) {
        slowMotion = aSlowMotion;
    }
    
    int motionType;

    private static final int FRICTION = 2;

    private static final int DECELERATION = 3;
    private static final int CUBIC = 4;
    private static final int COLOR_LINEAR = 5;
    
    private int sourceValue;
    private int destinationValue;
    private int duration;
    private long startTime;
    private double initVelocity,  friction;
    private int lastReturnedValue;
    private long currentMotionTime = -1;

    private float p0, p1, p2, p3;
    
    /**
     * Construct a point/destination motion
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     */
    protected Motion(int sourceValue, int destinationValue, int duration) {
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.duration = duration;
        lastReturnedValue = sourceValue;
        if(slowMotion) {
            this.duration *= 50;
        }
    }

    /**
     * Sends the motion to the end time instantly which is useful for flushing an animation
     */
    public void finish() {
        if(!isFinished()) {
            startTime = System.currentTimeMillis() - duration;
            currentMotionTime = -1;
        }
    }
    
    /**
     * Construct a velocity motion
     * 
     * @param sourceValue starting value
     * @param initVelocity initial velocity
     * @param friction degree of friction
     */
    protected Motion(int sourceValue, float initVelocity, float friction) {
        this.sourceValue = sourceValue;
        this.initVelocity = initVelocity;
        this.friction = friction;
        duration = (int) ((Math.abs(initVelocity)) / friction);
    }

    
    /**
     * Creates a standard Cubic Bezier motion to implement functions such as ease-in/out etc.
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     * @param p0 argument to the bezier function
     * @param p1 argument to the bezier function
     * @param p2 argument to the bezier function
     * @param p3 argument to the bezier function
     * @return Motion instance 
     */
    public static Motion createCubicBezierMotion(int sourceValue, int destinationValue, int duration, 
            float p0, float p1, float p2, float p3) {
        Motion m = new Motion(sourceValue, destinationValue, duration);
        m.motionType = CUBIC;
        m.p0 = p0;
        m.p1 = p1;
        m.p2 = p2;
        m.p3 = p3;
        return m;
    }

    /**
     * Equivalent to createCubicBezierMotion with 0, 0.42, 0.58, 1.0 as arguments.
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     * @return Motion instance 
     */
    public static Motion createEaseInOutMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0, 0.42f, 0.58f, 1);
    }
    
    /**
     * Equivalent to createCubicBezierMotion with 0f, 0.25f, 0.25f, 1 as arguments.
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     * @return Motion instance 
     */
    public static Motion createEaseMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0.25f, 0.25f, 1.0f);
    }
    
    /**
     * Equivalent to createCubicBezierMotion with 0f, 0.42f, 1f, 1f as arguments.
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     * @return Motion instance 
     */
    public static Motion createEaseInMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0.42f, 1f, 1f);
    }
    
    /**
     * Equivalent to createCubicBezierMotion with 0f, 0f, 0.58f, 1.0f as arguments.
     * 
     * @param sourceValue starting value
     * @param destinationValue destination value
     * @param duration motion duration
     * @return Motion instance 
     */
    public static Motion createEaseOutMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0f, 0.58f, 1.0f);
    }
    
    /**
     * Creates a linear motion starting from source value all the way to destination value
     * 
     * @param sourceValue the number from which we are starting (usually indicating animation start position)
     * @param destinationValue the number to which we are heading (usually indicating animation destination)
     * @param duration the length in milliseconds of the motion (time it takes to get from sourceValue to
     * destinationValue)
     * @return new motion object
     */
    public static Motion createLinearMotion(int sourceValue, int destinationValue, int duration) {
        Motion l = new Motion(sourceValue, destinationValue, duration);
        l.motionType = LINEAR;
        return l;
    }
    
    
    /**
     * Creates a linear motion starting from source value all the way to destination value for a color value.
     * Unlike a regular linear motion a color linear motion is shifted based on channels where red, green & blue 
     * get shifted separately.
     * 
     * @param sourceValue the color from which we are starting 
     * @param destinationValue the destination color
     * @param duration the length in milliseconds of the motion (time it takes to get from sourceValue to
     * destinationValue)
     * @return new motion object
     */
    public static Motion createLinearColorMotion(int sourceValue, int destinationValue, int duration) {
        Motion l = new Motion(sourceValue, destinationValue, duration);
        l.motionType = COLOR_LINEAR;
        return l;
    }

    /**
     * Creates a spline motion starting from source value all the way to destination value
     * 
     * @param sourceValue the number from which we are starting (usually indicating animation start position)
     * @param destinationValue the number to which we are heading (usually indicating animation destination)
     * @param duration the length in milliseconds of the motion (time it takes to get from sourceValue to
     * destinationValue)
     * @return new motion object
     */
    public static Motion createSplineMotion(int sourceValue, int destinationValue, int duration) {
        Motion spline = new Motion(sourceValue, destinationValue, duration);
        spline.motionType = SPLINE;
        return spline;
    }

    /**
     * Creates a deceleration motion starting from source value all the way to destination value
     * 
     * @param sourceValue the number from which we are starting (usually indicating animation start position)
     * @param destinationValue the number to which we are heading (usually indicating animation destination)
     * @param duration the length in milliseconds of the motion (time it takes to get from sourceValue to
     * destinationValue)
     * @return new motion object
     */
    public static Motion createDecelerationMotion(int sourceValue, int destinationValue, int duration) {
        Motion  deceleration = new Motion(sourceValue, destinationValue, duration);
        deceleration.motionType = DECELERATION;
        return  deceleration;
    }
    
    /**
     * Creates a friction motion starting from source with initial speed and the friction
     *
     * @param sourceValue the number from which we are starting (usually indicating animation start position)
     * @param maxValue the maximum value for the friction
     * @param initVelocity the starting velocity
     * @param friction the motion friction
     * @return new motion object
     */
    public static Motion createFrictionMotion(int sourceValue, int maxValue, float initVelocity, float friction) {
        Motion frictionMotion = new Motion(sourceValue, initVelocity, friction);
        frictionMotion.destinationValue = maxValue;
        frictionMotion.motionType = FRICTION;
        return frictionMotion;
    }
    
    /**
     * Sets the start time to the current time
     */
    public void start() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Returns the current time within the motion relative to start time
     * 
     * @return long value representing System.currentTimeMillis() - startTime
     */
    public long getCurrentMotionTime() {
        if(currentMotionTime < 0) {
            return System.currentTimeMillis() - startTime;
        }
        return currentMotionTime;
    }

    /**
     * Allows overriding the getCurrentMotionTime method value with a manual value
     * to provide full developer control over animation speed/position.
     *
     * @param currentMotionTime the time in milliseconds for the motion.
     */
    public void setCurrentMotionTime(long currentMotionTime) {
        this.currentMotionTime = currentMotionTime;
        
        // workaround allowing the motion to be restarted when manually setting the current time
        if(lastReturnedValue == destinationValue) {
            lastReturnedValue = sourceValue;
        }
    }

    /**
     * Sets the start time of the motion
     * 
     * @param startTime the starting time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns true if the motion has run its course and has finished meaning the current
     * time is greater than startTime + duration.
     * 
     * @return true if System.currentTimeMillis() > duration + startTime or the last returned value is the destination value
     */
    public boolean isFinished() {
        if(currentMotionTime < 0) {
            return getCurrentMotionTime() > duration || destinationValue == lastReturnedValue;
        }
        return getCurrentMotionTime() > duration || destinationValue == lastReturnedValue;
    }

    private int getSplineValue() {
        //make sure we reach the destination value.
        if(isFinished()){
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        currentTime = Math.min(currentTime, totalTime);
        int p = Math.abs(destinationValue - sourceValue);
        float centerTime = totalTime / 2;
        float l = p / (centerTime * centerTime);
        int x;
        if (sourceValue < destinationValue) {
            if (currentTime > centerTime) {
                x = sourceValue + (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                        currentTime * currentTime / 2));
            } else {
                x = sourceValue + (int) (l * currentTime * currentTime / 2);
            }
        } else {
            currentTime = totalTime - currentTime;
            if (currentTime > centerTime) {
                x = destinationValue + (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                        currentTime * currentTime / 2));
            } else {
                x = destinationValue + (int) (l * currentTime * currentTime / 2);
            }
        }
        return x;
    }


    private int getCubicValue() {
        //make sure we reach the destination value.
        if(isFinished()){
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        currentTime = Math.min(currentTime, totalTime);
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        float dis = Math.abs(destinationValue - sourceValue);
        float p = currentTime / totalTime;
        float a = (1 - p) * (1 - p) * (1 - p) * p0;
        float b = 3 * (1 - p) * (1 - p) * p * p1;
        float c = 3 * (1 - p) * p * p * p2;
        float d = p * p * p * p3;
        int current;
        if (destinationValue > sourceValue) {
            current = sourceValue + (int)((a + b + c + d) * dis);
        } else {
            int currentDis = (int)((a + b + c + d) * dis);
            current = sourceValue - currentDis;
        }
        return current;        
    }

//    private int[] values = new int[1000];
//    private int[] times = new int[1000];
//    private int vOff;
//    
//    /**
//     * Returns the value for the motion for the current clock time. 
//     * The value is dependent on the Motion type.
//     * 
//     * @return a value that is relative to the source value
//     */
//    public int getValue() {
//        int v = getValueImpl();
//        if(isFinished() && vOff > 0) {
//            System.out.println("initVelocity:\t"+initVelocity + "\tfriction:\t" + friction + "\tdestinationValue:\t" + destinationValue + "\tsourceValue:\t" + sourceValue);
//            System.out.println("Value\tTime");
//            for(int iter = 0 ; iter < vOff ; iter++) {
//                System.out.println("" + values[iter] + "\t" + times[iter]);
//            }
//            vOff = 0;
//        } else {
//            values[vOff] = v;
//            int time = (int) getCurrentMotionTime();
//            times[vOff] = time;
//
//            vOff++;
//        }
//        
//        return v;
//    }
//    
//    /**
//     * Returns the value for the motion for the current clock time. 
//     * The value is dependent on the Motion type.
//     * 
//     * @return a value that is relative to the source value
//     */
//    private int getValueImpl() {
//        if(currentMotionTime > -1 && startTime > getCurrentMotionTime()) {
//            return sourceValue;
//        }
//        switch(motionType) {
//            case SPLINE:
//                lastReturnedValue = getSplineValue();
//                break;
//            case CUBIC:
//                lastReturnedValue = getCubicValue();
//                break;
//            case FRICTION:
//                lastReturnedValue = getFriction();
//                break;
//            case DECELERATION:
//                lastReturnedValue = getRubber();
//                break;
//            case COLOR_LINEAR:
//                lastReturnedValue = getColorLinear();
//                break;
//            default:
//                lastReturnedValue = getLinear();
//                break;
//        }
//        return lastReturnedValue;
//    }

    /**
     * Returns the value for the motion for the current clock time. 
     * The value is dependent on the Motion type.
     * 
     * @return a value that is relative to the source value
     */
    public int getValue() {
        if(currentMotionTime > -1 && startTime > getCurrentMotionTime()) {
            return sourceValue;
        }
        switch(motionType) {
            case SPLINE:
                lastReturnedValue = getSplineValue();
                break;
            case CUBIC:
                lastReturnedValue = getCubicValue();
                break;
            case FRICTION:
                lastReturnedValue = getFriction();
                break;
            case DECELERATION:
                lastReturnedValue = getRubber();
                break;
            case COLOR_LINEAR:
                lastReturnedValue = getColorLinear();
                break;
            default:
                lastReturnedValue = getLinear();
                break;
        }
        return lastReturnedValue;
    }
    
    private int getLinear() {
        //make sure we reach the destination value.
        if(isFinished()){
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        int dis = destinationValue - sourceValue;
        int val = (int)(sourceValue + (currentTime / totalTime * dis));
        
        if(destinationValue < sourceValue) {
            return Math.max(destinationValue, val);
        } else {
            return Math.min(destinationValue, val);
        }
    }

    private int getColorLinear() {
        if(isFinished()){
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        
        int sourceR = (sourceValue >> 16) & 0xff;
        int destR = (destinationValue >> 16) & 0xff;
        int sourceG = (sourceValue >> 8) & 0xff;
        int destG = (destinationValue >> 8) & 0xff;
        int sourceB = sourceValue & 0xff;
        int destB = destinationValue & 0xff;
        
        int disR = destR - sourceR;
        int disG = destG - sourceG;
        int disB = destB - sourceB;
        int valR = (int)(sourceR + (currentTime / totalTime * disR));
        int valG = (int)(sourceG + (currentTime / totalTime * disG));
        int valB = (int)(sourceB + (currentTime / totalTime * disB));
        
        if(destR < sourceR) {
            valR = Math.max(destR, valR);
        } else {
            valR = Math.min(destR, valR);
        }
        
        if(destG < sourceG) {
            valG = Math.max(destG, valG);
        } else {
            valG = Math.min(destG, valG);
        }
        
        if(destB < sourceB) {
            valB = Math.max(destB, valB);
        } else {
            valB = Math.min(destB, valB);
        }
        return (((valR) << 16) & 0xff0000) | (((valG) << 8) & 0xff00) | (valB & 0xff);
    }
    
    private int getFriction() {
        int time = (int) getCurrentMotionTime();
        int retVal = 0;

        retVal = (int)((Math.abs(initVelocity) * time) - (friction * (((double)time * time) / 2)));
        if (initVelocity < 0) {
            retVal *= -1;
        }
        retVal += (int) sourceValue;
        if(destinationValue > sourceValue) {
            return Math.min(retVal, destinationValue);
        } else {
            return Math.max(retVal, destinationValue);
        }
    }

    private int getRubber() {
        if(isFinished()){
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if(currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        currentTime = Math.min(currentTime, totalTime);
        int p = Math.abs(destinationValue - sourceValue);
        float centerTime = totalTime/2;
        float l = p / (centerTime * centerTime);
        int x;
        int dis =  (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                currentTime * currentTime / 2));
        
        if (sourceValue < destinationValue) {
                x = Math.max(sourceValue, sourceValue + dis);
                x = Math.min(destinationValue, x);
                
        } else {
                x = Math.min(sourceValue, sourceValue - dis);
                x = Math.max(destinationValue, x);
        }
        return x;
    }
    
    /**
     * The number from which we are starting (usually indicating animation start position)
     * 
     * @return the source value
     */
    public int getSourceValue() {
        return sourceValue;
    }

    /**
     * The number to which we will reach when the motion is finished
     *
     * @return the source value
     */
    public int getDestinationValue() {
        return destinationValue;
    }

    /**
     * The number from which we are starting (usually indicating animation start position)
     * 
     * @param sourceValue  the source value
     */
    public void setSourceValue(int sourceValue) {
        this.sourceValue = sourceValue;
    }

    /**
     * The value of System.currentTimemillis() when motion was started
     * 
     * @return the start time
     */
    protected long getStartTime() {
        return startTime;
    }

    /**
     * Returns the animation duration
     *
     * @return animation duration in milliseconds
     */
    public int getDuration() {
        return duration;
    }
}
