/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.hardware;

import android.os.Handler;
import com.codename1.sensors.MotionEvent;
import com.codename1.sensors.MotionSensor;
import com.codename1.sensors.MotionSensorListener;
import com.codename1.sensors.MotionSensorManager;

import java.util.ArrayList;
import java.util.List;

/// Android's sensor service over `com.codename1.sensors.MotionSensorManager`.
///
/// The raw sensors map one to one and in the same units (m/s^2, rad/s,
/// microtesla): accelerometer, gyroscope, magnetic field, and Codename One's
/// gravity and linear acceleration. The fused ones Android computes from them
/// are computed here from Codename One's gravity and magnetometer with the
/// same matrix math [#getRotationMatrix] performs:
///
/// - `TYPE_ROTATION_VECTOR` and `TYPE_GEOMAGNETIC_ROTATION_VECTOR`, the unit
///   quaternion of that matrix (x, y, z, w, and -1 for the unknown heading
///   accuracy);
/// - the deprecated `TYPE_ORIENTATION`, in degrees, azimuth in [0, 360).
///
/// Both reference magnetic north, as Android's do; add
/// `GeomagneticField.getDeclination()` for true north. Android also fuses the
/// gyroscope into its rotation vector, so this one is noisier between two
/// magnetometer readings -- low-pass the azimuth for a compass needle.
///
/// A type Codename One cannot feed (light, pressure, proximity, step counter,
/// game rotation vector) is absent: [#getDefaultSensor(int)] answers null for
/// it, which Android code already handles for a device without the hardware.
///
/// Readings arrive on the main thread (Codename One's EDT), or on the looper
/// of the `Handler` a registration names. The sampling period applies to every
/// sensor at once, because Codename One samples on one interval: the shortest
/// period any registration asked for.
public class SensorManager {

    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_UI = 2;
    public static final int SENSOR_DELAY_NORMAL = 3;

    public static final int SENSOR_STATUS_NO_CONTACT = -1;
    public static final int SENSOR_STATUS_UNRELIABLE = 0;
    public static final int SENSOR_STATUS_ACCURACY_LOW = 1;
    public static final int SENSOR_STATUS_ACCURACY_MEDIUM = 2;
    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;

    public static final float STANDARD_GRAVITY = 9.80665f;
    public static final float GRAVITY_EARTH = 9.80665f;
    public static final float MAGNETIC_FIELD_EARTH_MAX = 60.0f;
    public static final float MAGNETIC_FIELD_EARTH_MIN = 30.0f;
    public static final float PRESSURE_STANDARD_ATMOSPHERE = 1013.25f;

    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;
    public static final int AXIS_Z = 3;
    public static final int AXIS_MINUS_X = AXIS_X | 0x80;
    public static final int AXIS_MINUS_Y = AXIS_Y | 0x80;
    public static final int AXIS_MINUS_Z = AXIS_Z | 0x80;

    /// The Codename One manager to use instead of the platform's; set by tests.
    static MotionSensorManager override;

    private static SensorManager instance;

    private final Source[] sources = new Source[7];
    private final List<Registration> registrations = new ArrayList<Registration>();
    private final float[] matrix = new float[9];
    private final float[] scratch = new float[3];

    public static SensorManager getInstance() {
        if (instance == null) {
            instance = new SensorManager();
        }
        return instance;
    }

    SensorManager() {
    }

    static MotionSensorManager platform() {
        return override != null ? override : MotionSensorManager.getInstance();
    }

    // -------------------------------------------------------------- sensors

    /// The Codename One sensor types an Android type is computed from.
    static int[] sourcesOf(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return new int[] {MotionSensorManager.TYPE_ACCELEROMETER};
            case Sensor.TYPE_MAGNETIC_FIELD:
                return new int[] {MotionSensorManager.TYPE_MAGNETOMETER};
            case Sensor.TYPE_GYROSCOPE:
                return new int[] {MotionSensorManager.TYPE_GYROSCOPE};
            case Sensor.TYPE_GRAVITY:
                return new int[] {MotionSensorManager.TYPE_GRAVITY};
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return new int[] {MotionSensorManager.TYPE_LINEAR_ACCELERATION};
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
            case Sensor.TYPE_ORIENTATION:
                return new int[] {MotionSensorManager.TYPE_GRAVITY, MotionSensorManager.TYPE_MAGNETOMETER};
            default:
                return null;
        }
    }

    private static Sensor describe(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return new Sensor(type, "Accelerometer", Sensor.STRING_TYPE_ACCELEROMETER, 4 * GRAVITY_EARTH, 0.01f);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return new Sensor(type, "Magnetic field", Sensor.STRING_TYPE_MAGNETIC_FIELD, 2000f, 0.1f);
            case Sensor.TYPE_GYROSCOPE:
                return new Sensor(type, "Gyroscope", Sensor.STRING_TYPE_GYROSCOPE, 35f, 0.001f);
            case Sensor.TYPE_GRAVITY:
                return new Sensor(type, "Gravity", Sensor.STRING_TYPE_GRAVITY, GRAVITY_EARTH, 0.01f);
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return new Sensor(type, "Linear acceleration", Sensor.STRING_TYPE_LINEAR_ACCELERATION,
                        4 * GRAVITY_EARTH, 0.01f);
            case Sensor.TYPE_ROTATION_VECTOR:
                return new Sensor(type, "Rotation vector", Sensor.STRING_TYPE_ROTATION_VECTOR, 1f, 0.0001f);
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return new Sensor(type, "Geomagnetic rotation vector",
                        Sensor.STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR, 1f, 0.0001f);
            case Sensor.TYPE_ORIENTATION:
                return new Sensor(type, "Orientation", Sensor.STRING_TYPE_ORIENTATION, 360f, 0.1f);
            default:
                return null;
        }
    }

    private static final int[] ALL_TYPES = {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_ORIENTATION,
        Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
    };

    private final Sensor[] described = new Sensor[64];

    public Sensor getDefaultSensor(int type) {
        int[] needs = sourcesOf(type);
        if (needs == null) {
            return null;
        }
        MotionSensorManager m = platform();
        for (int i = 0; i < needs.length; i++) {
            if (!m.isSensorSupported(needs[i])) {
                return null;
            }
        }
        if (described[type] == null) {
            described[type] = describe(type);
        }
        return described[type];
    }

    public Sensor getDefaultSensor(int type, boolean wakeUp) {
        return wakeUp ? null : getDefaultSensor(type);
    }

    public List<Sensor> getSensorList(int type) {
        List<Sensor> out = new ArrayList<Sensor>();
        for (int i = 0; i < ALL_TYPES.length; i++) {
            if (type == Sensor.TYPE_ALL || type == ALL_TYPES[i]) {
                Sensor s = getDefaultSensor(ALL_TYPES[i]);
                if (s != null) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    public List<Sensor> getDynamicSensorList(int type) {
        return new ArrayList<Sensor>();
    }

    public boolean isDynamicSensorDiscoverySupported() {
        return false;
    }

    // -------------------------------------------------------- registration

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
        return registerListener(listener, sensor, samplingPeriodUs, (Handler) null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs,
            int maxReportLatencyUs) {
        return registerListener(listener, sensor, samplingPeriodUs, (Handler) null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs,
            int maxReportLatencyUs, Handler handler) {
        return registerListener(listener, sensor, samplingPeriodUs, handler);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs,
            Handler handler) {
        if (listener == null || sensor == null) {
            return false;
        }
        for (int i = 0; i < registrations.size(); i++) {
            Registration r = registrations.get(i);
            if (r.listener == listener && r.sensor == sensor) {
                // Android keeps the first registration of the pair.
                return true;
            }
        }
        int[] needs = sourcesOf(sensor.getType());
        if (needs == null) {
            return false;
        }
        registrations.add(new Registration(listener, sensor, periodUs(samplingPeriodUs), handler));
        for (int i = 0; i < needs.length; i++) {
            acquire(needs[i]);
        }
        retune();
        return true;
    }

    public void unregisterListener(SensorEventListener listener) {
        unregisterListener(listener, null);
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor) {
        for (int i = registrations.size() - 1; i >= 0; i--) {
            Registration r = registrations.get(i);
            if (r.listener == listener && (sensor == null || r.sensor == sensor)) {
                registrations.remove(i);
                int[] needs = sourcesOf(r.sensor.getType());
                for (int j = 0; j < needs.length; j++) {
                    release(needs[j]);
                }
            }
        }
        retune();
    }

    public boolean flush(SensorEventListener listener) {
        return false;
    }

    /// Android's SENSOR_DELAY_* constants or a period in microseconds.
    static int periodUs(int samplingPeriodUs) {
        switch (samplingPeriodUs) {
            case SENSOR_DELAY_FASTEST:
                return 0;
            case SENSOR_DELAY_GAME:
                return 20000;
            case SENSOR_DELAY_UI:
                return 66667;
            case SENSOR_DELAY_NORMAL:
                return 200000;
            default:
                return Math.max(0, samplingPeriodUs);
        }
    }

    private void retune() {
        if (registrations.isEmpty()) {
            return;
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < registrations.size(); i++) {
            min = Math.min(min, registrations.get(i).periodUs);
        }
        // Codename One samples in milliseconds; 10ms is as fast as it is asked to go.
        platform().setSamplingInterval(Math.max(10, min / 1000));
    }

    private void acquire(int cn1Type) {
        Source s = sources[cn1Type];
        if (s == null) {
            s = new Source(cn1Type);
            sources[cn1Type] = s;
        }
        if (s.users++ == 0) {
            s.start();
        }
    }

    private void release(int cn1Type) {
        Source s = sources[cn1Type];
        if (s != null && s.users > 0 && --s.users == 0) {
            s.stop();
        }
    }

    // ------------------------------------------------------------ delivery

    /// A Codename One reading arrived; every registration computed from that
    /// sensor gets an event.
    void feed(int cn1Type, float x, float y, float z) {
        Source src = sources[cn1Type];
        if (src == null) {
            return;
        }
        src.values[0] = x;
        src.values[1] = y;
        src.values[2] = z;
        src.has = true;
        long now = android.os.SystemClock.elapsedRealtimeNanos();
        Registration[] snapshot = registrations.toArray(new Registration[registrations.size()]);
        for (int i = 0; i < snapshot.length; i++) {
            Registration r = snapshot[i];
            int[] needs = sourcesOf(r.sensor.getType());
            if (!contains(needs, cn1Type)) {
                continue;
            }
            if (r.lastNanos != 0 && now - r.lastNanos < r.periodUs * 1000L) {
                continue;
            }
            if (!compute(r.sensor.getType(), r.event.values)) {
                continue;
            }
            r.lastNanos = now;
            r.event.timestamp = now;
            r.deliver();
        }
    }

    private static boolean contains(int[] a, int v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == v) {
                return true;
            }
        }
        return false;
    }

    /// The values of one Android event, or false while a source has not
    /// reported yet.
    private boolean compute(int type, float[] out) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return copy(MotionSensorManager.TYPE_ACCELEROMETER, out);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return copy(MotionSensorManager.TYPE_MAGNETOMETER, out);
            case Sensor.TYPE_GYROSCOPE:
                return copy(MotionSensorManager.TYPE_GYROSCOPE, out);
            case Sensor.TYPE_GRAVITY:
                return copy(MotionSensorManager.TYPE_GRAVITY, out);
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return copy(MotionSensorManager.TYPE_LINEAR_ACCELERATION, out);
            default:
                break;
        }
        Source g = sources[MotionSensorManager.TYPE_GRAVITY];
        Source m = sources[MotionSensorManager.TYPE_MAGNETOMETER];
        if (g == null || m == null || !g.has || !m.has) {
            return false;
        }
        if (!getRotationMatrix(matrix, null, g.values, m.values)) {
            // Free fall, or next to a magnet: Android delivers nothing either.
            return false;
        }
        if (type == Sensor.TYPE_ORIENTATION) {
            getOrientation(matrix, scratch);
            float azimuth = (float) Math.toDegrees(scratch[0]);
            out[0] = azimuth < 0 ? azimuth + 360f : azimuth;
            out[1] = (float) Math.toDegrees(scratch[1]);
            out[2] = (float) Math.toDegrees(scratch[2]);
            return true;
        }
        quaternionFromMatrix(matrix, out);
        return true;
    }

    private boolean copy(int cn1Type, float[] out) {
        Source s = sources[cn1Type];
        if (s == null || !s.has) {
            return false;
        }
        out[0] = s.values[0];
        out[1] = s.values[1];
        out[2] = s.values[2];
        return true;
    }

    /// The rotation vector of a device-to-world rotation matrix: the unit
    /// quaternion's x, y, z, its w (never negative, as Android's), and -1
    /// for the heading accuracy nobody estimated. Shepperd's method, so it
    /// stays accurate near a half turn.
    static void quaternionFromMatrix(float[] r, float[] out) {
        double m00 = r[0], m01 = r[1], m02 = r[2];
        double m10 = r[3], m11 = r[4], m12 = r[5];
        double m20 = r[6], m21 = r[7], m22 = r[8];
        double trace = m00 + m11 + m22;
        double w, x, y, z;
        if (trace > 0) {
            double s = Math.sqrt(trace + 1.0) * 2;
            w = 0.25 * s;
            x = (m21 - m12) / s;
            y = (m02 - m20) / s;
            z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1.0 + m00 - m11 - m22) * 2;
            w = (m21 - m12) / s;
            x = 0.25 * s;
            y = (m01 + m10) / s;
            z = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0 + m11 - m00 - m22) * 2;
            w = (m02 - m20) / s;
            x = (m01 + m10) / s;
            y = 0.25 * s;
            z = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1.0 + m22 - m00 - m11) * 2;
            w = (m10 - m01) / s;
            x = (m02 + m20) / s;
            y = (m12 + m21) / s;
            z = 0.25 * s;
        }
        if (w < 0) {
            w = -w;
            x = -x;
            y = -y;
            z = -z;
        }
        out[0] = (float) x;
        out[1] = (float) y;
        out[2] = (float) z;
        if (out.length > 3) {
            out[3] = (float) w;
        }
        if (out.length > 4) {
            out[4] = -1f;
        }
    }

    // ----------------------------------------------------------- static math

    /// Android's rotation and inclination matrices from a gravity and a
    /// geomagnetic vector, both in device coordinates. `R` and `I` may be
    /// 9 (3x3) or 16 (4x4) long, row major; either may be null. Answers false
    /// in free fall or when the field is parallel to gravity.
    public static boolean getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic) {
        return SensorMath.getRotationMatrix(R, I, gravity, geomagnetic);
    }

    /// The magnetic dip angle, in radians, of an inclination matrix.
    public static float getInclination(float[] I) {
        return SensorMath.getInclination(I);
    }

    /// Azimuth, pitch and roll in radians from a rotation matrix: azimuth in
    /// [-pi, pi], 0 at magnetic north.
    public static float[] getOrientation(float[] R, float[] values) {
        return SensorMath.getOrientation(R, values);
    }

    /// The rotation matrix of a rotation vector (x, y, z[, w]).
    public static void getRotationMatrixFromVector(float[] R, float[] rotationVector) {
        SensorMath.getRotationMatrixFromVector(R, rotationVector);
    }

    /// The unit quaternion (w, x, y, z) of a rotation vector.
    public static void getQuaternionFromVector(float[] Q, float[] rv) {
        SensorMath.getQuaternionFromVector(Q, rv);
    }

    /// Re-expresses `inR` for a device whose `X` and `Y` axes lie along the
    /// given world axes (the AXIS_* constants).
    public static boolean remapCoordinateSystem(float[] inR, int X, int Y, float[] outR) {
        return SensorMath.remapCoordinateSystem(inR, X, Y, outR);
    }

    /// The angle change between two rotation matrices.
    public static void getAngleChange(float[] angleChange, float[] R, float[] prevR) {
        SensorMath.getAngleChange(angleChange, R, prevR);
    }

    /// Altitude in meters from the pressure at sea level and here, in hPa.
    public static float getAltitude(float p0, float p) {
        return SensorMath.getAltitude(p0, p);
    }

    // ------------------------------------------------------------- helpers

    /// One Codename One sensor, listened to while any registration needs it.
    final class Source {
        final int type;
        final float[] values = new float[3];
        boolean has;
        int users;
        private MotionSensor sensor;
        private final MotionSensorListener listener = new MotionSensorListener() {
            @Override
            public void motionReceived(MotionEvent evt) {
                feed(type, evt.getX(), evt.getY(), evt.getZ());
            }
        };

        Source(int type) {
            this.type = type;
        }

        void start() {
            has = false;
            if (override == null || sensor == null) {
                sensor = platform().getSensor(type);
            }
            if (sensor != null) {
                sensor.addListener(listener);
            }
        }

        void stop() {
            if (sensor != null) {
                sensor.removeListener(listener);
            }
            has = false;
        }
    }

    final class Registration {
        final SensorEventListener listener;
        final Sensor sensor;
        final int periodUs;
        final Handler handler;
        final SensorEvent event;
        long lastNanos;
        private boolean accuracyReported;

        Registration(SensorEventListener listener, Sensor sensor, int periodUs, Handler handler) {
            this.listener = listener;
            this.sensor = sensor;
            this.periodUs = periodUs;
            this.handler = handler;
            int size = sensor.getType() == Sensor.TYPE_ROTATION_VECTOR
                    || sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ? 5 : 3;
            event = new SensorEvent(size);
            event.sensor = sensor;
            event.accuracy = SENSOR_STATUS_ACCURACY_HIGH;
        }

        void deliver() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (!registrations.contains(Registration.this)) {
                        return;
                    }
                    if (!accuracyReported) {
                        accuracyReported = true;
                        listener.onAccuracyChanged(sensor, event.accuracy);
                    }
                    listener.onSensorChanged(event);
                }
            };
            if (handler != null && !handler.getLooper().isCurrentThread()) {
                handler.post(r);
            } else {
                r.run();
            }
        }
    }
}
