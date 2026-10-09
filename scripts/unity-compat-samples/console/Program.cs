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
using System;
using System.Collections;
using System.Collections.Generic;

// The CIL translator's conformance program. Every section exercises one lowering
// rule, and the whole output is compared byte for byte with what `dotnet run`
// prints for the same sources. Two things are kept out on purpose so that the
// comparison stays exact on every target:
//
//  - Nothing prints a float directly. .NET, the JVM, C and JavaScript each format
//    a float their own way, so floats are scaled and truncated to an int first.
//  - Nothing depends on culture, hash iteration order or object identity.
namespace Spike
{
    public struct Vec2
    {
        public float x;
        public float y;

        public Vec2(float x, float y)
        {
            this.x = x;
            this.y = y;
        }

        public float SqrMagnitude
        {
            get { return x * x + y * y; }
        }

        public void Scale(float f)
        {
            x *= f;
            y *= f;
        }

        public static Vec2 operator +(Vec2 a, Vec2 b)
        {
            return new Vec2(a.x + b.x, a.y + b.y);
        }

        public static Vec2 operator *(Vec2 a, float f)
        {
            return new Vec2(a.x * f, a.y * f);
        }

        public override string ToString()
        {
            return "(" + (int)(x * 100) + "," + (int)(y * 100) + ")";
        }
    }

    public struct Segment
    {
        public Vec2 from;
        public Vec2 to;
    }

    public class Holder
    {
        public Vec2 position;
        public Vec2[] trail = new Vec2[3];
        public Vec2 Position
        {
            get { return position; }
            set { position = value; }
        }
    }

    public enum Phase
    {
        Began,
        Moved,
        Ended = 7
    }

    [Flags]
    public enum Layers
    {
        None = 0,
        Ground = 1,
        Player = 2,
        Enemy = 4
    }

    public interface IShape
    {
        int Area();
        string Name { get; }
    }

    public abstract class Shape : IShape
    {
        public abstract int Area();

        public virtual string Name
        {
            get { return "shape"; }
        }

        public string Describe()
        {
            return Name + ":" + Area();
        }
    }

    public class Rect : Shape
    {
        private readonly int w;
        private readonly int h;

        public Rect(int w, int h)
        {
            this.w = w;
            this.h = h;
        }

        public override int Area()
        {
            return w * h;
        }

        public override string Name
        {
            get { return "rect"; }
        }
    }

    public class Square : Rect
    {
        public Square(int s) : base(s, s)
        {
        }

        public override string Name
        {
            get { return "square/" + base.Name; }
        }
    }

    public class Box<T>
    {
        private T value;
        public int Sets;

        public T Value
        {
            get { return value; }
            set
            {
                this.value = value;
                Sets++;
            }
        }
    }

    public class GameException : Exception
    {
        public readonly int Code;

        public GameException(string message, int code) : base(message)
        {
            Code = code;
        }
    }

    public class Publisher
    {
        public event Action<int> Changed;

        public void Fire(int v)
        {
            if (Changed != null)
            {
                Changed(v);
            }
        }
    }

    public static class Order
    {
        public static readonly int First;
        public static int Touched;

        static Order()
        {
            Program.Log("Order.cctor");
            First = 41;
        }

        public static int Next()
        {
            Touched++;
            return First + Touched;
        }
    }

    public class Grid
    {
        private readonly int[] cells = new int[4];

        public int this[int i]
        {
            get { return cells[i]; }
            set { cells[i] = value; }
        }

        public int Count { get; private set; }

        public void Bump()
        {
            Count++;
        }
    }

    public static class Program
    {
        public static void Log(string s)
        {
            Console.WriteLine(s);
        }

        private static void Section(string name)
        {
            Console.WriteLine("== " + name);
        }

        // ---- value types -------------------------------------------------------

        private static void MutateParam(Vec2 v)
        {
            v.x = 99;
            Log("inside " + v);
        }

        private static Vec2 MakeAndReturn(Holder h)
        {
            Vec2 r = h.position;
            r.y += 1;
            return r;
        }

        private static void ValueTypes()
        {
            Section("value types");
            Vec2 a = new Vec2(1, 2);
            Vec2 b = a;
            b.x = 5;
            Log("a=" + a + " b=" + b);

            MutateParam(a);
            Log("after call a=" + a);

            Holder h = new Holder();
            h.position = a;
            a.y = 8;
            Log("field=" + h.position + " local=" + a);

            Vec2 fromField = h.position;
            fromField.Scale(2);
            Log("field=" + h.position + " copy=" + fromField);

            h.position.Scale(3);
            Log("scaled in place=" + h.position);

            Vec2 viaProp = h.Position;
            viaProp.x = -1;
            Log("prop source=" + h.position + " prop copy=" + viaProp);

            Vec2 ret = MakeAndReturn(h);
            Log("returned=" + ret + " source=" + h.position);

            Vec2[] arr = new Vec2[3];
            Log("default element=" + arr[1]);
            arr[0] = a;
            arr[0].x = 42;
            Vec2 elem = arr[0];
            elem.y = 43;
            Log("arr0=" + arr[0] + " a=" + a + " elem=" + elem);

            h.trail[2] = arr[0];
            arr[0].y = 0;
            Log("trail2=" + h.trail[2] + " arr0=" + arr[0]);

            Segment s = new Segment();
            s.from = a;
            s.to = a + new Vec2(1, 1) * 2;
            Segment t = s;
            t.to.x = 1000;
            Log("s.to=" + s.to + " t.to=" + t.to);

            Vec2 d = default(Vec2);
            Log("default=" + d + " sqr=" + (int)a.SqrMagnitude);
        }

        // ---- ref and out -------------------------------------------------------

        private static void Swap(ref int a, ref int b)
        {
            int t = a;
            a = b;
            b = t;
        }

        private static void SwapVec(ref Vec2 a, ref Vec2 b)
        {
            Vec2 t = a;
            a = b;
            b = t;
        }

        private static void Grow(ref Vec2 v)
        {
            v.x += 10;
        }

        private static bool TryHalf(int v, out int half)
        {
            if ((v & 1) != 0)
            {
                half = -1;
                return false;
            }
            half = v / 2;
            return true;
        }

        private static void Rename(ref string s)
        {
            s = s + "!";
        }

        private static void RefAndOut()
        {
            Section("ref and out");
            int x = 1;
            int y = 2;
            Swap(ref x, ref y);
            Log("x=" + x + " y=" + y);

            Vec2 p = new Vec2(1, 1);
            Vec2 q = new Vec2(2, 2);
            SwapVec(ref p, ref q);
            Grow(ref p);
            Log("p=" + p + " q=" + q);

            int half;
            bool ok = TryHalf(10, out half);
            Log("ok=" + ok + " half=" + half);
            ok = TryHalf(7, out half);
            Log("ok=" + ok + " half=" + half);

            string name = "ball";
            Rename(ref name);
            Log(name);
        }

        // ---- generics ----------------------------------------------------------

        private static T Largest<T>(T a, T b) where T : IComparable<T>
        {
            return a.CompareTo(b) >= 0 ? a : b;
        }

        private static void Generics()
        {
            Section("generics");
            List<int> ints = new List<int>();
            for (int i = 0; i < 5; i++)
            {
                ints.Add(i * i);
            }
            ints.RemoveAt(1);
            int sum = 0;
            foreach (int v in ints)
            {
                sum += v;
            }
            Log("count=" + ints.Count + " sum=" + sum + " [2]=" + ints[2] + " has9=" + ints.Contains(9));

            List<Vec2> points = new List<Vec2>();
            Vec2 seed = new Vec2(1, 1);
            points.Add(seed);
            seed.x = 7;
            points.Add(seed);
            Vec2 first = points[0];
            first.y = 50;
            Log("p0=" + points[0] + " p1=" + points[1] + " first=" + first);

            Dictionary<int, string> names = new Dictionary<int, string>();
            names[3] = "three";
            names.Add(4, "four");
            string found;
            Log("3=" + names[3] + " has4=" + names.ContainsKey(4) + " try5=" + names.TryGetValue(5, out found)
                + " n=" + names.Count);

            Dictionary<string, int> scores = new Dictionary<string, int>();
            scores["a"] = 1;
            scores["a"] = scores["a"] + 10;
            Log("a=" + scores["a"]);

            Box<int> bi = new Box<int>();
            bi.Value = 12;
            bi.Value = bi.Value + 1;
            Box<string> bs = new Box<string>();
            bs.Value = "boxed";
            Box<Vec2> bv = new Box<Vec2>();
            Vec2 into = new Vec2(3, 4);
            bv.Value = into;
            into.x = 0;
            Log("bi=" + bi.Value + "/" + bi.Sets + " bs=" + bs.Value + " bv=" + bv.Value);

            Log("largest=" + Largest(3, 9) + " " + Largest("pear", "apple"));
        }

        // ---- delegates and events ---------------------------------------------

        private static int total;

        private static void AddToTotal(int v)
        {
            total += v;
        }

        private static int Apply(Func<int, int> f, int v)
        {
            return f(v);
        }

        private static void Delegates()
        {
            Section("delegates");
            Action<int> a = AddToTotal;
            a(5);
            int captured = 100;
            Action<int> b = delegate (int v) { captured += v; };
            Action<int> both = a + b;
            both(1);
            both -= a;
            both(2);
            Log("total=" + total + " captured=" + captured);

            Func<int, int> twice = v => v * 2;
            int offset = 3;
            Func<int, int> shifted = v => v + offset;
            offset = 30;
            Log("twice=" + Apply(twice, 21) + " shifted=" + Apply(shifted, 1));

            Publisher pub = new Publisher();
            pub.Fire(1);
            int seen = 0;
            Action<int> handler = v => seen += v;
            pub.Changed += handler;
            pub.Changed += handler;
            pub.Fire(4);
            pub.Changed -= handler;
            pub.Fire(8);
            Log("seen=" + seen);

            List<Action> later = new List<Action>();
            for (int i = 0; i < 3; i++)
            {
                int copy = i;
                later.Add(() => Log("later " + copy));
            }
            foreach (Action act in later)
            {
                act();
            }
        }

        // ---- enums, properties, indexers ----------------------------------------

        private static string Describe(Phase p)
        {
            switch (p)
            {
                case Phase.Began:
                    return "began";
                case Phase.Moved:
                    return "moved";
                case Phase.Ended:
                    return "ended";
                default:
                    return "other";
            }
        }

        private static void EnumsAndProperties()
        {
            Section("enums and properties");
            Phase p = Phase.Moved;
            Log(Describe(p) + " " + Describe(Phase.Ended) + " " + Describe((Phase)3) + " raw=" + (int)Phase.Ended);
            Layers mask = Layers.Ground | Layers.Enemy;
            Log("ground=" + ((mask & Layers.Ground) != 0) + " player=" + ((mask & Layers.Player) != 0)
                + " bits=" + (int)mask + " eq=" + (p == Phase.Moved));

            Grid g = new Grid();
            g[2] = 9;
            g[2] += 1;
            g.Bump();
            g.Bump();
            Log("g2=" + g[2] + " count=" + g.Count);
        }

        // ---- strings -------------------------------------------------------------

        private static string Classify(string s)
        {
            switch (s)
            {
                case "up":
                    return "U";
                case "down":
                    return "D";
                default:
                    return "?";
            }
        }

        private static void Strings()
        {
            Section("strings");
            string s = "Codename" + " " + "One";
            Log(s + " len=" + s.Length + " idx=" + s.IndexOf("One") + " sub=" + s.Substring(4, 4)
                + " tail=" + s.Substring(9));
            Log("eq=" + (s == "Codename One") + " neq=" + (s != "x") + " starts=" + s.StartsWith("Code")
                + " ends=" + s.EndsWith("Two") + " empty=" + string.IsNullOrEmpty(""));
            int n = 7;
            Log($"n={n} twice={n * 2} name={s}");
            Log(string.Format("{0}-{1}-{0}", "a", 3));
            int vowels = 0;
            foreach (char c in s)
            {
                if (c == 'o' || c == 'e' || c == 'a' || c == 'O')
                {
                    vowels++;
                }
            }
            Log("vowels=" + vowels + " first=" + s[0] + " code=" + (int)s[1]);
            Log(Classify("up") + Classify("down") + Classify("left"));
            string nothing = null;
            Log("null concat=[" + nothing + "] " + (nothing == null));
            Log("char math=" + (char)('a' + 2) + " bool=" + true + " long=" + 12345678901L);
        }

        // ---- the edges of integer arithmetic ---------------------------------------

        private static int Quotient(int a, int b)
        {
            return a / b;
        }

        private static int Remainder(int a, int b)
        {
            return a % b;
        }

        private static long Quotient(long a, long b)
        {
            return a / b;
        }

        private static long Remainder(long a, long b)
        {
            return a % b;
        }

        // What an integer operation answers, or the exception it ends in. The
        // operands arrive as arguments so that nothing is folded by the compiler.
        private static string Edge(int op, long a, long b)
        {
            try
            {
                switch (op)
                {
                    case 0: return "" + Quotient((int)a, (int)b);
                    case 1: return "" + Remainder((int)a, (int)b);
                    case 2: return "" + Quotient(a, b);
                    case 3: return "" + Remainder(a, b);
                    case 4: return "" + ((uint)a / (uint)b);
                    case 5: return "" + ((uint)a % (uint)b);
                    case 6: return "" + ((ulong)a / (ulong)b);
                    case 7: return "" + ((ulong)a % (ulong)b);
                    case 8: return "" + Math.Abs((int)a);
                    default: return "" + Math.Abs(a);
                }
            }
            catch (DivideByZeroException)
            {
                return "zero";
            }
            catch (OverflowException)
            {
                return "overflow";
            }
        }

        // The smallest value divided by -1 has no answer an int or a long holds:
        // division throws, and so does the remainder, where C# says x % y throws
        // wherever x / y would. Every other division by -1 is ordinary, the
        // unsigned operators read the same bits as large numbers and never
        // overflow, and Math.Abs has no answer for the smallest value either.
        private static void IntegerEdges()
        {
            Section("integer edges");
            long min = int.MinValue;
            long wide = long.MinValue;
            Log("int div " + Edge(0, min, -1) + " " + Edge(0, min + 1, -1) + " " + Edge(0, min, 1) + " "
                + Edge(0, min, 2) + " " + Edge(0, -7, -1) + " " + Edge(0, 0, -1) + " " + Edge(0, 7, 0) + " "
                + Edge(0, min, 0));
            Log("int rem " + Edge(1, min, -1) + " " + Edge(1, min + 1, -1) + " " + Edge(1, min, 1) + " "
                + Edge(1, min, 2) + " " + Edge(1, -7, -1) + " " + Edge(1, -7, 3) + " " + Edge(1, 7, 0));
            Log("long div " + Edge(2, wide, -1) + " " + Edge(2, wide + 1, -1) + " " + Edge(2, wide, 1) + " "
                + Edge(2, min, -1) + " " + Edge(2, -7, -1) + " " + Edge(2, 7, 0) + " " + Edge(2, wide, 0));
            Log("long rem " + Edge(3, wide, -1) + " " + Edge(3, wide + 1, -1) + " " + Edge(3, wide, 10) + " "
                + Edge(3, -7, -1) + " " + Edge(3, 7, 0));
            Log("unsigned " + Edge(4, min, -1) + " " + Edge(5, min, -1) + " " + Edge(6, wide, -1) + " "
                + Edge(7, wide, -1) + " " + Edge(4, -1, 1) + " " + Edge(4, 7, 0) + " " + Edge(7, 7, 0));
            Log("abs " + Edge(8, min, 0) + " " + Edge(8, min + 1, 0) + " " + Edge(8, -4, 0) + " " + Edge(9, wide, 0)
                + " " + Edge(9, wide + 1, 0) + " " + Edge(9, min, 0));
            int smallest = int.Parse("-2147483648");
            int value;
            Log("smallest " + smallest + " " + int.TryParse(" -2147483648 ", out value) + "/" + value + " "
                + int.TryParse("-2147483649", out value) + "/" + value);
        }

        // ---- exceptions ----------------------------------------------------------

        private static int Divide(int a, int b)
        {
            return a / b;
        }

        private static int Depth(int n)
        {
            try
            {
                if (n == 0)
                {
                    throw new GameException("bottom", 17);
                }
                return Depth(n - 1) + 1;
            }
            finally
            {
                Log("unwind " + n);
            }
        }

        private static int FinallyOrder()
        {
            int v = 1;
            try
            {
                try
                {
                    v = 2;
                    return v;
                }
                finally
                {
                    v = 3;
                    Log("inner finally");
                }
            }
            finally
            {
                Log("outer finally v=" + v);
            }
        }

        private static void Exceptions()
        {
            Section("exceptions");
            try
            {
                Depth(2);
            }
            catch (GameException e)
            {
                Log("caught " + e.Message + " code=" + e.Code);
            }

            Log("returned " + FinallyOrder());

            try
            {
                Log("div " + Divide(7, 2) + " " + Divide(-7, 2) + " rem " + (-7 % 3));
                Log("never " + Divide(1, 0));
            }
            catch (DivideByZeroException)
            {
                Log("divide by zero");
            }

            try
            {
                object o = "text";
                Shape sh = (Shape)o;
                Log("never " + sh.Describe());
            }
            catch (InvalidCastException)
            {
                Log("invalid cast");
            }

            try
            {
                Shape none = null;
                Log("never " + none.Area());
            }
            catch (NullReferenceException)
            {
                Log("null reference");
            }

            try
            {
                int[] small = new int[2];
                small[2] = 1;
            }
            catch (IndexOutOfRangeException)
            {
                Log("index out of range");
            }

            try
            {
                try
                {
                    throw new InvalidOperationException("first");
                }
                catch (Exception e)
                {
                    Log("rethrowing " + e.Message);
                    throw;
                }
            }
            catch (InvalidOperationException e)
            {
                Log("outer " + e.Message);
            }
            catch (Exception)
            {
                Log("wrong handler");
            }

            try
            {
                throw new ArgumentException("bad arg");
            }
            catch (Exception e)
            {
                Log("base catch " + e.Message + " is arg=" + (e is ArgumentException));
            }
        }

        // ---- iterators (what a coroutine is) -------------------------------------

        private static IEnumerator Routine(int steps)
        {
            Log("routine start");
            for (int i = 0; i < steps; i++)
            {
                yield return i;
            }
            Log("routine mid");
            yield return null;
            Log("routine end");
        }

        private static IEnumerable<int> Evens(int limit)
        {
            for (int i = 0; i < limit; i++)
            {
                if (i % 2 == 0)
                {
                    yield return i;
                }
            }
        }

        private static void Iterators()
        {
            Section("iterators");
            IEnumerator r = Routine(2);
            int ticks = 0;
            while (r.MoveNext())
            {
                ticks++;
                Log("tick " + ticks + " current=" + (r.Current == null ? "null" : r.Current.ToString()));
            }
            int sum = 0;
            foreach (int e in Evens(9))
            {
                sum += e;
            }
            Log("evens=" + sum);
        }

        // ---- boxing, casts, interfaces --------------------------------------------

        private static void BoxingAndTypes()
        {
            Section("boxing and types");
            object o = 5;
            int back = (int)o;
            object f = 2.5f;
            Log("back=" + (back + 1) + " isInt=" + (o is int) + " isString=" + (o is string)
                + " f=" + (int)((float)f * 10));

            Vec2 v = new Vec2(1, 2);
            object boxed = v;
            v.x = 9;
            Vec2 unboxed = (Vec2)boxed;
            Log("boxed=" + unboxed + " v=" + v + " str=" + boxed);

            try
            {
                long wrong = (long)o;
                Log("never " + wrong);
            }
            catch (InvalidCastException)
            {
                Log("unbox mismatch");
            }

            Shape[] shapes = new Shape[] { new Rect(2, 3), new Square(4) };
            int area = 0;
            foreach (Shape s in shapes)
            {
                Log(s.Describe());
                area += s.Area();
            }
            IShape viaInterface = shapes[1];
            Square sq = shapes[1] as Square;
            Square notSquare = shapes[0] as Square;
            Log("area=" + area + " iface=" + viaInterface.Name + " as=" + (sq != null) + " asNull=" + (notSquare == null)
                + " isRect=" + (shapes[1] is Rect));
        }

        // ---- integers, unsigned, conversions ----------------------------------------

        private static uint Fnv(byte[] data)
        {
            uint hash = 2166136261;
            for (int i = 0; i < data.Length; i++)
            {
                hash ^= data[i];
                hash *= 16777619;
            }
            return hash;
        }

        private static void Arithmetic()
        {
            Section("arithmetic");
            byte[] data = new byte[] { 1, 200, 255, 16 };
            uint h = Fnv(data);
            Log("fnv=" + h + " div=" + (h / 7) + " rem=" + (h % 1000) + " shr=" + (h >> 28) + " big=" + (h > 2147483648));
            int signed = (int)h;
            Log("as int=" + signed + " sar=" + (signed >> 28) + " byte=" + (int)data[2] + " sbyte=" + (sbyte)data[2]);

            int max = int.MaxValue;
            int wrapped = unchecked(max + 1);
            long wide = (long)max * 4;
            int seventy = 70000;
            Log("wrapped=" + wrapped + " wide=" + wide + " narrow=" + (int)(wide + 3) + " short=" + (short)seventy
                + " ushort=" + (ushort)seventy);

            ulong big = 18446744073709551615;
            Log("ulong=" + big + " half=" + (big / 2) + " cmp=" + (big > 5));

            float fl = 7.9f;
            double db = -7.9;
            Log("trunc=" + (int)fl + " " + (int)db + " round=" + (int)Math.Round(2.5) + " floor=" + (int)Math.Floor(db)
                + " sqrt=" + (int)(Math.Sqrt(2) * 1000) + " abs=" + Math.Abs(-4) + " max=" + Math.Max(3, 8));
            float third = 1f / 3f;
            double dthird = 1.0 / 3.0;
            Log("float bits=" + (int)(third * 100000000f) + " double bits=" + (long)(dthird * 100000000000000.0));

            int bits = 0x0F0F;
            Log("and=" + (bits & 0xFF) + " or=" + (bits | 0xF000) + " xor=" + (bits ^ 0xFFFF) + " not=" + (~bits)
                + " shl=" + (bits << 20) + " shift mask=" + (1 << 33));

            int[][] jagged = new int[2][];
            jagged[0] = new int[] { 1, 2, 3 };
            jagged[1] = new int[] { 4 };
            int total = 0;
            for (int i = 0; i < jagged.Length; i++)
            {
                for (int j = 0; j < jagged[i].Length; j++)
                {
                    total += jagged[i][j];
                }
            }
            long[] longs = new long[] { 1L << 40, -5 };
            bool[] flags = new bool[2];
            flags[1] = true;
            char[] chars = new char[] { 'o', 'k' };
            Log("jagged=" + total + " longs=" + (longs[0] + longs[1]) + " flags=" + flags[0] + flags[1]
                + " chars=" + new string(chars));
        }

        // ---- multi-dimensional arrays ------------------------------------------

        private static int[,] table = new int[2, 2];

        private static int Sum(int[,] a)
        {
            int total = 0;
            for (int i = 0; i < a.GetLength(0); i++)
            {
                for (int j = 0; j < a.GetLength(1); j++)
                {
                    total += a[i, j];
                }
            }
            return total;
        }

        private static float[,] Identity(int n)
        {
            float[,] m = new float[n, n];
            for (int i = 0; i < n; i++)
            {
                m[i, i] = 1f;
            }
            return m;
        }

        private static void Bump(ref int cell, int by)
        {
            cell += by;
        }

        private static string Probe(int[,] a, int i, int j)
        {
            try
            {
                return "" + a[i, j];
            }
            catch (IndexOutOfRangeException)
            {
                return "range";
            }
        }

        private static string ProbeStore(int[,,] a, int i, int j, int k)
        {
            try
            {
                a[i, j, k] = 1;
                return "ok";
            }
            catch (IndexOutOfRangeException)
            {
                return "range";
            }
        }

        private static void MultiDimensional()
        {
            Section("multi-dimensional arrays");
            int[,] grid = new int[3, 4];
            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 4; j++)
                {
                    grid[i, j] = i * 10 + j;
                }
            }
            Log("grid[2,3]=" + grid[2, 3] + " [0,3]=" + grid[0, 3] + " [1,0]=" + grid[1, 0] + " sum=" + Sum(grid)
                + " length=" + grid.Length + " rank=" + grid.Rank + " dims=" + grid.GetLength(0) + "x" + grid.GetLength(1)
                + " upper=" + grid.GetUpperBound(0) + "," + grid.GetUpperBound(1) + " lower=" + grid.GetLowerBound(1));

            // A row that ends is not the start of the next one.
            Log("range: " + Probe(grid, 0, 4) + " " + Probe(grid, 3, 0) + " " + Probe(grid, -1, 0) + " "
                + Probe(grid, 0, -1) + " " + Probe(grid, 2, 3) + " " + Probe(grid, 1, 4));

            int[,] literal = new int[,] { { 1, 2, 3 }, { 4, 5, 6 } };
            int walked = 0;
            string order = "";
            foreach (int v in literal)
            {
                walked += v;
                order += v;
            }
            Log("literal " + literal.GetLength(0) + "x" + literal.GetLength(1) + " [1,0]=" + literal[1, 0] + " foreach="
                + walked + " order=" + order);

            grid[1, 2]++;
            grid[1, 2] += 5;
            Bump(ref grid[1, 2], 100);
            ref int cell = ref grid[2, 0];
            cell = -7;
            Log("compound=" + grid[1, 2] + " ref=" + grid[2, 0]);

            int[,,] cube = new int[2, 3, 4];
            int n = 0;
            for (int i = 0; i < 2; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    for (int k = 0; k < 4; k++)
                    {
                        cube[i, j, k] = n++;
                    }
                }
            }
            Log("cube[1,2,3]=" + cube[1, 2, 3] + " [1,0,0]=" + cube[1, 0, 0] + " [0,2,1]=" + cube[0, 2, 1] + " length="
                + cube.Length + " rank=" + cube.Rank + " dim2=" + cube.GetLength(2) + " range: "
                + ProbeStore(cube, 0, 0, 4) + " " + ProbeStore(cube, 0, 3, 0) + " " + ProbeStore(cube, 2, 0, 0) + " "
                + ProbeStore(cube, 1, 2, 3));

            int[,,,] hyper = new int[2, 2, 2, 3];
            hyper[1, 1, 1, 2] = 42;
            hyper[0, 1, 0, 1] = 7;
            int hsum = 0;
            foreach (int v in hyper)
            {
                hsum += v;
            }
            string hrange;
            try
            {
                hyper[0, 0, 2, 0] = 1;
                hrange = "ok";
            }
            catch (IndexOutOfRangeException)
            {
                hrange = "range";
            }
            Log("hyper=" + hyper[1, 1, 1, 2] + " sum=" + hsum + " length=" + hyper.Length + " rank=" + hyper.Rank
                + " dim3=" + hyper.GetLength(3) + " " + hrange);

            float[,] id = Identity(3);
            double[,] dd = new double[,] { { 0.5, 1.5 }, { 2.5, 3.5 } };
            long[,] ll = new long[2, 2];
            ll[1, 1] = 1L << 40;
            bool[,] bb = new bool[2, 3];
            bb[1, 2] = true;
            char[,] cc = new char[,] { { 'a', 'b' }, { 'c', 'd' } };
            byte[,] by = new byte[,] { { 1, 200 }, { 255, 16 } };
            short[,] sh = new short[1, 2];
            sh[0, 1] = -300;
            Log("float=" + (int)(id[0, 0] + id[1, 1] + id[2, 2] + id[0, 1]) + " double=" + (int)(dd[0, 1] + dd[1, 0])
                + " long=" + ll[1, 1] + " bool=" + bb[1, 2] + bb[0, 0] + " char=" + cc[1, 0] + cc[0, 1] + " byte="
                + (by[0, 1] + by[1, 0]) + " short=" + sh[0, 1]);

            string[,] names = new string[2, 2];
            names[0, 1] = "north";
            names[1, 0] = "south";
            Log("strings=" + names[0, 1] + "/" + names[1, 0] + "/" + (names[0, 0] == null) + " length=" + names.Length);

            // Elements that are structs: each one is a value of its own.
            Vec2[,] field = new Vec2[2, 2];
            field[0, 1] = new Vec2(1, 2);
            field[1, 0].x = 5;
            Vec2 taken = field[0, 1];
            taken.x = 9;
            field[1, 1] = field[0, 1];
            field[1, 1].y = 7;
            field[0, 1].Scale(2);
            Log("structs=" + field[0, 0] + field[0, 1] + field[1, 0] + field[1, 1] + " taken=" + taken);
            Vec2 fsum = new Vec2(0, 0);
            foreach (Vec2 v in field)
            {
                fsum = fsum + v;
            }
            Log("struct foreach=" + fsum);

            // A copy is independent of what it was copied from.
            int[,] copy = (int[,])literal.Clone();
            copy[0, 0] = 50;
            Vec2[,] fcopy = (Vec2[,])field.Clone();
            fcopy[0, 1].x = -1;
            Log("clone=" + copy[0, 0] + "/" + literal[0, 0] + " " + copy.GetLength(1) + " struct clone=" + fcopy[0, 1]
                + field[0, 1]);
            Array.Clear(copy, 1, 3);
            Array.Clear(fcopy, 0, 2);
            Log("clear=" + copy[0, 0] + copy[0, 1] + copy[0, 2] + copy[1, 0] + copy[1, 1] + copy[1, 2] + " "
                + fcopy[0, 1] + fcopy[1, 1]);

            table[1, 0] = 3;
            table[0, 1] = table[1, 0] * 2;
            int[,] alias = table;
            alias[1, 1] = 4;
            table = new int[1, 5];
            Log("field=" + Sum(alias) + " " + alias[0, 1] + " replaced=" + table.Length + " " + Sum(table));

            int[][,] several = new int[2][,];
            several[0] = literal;
            several[1] = grid;
            Log("array of grids=" + several[0][1, 2] + " " + several[1][2, 3]);

            int[,] empty = new int[0, 5];
            Log("empty=" + empty.Length + " " + empty.GetLength(1) + " " + Probe(empty, 0, 0));
            try
            {
                int negative = -1;
                Log("" + new int[2, negative].Length);
            }
            catch (OverflowException)
            {
                Log("negative size");
            }
        }

        // ---- splitting, parsing and dictionaries --------------------------------

        private static string Show(string[] parts)
        {
            string text = "" + parts.Length + ":";
            foreach (string part in parts)
            {
                text += "[" + part + "]";
            }
            return text;
        }

        private static string Parsed(string text)
        {
            int value;
            bool ok = int.TryParse(text, out value);
            return ok + "/" + value;
        }

        private static void SplittingAndParsing()
        {
            Section("splitting and parsing");
            string level = "1,2,-3\r\n4,,6\n\n7,x,9";
            string[] lines = level.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
            Log("lines " + Show(lines) + " kept " + Show(level.Split(new[] { '\r', '\n' })));
            Log("cells " + Show(lines[1].Split(new[] { ',' })) + " " + Show(lines[1].Split(',')) + " "
                + Show(lines[1].Split(new[] { ',' }, StringSplitOptions.RemoveEmptyEntries)));
            Log("count " + Show("a,b,c,d".Split(new[] { ',' }, 2)) + " " + Show("a,,b,,c".Split(new[] { ',' }, 2,
                StringSplitOptions.RemoveEmptyEntries)) + " " + Show(",a,b".Split(new[] { ',' }, 1)) + " "
                + Show("a,b".Split(new[] { ',' }, 0)));
            Log("white " + Show(" a  b\tc ".Split(null)) + " " + Show(" a  b\tc ".Split((char[])null,
                StringSplitOptions.RemoveEmptyEntries)) + " empty " + Show("".Split(',')) + " "
                + Show("".Split(new[] { ',' }, StringSplitOptions.RemoveEmptyEntries)) + " " + Show(",".Split(',')));
            Log("strings " + Show("a::b:c".Split(new[] { "::", ":" }, StringSplitOptions.None)) + " "
                + Show("a--b--".Split(new[] { "--" }, StringSplitOptions.RemoveEmptyEntries)) + " "
                + Show("one and two and three".Split(new[] { " and " }, 2, StringSplitOptions.None)));

            Log("parse " + Parsed("42") + " " + Parsed("-7") + " " + Parsed(" +19 ") + " " + Parsed("1-") + " "
                + Parsed("") + " " + Parsed(null) + " " + Parsed("2147483647") + " " + Parsed("2147483648") + " "
                + Parsed("-2147483648") + " " + Parsed("99999999999999999999") + " " + Parsed("1.5") + " "
                + Parsed("x") + " " + Parsed("007"));
            string thrown = "";
            try
            {
                thrown += int.Parse(" 12 ");
                thrown += int.Parse("twelve");
            }
            catch (FormatException)
            {
                thrown += " format";
            }
            try
            {
                thrown += int.Parse("3000000000");
            }
            catch (OverflowException)
            {
                thrown += " overflow";
            }
            Log("int.Parse " + thrown);

            int[,] cells = new int[lines.Length, 3];
            for (int i = 0; i < lines.Length; i++)
            {
                string[] row = lines[i].Split(new[] { ',' });
                for (int j = 0; j < 3; j++)
                {
                    int v;
                    cells[i, j] = int.TryParse(row[j], out v) ? v : -1;
                }
            }
            Log("grid " + cells[0, 2] + " " + cells[1, 1] + " " + cells[2, 1] + " " + cells[2, 2]);
        }

        private static void Dictionaries()
        {
            Section("dictionaries");
            // Keys that are objects: the order is the order they were added in.
            Holder first = new Holder();
            Holder second = new Holder();
            Holder third = new Holder();
            Dictionary<Holder, Vec2> where = new Dictionary<Holder, Vec2>();
            where.Add(second, new Vec2(2, 0));
            where.Add(first, new Vec2(1, 0));
            where.Add(third, new Vec2(3, 0));
            Vec2 found;
            bool has = where.TryGetValue(first, out found);
            Vec2 missing = new Vec2(8, 8);
            bool hasNot = where.TryGetValue(new Holder(), out missing);
            Log("out struct " + has + found + " " + hasNot + missing);
            found.x = 50;
            where[third] = found;
            found.y = 60;
            string walk = "";
            Holder match = null;
            foreach (KeyValuePair<Holder, Vec2> pair in where)
            {
                walk += pair.Value;
                if (pair.Value.x == 50)
                {
                    match = pair.Key;
                }
            }
            Log("pairs " + walk + " match=" + (match == third) + " count=" + where.Count);

            Dictionary<string, int> scores = new Dictionary<string, int>();
            scores["zed"] = 3;
            scores["amy"] = 1;
            scores.Add("bob", 2);
            int score;
            bool gotAmy = scores.TryGetValue("amy", out score);
            int none = 99;
            bool gotNone = scores.TryGetValue("nobody", out none);
            Log("out int " + gotAmy + score + " " + gotNone + none);
            scores.Remove("amy");
            scores["cat"] = 4;
            scores["dan"] = 5;
            scores["zed"] += 10;
            string names = "";
            int total = 0;
            foreach (KeyValuePair<string, int> pair in scores)
            {
                names += pair.Key + "=" + pair.Value + " ";
            }
            foreach (string key in scores.Keys)
            {
                names += key;
            }
            foreach (int value in scores.Values)
            {
                total += value;
            }
            Log("order " + names + " total=" + total + " " + scores.ContainsValue(4) + scores.ContainsValue(1)
                + scores.Keys.Count);

            Dictionary<int, string> byNumber = new Dictionary<int, string>(8);
            for (int i = 0; i < 6; i++)
            {
                byNumber[i * 7] = "n" + i;
            }
            byNumber.Remove(14);
            byNumber.Remove(0);
            byNumber[100] = "x";
            byNumber[200] = "y";
            byNumber[300] = "z";
            string numbered = "";
            foreach (var pair in byNumber)
            {
                numbered += pair.Key + pair.Value + " ";
            }
            string name;
            Log("slots " + numbered + byNumber.TryGetValue(21, out name) + name + byNumber.TryGetValue(14, out name)
                + (name == null));
            try
            {
                foreach (var pair in byNumber)
                {
                    byNumber[pair.Key] = "same";
                    byNumber[pair.Key + 1] = "more";
                }
            }
            catch (InvalidOperationException)
            {
                Log("changed while walked");
            }
            byNumber.Clear();
            byNumber[5] = "five";
            foreach (var pair in byNumber)
            {
                Log("cleared " + byNumber.Count + " " + pair.Key + pair.Value);
            }
        }

        private static void StaticInit()
        {
            Section("static init");
            Log("before");
            int n = Order.Next();
            Log("n=" + n + " again=" + Order.Next() + " first=" + Order.First);
        }

        public static void Main(string[] args)
        {
            ValueTypes();
            RefAndOut();
            Generics();
            Delegates();
            EnumsAndProperties();
            Strings();
            Exceptions();
            Iterators();
            BoxingAndTypes();
            Arithmetic();
            IntegerEdges();
            MultiDimensional();
            SplittingAndParsing();
            Dictionaries();
            StaticInit();
            Console.WriteLine("done");
        }
    }
}
