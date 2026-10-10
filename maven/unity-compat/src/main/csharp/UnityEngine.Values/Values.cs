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
// UnityEngine value types, implemented from the public scripting reference.
// Not derived from, and written without reading, any Unity source or binary.
using System;

namespace UnityEngine
{
    public struct Vector2
    {
        public float x;
        public float y;

        public Vector2(float x, float y)
        {
            this.x = x;
            this.y = y;
        }

        public static Vector2 zero { get { return new Vector2(0f, 0f); } }
        public static Vector2 one { get { return new Vector2(1f, 1f); } }
        public static Vector2 up { get { return new Vector2(0f, 1f); } }
        public static Vector2 down { get { return new Vector2(0f, -1f); } }
        public static Vector2 left { get { return new Vector2(-1f, 0f); } }
        public static Vector2 right { get { return new Vector2(1f, 0f); } }

        public float sqrMagnitude { get { return x * x + y * y; } }
        public float magnitude { get { return (float)Math.Sqrt(x * x + y * y); } }

        public Vector2 normalized
        {
            get
            {
                float m = magnitude;
                return m > 1e-5f ? new Vector2(x / m, y / m) : new Vector2(0f, 0f);
            }
        }

        public void Set(float newX, float newY)
        {
            x = newX;
            y = newY;
        }

        public void Normalize()
        {
            float m = magnitude;
            if (m > 1e-5f)
            {
                x = x / m;
                y = y / m;
            }
            else
            {
                x = 0f;
                y = 0f;
            }
        }

        public static float Dot(Vector2 a, Vector2 b) { return a.x * b.x + a.y * b.y; }

        public static Vector2 Perpendicular(Vector2 v) { return new Vector2(-v.y, v.x); }

        public static Vector2 Scale(Vector2 a, Vector2 b) { return new Vector2(a.x * b.x, a.y * b.y); }

        public static Vector2 LerpUnclamped(Vector2 a, Vector2 b, float t)
        {
            return new Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        }

        public static Vector2 ClampMagnitude(Vector2 v, float maxLength)
        {
            float sqr = v.x * v.x + v.y * v.y;
            if (sqr > maxLength * maxLength)
            {
                float m = (float)Math.Sqrt(sqr);
                return new Vector2(v.x / m * maxLength, v.y / m * maxLength);
            }
            return v;
        }

        public static Vector2 MoveTowards(Vector2 current, Vector2 target, float maxDistanceDelta)
        {
            float dx = target.x - current.x;
            float dy = target.y - current.y;
            float sqr = dx * dx + dy * dy;
            if (sqr == 0f || (maxDistanceDelta >= 0f && sqr <= maxDistanceDelta * maxDistanceDelta))
            {
                return target;
            }
            float d = (float)Math.Sqrt(sqr);
            return new Vector2(current.x + dx / d * maxDistanceDelta, current.y + dy / d * maxDistanceDelta);
        }

        // The unsigned angle in degrees between two directions, 0 to 180.
        public static float Angle(Vector2 from, Vector2 to)
        {
            float denominator = (float)Math.Sqrt(from.sqrMagnitude * to.sqrMagnitude);
            if (denominator < 1e-15f)
            {
                return 0f;
            }
            float dot = Mathf.Clamp(Dot(from, to) / denominator, -1f, 1f);
            return (float)Math.Acos(dot) * Mathf.Rad2Deg;
        }

        // Positive when `to` is counter-clockwise of `from`.
        public static float SignedAngle(Vector2 from, Vector2 to)
        {
            float unsigned = Angle(from, to);
            float sign = Mathf.Sign(from.x * to.y - from.y * to.x);
            return unsigned * sign;
        }

        public static Vector2 Reflect(Vector2 inDirection, Vector2 inNormal)
        {
            float factor = -2f * Dot(inNormal, inDirection);
            return new Vector2(factor * inNormal.x + inDirection.x, factor * inNormal.y + inDirection.y);
        }

        public static float Distance(Vector2 a, Vector2 b)
        {
            float dx = a.x - b.x;
            float dy = a.y - b.y;
            return (float)Math.Sqrt(dx * dx + dy * dy);
        }

        public static Vector2 Lerp(Vector2 a, Vector2 b, float t)
        {
            t = Mathf.Clamp01(t);
            return new Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        }

        public static Vector2 operator +(Vector2 a, Vector2 b) { return new Vector2(a.x + b.x, a.y + b.y); }
        public static Vector2 operator -(Vector2 a, Vector2 b) { return new Vector2(a.x - b.x, a.y - b.y); }
        public static Vector2 operator -(Vector2 a) { return new Vector2(-a.x, -a.y); }
        public static Vector2 operator *(Vector2 a, float d) { return new Vector2(a.x * d, a.y * d); }
        public static Vector2 operator *(float d, Vector2 a) { return new Vector2(a.x * d, a.y * d); }
        public static Vector2 operator /(Vector2 a, float d) { return new Vector2(a.x / d, a.y / d); }
        public static Vector2 operator *(Vector2 a, Vector2 b) { return new Vector2(a.x * b.x, a.y * b.y); }

        public static bool operator ==(Vector2 a, Vector2 b)
        {
            float dx = a.x - b.x;
            float dy = a.y - b.y;
            return dx * dx + dy * dy < 9.99999944e-11f;
        }

        public static bool operator !=(Vector2 a, Vector2 b) { return !(a == b); }

        public static implicit operator Vector2(Vector3 v) { return new Vector2(v.x, v.y); }
        public static implicit operator Vector3(Vector2 v) { return new Vector3(v.x, v.y, 0f); }

        public override bool Equals(object other)
        {
            return other is Vector2 && ((Vector2)other).x == x && ((Vector2)other).y == y;
        }

        public override int GetHashCode() { return x.GetHashCode() ^ (y.GetHashCode() << 2); }

        // As Unity writes one: two decimals.
        public override string ToString()
        {
            return "(" + ValueText.Fixed(x, 2) + ", " + ValueText.Fixed(y, 2) + ")";
        }
    }

    public struct Vector3
    {
        public float x;
        public float y;
        public float z;

        public Vector3(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector3(float x, float y)
        {
            this.x = x;
            this.y = y;
            this.z = 0f;
        }

        public static Vector3 zero { get { return new Vector3(0f, 0f, 0f); } }
        public static Vector3 one { get { return new Vector3(1f, 1f, 1f); } }
        public static Vector3 up { get { return new Vector3(0f, 1f, 0f); } }
        public static Vector3 right { get { return new Vector3(1f, 0f, 0f); } }
        public static Vector3 forward { get { return new Vector3(0f, 0f, 1f); } }
        public static Vector3 down { get { return new Vector3(0f, -1f, 0f); } }
        public static Vector3 left { get { return new Vector3(-1f, 0f, 0f); } }
        public static Vector3 back { get { return new Vector3(0f, 0f, -1f); } }

        public float sqrMagnitude { get { return x * x + y * y + z * z; } }
        public float magnitude { get { return (float)Math.Sqrt(x * x + y * y + z * z); } }

        public Vector3 normalized
        {
            get
            {
                float m = magnitude;
                return m > 1e-5f ? new Vector3(x / m, y / m, z / m) : new Vector3(0f, 0f, 0f);
            }
        }

        public void Normalize()
        {
            float m = magnitude;
            if (m > 1e-5f)
            {
                x = x / m;
                y = y / m;
                z = z / m;
            }
            else
            {
                x = 0f;
                y = 0f;
                z = 0f;
            }
        }

        public void Set(float newX, float newY, float newZ)
        {
            x = newX;
            y = newY;
            z = newZ;
        }

        public static float Dot(Vector3 a, Vector3 b) { return a.x * b.x + a.y * b.y + a.z * b.z; }

        public static Vector3 Normalize(Vector3 value) { return value.normalized; }

        public static float Magnitude(Vector3 vector) { return vector.magnitude; }

        public static float SqrMagnitude(Vector3 vector) { return vector.sqrMagnitude; }

        public static Vector3 Cross(Vector3 a, Vector3 b)
        {
            return new Vector3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
        }

        public static float Distance(Vector3 a, Vector3 b)
        {
            float dx = a.x - b.x;
            float dy = a.y - b.y;
            float dz = a.z - b.z;
            return (float)Math.Sqrt(dx * dx + dy * dy + dz * dz);
        }

        public static Vector3 Lerp(Vector3 a, Vector3 b, float t)
        {
            t = Mathf.Clamp01(t);
            return new Vector3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        }

        public static Vector3 Scale(Vector3 a, Vector3 b) { return new Vector3(a.x * b.x, a.y * b.y, a.z * b.z); }

        public static Vector3 MoveTowards(Vector3 current, Vector3 target, float maxDistanceDelta)
        {
            float dx = target.x - current.x;
            float dy = target.y - current.y;
            float dz = target.z - current.z;
            float sqr = dx * dx + dy * dy + dz * dz;
            if (sqr == 0f || (maxDistanceDelta >= 0f && sqr <= maxDistanceDelta * maxDistanceDelta))
            {
                return target;
            }
            float d = (float)Math.Sqrt(sqr);
            return new Vector3(current.x + dx / d * maxDistanceDelta, current.y + dy / d * maxDistanceDelta,
                current.z + dz / d * maxDistanceDelta);
        }

        public static Vector3 operator +(Vector3 a, Vector3 b) { return new Vector3(a.x + b.x, a.y + b.y, a.z + b.z); }
        public static Vector3 operator -(Vector3 a, Vector3 b) { return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z); }
        public static Vector3 operator -(Vector3 a) { return new Vector3(-a.x, -a.y, -a.z); }
        public static Vector3 operator *(Vector3 a, float d) { return new Vector3(a.x * d, a.y * d, a.z * d); }
        public static Vector3 operator *(float d, Vector3 a) { return new Vector3(a.x * d, a.y * d, a.z * d); }
        public static Vector3 operator /(Vector3 a, float d) { return new Vector3(a.x / d, a.y / d, a.z / d); }

        public static bool operator ==(Vector3 a, Vector3 b)
        {
            float dx = a.x - b.x;
            float dy = a.y - b.y;
            float dz = a.z - b.z;
            return dx * dx + dy * dy + dz * dz < 9.99999944e-11f;
        }

        public static bool operator !=(Vector3 a, Vector3 b) { return !(a == b); }

        public override bool Equals(object other)
        {
            return other is Vector3 && ((Vector3)other).x == x && ((Vector3)other).y == y && ((Vector3)other).z == z;
        }

        public override int GetHashCode() { return x.GetHashCode() ^ (y.GetHashCode() << 2) ^ (z.GetHashCode() >> 2); }

        // As Unity writes one: two decimals.
        public override string ToString()
        {
            return "(" + ValueText.Fixed(x, 2) + ", " + ValueText.Fixed(y, 2) + ", " + ValueText.Fixed(z, 2) + ")";
        }
    }

    // A rotation. Unity composes Euler angles z first, then x, then y, and
    // turns counter-clockwise about z when seen from the camera of a 2D scene.
    public struct Quaternion
    {
        public float x;
        public float y;
        public float z;
        public float w;

        public Quaternion(float x, float y, float z, float w)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public static Quaternion identity { get { return new Quaternion(0f, 0f, 0f, 1f); } }

        public static Quaternion Euler(float x, float y, float z)
        {
            double hx = x * (Math.PI / 360.0);
            double hy = y * (Math.PI / 360.0);
            double hz = z * (Math.PI / 360.0);
            float sx = (float)Math.Sin(hx);
            float cx = (float)Math.Cos(hx);
            float sy = (float)Math.Sin(hy);
            float cy = (float)Math.Cos(hy);
            float sz = (float)Math.Sin(hz);
            float cz = (float)Math.Cos(hz);
            // qy * qx * qz, each product stored before it is summed.
            float a = cy * sx;
            float b = sy * cx;
            float c = cy * cx;
            float d = sy * sx;
            return new Quaternion(a * cz + b * sz, b * cz - a * sz, c * sz - d * cz, c * cz + d * sz);
        }

        public static Quaternion Euler(Vector3 euler) { return Euler(euler.x, euler.y, euler.z); }

        public static Quaternion AngleAxis(float angle, Vector3 axis)
        {
            float m = axis.magnitude;
            if (m < 1e-6f)
            {
                return identity;
            }
            double half = angle * (Math.PI / 360.0);
            float s = (float)Math.Sin(half) / m;
            return new Quaternion(axis.x * s, axis.y * s, axis.z * s, (float)Math.Cos(half));
        }

        public static Quaternion Inverse(Quaternion q)
        {
            float n = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w;
            if (n < 1e-12f)
            {
                return identity;
            }
            return new Quaternion(-q.x / n, -q.y / n, -q.z / n, q.w / n);
        }

        public static float Dot(Quaternion a, Quaternion b) { return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w; }

        public Quaternion normalized
        {
            get
            {
                float m = (float)Math.Sqrt(x * x + y * y + z * z + w * w);
                return m < 1e-6f ? identity : new Quaternion(x / m, y / m, z / m, w / m);
            }
        }

        // Degrees about x, y and z, each from 0 up to 360, such that
        // Euler(eulerAngles) is this rotation again.
        public Vector3 eulerAngles
        {
            get
            {
                // The sine of the pitch: how far the rotated z axis leans
                // out of the horizontal plane.
                float sinPitch = 2f * (w * x - y * z);
                float ex;
                float ey;
                float ez;
                if (sinPitch > 0.99999f || sinPitch < -0.99999f)
                {
                    // Looking straight up or down, yaw and roll turn about
                    // the same axis; all of it is reported as yaw.
                    ex = sinPitch > 0f ? 90f : -90f;
                    ey = (float)Math.Atan2(2f * (w * y - x * z), 1f - 2f * (y * y + z * z)) * Mathf.Rad2Deg;
                    ez = 0f;
                }
                else
                {
                    ex = (float)Math.Asin(sinPitch) * Mathf.Rad2Deg;
                    ey = (float)Math.Atan2(2f * (x * z + w * y), 1f - 2f * (x * x + y * y)) * Mathf.Rad2Deg;
                    ez = (float)Math.Atan2(2f * (x * y + w * z), 1f - 2f * (x * x + z * z)) * Mathf.Rad2Deg;
                }
                return new Vector3(Positive(ex), Positive(ey), Positive(ez));
            }
        }

        private static float Positive(float degrees)
        {
            if (degrees < -0.0001f)
            {
                return degrees + 360f;
            }
            return degrees < 0f ? 0f : degrees;
        }

        public static Quaternion operator *(Quaternion a, Quaternion b)
        {
            return new Quaternion(
                a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
                a.w * b.y + a.y * b.w + a.z * b.x - a.x * b.z,
                a.w * b.z + a.z * b.w + a.x * b.y - a.y * b.x,
                a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z);
        }

        public static Vector3 operator *(Quaternion q, Vector3 v)
        {
            float x2 = q.x * 2f;
            float y2 = q.y * 2f;
            float z2 = q.z * 2f;
            float xx = q.x * x2;
            float yy = q.y * y2;
            float zz = q.z * z2;
            float xy = q.x * y2;
            float xz = q.x * z2;
            float yz = q.y * z2;
            float wx = q.w * x2;
            float wy = q.w * y2;
            float wz = q.w * z2;
            return new Vector3(
                (1f - (yy + zz)) * v.x + (xy - wz) * v.y + (xz + wy) * v.z,
                (xy + wz) * v.x + (1f - (xx + zz)) * v.y + (yz - wx) * v.z,
                (xz - wy) * v.x + (yz + wx) * v.y + (1f - (xx + yy)) * v.z);
        }

        public static bool operator ==(Quaternion a, Quaternion b) { return Dot(a, b) > 0.999999f; }
        public static bool operator !=(Quaternion a, Quaternion b) { return !(a == b); }

        public override bool Equals(object other)
        {
            return other is Quaternion && ((Quaternion)other).x == x && ((Quaternion)other).y == y
                && ((Quaternion)other).z == z && ((Quaternion)other).w == w;
        }

        public override int GetHashCode()
        {
            return x.GetHashCode() ^ (y.GetHashCode() << 2) ^ (z.GetHashCode() >> 2) ^ (w.GetHashCode() >> 1);
        }
    }

    // One point where two colliders touch.
    public struct ContactPoint2D
    {
        public Vector2 m_Point;
        public Vector2 m_Normal;

        public Vector2 point { get { return m_Point; } }
        public Vector2 normal { get { return m_Normal; } }
    }

    // An axis-aligned box given by its centre and half its size.
    public struct Bounds
    {
        public Vector3 m_Center;
        public Vector3 m_Extents;

        public Bounds(Vector3 center, Vector3 size)
        {
            m_Center = center;
            m_Extents = size * 0.5f;
        }

        public Vector3 center { get { return m_Center; } set { m_Center = value; } }
        public Vector3 extents { get { return m_Extents; } set { m_Extents = value; } }
        public Vector3 size { get { return m_Extents * 2f; } set { m_Extents = value * 0.5f; } }
        public Vector3 min { get { return m_Center - m_Extents; } }
        public Vector3 max { get { return m_Center + m_Extents; } }

        public bool Contains(Vector3 point)
        {
            return point.x >= m_Center.x - m_Extents.x && point.x <= m_Center.x + m_Extents.x
                && point.y >= m_Center.y - m_Extents.y && point.y <= m_Center.y + m_Extents.y
                && point.z >= m_Center.z - m_Extents.z && point.z <= m_Center.z + m_Extents.z;
        }
    }

    // How the value types write themselves, which is what Debug.Log shows
    // of one. The digits are worked out in whole numbers, so that every
    // target writes the same ones.
    internal static class ValueText
    {
        internal static string Fixed(float value, int decimals)
        {
            if (value != value)
            {
                return "NaN";
            }
            double v = value;
            bool negative = v < 0;
            if (negative)
            {
                v = -v;
            }
            if (v > 1e15)
            {
                return negative ? "-Infinity" : "Infinity";
            }
            long scale = 1;
            for (int i = 0; i < decimals; i++)
            {
                scale *= 10;
            }
            long n = (long)(v * scale + 0.5);
            long whole = n / scale;
            long part = n - whole * scale;
            string digits = part.ToString();
            while (digits.Length < decimals)
            {
                digits = "0" + digits;
            }
            string text = whole.ToString() + "." + digits;
            return negative && n != 0 ? "-" + text : text;
        }
    }

    public struct Color
    {
        public float r;
        public float g;
        public float b;
        public float a;

        // As Unity writes one: three decimals.
        public override string ToString()
        {
            return "RGBA(" + ValueText.Fixed(r, 3) + ", " + ValueText.Fixed(g, 3) + ", " + ValueText.Fixed(b, 3)
                + ", " + ValueText.Fixed(a, 3) + ")";
        }

        public Color(float r, float g, float b, float a)
        {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public Color(float r, float g, float b)
        {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = 1f;
        }

        public static Color white { get { return new Color(1f, 1f, 1f, 1f); } }
        public static Color black { get { return new Color(0f, 0f, 0f, 1f); } }
        public static Color red { get { return new Color(1f, 0f, 0f, 1f); } }
        public static Color green { get { return new Color(0f, 1f, 0f, 1f); } }
        public static Color blue { get { return new Color(0f, 0f, 1f, 1f); } }
        public static Color yellow { get { return new Color(1f, 0.921568632f, 0.0156862754f, 1f); } }
        public static Color cyan { get { return new Color(0f, 1f, 1f, 1f); } }
        public static Color magenta { get { return new Color(1f, 0f, 1f, 1f); } }
        public static Color gray { get { return new Color(0.5f, 0.5f, 0.5f, 1f); } }
        public static Color grey { get { return new Color(0.5f, 0.5f, 0.5f, 1f); } }
        public static Color clear { get { return new Color(0f, 0f, 0f, 0f); } }

        public static Color Lerp(Color a, Color b, float t)
        {
            t = Mathf.Clamp01(t);
            return new Color(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t);
        }

        public static Color operator *(Color a, float d) { return new Color(a.r * d, a.g * d, a.b * d, a.a * d); }
        public static Color operator *(Color a, Color b) { return new Color(a.r * b.r, a.g * b.g, a.b * b.b, a.a * b.a); }
    }

    public static class Mathf
    {
        public const float PI = 3.14159274f;
        public const float Deg2Rad = 0.0174532924f;
        public const float Rad2Deg = 57.29578f;
        public const float Infinity = float.PositiveInfinity;
        public const float NegativeInfinity = float.NegativeInfinity;
        public const float Epsilon = 1.401298E-45f;

        public static float Abs(float f) { return f < 0f ? -f : f; }
        public static float Min(float a, float b) { return a < b ? a : b; }
        public static float Max(float a, float b) { return a > b ? a : b; }
        public static float Sqrt(float f) { return (float)Math.Sqrt(f); }
        public static float Sin(float f) { return (float)Math.Sin(f); }
        public static float Cos(float f) { return (float)Math.Cos(f); }
        public static float Sign(float f) { return f >= 0f ? 1f : -1f; }
        public static float Clamp(float value, float min, float max) { return value < min ? min : value > max ? max : value; }
        public static float Clamp01(float value) { return value < 0f ? 0f : value > 1f ? 1f : value; }
        public static float Lerp(float a, float b, float t) { return a + (b - a) * Clamp01(t); }
        public static int RoundToInt(float f) { return (int)Math.Round(f); }
        public static int FloorToInt(float f) { return (int)Math.Floor(f); }
        public static int CeilToInt(float f) { return (int)Math.Ceiling(f); }
        public static float Floor(float f) { return (float)Math.Floor(f); }
        public static float Ceil(float f) { return (float)Math.Ceiling(f); }
        public static float Round(float f) { return (float)Math.Round(f); }
        public static float Tan(float f) { return (float)Math.Tan(f); }
        public static float Asin(float f) { return (float)Math.Asin(f); }
        public static float Acos(float f) { return (float)Math.Acos(f); }
        public static float Atan(float f) { return (float)Math.Atan(f); }
        public static float Atan2(float y, float x) { return (float)Math.Atan2(y, x); }
        public static float Pow(float f, float p) { return (float)Math.Pow(f, p); }
        public static float Exp(float power) { return (float)Math.Exp(power); }
        public static float Log(float f) { return (float)Math.Log(f); }
        // Not System.Math.Abs, which throws an OverflowException for the smallest
        // int: Unity's documentation of Mathf.Abs(int) promises no exception, so
        // this one wraps and answers the smallest int again.
        public static int Abs(int value) { return value < 0 ? -value : value; }
        public static int Min(int a, int b) { return a < b ? a : b; }
        public static int Max(int a, int b) { return a > b ? a : b; }
        public static int Clamp(int value, int min, int max) { return value < min ? min : value > max ? max : value; }
        public static float LerpUnclamped(float a, float b, float t) { return a + (b - a) * t; }

        public static float InverseLerp(float a, float b, float value)
        {
            return a != b ? Clamp01((value - a) / (b - a)) : 0f;
        }

        public static float MoveTowards(float current, float target, float maxDelta)
        {
            if (Abs(target - current) <= maxDelta)
            {
                return target;
            }
            return current + Sign(target - current) * maxDelta;
        }

        // The value wrapped into 0 up to, and not including, length.
        public static float Repeat(float t, float length)
        {
            return Clamp(t - Floor(t / length) * length, 0f, length);
        }

        public static float PingPong(float t, float length)
        {
            t = Repeat(t, length * 2f);
            return length - Abs(t - length);
        }

        // The shortest way from one angle to another, in degrees.
        public static float DeltaAngle(float current, float target)
        {
            float delta = Repeat(target - current, 360f);
            return delta > 180f ? delta - 360f : delta;
        }

        public static float LerpAngle(float a, float b, float t)
        {
            return a + DeltaAngle(a, b) * Clamp01(t);
        }

        public static float MoveTowardsAngle(float current, float target, float maxDelta)
        {
            float delta = DeltaAngle(current, target);
            if (-maxDelta < delta && delta < maxDelta)
            {
                return target;
            }
            return MoveTowards(current, current + delta, maxDelta);
        }

        public static float SmoothStep(float from, float to, float t)
        {
            t = Clamp01(t);
            t = -2f * t * t * t + 3f * t * t;
            return to * t + from * (1f - t);
        }

        public static bool Approximately(float a, float b)
        {
            return Abs(b - a) < Max(1e-6f * Max(Abs(a), Abs(b)), 1e-5f);
        }
    }

    public enum ForceMode2D
    {
        Force = 0,
        Impulse = 1
    }

    public enum RigidbodyType2D
    {
        Dynamic = 0,
        Kinematic = 1,
        Static = 2
    }

    public enum CollisionDetectionMode2D
    {
        Discrete = 0,
        Continuous = 1
    }

    [Flags]
    public enum RigidbodyConstraints2D
    {
        None = 0,
        FreezePositionX = 1,
        FreezePositionY = 2,
        FreezePosition = 3,
        FreezeRotation = 4,
        FreezeAll = 7
    }

    // Which way the long side of a capsule runs.
    public enum CapsuleDirection2D
    {
        Vertical = 0,
        Horizontal = 1
    }

    // Where a finger is in its life on the screen.
    public enum TouchPhase
    {
        Began = 0,
        Moved = 1,
        Stationary = 2,
        Ended = 3,
        Canceled = 4
    }

    // One finger on the screen, as it was at the start of the frame.
    public struct Touch
    {
        public int m_FingerId;
        public Vector2 m_Position;
        public Vector2 m_RawPosition;
        public Vector2 m_PositionDelta;
        public float m_TimeDelta;
        public int m_TapCount;
        public TouchPhase m_Phase;

        public int fingerId { get { return m_FingerId; } set { m_FingerId = value; } }
        public Vector2 position { get { return m_Position; } set { m_Position = value; } }
        public Vector2 rawPosition { get { return m_RawPosition; } set { m_RawPosition = value; } }
        public Vector2 deltaPosition { get { return m_PositionDelta; } set { m_PositionDelta = value; } }
        public float deltaTime { get { return m_TimeDelta; } set { m_TimeDelta = value; } }
        public int tapCount { get { return m_TapCount; } set { m_TapCount = value; } }
        public TouchPhase phase { get { return m_Phase; } set { m_Phase = value; } }
    }

    public enum Space
    {
        World = 0,
        Self = 1
    }

    public enum TextAnchor
    {
        UpperLeft = 0,
        UpperCenter = 1,
        UpperRight = 2,
        MiddleLeft = 3,
        MiddleCenter = 4,
        MiddleRight = 5,
        LowerLeft = 6,
        LowerCenter = 7,
        LowerRight = 8
    }

    public enum FontStyle
    {
        Normal = 0,
        Bold = 1,
        Italic = 2,
        BoldAndItalic = 3
    }

    // Where a game runs, by the numbers Unity gives the members. The ones a
    // script is likely to compare with; a Codename One application reports
    // the players of the two phones, the three desktops and the browser, and
    // never an editor.
    public enum RuntimePlatform
    {
        OSXEditor = 0,
        OSXPlayer = 1,
        WindowsPlayer = 2,
        WindowsEditor = 7,
        IPhonePlayer = 8,
        Android = 11,
        LinuxPlayer = 13,
        LinuxEditor = 16,
        WebGLPlayer = 17,
        WSAPlayerX86 = 18,
        WSAPlayerX64 = 19,
        WSAPlayerARM = 20,
        PS4 = 25,
        XboxOne = 27,
        tvOS = 31,
        Switch = 32
    }

    public enum RenderMode
    {
        ScreenSpaceOverlay = 0,
        ScreenSpaceCamera = 1,
        WorldSpace = 2
    }

    // The keys of a keyboard, by the numbers Unity gives them.
    public enum KeyCode
    {
        None = 0,
        Backspace = 8,
        Tab = 9,
        Return = 13,
        Escape = 27,
        Space = 32,
        Exclaim = 33,
        DoubleQuote = 34,
        Hash = 35,
        Dollar = 36,
        Percent = 37,
        Ampersand = 38,
        Quote = 39,
        LeftParen = 40,
        RightParen = 41,
        Asterisk = 42,
        Plus = 43,
        Comma = 44,
        Minus = 45,
        Period = 46,
        Slash = 47,
        Alpha0 = 48,
        Alpha1 = 49,
        Alpha2 = 50,
        Alpha3 = 51,
        Alpha4 = 52,
        Alpha5 = 53,
        Alpha6 = 54,
        Alpha7 = 55,
        Alpha8 = 56,
        Alpha9 = 57,
        Colon = 58,
        Semicolon = 59,
        Less = 60,
        Equals = 61,
        Greater = 62,
        Question = 63,
        At = 64,
        LeftBracket = 91,
        Backslash = 92,
        RightBracket = 93,
        Caret = 94,
        Underscore = 95,
        BackQuote = 96,
        A = 97,
        B = 98,
        C = 99,
        D = 100,
        E = 101,
        F = 102,
        G = 103,
        H = 104,
        I = 105,
        J = 106,
        K = 107,
        L = 108,
        M = 109,
        N = 110,
        O = 111,
        P = 112,
        Q = 113,
        R = 114,
        S = 115,
        T = 116,
        U = 117,
        V = 118,
        W = 119,
        X = 120,
        Y = 121,
        Z = 122,
        Delete = 127,
        Keypad0 = 256,
        Keypad1 = 257,
        Keypad2 = 258,
        Keypad3 = 259,
        Keypad4 = 260,
        Keypad5 = 261,
        Keypad6 = 262,
        Keypad7 = 263,
        Keypad8 = 264,
        Keypad9 = 265,
        KeypadPeriod = 266,
        KeypadDivide = 267,
        KeypadMultiply = 268,
        KeypadMinus = 269,
        KeypadPlus = 270,
        KeypadEnter = 271,
        KeypadEquals = 272,
        UpArrow = 273,
        DownArrow = 274,
        RightArrow = 275,
        LeftArrow = 276,
        Insert = 277,
        Home = 278,
        End = 279,
        PageUp = 280,
        PageDown = 281,
        F1 = 282,
        F2 = 283,
        F3 = 284,
        F4 = 285,
        F5 = 286,
        F6 = 287,
        F7 = 288,
        F8 = 289,
        F9 = 290,
        F10 = 291,
        F11 = 292,
        F12 = 293,
        RightShift = 303,
        LeftShift = 304,
        RightControl = 305,
        LeftControl = 306,
        RightAlt = 307,
        LeftAlt = 308,
        Mouse0 = 323,
        Mouse1 = 324,
        Mouse2 = 325
    }

    // A cell of a grid: three whole numbers.
    public struct Vector3Int
    {
        public int x;
        public int y;
        public int z;

        public Vector3Int(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static Vector3Int zero { get { return new Vector3Int(0, 0, 0); } }
        public static Vector3Int one { get { return new Vector3Int(1, 1, 1); } }
        public static Vector3Int up { get { return new Vector3Int(0, 1, 0); } }
        public static Vector3Int down { get { return new Vector3Int(0, -1, 0); } }
        public static Vector3Int left { get { return new Vector3Int(-1, 0, 0); } }
        public static Vector3Int right { get { return new Vector3Int(1, 0, 0); } }

        public static Vector3Int operator +(Vector3Int a, Vector3Int b) { return new Vector3Int(a.x + b.x, a.y + b.y, a.z + b.z); }
        public static Vector3Int operator -(Vector3Int a, Vector3Int b) { return new Vector3Int(a.x - b.x, a.y - b.y, a.z - b.z); }
        public static Vector3Int operator *(Vector3Int a, int b) { return new Vector3Int(a.x * b, a.y * b, a.z * b); }
        public static bool operator ==(Vector3Int a, Vector3Int b) { return a.x == b.x && a.y == b.y && a.z == b.z; }
        public static bool operator !=(Vector3Int a, Vector3Int b) { return a.x != b.x || a.y != b.y || a.z != b.z; }
        public static implicit operator Vector3(Vector3Int v) { return new Vector3(v.x, v.y, v.z); }
        public static explicit operator Vector2Int(Vector3Int v) { return new Vector2Int(v.x, v.y); }

        public override bool Equals(object other)
        {
            return other is Vector3Int && this == (Vector3Int)other;
        }

        public override int GetHashCode()
        {
            return x * 73856093 ^ y * 19349663 ^ z * 83492791;
        }

        public override string ToString()
        {
            return "(" + x + ", " + y + ", " + z + ")";
        }
    }

    // A cell of a flat grid: two whole numbers.
    public struct Vector2Int
    {
        public int x;
        public int y;

        public Vector2Int(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public static Vector2Int zero { get { return new Vector2Int(0, 0); } }
        public static Vector2Int one { get { return new Vector2Int(1, 1); } }
        public static Vector2Int up { get { return new Vector2Int(0, 1); } }
        public static Vector2Int down { get { return new Vector2Int(0, -1); } }
        public static Vector2Int left { get { return new Vector2Int(-1, 0); } }
        public static Vector2Int right { get { return new Vector2Int(1, 0); } }

        public static Vector2Int operator +(Vector2Int a, Vector2Int b) { return new Vector2Int(a.x + b.x, a.y + b.y); }
        public static Vector2Int operator -(Vector2Int a, Vector2Int b) { return new Vector2Int(a.x - b.x, a.y - b.y); }
        public static Vector2Int operator *(Vector2Int a, int b) { return new Vector2Int(a.x * b, a.y * b); }
        public static bool operator ==(Vector2Int a, Vector2Int b) { return a.x == b.x && a.y == b.y; }
        public static bool operator !=(Vector2Int a, Vector2Int b) { return a.x != b.x || a.y != b.y; }
        public static implicit operator Vector2(Vector2Int v) { return new Vector2(v.x, v.y); }
        public static explicit operator Vector3Int(Vector2Int v) { return new Vector3Int(v.x, v.y, 0); }

        public override bool Equals(object other)
        {
            return other is Vector2Int && this == (Vector2Int)other;
        }

        public override int GetHashCode()
        {
            return x * 73856093 ^ y * 19349663;
        }

        public override string ToString()
        {
            return "(" + x + ", " + y + ")";
        }
    }

    // What stopping a particle system does to the particles already alive.
    public enum ParticleSystemStopBehavior
    {
        StopEmittingAndClear = 0,
        StopEmitting = 1
    }

    // How a value of a particle system module is given.
    public enum ParticleSystemCurveMode
    {
        Constant = 0,
        Curve = 1,
        TwoCurves = 2,
        TwoConstants = 3
    }
}

namespace TMPro
{
    // Where the text of a TextMesh Pro component sits in its rectangle. The
    // low byte is the column and the high byte the row.
    public enum TextAlignmentOptions
    {
        TopLeft = 0x101,
        Top = 0x102,
        TopRight = 0x104,
        TopJustified = 0x108,
        Left = 0x201,
        Center = 0x202,
        Right = 0x204,
        Justified = 0x208,
        BottomLeft = 0x401,
        Bottom = 0x402,
        BottomRight = 0x404,
        BottomJustified = 0x408,
        BaselineLeft = 0x801,
        Baseline = 0x802,
        BaselineRight = 0x804,
        MidlineLeft = 0x4001,
        Midline = 0x4002,
        MidlineRight = 0x4004
    }

    [System.Flags]
    public enum FontStyles
    {
        Normal = 0,
        Bold = 1,
        Italic = 2,
        Underline = 4
    }
}

namespace UnityEngine.SceneManagement
{
    // A loaded scene: its name and where it stands in the build's scene list.
    public struct Scene
    {
        public string m_Name;
        public int m_BuildIndex;

        public string name { get { return m_Name; } }
        public int buildIndex { get { return m_BuildIndex; } }
    }
}
