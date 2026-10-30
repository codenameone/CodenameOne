/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (c) 2026, Codename One and/or its affiliates:
 * rewritten from the static methods of android.hardware.SensorManager for the
 * Codename One compatibility runtime (com.codename1.util.MathUtil in place of
 * the trigonometry CLDC lacks).
 */
package android.hardware;

import com.codename1.util.MathUtil;

/// The sensor math of Android's `SensorManager`, so a compass computed with
/// `getRotationMatrix` and `getOrientation` reads the same numbers here as on
/// Android. [SensorManager] exposes it under Android's names.
final class SensorMath {

    private SensorMath() {
    }


    /// Android's rotation and inclination matrices from a gravity and a
    /// geomagnetic vector, both in device coordinates. `R` and `I` may be
    /// 9 (3x3) or 16 (4x4) long, row major; either may be null. Answers false
    /// in free fall or when the field is parallel to gravity.
    static boolean getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic) {
        float ax = gravity[0], ay = gravity[1], az = gravity[2];
        final float normsqA = ax * ax + ay * ay + az * az;
        final float g = 9.81f;
        final float freeFallGravitySquared = 0.01f * g * g;
        if (normsqA < freeFallGravitySquared) {
            return false;
        }
        final float ex = geomagnetic[0], ey = geomagnetic[1], ez = geomagnetic[2];
        float hx = ey * az - ez * ay;
        float hy = ez * ax - ex * az;
        float hz = ex * ay - ey * ax;
        final float normH = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (normH < 0.1f) {
            return false;
        }
        final float invH = 1.0f / normH;
        hx *= invH;
        hy *= invH;
        hz *= invH;
        final float invA = 1.0f / (float) Math.sqrt(normsqA);
        ax *= invA;
        ay *= invA;
        az *= invA;
        final float mx = ay * hz - az * hy;
        final float my = az * hx - ax * hz;
        final float mz = ax * hy - ay * hx;
        if (R != null) {
            if (R.length == 9) {
                R[0] = hx; R[1] = hy; R[2] = hz;
                R[3] = mx; R[4] = my; R[5] = mz;
                R[6] = ax; R[7] = ay; R[8] = az;
            } else if (R.length == 16) {
                R[0] = hx; R[1] = hy; R[2] = hz; R[3] = 0;
                R[4] = mx; R[5] = my; R[6] = mz; R[7] = 0;
                R[8] = ax; R[9] = ay; R[10] = az; R[11] = 0;
                R[12] = 0; R[13] = 0; R[14] = 0; R[15] = 1;
            }
        }
        if (I != null) {
            final float invE = 1.0f / (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
            final float c = (ex * mx + ey * my + ez * mz) * invE;
            final float s = (ex * ax + ey * ay + ez * az) * invE;
            if (I.length == 9) {
                I[0] = 1; I[1] = 0; I[2] = 0;
                I[3] = 0; I[4] = c; I[5] = s;
                I[6] = 0; I[7] = -s; I[8] = c;
            } else if (I.length == 16) {
                I[0] = 1; I[1] = 0; I[2] = 0; I[3] = 0;
                I[4] = 0; I[5] = c; I[6] = s; I[7] = 0;
                I[8] = 0; I[9] = -s; I[10] = c; I[11] = 0;
                I[12] = 0; I[13] = 0; I[14] = 0; I[15] = 1;
            }
        }
        return true;
    }

    /// The magnetic dip angle, in radians, of an inclination matrix.
    static float getInclination(float[] I) {
        if (I.length == 9) {
            return (float) MathUtil.atan2(I[5], I[4]);
        }
        return (float) MathUtil.atan2(I[6], I[5]);
    }

    /// Azimuth, pitch and roll in radians from a rotation matrix, as Android
    /// computes them: azimuth in [-pi, pi], 0 at magnetic north.
    static float[] getOrientation(float[] R, float[] values) {
        if (R.length == 9) {
            values[0] = (float) MathUtil.atan2(R[1], R[4]);
            values[1] = (float) MathUtil.asin(-R[7]);
            values[2] = (float) MathUtil.atan2(-R[6], R[8]);
        } else {
            values[0] = (float) MathUtil.atan2(R[1], R[5]);
            values[1] = (float) MathUtil.asin(-R[9]);
            values[2] = (float) MathUtil.atan2(-R[8], R[10]);
        }
        return values;
    }

    /// The rotation matrix of a rotation vector (x, y, z[, w]); w is derived
    /// when the vector has only three components.
    static void getRotationMatrixFromVector(float[] R, float[] rotationVector) {
        float q0;
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];
        if (rotationVector.length >= 4) {
            q0 = rotationVector[3];
        } else {
            q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3;
            q0 = q0 > 0 ? (float) Math.sqrt(q0) : 0;
        }
        float sqq1 = 2 * q1 * q1;
        float sqq2 = 2 * q2 * q2;
        float sqq3 = 2 * q3 * q3;
        float q1q2 = 2 * q1 * q2;
        float q3q0 = 2 * q3 * q0;
        float q1q3 = 2 * q1 * q3;
        float q2q0 = 2 * q2 * q0;
        float q2q3 = 2 * q2 * q3;
        float q1q0 = 2 * q1 * q0;
        if (R.length == 9) {
            R[0] = 1 - sqq2 - sqq3; R[1] = q1q2 - q3q0; R[2] = q1q3 + q2q0;
            R[3] = q1q2 + q3q0; R[4] = 1 - sqq1 - sqq3; R[5] = q2q3 - q1q0;
            R[6] = q1q3 - q2q0; R[7] = q2q3 + q1q0; R[8] = 1 - sqq1 - sqq2;
        } else if (R.length == 16) {
            R[0] = 1 - sqq2 - sqq3; R[1] = q1q2 - q3q0; R[2] = q1q3 + q2q0; R[3] = 0;
            R[4] = q1q2 + q3q0; R[5] = 1 - sqq1 - sqq3; R[6] = q2q3 - q1q0; R[7] = 0;
            R[8] = q1q3 - q2q0; R[9] = q2q3 + q1q0; R[10] = 1 - sqq1 - sqq2; R[11] = 0;
            R[12] = 0; R[13] = 0; R[14] = 0; R[15] = 1;
        }
    }

    /// The unit quaternion (w, x, y, z) of a rotation vector.
    static void getQuaternionFromVector(float[] Q, float[] rv) {
        if (rv.length >= 4) {
            Q[0] = rv[3];
        } else {
            float w = 1 - rv[0] * rv[0] - rv[1] * rv[1] - rv[2] * rv[2];
            Q[0] = w > 0 ? (float) Math.sqrt(w) : 0;
        }
        Q[1] = rv[0];
        Q[2] = rv[1];
        Q[3] = rv[2];
    }

    /// Re-expresses `inR` for a device held so that its `X` axis lies along
    /// world axis `X` and its `Y` along `Y` (the AXIS_* constants), the way
    /// landscape compass and camera code calls it.
    static boolean remapCoordinateSystem(float[] inR, int X, int Y, float[] outR) {
        if (inR == outR) {
            float[] temp = new float[outR.length];
            boolean ok = remap(inR, X, Y, temp);
            if (ok) {
                System.arraycopy(temp, 0, outR, 0, outR.length);
            }
            return ok;
        }
        return remap(inR, X, Y, outR);
    }

    private static boolean remap(float[] inR, int X, int Y, float[] outR) {
        if ((X & 0x7C) != 0 || (Y & 0x7C) != 0) {
            return false;
        }
        if ((X & 0x3) == 0 || (Y & 0x3) == 0) {
            return false;
        }
        if ((X & 0x3) == (Y & 0x3)) {
            return false;
        }
        // Z is X cross Y; its sign follows from the axes and their signs.
        int Z = X ^ Y;
        final int x = (X & 0x3) - 1;
        final int y = (Y & 0x3) - 1;
        final int z = (Z & 0x3) - 1;
        final int axisY = (z + 1) % 3;
        final int axisZ = (z + 2) % 3;
        if (((x ^ axisY) | (y ^ axisZ)) != 0) {
            Z ^= 0x80;
        }
        final boolean sx = (X >= 0x80);
        final boolean sy = (Y >= 0x80);
        final boolean sz = (Z >= 0x80);
        final int rowLength = (outR.length == 16) ? 4 : 3;
        for (int j = 0; j < 3; j++) {
            final int offset = j * rowLength;
            for (int i = 0; i < 3; i++) {
                if (x == i) {
                    outR[offset + i] = sx ? -inR[offset + 0] : inR[offset + 0];
                }
                if (y == i) {
                    outR[offset + i] = sy ? -inR[offset + 1] : inR[offset + 1];
                }
                if (z == i) {
                    outR[offset + i] = sz ? -inR[offset + 2] : inR[offset + 2];
                }
            }
        }
        if (outR.length == 16) {
            outR[3] = outR[7] = outR[11] = outR[12] = outR[13] = outR[14] = 0;
            outR[15] = 1;
        }
        return true;
    }

    /// The angle change between two rotation matrices, as Android defines it.
    static void getAngleChange(float[] angleChange, float[] R, float[] prevR) {
        float rd1, rd4, rd6, rd7, rd8;
        float ri0, ri1, ri2, ri3, ri4, ri5, ri6, ri7, ri8;
        float pri0, pri1, pri2, pri3, pri4, pri5, pri6, pri7, pri8;
        if (R.length == 9) {
            ri0 = R[0]; ri1 = R[1]; ri2 = R[2]; ri3 = R[3]; ri4 = R[4];
            ri5 = R[5]; ri6 = R[6]; ri7 = R[7]; ri8 = R[8];
        } else {
            ri0 = R[0]; ri1 = R[1]; ri2 = R[2]; ri3 = R[4]; ri4 = R[5];
            ri5 = R[6]; ri6 = R[8]; ri7 = R[9]; ri8 = R[10];
        }
        if (prevR.length == 9) {
            pri0 = prevR[0]; pri1 = prevR[1]; pri2 = prevR[2]; pri3 = prevR[3]; pri4 = prevR[4];
            pri5 = prevR[5]; pri6 = prevR[6]; pri7 = prevR[7]; pri8 = prevR[8];
        } else {
            pri0 = prevR[0]; pri1 = prevR[1]; pri2 = prevR[2]; pri3 = prevR[4]; pri4 = prevR[5];
            pri5 = prevR[6]; pri6 = prevR[8]; pri7 = prevR[9]; pri8 = prevR[10];
        }
        rd1 = pri0 * ri1 + pri3 * ri4 + pri6 * ri7;
        rd4 = pri1 * ri1 + pri4 * ri4 + pri7 * ri7;
        rd6 = pri2 * ri0 + pri5 * ri3 + pri8 * ri6;
        rd7 = pri2 * ri1 + pri5 * ri4 + pri8 * ri7;
        rd8 = pri2 * ri2 + pri5 * ri5 + pri8 * ri8;
        angleChange[0] = (float) MathUtil.atan2(rd1, rd4);
        angleChange[1] = (float) MathUtil.asin(-rd7);
        angleChange[2] = (float) MathUtil.atan2(-rd6, rd8);
    }

    /// Altitude in meters from the pressure at sea level and here, in hPa.
    static float getAltitude(float p0, float p) {
        final float coef = 1.0f / 5.255f;
        return 44330.0f * (1.0f - (float) MathUtil.pow(p / p0, coef));
    }
}
