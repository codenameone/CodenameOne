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
using System.Linq;

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

    // No Equals and no GetHashCode of its own: what a struct gets from
    // ValueType, which compares each field with that field's Equals.
    public struct Reading
    {
        public float level;
        public double fine;
        public int tag;
    }

    // A field of a type argument that nothing assigns.
    /// Equal by IEquatable alone: Equals(object) is not overridden.
    public class EqCell : IEquatable<EqCell>
    {
        public int X;
        public EqCell(int x) { X = x; }
        public bool Equals(EqCell other) { return other != null && other.X == X; }
        public override int GetHashCode() { return X; }
    }

    public class Slot<T>
    {
        public T held;
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

    // Enums whose values are not in ascending order as written, or are stored
    // as something other than an int.
    public enum Signed
    {
        All = -1,
        None = 0,
        One = 1
    }

    public enum Big : long
    {
        Small = 1,
        Huge = 5000000000,
        Neg = -2
    }

    public enum Tiny : byte
    {
        A = 200,
        B = 3
    }

    public enum Wide : ushort
    {
        A = 60000,
        B = 3
    }

    public enum Unsigned : uint
    {
        Top = 4000000000,
        Low = 1
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

    public enum Pitch : byte { Low = 1, High = 200 }

    public enum Span : uint { Small = 1, Big = 4000000000 }

    public delegate void Mine(int x);

    public delegate void Yours(int x);

    // Static state of generic types: every constructed type has its own
    // variables and its own run of the static constructor, although all of
    // them are one class once generics are erased.
    public struct GsVec { public int x; public int y; public GsVec(int x, int y) { this.x = x; this.y = y; } }

    public class GsSingleton<T> where T : GsSingleton<T>
    {
        static T instance;
        public static int Made;
        public static T Instance { get { return instance; } }
        public static bool Has { get { return instance != null; } }
        public string Tag;
        protected GsSingleton() { Made++; if (instance == null) instance = this as T; }
        public T Self() { return instance; }
        public static void Reset() { instance = null; }
    }
    public sealed class GsGame : GsSingleton<GsGame> { public int score = 7; public GsGame() { Tag = "game"; } }
    public sealed class GsAudio : GsSingleton<GsAudio> { public string clip = "beep"; public GsAudio() { Tag = "audio"; } }

    public static class GsLog { public static int Inits; public static string Order = ""; }

    public static class GsCache<T>
    {
        public static T Value;
        public static int Hits;
        public static readonly List<T> Items = new List<T>();
        public static readonly T[] Empty = new T[0];
        static GsCache() { GsLog.Inits++; GsLog.Order += (typeof(T) == typeof(int) ? "int" : typeof(T) == typeof(string) ? "string" : "other") + ";"; Hits = 100; }
        public static void Put(T v) { Value = v; Hits++; Items.Add(v); }
        public static T Get() { Hits++; return Value; }
        public static int Count { get { return Items.Count; } }
    }

    public static class GsBus<T>
    {
        public static event Action<T> Raised;
        public static void Raise(T v) { var h = Raised; if (h != null) h(v); }
    }

    public static class GsTypeIds { public static int Next; }
    public static class GsTypeId<T> { public static readonly int Value = GsTypeIds.Next++; }

    public class GsCounted<T>
    {
        static int count;
        public readonly int id;
        public T item;
        public GsCounted(T item) { this.item = item; id = ++count; }
        public static int Count { get { return count; } }
        public int Seen() { return count; }
        public GsCounted<T> Another(T v) { return new GsCounted<T>(v); }
        public Func<int> Reader() { return () => count * 10 + id; }
    }

    public class GsOuter<T>
    {
        public static int Total;
        public class Inner
        {
            public static int Mine;
            public void Bump() { Mine++; Total += 2; }
        }
    }

    public class GsMid<T> : GsCounted<T>
    {
        public static string Label = "mid";
        public GsMid(T v) : base(v) { }
        public string Describe() { return Label + ":" + Seen(); }
    }
    public sealed class GsLeaf : GsMid<long> { public GsLeaf() : base(5L) { } }

    public static class GsUse
    {
        public static T Read<T>() { return GsCache<T>.Get(); }
        public static void Write<T>(T v) { GsCache<T>.Put(v); }
        public static int Id<T>() { return GsTypeId<T>.Value; }
        public static int ListId<T>() { return GsTypeId<List<T>>.Value; }
    }

    public enum GsKind : byte { Low = 1, High = 200 }

    public static class GsPool<T>
    {
        static readonly List<T> free = new List<T>();
        public static int Returned;
        public static Func<T> Factory;
        public static void Return(T item) { free.Add(item); Returned++; }
        public static T Take() { if (free.Count == 0) return Factory(); T last = free[free.Count - 1]; free.RemoveAt(free.Count - 1); return last; }
        public static int Free { get { return free.Count; } }
        public static IEnumerable<T> All() { foreach (T t in free) yield return t; }
        public static int CountWhere(Func<T, bool> test) { return free.Count(x => test(x) && Returned > 0); }
        public static Action<T> Returner() { return Return; }
        public static Func<int> Counter() { return () => Returned; }
        public static string Name() { return typeof(T) == typeof(int) ? "int" : typeof(T) == typeof(GsKind) ? "kind" : typeof(T) == typeof(string) ? "string" : "?"; }
        public static U Convert<U>(Func<T, U> f) { GsPool<U>.Return(f(Take())); return GsPool<U>.Take(); }
        public static void Bump(ref int by) { by += Returned; }
    }

    public class GsWrapper<T>
    {
        public int Returned() { return GsPool<T>.Returned; }
        public static int Twice() { return GsPool<T>.Returned * 2; }
        public GsWrapper<T[]> Lift() { return null; }
        public int Lifted() { return GsPool<List<T>>.Returned; }
    }

    public abstract class GsBase<T>
    {
        protected static int made;
        public static string Last = "none";
        protected GsBase() { made++; Announce(); }
        protected virtual void Announce() { Last = "base"; }
        public virtual string Who() { return "base" + made; }
        public static int Made { get { return made; } }
    }
    public class GsDerived<T> : GsBase<T>
    {
        static int derived;
        public GsDerived() { derived++; }
        protected override void Announce() { Last = "derived"; }
        public override string Who() { return "derived" + made + "/" + derived + "/" + base.Who(); }
    }
    public sealed class GsFinal : GsDerived<GsVec> { }

    public struct GsSlot<T>
    {
        public static int Count;
        public T value;
        public static GsSlot<T> Make(T v) { Count++; GsSlot<T> s; s.value = v; return s; }
    }

    public static class GsMath
    {
        public static int Sum<T>(T a, T b) where T : IComparable<T> { GsPool<T>.Returned += 10; return a.CompareTo(b) + GsPool<T>.Returned; }
        public static int Relay<T>(T a) where T : IComparable<T> { return Sum(a, a) + Sum("a", "b"); }
        public static Func<int> Later<T>(T v) { return () => { GsPool<T>.Return(v); return GsPool<T>.Returned; }; }
    }

    // Delegates and typeof made by generic code from its own type
    // parameters, in code that has static state per constructed type.
    public class DkKeyed<T>
    {
        public static int Made;
        public Action<T> Make() { Made++; return x => { }; }
        public static Action<T> MakeStatic() { Made++; return x => { }; }
        public static Type ListType() { return typeof(List<T>); }
    }

    public static class DkMethods
    {
        public static Action<U> Make<U>() { DkKeyed<U>.Made++; return x => { }; }
        public static Type ListOf<U>() { DkKeyed<U>.Made++; return typeof(Dictionary<string, U>); }
    }

    public class DkNest<T> { public class Inner { } }

    // Generic members of the program's own that take a T[], handed arrays
    // of primitives.
    public static class GaArrays
    {
        public static void Reverse<T>(T[] a)
        {
            for (int i = 0, j = a.Length - 1; i < j; i++, j--)
            {
                T t = a[i];
                a[i] = a[j];
                a[j] = t;
            }
        }

        public static void Clear<T>(T[] a, int at) { a[at] = default(T); }

        public static string Join<T>(T[] a)
        {
            string s = "";
            foreach (T x in a) s += x + ";";
            return s;
        }
    }

    public class GaHolder<T>
    {
        public readonly string Seen;
        public GaHolder(T[] a) { Seen = GaArrays.Join(a); a[0] = a[a.Length - 1]; }
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
            // One null or empty string separator splits nowhere; only a null array means white space.
            Log("nosep " + Show("a b".Split((string)null, StringSplitOptions.None)) + " "
                + Show("a b".Split("", StringSplitOptions.RemoveEmptyEntries)) + " "
                + Show("a b".Split(new string[] { "", null }, StringSplitOptions.None)) + " "
                + Show("a b".Split((string[])null, StringSplitOptions.None)));

            // A method that hides a base one is bound by the static type, and a
            // private helper of the same name does not override the base's.
            HideDerived hd = new HideDerived();
            HideBase hb = hd;
            Log("hiding " + hb.Name() + " " + hd.Name() + " " + hb.Virt() + " " + hd.Virt() + " " + hb.Ask() + " "
                + hd.AskMine() + " " + ((HideBase)new HideLeaf()).Virt() + " " + new HideLeaf().Virt());
            string ftn = "none";
            try
            {
                HideBase gone = null;
                Func<string> f = gone.Virt;
                // Read, never called: an unread delegate is dropped by the compiler.
                ftn = f != null ? "built" : "null";
            }
            catch (NullReferenceException)
            {
                ftn = "NullReference";
            }
            string unboxed = "none";
            try
            {
                object boxed = null;
                HidePoint pt = (HidePoint)boxed;
                unboxed = "unboxed " + pt.X;
            }
            catch (NullReferenceException)
            {
                unboxed = "NullReference";
            }
            Log("null receiver " + ftn + " " + unboxed);

            // conv.u8 of a floating value: the upper half of ulong, and
            // .NET's saturation either side of the range.
            double[] wide = { 1e19, 9.3e18, 1e20, -1.0, double.NaN, 3.5 };
            string u8 = "";
            for (int i = 0; i < wide.Length; i++)
            {
                double dw = wide[i];
                float fw = (float)dw;
                u8 += " " + unchecked((ulong)dw) + "/" + unchecked((ulong)fw);
            }
            Log("to ulong" + u8);

            // Tuples: the typed Equals overload, and defaults that are zeros.
            var pairA = (1, 2);
            var pairB = (1, 2);
            (int, int) none = default;
            var pairs = new (int, int)[2];
            object boxedNone = none;
            Log("tuple equals " + pairA.Equals(pairB) + " " + pairA.Equals((2, 1)) + " default " + none
                + " " + boxedNone.Equals((0, 0)) + " array " + pairs[1] + " " + ((object)pairs[0]).Equals((0, 0))
                + " hash " + (none.GetHashCode() == (0, 0).GetHashCode()));

            // The default comparer asks IEquatable<T>, not Equals(object).
            var eqCells = new System.Collections.Generic.Dictionary<EqCell, int>();
            eqCells[new EqCell(3)] = 7;
            var eqList = new System.Collections.Generic.List<EqCell> { new EqCell(4) };
            Log("equatable " + eqCells.ContainsKey(new EqCell(3)) + " " + eqCells.Count + " " + eqList.Contains(new EqCell(4))
                + " " + ((object)new EqCell(5)).Equals(new EqCell(5)) + " " + (new EqCell(6), 1).Equals((new EqCell(6), 1)));

            // Exception.ToString: the .NET type name and the inner chain.
            string chained = new InvalidOperationException("outer", new ArgumentException("inner")).ToString();
            Log("exception text " + chained.Replace("\n", "|") + " / " + new Exception() + " / " + new Exception("")
                + " / " + new System.Collections.Generic.KeyNotFoundException("k"));

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

        // ---- System.Enum ---------------------------------------------------------

        // What a call gives, or the kind of exception it ends in. Only the kind:
        // the wording of a message changes between versions of .NET.
        private static string Outcome(Func<object> call)
        {
            try
            {
                object o = call();
                return o == null ? "null" : o.ToString();
            }
            catch (ArgumentNullException)
            {
                return "ArgumentNull";
            }
            catch (ArgumentException)
            {
                return "Argument";
            }
            catch (OverflowException)
            {
                return "Overflow";
            }
            catch (InvalidOperationException)
            {
                return "InvalidOperation";
            }
            catch (FormatException)
            {
                return "Format";
            }
        }

        private static void EnumMethods()
        {
            Section("enum methods");
            // Values and names come sorted by value as an unsigned number, in an
            // array of the enum's own integer type.
            string s = "";
            foreach (Signed v in (Signed[])Enum.GetValues(typeof(Signed)))
            {
                s += v + "=" + (int)v + " ";
            }
            foreach (string name in Enum.GetNames(typeof(Signed)))
            {
                s += name + " ";
            }
            Log("signed " + s);
            Big[] bigs = (Big[])Enum.GetValues(typeof(Big));
            s = "";
            for (int i = 0; i < bigs.Length; i++)
            {
                s += bigs[i] + "=" + (long)bigs[i] + " ";
            }
            Tiny[] tinies = (Tiny[])Enum.GetValues(typeof(Tiny));
            Wide[] wides = (Wide[])Enum.GetValues(typeof(Wide));
            Unsigned[] unsigneds = (Unsigned[])Enum.GetValues(typeof(Unsigned));
            Log("big " + s + "tiny " + tinies[0] + "=" + (int)tinies[0] + " " + tinies[1] + "=" + (int)tinies[1]
                + " wide " + wides[0] + " " + wides[1] + "=" + (int)wides[1] + " unsigned " + unsigneds[0] + " "
                + unsigneds[1] + "=" + (uint)unsigneds[1]);
            int phases = 0;
            foreach (Phase p in Enum.GetValues(typeof(Phase)))
            {
                phases = phases * 10 + (int)p;
            }
            object boxedWide = Wide.A;
            object boxedTiny = Tiny.A;
            object boxedBig = Big.Huge;
            Log("walked " + phases + " unboxed " + (int)(Wide)boxedWide + " " + (int)(Tiny)boxedTiny + " "
                + (long)(Big)boxedBig + " undefined " + (Unsigned)3000000000 + " " + (Tiny)250 + " " + (Wide)65535
                + " " + (Big)(-7) + " " + (Signed)(-1));
            Log("defined " + Enum.IsDefined(typeof(Unsigned), Unsigned.Top) + Enum.IsDefined(typeof(Big), 5000000000)
                + Enum.IsDefined(typeof(Tiny), (byte)200) + Enum.IsDefined(typeof(Tiny), (byte)4)
                + Enum.IsDefined(typeof(Signed), -1) + Enum.IsDefined(typeof(Signed), "One")
                + Enum.IsDefined(typeof(Signed), "one"));

            // A value of another enum is refused, where the numbers alone would
            // have answered; GetName is the one that reads the number out of anything.
            Log("other enum " + Outcome(() => Enum.IsDefined(typeof(Phase), Layers.Ground)) + " "
                + Outcome(() => Enum.IsDefined(typeof(Phase), 1L)) + " "
                + Outcome(() => Enum.IsDefined(typeof(Phase), 1f)) + " "
                + Outcome(() => Enum.IsDefined(typeof(Phase), Phase.Moved)) + " "
                + Outcome(() => Enum.GetName(typeof(Phase), Layers.Ground)) + " "
                + Outcome(() => Enum.GetName(typeof(Phase), 7L)) + " "
                + Outcome(() => Enum.GetName(typeof(Phase), 3)) + " "
                + Outcome(() => Enum.GetName(typeof(Phase), "Moved")));
            // GetName takes an integer of any width and a member of any enum,
            // but compares the whole number: each side widened to 64 bits from
            // its own type, nothing cut down to the enum's width.
            Log("name width " + Outcome(() => Enum.GetName(typeof(Tiny), 3)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), 200L)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), 456)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), (sbyte)-56)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), Layers.Player | Layers.Ground)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), (char)3)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), true)) + " "
                + Outcome(() => Enum.GetName(typeof(Tiny), 3f)) + "|"
                + Outcome(() => Enum.GetName(typeof(Signed), (sbyte)-1)) + " "
                + Outcome(() => Enum.GetName(typeof(Signed), -1L)) + " "
                + Outcome(() => Enum.GetName(typeof(Signed), 4294967295u)) + " "
                + Outcome(() => Enum.GetName(typeof(Signed), 4294967295L)) + " "
                + Outcome(() => Enum.GetName(typeof(Signed), ulong.MaxValue)) + "|"
                + Outcome(() => Enum.GetName(typeof(Unsigned), 4000000000u)) + " "
                + Outcome(() => Enum.GetName(typeof(Unsigned), 4000000000L)) + " "
                + Outcome(() => Enum.GetName(typeof(Unsigned), -294967296)) + " "
                + Outcome(() => Enum.GetName(typeof(Unsigned), Unsigned.Top)) + "|"
                + Outcome(() => Enum.GetName(typeof(Wide), (ushort)60000)) + " "
                + Outcome(() => Enum.GetName(typeof(Wide), (short)-5536)) + " "
                + Outcome(() => Enum.GetName(typeof(Wide), 60000)) + " "
                + Outcome(() => Enum.GetName(typeof(Wide), (char)60000)) + "|"
                + Outcome(() => Enum.GetName(typeof(Big), 1)) + " "
                + Outcome(() => Enum.GetName(typeof(Big), (byte)1)) + " "
                + Outcome(() => Enum.GetName(typeof(Big), 5000000000)) + " "
                + Outcome(() => Enum.GetName(typeof(Big), Tiny.B)) + "|"
                + Outcome(() => Enum.IsDefined(typeof(Wide), (char)3)) + " "
                + Outcome(() => Enum.IsDefined(typeof(Tiny), true)) + " "
                + Outcome(() => Enum.ToObject(typeof(Tiny), 456)) + " "
                + Outcome(() => Enum.ToObject(typeof(Unsigned), -294967296)));
            Enum ground = Layers.Ground;
            Layers both = Layers.Ground | Layers.Enemy;
            Phase moved = Phase.Moved;
            Log("has flag " + Outcome(() => ground.HasFlag(Phase.Moved)) + " "
                + Outcome(() => ground.HasFlag(Layers.Ground)) + " " + Outcome(() => ground.HasFlag(null)) + " "
                + Outcome(() => both.HasFlag(Layers.Enemy)) + " " + Outcome(() => both.HasFlag(Layers.Player)) + " "
                + Outcome(() => both.HasFlag(Phase.Moved)));
            Log("compare " + Outcome(() => ground.CompareTo(Phase.Moved)) + " "
                + Outcome(() => ground.CompareTo(1)) + " " + Outcome(() => ground.CompareTo(null)) + " "
                + Outcome(() => ground.CompareTo(Layers.Enemy)) + " " + Outcome(() => moved.CompareTo(Phase.Began)) + " "
                + Outcome(() => moved.CompareTo(Layers.Ground)) + " "
                + Outcome(() => Unsigned.Top.CompareTo(Unsigned.Low)) + " "
                + Outcome(() => Signed.All.CompareTo(Signed.One)) + " equals " + ground.Equals(Phase.Moved)
                + ground.Equals(1) + ground.Equals(Layers.Ground) + moved.Equals(Phase.Moved)
                + moved.Equals(Layers.Ground) + moved.Equals(1));

            // Parse: one name or several with commas between, or a number.
            Log("parse " + Outcome(() => Enum.Parse(typeof(Layers), "Ground, Enemy")) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "  Ground ,Player  ")) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "ground,ENEMY", true)) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "ground,ENEMY")) + "|"
                + Outcome(() => (int)(Phase)Enum.Parse(typeof(Phase), "Moved, Ended")) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "\u00a0Ground\u2003")) + "|"
                + Outcome(() => (long)(Big)Enum.Parse(typeof(Big), "Huge")) + "|"
                + Outcome(() => Enum.Parse(typeof(Phase), "Ended")));
            Log("parse bad " + Outcome(() => Enum.Parse(typeof(Layers), "1, Enemy")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "Enemy, 1")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "   ")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "Ground,")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "Ground,,Enemy")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), ",Enemy")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), "Nope")) + " "
                + Outcome(() => Enum.Parse(typeof(Layers), null)));
            Log("parse number " + Outcome(() => Enum.Parse(typeof(Layers), " 6 ")) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "+3")) + "|"
                + Outcome(() => (int)(Layers)Enum.Parse(typeof(Layers), "-1")) + "|"
                + Outcome(() => Enum.Parse(typeof(Layers), "5000000000")) + "|"
                + Outcome(() => Enum.Parse(typeof(Tiny), "300")) + "|"
                + Outcome(() => Enum.Parse(typeof(Tiny), "-1")) + "|"
                + Outcome(() => Enum.Parse(typeof(Tiny), "200")) + "|"
                + Outcome(() => Enum.Parse(typeof(Unsigned), "4000000000")) + "|"
                + Outcome(() => Enum.Parse(typeof(Unsigned), "4294967296")) + "|"
                + Outcome(() => Enum.Parse(typeof(Big), "-9223372036854775808")) + "|"
                + Outcome(() => Enum.Parse(typeof(Big), "9223372036854775808")) + "|"
                + Outcome(() => Enum.Parse(typeof(Signed), "-")));
            object parsed;
            bool ok = Enum.TryParse(typeof(Layers), "Ground, Enemy", out parsed);
            string tried = ok + " " + parsed;
            ok = Enum.TryParse(typeof(Layers), "player", true, out parsed);
            tried += " " + ok + " " + parsed;
            ok = Enum.TryParse(typeof(Layers), "Nope", out parsed);
            tried += " " + ok + (parsed == null);
            ok = Enum.TryParse(typeof(Layers), null, out parsed);
            tried += " " + ok + (parsed == null);
            ok = Enum.TryParse(typeof(Tiny), "300", out parsed);
            Log("try parse " + tried + " " + ok + (parsed == null));
        }

        // ---- the base library, where it differs from java.lang ----------------------

        private static int released;

        // An iterator that holds something: its finally block is what a using
        // statement or a lock inside a real one comes down to.
        private static IEnumerable<int> Guarded(int limit)
        {
            try
            {
                for (int i = 0; i < limit; i++)
                {
                    yield return i;
                }
            }
            finally
            {
                released++;
            }
        }

        private static void BaseLibrary()
        {
            Section("base library");
            // A query that stops early still disposes the enumerator, which is
            // what runs the iterator's finally block.
            string linq = "";
            linq += Guarded(5).Any() + "/" + released;
            linq += " " + Guarded(5).First() + "/" + released;
            linq += " " + Guarded(5).Contains(2) + "/" + released;
            linq += " " + Guarded(5).ElementAt(3) + "/" + released;
            linq += " " + Guarded(5).Any(v => v == 1) + "/" + released;
            linq += " " + Guarded(5).All(v => v < 2) + "/" + released;
            linq += " " + Guarded(5).First(v => v > 2) + "/" + released;
            linq += " " + Guarded(5).FirstOrDefault() + "/" + released;
            linq += " " + Guarded(5).FirstOrDefault(v => v > 3) + "/" + released;
            int taken = 0;
            foreach (int v in Guarded(5).Take(2))
            {
                taken = taken * 10 + v + 1;
            }
            linq += " " + taken + "/" + released;
            linq += " " + Guarded(5).Count() + "/" + released;
            linq += " " + Guarded(5).Last() + "/" + released;
            linq += " " + Guarded(4).ToList().Count + "/" + released;
            List<int> grown = new List<int>();
            grown.AddRange(Guarded(3));
            linq += " " + grown.Count + "/" + released;
            try
            {
                linq += Guarded(5).First(v => v / (v - 2) > 9);
            }
            catch (DivideByZeroException)
            {
                linq += " thrown/" + released;
            }
            try
            {
                linq += Guarded(0).First();
            }
            catch (InvalidOperationException)
            {
                linq += " empty/" + released;
            }
            Log("linq " + linq);

            // A type is its .NET name: int and uint are one class on the JVM.
            Log("types " + (typeof(int) == typeof(uint)) + (typeof(long) == typeof(ulong))
                + (typeof(byte) == typeof(sbyte)) + (typeof(short) == typeof(ushort)) + (typeof(char) == typeof(ushort))
                + (typeof(int) == typeof(int)) + (typeof(uint) == typeof(uint)) + (typeof(ulong) != typeof(long))
                + " " + typeof(uint).FullName + " " + typeof(ulong).FullName + " " + typeof(int).FullName + " "
                + typeof(sbyte) + " " + typeof(ushort) + " " + typeof(Holder).FullName
                + (typeof(Holder) == typeof(Shape)));

            // Zero and negative zero are one number, so they are one key.
            Dictionary<float, string> byFloat = new Dictionary<float, string>();
            byFloat[0f] = "pos";
            string zeros = byFloat.ContainsKey(-0f) + " " + byFloat[-0f] + " " + byFloat.Count;
            byFloat[-0f] = "neg";
            byFloat[float.NaN] = "nan";
            zeros += " " + byFloat.Count + " " + byFloat[0f] + " " + byFloat.ContainsKey(float.NaN) + " "
                + byFloat.Remove(-0f) + " " + byFloat.Count;
            Dictionary<double, int> byDouble = new Dictionary<double, int>();
            byDouble.Add(-0.0, 1);
            try
            {
                byDouble.Add(0.0, 2);
                zeros += " added";
            }
            catch (ArgumentException)
            {
                double kept = 0;
                foreach (double key in byDouble.Keys)
                {
                    kept = key;
                }
                // The key that went in first is the one kept, sign and all.
                zeros += " same " + byDouble[0.0] + " " + (1 / kept < 0);
            }
            Dictionary<(float, int), int> byPair = new Dictionary<(float, int), int>();
            byPair[(0f, 1)] = 5;
            List<double> doubles = new List<double>();
            doubles.Add(-0.0);
            List<float> floats = new List<float>();
            floats.Add(float.NaN);
            object boxedZero = 0f;
            Log("zeros " + zeros + " " + byPair.ContainsKey((-0f, 1)) + " "
                + doubles.Contains(0.0) + floats.IndexOf(float.NaN) + boxedZero.Equals(-0f)
                + ((0f).GetHashCode() == (-0f).GetHashCode()) + ((0.0).GetHashCode() == (-0.0).GetHashCode()));

            // Only a doubled brace is a literal one.
            Log("format " + Outcome(() => string.Format("a}b", 1)) + " " + Outcome(() => string.Format("a}}b{0}{{", 1))
                + " " + Outcome(() => string.Format("{0}}}", 1)) + " " + Outcome(() => string.Format("{0}}", 1)) + " "
                + Outcome(() => string.Format("{0", 1)) + " " + Outcome(() => string.Format("}", 1)));

            // Trim takes off what char.IsWhiteSpace calls white space: a no-break
            // space and an ideographic one, and not a control character.
            string padded = "\u0001\u00a0 x\u3000\u0001 \u2003";
            string trimmed = padded.Trim();
            Log("trim " + trimmed.Length + (int)trimmed[0] + " " + "\u00a0 x \u3000".TrimStart().Length
                + "\u00a0 x \u3000".TrimEnd().Length + " " + "xxaxx".Trim('x') + " " + "xyaxy".TrimStart('x', 'y') + " "
                + "xyaxy".TrimEnd('x', 'y') + " " + " \u0001 ".Trim().Length + "\u200b ".Trim().Length
                + "\ufeff".Trim().Length + "\t\r\n a b \u000b\u000c\u0085".Trim().Length + "   ".Trim().Length + "[" + "".Trim()
                + "]");

            // char.IsDigit asks Unicode for a decimal digit of any script;
            // int.Parse reads the ASCII ten alone. Every char is asked, and
            // the answers summed, so a run of ten that is off by one shows.
            char[] probes = { '0', '9', '/', ':', 'a', '\u0663', '\u06f9', '\u0966', '\u0e59', '\uff15', '\uff1a',
                '\u00b2', '\u2460', '\u3007', '\u0669', '\u066a', '\ua9f0', '\uabf9', '\uabfa', '\ud835' };
            string digits = "";
            for (int i = 0; i < probes.Length; i++)
            {
                digits += char.IsDigit(probes[i]) ? "1" : "0";
            }
            int digitCount = 0;
            long digitSum = 0;
            for (int c = 0; c < 65536; c++)
            {
                if (char.IsDigit((char)c))
                {
                    digitCount++;
                    digitSum += c;
                }
            }
            int arabic;
            bool parsed = int.TryParse("\u0663", out arabic);
            Log("digits " + digits + " " + digitCount + " " + digitSum + " " + parsed + arabic + " "
                + Outcome(() => int.Parse("\uff15")) + " " + Outcome(() => Enum.Parse(typeof(Layers), "\u0663")));

            // A collection made from another: an array, a list, an iterator,
            // whose finally block runs, and a dictionary. Each is a copy.
            int[] numbers = { 3, 1, 2 };
            List<int> fromArray = new List<int>(numbers);
            numbers[0] = 9;
            List<int> fromList = new List<int>(fromArray);
            fromArray.Add(4);
            List<int> fromIterator = new List<int>(Guarded(3));
            Vec2[] spots = { new Vec2(1, 2), new Vec2(3, 4) };
            List<Vec2> fromStructs = new List<Vec2>(spots);
            List<Vec2> ranged = new List<Vec2>();
            ranged.AddRange(spots);
            spots[0].x = 7;
            Log("copies " + fromArray.Count + fromArray[0] + " " + fromList.Count + fromList[2] + " " + fromIterator.Count
                + "/" + released + " " + fromStructs[0] + ranged[0] + spots[0] + " " + new List<string>(new string[0]).Count
                + " " + Outcome(() => new List<int>((IEnumerable<int>)null)));
            Dictionary<string, int> source = new Dictionary<string, int>();
            source.Add("a", 1);
            source.Add("b", 2);
            source.Add("c", 3);
            source.Remove("b");
            source.Add("d", 4);
            Dictionary<string, int> copy = new Dictionary<string, int>(source);
            source["a"] = 100;
            IDictionary<string, int> through = copy;
            through["e"] = 5;
            through.Add("f", 6);
            through.Remove("c");
            int got;
            string walked = "";
            foreach (KeyValuePair<string, int> pair in copy)
            {
                walked += pair.Key + pair.Value;
            }
            Log("dictionary copy " + walked + " " + through.Count + through.ContainsKey("d") + through.TryGetValue("a", out got)
                + got + through["f"] + " " + Outcome(() => new Dictionary<string, int>((IDictionary<string, int>)null)));
        }

        // float.Equals is not ==: NaN equals NaN and zero equals negative
        // zero, and a struct that leaves equality to ValueType compares its
        // fields that way, so one holding a NaN is still a key that is found.
        private static void StructEquality()
        {
            Section("struct equality");
            Reading nan = new Reading();
            nan.level = float.NaN;
            nan.fine = 1.5;
            nan.tag = 1;
            Reading same = nan;
            Reading zero = new Reading();
            Reading negative = new Reading();
            negative.level = -0f;
            negative.fine = -0.0;
            Log("nan " + nan.Equals(same) + (nan.GetHashCode() == same.GetHashCode()) + " zero " + zero.Equals(negative)
                + (zero.GetHashCode() == negative.GetHashCode()) + " prim " + (nan.level == same.level)
                + nan.level.Equals(same.level));
            Reading fineNan = zero;
            fineNan.fine = double.NaN;
            Reading fineSame = fineNan;
            Reading other = nan;
            other.tag = 2;
            Reading number = nan;
            number.level = 3f;
            Reading wide = fineNan;
            wide.fine = 2.0;
            Log("double nan " + fineNan.Equals(fineSame) + " differ " + nan.Equals(other) + nan.Equals(number)
                + number.Equals(nan) + fineNan.Equals(wide) + wide.Equals(fineNan) + nan.Equals(zero));
            Dictionary<Reading, string> byReading = new Dictionary<Reading, string>();
            byReading[nan] = "nan";
            byReading[zero] = "zero";
            byReading[same] = "nan again";
            byReading[negative] = "zero again";
            byReading[fineNan] = "fine";
            Log("keys " + byReading.Count + byReading.ContainsKey(same) + byReading.ContainsKey(negative)
                + byReading.ContainsKey(number) + byReading.ContainsKey(fineSame) + " " + byReading[nan] + "/"
                + byReading[negative] + "/" + byReading[fineSame]);
        }

        // A struct handed to a delegate is the delegate's own, whoever calls
        // it: generic library code that walks a list or an array, or the
        // invocation list of a multicast delegate. What the lambda does to
        // its parameter stays in the lambda, and what it does to the
        // collection does not change the value it was handed.
        private static void StructsThroughDelegates()
        {
            Section("structs through delegates");
            List<Vec2> points = new List<Vec2>();
            points.Add(new Vec2(1, 2));
            points.Add(new Vec2(3, 4));
            int far = points.Where(p => { p.x += 10; return p.x > 10; }).Count();
            Func<Vec2, Vec2> grow = p => { p.Scale(2); return p; };
            string grown = "";
            foreach (Vec2 g in points.Select(grow))
            {
                grown += g;
            }
            Log("list where " + far + " select " + grown + " kept " + points[0] + points[1]);

            Vec2[] cells = new Vec2[] { new Vec2(1, 2), new Vec2(3, 4) };
            bool any = cells.Any(p => { p.y = 77; return false; });
            Log("array any " + any + " kept " + cells[0] + cells[1]);

            // The lambda writes the array while it still reads its argument.
            string seen = "";
            foreach (float x in cells.Select(p => { cells[0].x = 50; cells[1] = new Vec2(60, 0); return p.x; }))
            {
                seen += (int)x + " ";
            }
            Log("array select saw " + seen + "now " + cells[0] + cells[1]);

            // A list made from an array has the values, not the elements.
            List<Vec2> taken = new List<Vec2>(cells);
            List<Vec2> listed = cells.ToList();
            cells[0].x = -1;
            cells[1] = new Vec2(-2, -2);
            Log("taken " + taken[0] + taken[1] + " listed " + listed[0] + listed[1] + " cells " + cells[0] + cells[1]);

            Action<Vec2> first = p => { p.x = 9; Log("first " + p); };
            Action<Vec2> second = p => Log("second " + p);
            Action<Vec2> each = first + second;
            each(cells[0]);
            each(new Vec2(5, 5));
            Log("after " + cells[0]);
        }

        // Erased library code carries a struct as an object, and a missing
        // one as null. What comes back out is a value all the same: the
        // default of an empty query is a struct with every field zero, and a
        // tuple copied is copied all the way down, since a struct inside a
        // struct is part of it.
        private static void ErasedStructValues()
        {
            Section("erased struct values");
            List<Vec2> none = new List<Vec2>();
            Vec2 first = none.FirstOrDefault();
            Vec2 last = none.LastOrDefault();
            Vec2 found = none.FirstOrDefault(p => p.x > 5);
            first.x = 3;
            Log("defaults " + first + last + found + none.FirstOrDefault() + none.FirstOrDefault().SqrMagnitude);
            List<Segment> lines = new List<Segment>();
            Segment line = lines.LastOrDefault();
            line.to.y = 2;
            Log("nested default " + line.from + line.to + lines.FirstOrDefault(s => s.to.x > 0).to);
            Dictionary<string, Vec2> places = new Dictionary<string, Vec2>();
            Vec2 missing = new Vec2(9, 9);
            bool got = places.TryGetValue("nowhere", out missing);
            Log("missing " + got + missing);

            (Vec2, int) pair = (new Vec2(1, 2), 3);
            (Vec2, int) copy = pair;
            copy.Item1.x = 50;
            copy.Item2 = 4;
            Log("tuple " + pair.Item1 + pair.Item2 + " copy " + copy.Item1 + copy.Item2);
            (int, Vec2, Segment) triple = (1, new Vec2(3, 4), new Segment());
            (int, Vec2, Segment) other = triple;
            other.Item2.Scale(2);
            other.Item3.to.x = 8;
            Log("triple " + triple.Item2 + triple.Item3.to + " copy " + other.Item2 + other.Item3.to);
            ((Vec2, int), int) outer = (pair, 5);
            ((Vec2, int), int) inner = outer;
            inner.Item1.Item1.y = 60;
            Log("tuple in tuple " + outer.Item1.Item1 + " copy " + inner.Item1.Item1 + " source " + pair.Item1);
            (Vec2, int)[] pairs = new (Vec2, int)[2];
            pairs[0] = pair;
            pairs[1] = pairs[0];
            pairs[1].Item1.y = 70;
            List<(Vec2, int)> kept = new List<(Vec2, int)>();
            kept.Add(pair);
            (Vec2, int) back = kept[0];
            back.Item1.x = 90;
            Log("tuple array " + pairs[0].Item1 + pairs[1].Item1 + " list " + kept[0].Item1 + back.Item1 + " source "
                + pair.Item1);
            (Vec2, int) blank = default((Vec2, int));
            Log("blank " + blank.Item1 + blank.Item2 + new List<(Vec2, int)>().FirstOrDefault().Item1);
            blank.Item1.y = 7;
            blank.Item2 += 2;
            Log("blank written " + blank.Item1 + blank.Item2);
            Slot<Vec2> slot = new Slot<Vec2>();
            slot.held.x = 4;
            Log("generic default " + Nothing<int>() + Nothing<Vec2>() + (Nothing<string>() == null) + Nothing<bool>()
                + (int)new Slot<float>().held + new Slot<long>().held + slot.held);
            KeyValuePair<string, Vec2> entry = new KeyValuePair<string, Vec2>("a", new Vec2(1, 1));
            KeyValuePair<string, Vec2> same = entry;
            Vec2 held = same.Value;
            held.x = 5;
            Log("pair " + entry.Value + same.Value + held);
        }

        private static T Nothing<T>()
        {
            return default(T);
        }

        // The angle of a point on an axis depends on which zero it has: a
        // negative zero is below the axis, or left of it.
        private static string Angle(double y, double x)
        {
            double a = Math.Atan2(y, x);
            if (!(a >= 0) && !(a < 0))
            {
                return "nan";
            }
            if (a == 0)
            {
                return 1 / a < 0 ? "-0" : "0";
            }
            return "" + (int)Math.Round(a * 1000000);
        }

        private static void Angles()
        {
            Section("angles");
            double[] values = new double[] { 0.0, -0.0, 1, -1, double.NaN, double.PositiveInfinity,
                double.NegativeInfinity, 2.5 };
            for (int y = 0; y < values.Length; y++)
            {
                string row = "";
                for (int x = 0; x < values.Length; x++)
                {
                    row += Angle(values[y], values[x]) + " ";
                }
                Log("atan2 " + row);
            }
            float zero = 0f;
            float below = (float)Math.Atan2(-zero, -1f);
            Log("float " + (int)(below * 1000) + " " + (int)((float)Math.Atan2(zero, -zero) * 1000));
        }

        private static void StaticInit()
        {
            Section("static init");
            Log("before");
            int n = Order.Next();
            Log("n=" + n + " again=" + Order.Next() + " first=" + Order.First);
        }

        // ---- unsigned values in an object ------------------------------------

        private static void Try(string label, Func<string> f)
        {
            string r;
            try
            {
                r = f();
            }
            catch (InvalidCastException)
            {
                r = "threw InvalidCastException";
            }
            catch (ArgumentException)
            {
                r = "threw ArgumentException";
            }
            catch (Exception)
            {
                r = "threw another exception";
            }
            Log(label + ": " + r);
        }

        private static string BoxText(object o)
        {
            return "" + o;
        }

        private static string BoxKind(object o)
        {
            if (o is uint) return "uint";
            if (o is int) return "int";
            if (o is ulong) return "ulong";
            if (o is long) return "long";
            if (o is byte) return "byte";
            if (o is sbyte) return "sbyte";
            if (o is ushort) return "ushort";
            if (o is char) return "char";
            if (o is short) return "short";
            return "other";
        }

        private static string BoxSwitch(object o)
        {
            switch (o)
            {
                case int i: return "int " + i;
                case uint u: return "uint " + u;
                case char c: return "char " + c;
                case ushort s: return "ushort " + s;
                case sbyte s: return "sbyte " + s;
                case byte b: return "byte " + b;
                case long l: return "long " + l;
                case ulong l: return "ulong " + l;
                default: return "other";
            }
        }

        private static string BoxGeneric<T>(T v)
        {
            return v.ToString() + "/" + BoxText(v);
        }

        // A `byte`, a `ushort`, a `uint` and a `ulong` each share a Java
        // primitive with another C# type, and must not share its box: boxed,
        // each still prints, orders and answers `is` as itself.
        private static void UnsignedBoxes()
        {
            Section("unsigned boxes");
            uint big = 3000000000;
            byte b = 200;
            sbyte sb = -56;
            ulong ul = 18446744073709551615UL;
            ushort us = 65535;
            ushort us65 = 65;
            char ch = 'A';
            // Text: what a format argument, a concatenation and ToString print.
            Try("a1", () => string.Format("{0}", big));
            Try("a2", () => $"{b}");
            Try("a3", () => BoxText(big));
            Try("a4", () => ((object)big).ToString());
            Try("a5", () => "x" + (object)big);
            Try("a6", () => BoxText(ul) + " " + string.Format("{0}|{1}", ul, ul - 1));
            Try("a7", () => BoxText(us) + " " + BoxText(us65) + " " + BoxText(ch));
            Try("a8", () => BoxText(b) + " " + BoxText(sb) + " " + string.Format("{0} {1}", b, sb));
            Try("a9", () => big.ToString() + " " + b.ToString() + " " + ul.ToString() + " " + us.ToString());
            Try("a10", () => "c" + big + " " + b + " " + ul + " " + us + " " + sb + " " + ch);
            Try("a11", () => BoxGeneric(big) + " " + BoxGeneric(b) + " " + BoxGeneric(ul) + " " + BoxGeneric(us) + " " + BoxGeneric(ch) + " " + BoxGeneric(sb));
            Try("a14", () => string.Concat(big, b) + string.Concat((object)us65, (object)ch));
            // Order and generic storage.
            Try("b9", () => big.CompareTo(1u) + " " + b.CompareTo((byte)1) + " " + ul.CompareTo(1ul) + " " + big.Equals(big) + " " + ((IComparable)big).CompareTo((object)1u) + " " + ((IComparable<uint>)big).CompareTo(1u));
            Try("b16", () => { var d = new Dictionary<string, uint>(); d["a"] = big; uint v; d.TryGetValue("a", out v); foreach (var kv in d) return kv.Key + "=" + kv.Value + " " + v + " " + kv; return ""; });

            Try("e1", () => { var l = new List<uint> { 5, 3000000000, 1, 2147483648 }; return l.Contains(3000000000) + " " + l.IndexOf(2147483648) + " " + l[1] + " " + l.First() + " " + l.Last() + " " + l.Count(x => x > 2147483647) + " " + l.Where(x => x > 5).Count() + " " + l.Distinct().Count(); });
            Try("e2", () => { var l = new List<byte> { 5, 200, 1, 128 }; string s = ""; foreach (byte x in l) s += x + ","; return s + l.Contains(200) + " " + l[1] + " " + l.Count(x => x > 127) + " " + l.ToArray()[1] + " " + l.Select(x => x + 1).First(); });
            Try("e3", () => { var d = new Dictionary<uint, string>(); d[3000000000] = "a"; d[1] = "b"; string s = ""; foreach (var kv in d) s += kv.Key + "=" + kv.Value + ","; return s + d[3000000000] + d[1] + d.ContainsKey(3000000000); });
            Try("e4", () => { var d = new Dictionary<byte, int>(); d[200] = 7; string s = ""; foreach (var kv in d) s += kv.Key + "=" + kv.Value + ","; foreach (byte k in d.Keys) s += k; return s + d[200] + " " + d.ContainsKey(200); });
            Try("e5", () => { var l = new List<ulong> { 5, 18446744073709551615UL }; var m = new List<ushort> { 65535, 65 }; var n = new List<sbyte> { -3 }; var c = new List<char> { 'a' }; return l[1] + " " + m[0] + " " + m[1] + " " + n[0] + " " + c[0] + " " + BoxText(l[1]) + BoxText(m[1]) + BoxText(c[0]); });
            Try("e6", () => { var t = (big, b, us65); object o = t.Item1; return t.Item1 + " " + t.Item2 + " " + t.Item3 + " " + BoxText(o); });
            Try("e7", () => ((IComparable)big).CompareTo((object)1u) + " " + ((IComparable<uint>)big).CompareTo(1u) + " " + ((IComparable)b).CompareTo((object)(byte)1) + " " + ((IComparable)ul).CompareTo((object)1ul) + " " + ((IComparable)us).CompareTo((object)(ushort)1) + " " + ((IEquatable<uint>)big).Equals(big));
            Try("e8", () => { uint[] a = { 5, 3000000000, 1 }; byte[] bs = { 200 }; object o = a[1]; return a[1] + " " + BoxText(a[1]) + " " + o + " " + bs[0] + " " + BoxText(bs[0]) + " " + a.Contains(3000000000u) + " " + a.First(x => x > 5) + " " + a.ToList()[1]; });
            Try("e9", () => { Func<uint> f = () => big; Func<byte, string> g = x => "" + x; Action<object> h = x => Console.WriteLine(x); h(big); h(b); h(us65); h(ul); Console.WriteLine((object)b); return f() + " " + g(b) + " " + BoxText(f()); });
            Try("e10", () => big.Equals((object)big) + " " + big.Equals((object)(int)5) + " " + b.Equals((object)b) + " " + ((object)big).GetHashCode() + " " + ((object)b).GetHashCode() + " " + ((object)us).GetHashCode() + " " + ((object)ul).GetHashCode()+ " " + ((object)sb).GetHashCode());

            Try("f1", () => { var l = new List<Pitch> { Pitch.High, Pitch.Low }; object o = l[0]; return l[0] + " " + o + " " + l.Contains(Pitch.High) + " " + (int)l[0] + " " + Enum.IsDefined(typeof(Pitch), (byte)200) + " " + Enum.IsDefined(typeof(Pitch), (byte)7) + " " + (Pitch)(object)Pitch.High; });
            Try("f2", () => { var l = new List<Span> { Span.Big }; object o = Span.Big; var d = new Dictionary<Span, int>(); d[Span.Big] = 3; return l[0] + " " + o + " " + (uint)l[0] + " " + Enum.IsDefined(typeof(Span), 4000000000u) + " " + d[Span.Big] + " " + l.Contains(Span.Big); });
            Try("f3", () => { try { return "" + Enum.IsDefined(typeof(Pitch), 200); } catch (ArgumentException) { return "ArgumentException"; } });
            Try("f4", () => { ushort[] a = { 65, 65535 }; char[] c = { 'x', 'y' }; var l = new List<ushort>(a); var m = new List<char>(c); return l[0] + " " + l[1] + " " + m[0] + " " + a.Contains((ushort)65) + " " + a.ToList()[1] + " " + BoxText(l[0]) + BoxText(m[1]) + " " + a.First() + c.First(); });
            Try("f5", () => { ulong[] a = { 1, 18446744073709551615UL }; byte[] bs = { 1, 200 }; var l = new List<ulong>(a); var m = new List<byte>(bs); return l[1] + " " + m[1] + " " + a.ToList()[1] + " " + bs.ToList()[1] + " " + bs.Count(x => x > 127) + " " + bs.Select(x => (int)x).First(x => x > 1) + " " + m.ToArray()[1] + " " + l.ToArray()[1] + " " + new List<uint> { 4000000000 }.ToArray()[0] + " " + new List<ushort> { 65 }.ToArray()[0]; });
            Try("f6", () => { uint[,] g = new uint[2, 2]; g[1, 1] = 3000000000; byte[,] h = new byte[1, 2]; h[0, 1] = 200; string s = ""; foreach (uint v in g) s += v + ";"; foreach (byte v in h) s += v + ";"; object o = g[1, 1]; object p = h[0, 1]; return s + o + p; });
            Try("f7", () => { byte[] bs = { 1, 200 }; sbyte[] ss = { -3 }; object o = bs[1]; object q = ss[0]; return o + "," + (o is byte) + (o is sbyte) + "," + q + "," + (q is byte) + (q is sbyte); });
            Try("f8", () => { Func<uint, uint> twice = x => x * 2; Func<byte> two = () => 200; Action<ushort> act = x => Console.WriteLine("ushort " + x); act(65); Func<object> boxed = () => (uint)3000000000; return twice(2000000000) + " " + two() + " " + boxed(); });
            Try("f9", () => { var t = (a: (uint)3000000000, b: (byte)200); var u = t; return u.ToString() + " " + (t.a == u.a); });
            Try("f10", () => { object o = (uint)5; object p = (uint)5; object q = (byte)5; return o.Equals(p) + " " + o.Equals(q) + " " + (o.GetHashCode() == p.GetHashCode()) + " " + Equals(o, p); });
            Try("f11", () => { object[] mixed = { 1, 1u, 1L, 1UL, (byte)1, (sbyte)1, (short)1, (ushort)1, 'x', 1f, 1d, true, "s" }; string s = ""; foreach (object o in mixed) s += BoxKind(o) + ":" + o + " "; return s; });
            Try("f12", () => { uint a = 4000000000; uint c = 5; ulong x = 18446744073709551615UL; byte m = 250; return (a > c) + " " + (a / c) + " " + (a % 7) + " " + (x / 3) + " " + (x > 1) + " " + (byte)(m + 10) + " " + (m + 10) + " " + (a >> 4) + " " + (uint)(a + a) + " " + (long)a; });
            // Type tests.
            Try("c1", () => BoxKind(big) + " " + BoxKind(5) + " " + BoxKind(ul) + " " + BoxKind(5L) + " " + BoxKind(b) + " " + BoxKind(sb) + " " + BoxKind(us) + " " + BoxKind(ch) + " " + BoxKind((short)3));
            Try("c2", () => BoxSwitch(big) + "; " + BoxSwitch(5) + "; " + BoxSwitch(ul) + "; " + BoxSwitch(5L) + "; " + BoxSwitch(b) + "; " + BoxSwitch(sb) + "; " + BoxSwitch(us65) + "; " + BoxSwitch(ch));
            // A boxed value is of one type only.
            Try("d1", () => ((object)1).Equals((object)1u) + " " + ((object)1u).Equals((object)1) + " " + ((object)1u).Equals((object)1u) + " " + ((object)(byte)1).Equals((object)(sbyte)1) + " " + ((object)'A').Equals((object)us65) + " " + ((object)1L).Equals((object)1UL) + " " + object.Equals(1, 1u));
            Try("d2", () => { object o = 1; return "" + (uint)o; });
            Try("d3", () => { object o = 1u; return "" + (int)o; });
            Try("d4", () => { object o = ch; return "" + (ushort)o; });
            Try("d5", () => { object o = (byte)1; return "" + (sbyte)o; });
            Try("d6", () => { var d = new Dictionary<object, string>(); d[1] = "int"; d[1u] = "uint"; d['A'] = "char"; d[us65] = "ushort"; return d.Count + " " + d[1] + " " + d[1u] + " " + d['A'] + " " + d[us65]; });
        }

        // ---- delegate types --------------------------------------------------

        private static void TakesInt(int x)
        {
        }

        // Combine and Remove refuse two delegates of different delegate
        // types, whatever their signatures.
        private static void DelegateTypes()
        {
            Section("delegate types");
            Mine m = TakesInt; Yours y = TakesInt; Action<int> a = TakesInt; Action<int> a2 = x => { }; Func<int> f = () => 1; Action n = () => { };
            Try("combine mine+yours", () => "" + (Delegate.Combine(m, y) != null));
            Try("combine action+func", () => "" + (Delegate.Combine(a, f) != null));
            Try("combine action+action0", () => "" + (Delegate.Combine(a, n) != null));
            Try("combine mine+action", () => "" + (Delegate.Combine(m, a) != null));
            Try("combine same", () => "" + (Delegate.Combine(a, a2) != null) + (Delegate.Combine(m, m) != null));
            Try("combine null", () => "" + (Delegate.Combine(null, f) == (Delegate)f) + (Delegate.Combine(a, null) == (Delegate)a));
            Try("combine multi+other", () => "" + (Delegate.Combine(Delegate.Combine(m, m), y) != null));
            Try("remove mine-yours", () => "" + (Delegate.Remove(m, y) == (Delegate)m));
            Try("remove action-func", () => "" + (Delegate.Remove(a, f) == (Delegate)a));
            Try("remove multi-other", () => "" + (Delegate.Remove(Delegate.Combine(m, m), y) != null));
            Try("remove null", () => "" + (Delegate.Remove(null, f) == null) + (Delegate.Remove(a, null) == (Delegate)a));
            Try("remove same", () => "" + (Delegate.Remove(m, m) == null) + (Delegate.Remove(a, a2) == (Delegate)a));
            Try("equals mine/yours", () => "" + m.Equals(y) + ((Delegate)m == (Delegate)y));
        }

        // ---- powers ----------------------------------------------------------

        private static string Power(double d)
        {
            if (!(d >= 0) && !(d < 0))
            {
                return "NaN";
            }
            if (d > double.MaxValue)
            {
                return "+Inf";
            }
            if (d < -double.MaxValue)
            {
                return "-Inf";
            }
            if (d == 0)
            {
                return 1 / d < 0 ? "-0" : "0";
            }
            // Too large or too small to scale to an int: its sign and its
            // order of magnitude.
            if (Math.Abs(d) > 1e15 || Math.Abs(d) < 1e-6)
            {
                return (d < 0 ? "-" : "") + "e" + (int)Math.Floor(Math.Log10(Math.Abs(d)) + 0.5);
            }
            return "" + (long)Math.Round(d * 1000000);
        }

        // The sign of a negative base's power is the parity of its exponent,
        // however large the exponent is; a double of 2^53 or more is even.
        private static void Powers()
        {
            Section("powers");
            double[] bases = { -1, -0.5, -2, -0.0, double.NegativeInfinity, -1.0000001, -3, 2, 0.0, 1, double.PositiveInfinity, double.NaN, -0.9999999 };
            double[] exps = { 1025, 1026, -1025, -1026, 2047, 2048, 1e20, -1e20, 9007199254740991, 9007199254740992, 9007199254740993, -9007199254740991, 4503599627370497, 4503599627370496.5, 1024, 1023, -1023, 3, -3, 2, 0.5, -0.5, 1024.5, 2.5, double.PositiveInfinity, double.NegativeInfinity, double.NaN, 0, -0.0, 1, 65537, -65537, 2147483647, 2147483648, 2147483649, -2147483649 , 1e300};
            foreach (double b in bases)
            {
                string row = "";
                foreach (double e in exps) row += Power(Math.Pow(b, e)) + " ";
                Log("pow " + Power(b) + ": " + row);
            }
            float[] fb = { -1f, -0.5f, -2f, -0f, float.NegativeInfinity, -3f, 2f, 0f, 1f, float.NaN };
            float[] fe = { 1025f, 1026f, -1025f, 16777215f, 16777216f, 16777217f, -16777215f, 1e20f, -1e20f, 3f, 2f, 0.5f, 129f, 128f, -151f, -150f, float.PositiveInfinity, float.NegativeInfinity, float.NaN, 0f, 8388609f, 8388607.5f };
            foreach (float b in fb)
            {
                string row = "";
                foreach (float e in fe) row += Power((float)Math.Pow(b, e)) + " ";
                Log("powf " + Power(b) + ": " + row);
            }
        }

        // ---- rounding --------------------------------------------------------

        // Past 2^63 a double has no `long` to go through, an infinity and a
        // NaN have none either, and a result of zero keeps its argument's
        // sign: each of these once came back as another number on a device.
        private static void Rounding()
        {
            Section("rounding");
            double[] values = { 1e20, -1e20, double.NaN, double.PositiveInfinity, double.NegativeInfinity, -0.0, 0.0, 2.5, -2.5, 0.5, -0.5, 1.5, -1.5, 3.5, -3.5, 0.2, -0.2, 0.7, -0.7, 9223372036854775808.0, -9223372036854775808.0, 4503599627370497.0, -4503599627370497.0, 0.49999999999999994, 1e300, -1e300 };
            foreach (double v in values)
            {
                double f = Math.Floor(v), c = Math.Ceiling(v), r = Math.Round(v), t = Math.Truncate(v);
                Log("of " + Power(v) + ": floor " + Power(f) + " ceiling " + Power(c) + " round " + Power(r) + " truncate " + Power(t)
                    + " same " + (f == v) + (c == v) + (r == v) + (t == v));
            }
        }

        // ---- arrays as enumerables -------------------------------------------

        private struct Cell
        {
            public int n;

            public override string ToString()
            {
                return "Cell" + n;
            }
        }

        private static System.Collections.IEnumerable keptEnumerable;

        private static string Walk(System.Collections.IEnumerable e)
        {
            string s = "";
            foreach (object o in e) s += BoxKind(o) + ":" + o + " ";
            return s;
        }

        private static string WalkArray(Array a)
        {
            string s = a.Length + "/";
            foreach (object o in a) s += BoxKind(o) + ":" + o + " ";
            return s;
        }

        private static System.Collections.IEnumerable AsEnumerable(uint[] a)
        {
            return a;
        }

        // An array is an IEnumerable wherever it is put, not only where it is
        // passed; and what comes out of an array of an unsigned type is that
        // type's box, whichever of the types an array is known by it was
        // reached through.
        private static void ArraysAsEnumerables()
        {
            Section("arrays as enumerables");
            int[] ints = { 1, -2 };
            string[] strings = { "a", "b" };
            Cell[] cells = { new Cell { n = 1 }, new Cell { n = 2 } };
            uint[] uints = { 3000000000, 1 };
            byte[] bytes = { 200, 1 };
            sbyte[] sbytes = { -56, 1 };
            ushort[] ushorts = { 65535, 65 };
            char[] chars = { 'x', 'y' };
            ulong[] ulongs = { 18446744073709551615UL, 1 };
            uint[,] grid = new uint[1, 2];
            grid[0, 1] = 4000000000;
            // A local.
            Try("local ints", () => { System.Collections.IEnumerable e = ints; string s = ""; foreach (object o in e) s += o + ","; return s; });
            Try("local strings", () => { System.Collections.IEnumerable e = strings; string s = ""; foreach (object o in e) s += o + ","; return s; });
            Try("local cells", () => { System.Collections.IEnumerable e = cells; string s = ""; foreach (object o in e) s += o + ","; return s; });
            Try("local generic", () => { IEnumerable<int> e = ints; string s = ""; foreach (int o in e) s += o + ","; IEnumerable<string> t = strings; foreach (string o in t) s += o + ","; return s; });
            Try("local copies", () => { System.Collections.IEnumerable e = cells; foreach (object o in e) { Cell c = (Cell)o; c.n = 9; } return cells[0] + " " + cells[1]; });
            Try("local sees writes", () => { int[] a = { 1, 2 }; System.Collections.IEnumerable e = a; a[1] = 7; return Walk(e); });
            // A parameter, a field, a return value and an element.
            Try("parameter", () => Walk(ints) + Walk(strings) + Walk(cells));
            Try("field", () => { keptEnumerable = ints; string s = Walk(keptEnumerable); keptEnumerable = uints; return s + Walk(keptEnumerable); });
            Try("returned", () => Walk(AsEnumerable(uints)));
            Try("element", () => { var all = new System.Collections.IEnumerable[2]; all[0] = bytes; all[1] = strings; return Walk(all[0]) + Walk(all[1]); });
            Try("null", () => { int[] none = null; System.Collections.IEnumerable e = none; return "" + (e == null); });
            // Through object, where only the cast says what is wanted.
            Try("cast", () => { object o = ints; var e = (System.Collections.IEnumerable)o; object p = strings; var q = p as System.Collections.IEnumerable; return Walk(e) + Walk(q) + (o is System.Collections.IEnumerable) + (((object)5) is System.Collections.IEnumerable); });
            Try("cast list", () => { object o = new List<int> { 4 }; return Walk((System.Collections.IEnumerable)o) + (o is System.Collections.IEnumerable); });
            Try("cast wrong", () => { object o = 5; return Walk((System.Collections.IEnumerable)o); });
            // The unsigned types, by each road.
            Try("unsigned local", () => { System.Collections.IEnumerable a = uints, b = bytes, c = ushorts, d = ulongs, e = sbytes, f = chars; return Walk(a) + Walk(b) + Walk(c) + Walk(d) + Walk(e) + Walk(f); });
            Try("unsigned parameter", () => Walk(uints) + Walk(bytes) + Walk(ushorts) + Walk(ulongs) + Walk(sbytes) + Walk(chars) + Walk(grid));
            // As an Array too, while the array is still known for what it is.
            Try("unsigned direct", () => { string s = ""; foreach (object o in (Array)uints) s += BoxKind(o) + ":" + o + " "; foreach (object o in (Array)bytes) s += BoxKind(o) + ":" + o + " "; foreach (object o in (System.Collections.IEnumerable)ushorts) s += BoxKind(o) + ":" + o + " "; foreach (object o in (System.Collections.IEnumerable)ulongs) s += BoxKind(o) + ":" + o + " "; return s; });
            // Kept as an Array, an array of a signed type still is one. (One
            // of an unsigned type is not told from it there: a known gap,
            // said where Array_.GetEnumerator is.)
            Try("signed as Array", () => WalkArray(sbytes) + WalkArray(chars) + WalkArray(ints) + WalkArray(strings));
            Try("generic cast", () => { object o = uints; string s = ""; foreach (uint u in (IEnumerable<uint>)o) s += u + ","; object p = cells; foreach (Cell c in (IEnumerable<Cell>)p) s += c + ","; return s + (o is IEnumerable<uint>) + (p is IEnumerable<Cell>) + (((object)"s") is IEnumerable<uint>); });
        }

        // ---- arrays of structs from generic members ---------------------------

        private enum Rank { One = 1, Two = 2 }

        private static string Named(Func<string> f)
        {
            try
            {
                return f();
            }
            catch (ArgumentOutOfRangeException)
            {
                return "ArgumentOutOfRange";
            }
            catch (NullReferenceException)
            {
                return "NullReference";
            }
            catch (ArgumentException)
            {
                return "Argument";
            }
            catch (Exception)
            {
                return "another exception";
            }
        }

        private static string Xs(Vec2[] a)
        {
            string s = "";
            foreach (Vec2 v in a) s += (int)v.x + "," + (int)v.y + " ";
            return s;
        }

        // A T[] a generic member answers is a new array of values: writing
        // to one of its elements changes nothing the collection holds, and
        // the other way round.
        private static void StructArraysFromGenerics()
        {
            Section("struct arrays from generic members");
            var list = new List<Vec2> { new Vec2(1, 2), new Vec2(3, 4) };
            Vec2[] a = list.ToArray();
            a[0].x = 9;
            a[1].Scale(2);
            Log("ToArray " + a.Length + ": " + Xs(a) + "list " + Xs(list.ToArray()));
            list[1] = new Vec2(7, 8);
            Log("list written " + Xs(a));
            Vec2[] b = ((IEnumerable<Vec2>)list).ToArray();
            b[1].y = 5;
            Log("Enumerable.ToArray " + Xs(b) + "list " + Xs(list.ToArray()));
            Vec2[] c = list.ToList().ToArray();
            c[0].y = 6;
            Log("ToList.ToArray " + Xs(c) + "list " + Xs(list.ToArray()));
            Vec2[] d = list.Where(v => v.x > 1).ToArray();
            d[0].x = 0;
            Log("Where.ToArray " + Xs(d) + "list " + Xs(list.ToArray()));
            Vec2[] e = new Vec2[3];
            list.CopyTo(e);
            e[0].x = 4;
            Log("CopyTo " + Xs(e) + "list " + Xs(list.ToArray()));
            list.CopyTo(e, 1);
            e[2].y = 1;
            Log("CopyTo at 1 " + Xs(e) + "list " + Xs(list.ToArray()));
            Log("CopyTo short " + Named(() => { list.CopyTo(new Vec2[1]); return "copied"; })
                + " " + Named(() => { list.CopyTo(null); return "copied"; }));
            string[] names = new string[3];
            new List<string> { "a", "b" }.CopyTo(names, 1);
            Log("CopyTo strings " + (names[0] == null) + names[1] + names[2]);
            Log("empty " + Array.Empty<Vec2>().Length + " " + new List<Vec2>().ToArray().Length + " " + Array.Empty<Cell>().Length);
            Cell[] cells = new List<Cell> { new Cell { n = 1 }, new Cell { n = 2 } }.ToArray();
            cells[0].n = 4;
            Cell[] again = cells.ToList().ToArray();
            again[1].n = 8;
            Log("user struct " + cells[0] + " " + cells[1] + " " + again[0] + " " + again[1]);
            var pairs = new Dictionary<string, int> { { "a", 1 } }.ToArray();
            Log("pairs " + pairs.Length + " " + pairs[0].Key + pairs[0].Value);
        }

        // An array of a primitive handed to a generic member that declares
        // T[]: whatever the member writes into it is in the caller's array
        // when it returns, read back as the type it is -- a byte of 200, a
        // uint above int.MaxValue.
        private static string Show(int[] a)
        {
            string s = "";
            for (int i = 0; i < a.Length; i++) s += (i > 0 ? "," : "") + a[i];
            return s;
        }

        private static string ShowUnsigned(uint[] a)
        {
            string s = "";
            for (int i = 0; i < a.Length; i++) s += (i > 0 ? "," : "") + a[i];
            return s;
        }

        private static string Show(byte[] a)
        {
            string s = "";
            for (int i = 0; i < a.Length; i++) s += (i > 0 ? "," : "") + a[i];
            return s;
        }

        private static string Show(float[] a)
        {
            string s = "";
            for (int i = 0; i < a.Length; i++) s += (i > 0 ? "," : "") + a[i];
            return s;
        }

        private static void PrimitiveArraysToGenerics()
        {
            Section("primitive arrays to generic members");
            var ints = new List<int> { 1, 2, 3 };
            int[] a = new int[3];
            ints.CopyTo(a);
            int[] b = { 9, 9, 9, 9, 9 };
            ints.CopyTo(b, 2);
            int[] c = { 7, 7, 7 };
            ints.CopyTo(1, c, 0, 2);
            Log("int " + Show(a) + " " + Show(b) + " " + Show(c));
            Log("int errors " + Named(() => { ints.CopyTo(new int[2]); return "copied"; }) + " "
                + Named(() => { ints.CopyTo((int[])null); return "copied"; }) + " "
                + Named(() => { ints.CopyTo(a, -1); return "copied"; }) + " "
                + Named(() => { ints.CopyTo(2, c, 0, 2); return "copied"; }) + " "
                + Named(() => { ints.CopyTo(0, c, -1, 1); return "copied"; }) + " "
                + Named(() => { ints.CopyTo(0, c, 2, 2); return "copied"; }));
            uint[] u = new uint[2];
            new List<uint> { 4000000000u, 5u }.CopyTo(u);
            byte[] by = new byte[3];
            new List<byte> { 200, 1 }.CopyTo(by, 1);
            float[] f = new float[2];
            new List<float> { 1.5f, -0.25f }.CopyTo(f);
            bool[] bo = new bool[2];
            new List<bool> { false, true }.CopyTo(bo);
            char[] ch = new char[2];
            new List<char> { 'x', 'y' }.CopyTo(ch);
            Log("uint " + u[0] + " " + u[1] + " byte " + by[0] + " " + by[1] + " " + by[2] + " float " + f[0] + " "
                + f[1] + " bool " + bo[0] + " " + bo[1] + " char " + new string(ch));
            ulong[] ul = new ulong[1];
            new List<ulong> { 18000000000000000000ul }.CopyTo(ul);
            ushort[] us = new ushort[1];
            new List<ushort> { 65000 }.CopyTo(us);
            sbyte[] sb = new sbyte[1];
            new List<sbyte> { -5 }.CopyTo(sb);
            long[] lo = new long[1];
            new List<long> { -9000000000L }.CopyTo(lo);
            double[] d = new double[1];
            new List<double> { 0.125 }.CopyTo(d);
            short[] sh = new short[1];
            new List<short> { -300 }.CopyTo(sh);
            Rank[] ranks = new Rank[2];
            new List<Rank> { Rank.Two, Rank.One }.CopyTo(ranks);
            Log("ulong " + ul[0] + " ushort " + us[0] + " sbyte " + sb[0] + " long " + lo[0] + " double " + d[0]
                + " short " + sh[0] + " enum " + ranks[0] + ranks[1]);
            byte[] bytes = { 200, 1, 2 };
            GaArrays.Reverse(bytes);
            uint[] big = { 1u, 4000000000u };
            GaArrays.Reverse(big);
            float[] fl = { 0.5f, 2f, 3f };
            GaArrays.Clear(fl, 1);
            Log("own generic " + Show(bytes) + " " + ShowUnsigned(big) + " " + Show(fl)
                + " " + GaArrays.Join(bytes) + " " + GaArrays.Join(big));
            int[] held = { 3, 4, 5 };
            var holder = new GaHolder<int>(held);
            Log("constructor " + holder.Seen + " " + Show(held));
        }

        private static string Thrown(Func<string> f)
        {
            try
            {
                return f();
            }
            catch (ArgumentOutOfRangeException)
            {
                return "ArgumentOutOfRange";
            }
            catch (ArgumentNullException)
            {
                return "ArgumentNull";
            }
            catch (ArgumentException)
            {
                return "Argument";
            }
            catch (IndexOutOfRangeException)
            {
                return "IndexOutOfRange";
            }
            catch (NullReferenceException)
            {
                return "NullReference";
            }
            catch (OverflowException)
            {
                return "Overflow";
            }
            catch (Exception)
            {
                return "another exception";
            }
        }

        // What each string member throws for a count, an index or an
        // argument out of its range: the type is what a script's catch
        // clause selects on.
        private static void StringArgumentErrors()
        {
            Section("string argument errors");
            string abc = "abc";
            string none = null;
            Log("new string " + Thrown(() => new string('x', -1)) + " [" + new string('x', 0) + "] ["
                + new string((char[])null) + "]");
            Log("Substring " + Thrown(() => abc.Substring(-1)) + " " + Thrown(() => abc.Substring(4)) + " ["
                + abc.Substring(3) + "] " + Thrown(() => abc.Substring(1, 5)) + " " + Thrown(() => abc.Substring(1, -1))
                + " " + Thrown(() => abc.Substring(-1, 1)) + " [" + abc.Substring(3, 0) + "]");
            Log("chars " + Thrown(() => "" + abc[3]) + " " + Thrown(() => "" + abc[-1]));
            Log("null arguments " + Thrown(() => "" + abc.IndexOf(none)) + " " + Thrown(() => "" + abc.Contains(none))
                + " " + Thrown(() => "" + abc.StartsWith(none)) + " " + Thrown(() => "" + abc.EndsWith(none)) + " "
                + Thrown(() => abc.Replace(none, "x")) + " " + Thrown(() => abc.Replace("", "x")) + " "
                + Thrown(() => string.Join(",", (string[])null)) + " " + Thrown(() => abc.Split(',', -1)[0]));
        }

        // ---- non-generic CompareTo and arrays as collections -------------------

        private static string Kind(Func<string> f)
        {
            try
            {
                return f();
            }
            catch (NullReferenceException)
            {
                return "NullReference";
            }
            catch (ArgumentException)
            {
                return "Argument";
            }
            catch (InvalidCastException)
            {
                return "InvalidCast";
            }
            catch (NotSupportedException)
            {
                return "NotSupported";
            }
            catch (Exception)
            {
                return "another exception";
            }
        }

        // Through the non-generic interface the argument can be anything;
        // .NET throws ArgumentException for one not of the receiver's type,
        // and a call on null throws whatever the argument is.
        private static void ComparableArguments()
        {
            Section("comparable arguments");
            IComparable none = null;
            IComparable<int> noneOfInt = null;
            Log("null receiver " + Kind(() => "" + none.CompareTo(null)) + " " + Kind(() => "" + none.CompareTo(1))
                + " " + Kind(() => "" + noneOfInt.CompareTo(1)));
            Log("same type " + ((IComparable)1).CompareTo(2) + " " + ((IComparable)"b").CompareTo("a") + " "
                + ((IComparable)2L).CompareTo(2L) + " " + ((IComparable)1).CompareTo(null) + " "
                + ((IComparable)"a").CompareTo(null) + " " + ((IComparable)Rank.Two).CompareTo(Rank.One));
            Log("other type " + Kind(() => "" + ((IComparable)1).CompareTo("x")) + " "
                + Kind(() => "" + ((IComparable)1).CompareTo(1L)) + " " + Kind(() => "" + ((IComparable)1L).CompareTo(1))
                + " " + Kind(() => "" + ((IComparable)1).CompareTo(1u)) + " " + Kind(() => "" + ((IComparable)1u).CompareTo(1))
                + " " + Kind(() => "" + ((IComparable)Rank.One).CompareTo(1)) + " "
                + Kind(() => "" + ((IComparable)1).CompareTo(Rank.One)) + " " + Kind(() => "" + ((IComparable)"a").CompareTo(1)));
            Log("other primitive " + Kind(() => "" + ((IComparable)1.5f).CompareTo(1.5)) + " "
                + Kind(() => "" + ((IComparable)2.0).CompareTo(1.0f)) + " " + Kind(() => "" + ((IComparable)'a').CompareTo(1))
                + " " + Kind(() => "" + ((IComparable)(short)1).CompareTo((ushort)1)) + " "
                + Kind(() => "" + ((IComparable)(sbyte)1).CompareTo((byte)1)) + " " + Kind(() => "" + ((IComparable)true).CompareTo(1)));
        }

        private static int CountOf<T>(ICollection<T> c)
        {
            return c.Count;
        }

        // An array is an ICollection<T> on .NET: its count is its length,
        // it reads through to the array, and it cannot change size.
        private static void ArraysAsCollections()
        {
            Section("arrays as collections");
            int[] ints = { 3, 1, 2 };
            ICollection<int> c = ints;
            ints[0] = 9;
            string walked = "";
            foreach (int i in c) walked += i + ",";
            Log("int " + c.Count + " " + CountOf(ints) + " " + walked + " " + Kind(() => { c.Clear(); return "cleared"; })
                + " " + ints.Length);
            uint[] uints = { 4000000000u, 5u };
            ICollection<uint> cu = uints;
            string u = "";
            foreach (uint x in cu) u += x + ",";
            Vec2[] vs = { new Vec2(1, 2), new Vec2(3, 4) };
            ICollection<Vec2> cv = vs;
            vs[1].x = 7;
            string v = "";
            foreach (Vec2 x in cv) v += (int)x.x + ",";
            Log("uint " + cu.Count + " " + u + " struct " + cv.Count + " " + CountOf(vs) + " " + v);
            object o = ints;
            Log("cast " + (o is ICollection<int>) + " " + ((ICollection<int>)o).Count + " " + (o as ICollection<int>).Count);
            IReadOnlyList<int> r = ints;
            int sum = 0;
            foreach (int i in r) sum += i;
            Log("read only list walked " + sum);
        }

        // ---- boxes, null receivers and capacities ------------------------------

        private static void BoxesAndReceivers()
        {
            Section("boxes and receivers");
            // A boxed enum is not the integer under it, from either side.
            Log("int and enum " + 1.Equals((object)Rank.One) + " " + ((object)1).Equals(Rank.One) + " " + Rank.One.Equals(1)
                + " " + Rank.One.Equals(Rank.One) + " " + object.Equals(1, Rank.One) + " " + 1.Equals((object)1));
            Log("others and enum " + 5000000000L.Equals((object)Big.Huge) + " " + ((byte)200).Equals((object)Tiny.A)
                + " " + ((ushort)3).Equals((object)Wide.B) + " " + 1u.Equals((object)Unsigned.Low) + " " + ((short)1).Equals((object)Rank.One));
            // A method of object called on nothing throws; the static
            // Equals takes a null on either side.
            object none = null, one = 1;
            Log("null receiver " + Named(() => "" + none.Equals(one)) + " " + Named(() => "" + none.Equals(null))
                + " " + Named(() => "" + none.GetHashCode()) + " " + Named(() => none.ToString()));
            Log("null argument " + one.Equals(none) + " " + "s".Equals(none));
            Log("static " + object.Equals(none, one) + " " + object.Equals(one, none) + " " + object.Equals(none, none)
                + " " + object.Equals(one, 1) + " " + object.ReferenceEquals(none, none));
            Log("capacity " + Named(() => "" + new List<int>(-1).Count) + " " + Named(() => "" + new List<int>(0).Count)
                + " " + Named(() => "" + new List<Vec2>(int.MinValue).Count) + " " + Named(() => "" + new Dictionary<string, int>(-1).Count)
                + " " + Named(() => "" + new Dictionary<string, int>(0).Count));
        }

        // ---- unboxing enums ---------------------------------------------------

        private static T Cast<T>(object o)
        {
            return (T)o;
        }

        private static string Unboxed(Func<object> f)
        {
            try
            {
                return "" + f();
            }
            catch (InvalidCastException)
            {
                return "InvalidCast";
            }
        }

        // An unboxing goes by what the box is stored as and by nothing else:
        // an enum and the integer under it unbox as each other, and so do two
        // enums over one integer type. Another width, or the other
        // signedness of the same width, is refused.
        private static void UnboxingEnums()
        {
            Section("unboxing enums");
            object rank = Rank.Two, one = 1, tiny = Tiny.A, unsigned = Unsigned.Low, big = Big.Huge, wide = Wide.B;
            Log("same storage " + Unboxed(() => (int)rank) + " " + Unboxed(() => (Rank)one) + " " + Unboxed(() => (Phase)rank)
                + " " + Unboxed(() => (Rank)rank) + " " + Unboxed(() => (byte)tiny) + " " + Unboxed(() => (Tiny)(object)(byte)200)
                + " " + Unboxed(() => (uint)unsigned) + " " + Unboxed(() => (Unsigned)(object)1u) + " " + Unboxed(() => (long)big)
                + " " + Unboxed(() => (ushort)wide) + " " + Unboxed(() => (Pitch)tiny) + " " + Unboxed(() => (Span)unsigned));
            Log("other storage " + Unboxed(() => (uint)rank) + " " + Unboxed(() => (long)rank) + " " + Unboxed(() => (short)rank)
                + " " + Unboxed(() => (byte)rank) + " " + Unboxed(() => (int)unsigned) + " " + Unboxed(() => (Unsigned)one)
                + " " + Unboxed(() => (sbyte)tiny) + " " + Unboxed(() => (int)tiny) + " " + Unboxed(() => (Rank)(object)1L)
                + " " + Unboxed(() => (Rank)(object)1u) + " " + Unboxed(() => (int)big) + " " + Unboxed(() => (ulong)big)
                + " " + Unboxed(() => (short)wide) + " " + Unboxed(() => (char)wide) + " " + Unboxed(() => (Rank)unsigned)
                + " " + Unboxed(() => (Tiny)rank) + " " + Unboxed(() => (Big)rank));
            Log("generic " + Unboxed(() => Cast<int>(rank)) + " " + Unboxed(() => Cast<Rank>(one)) + " "
                + Unboxed(() => Cast<Phase>(rank)) + " " + Unboxed(() => Cast<uint>(rank)) + " " + Unboxed(() => Cast<Tiny>(rank))
                + " " + Unboxed(() => Cast<byte>(tiny)));
            Log("tests " + (rank is int) + " " + (one is Rank) + " " + (rank is Phase) + " " + (rank is Rank) + " "
                + (tiny is byte) + " " + (unsigned is uint));
        }

        // `Cache<int>.Value` and `Cache<string>.Value` are two variables. The
        // sections go from the shape every Unity project has -- a singleton
        // base class with one subclass per manager -- to a type that reaches
        // the static state of another through its own type parameter, a
        // generic method that does, and the classes the compiler writes for
        // lambdas and iterators inside such a type.
        private static void GenericStatics()
        {
            // Singleton with two subclasses.
            GsGame g = new GsGame();
            GsAudio a = new GsAudio();
            Log("singleton " + GsSingleton<GsGame>.Instance.Tag + " " + GsSingleton<GsAudio>.Instance.Tag
                + " " + GsGame.Instance.score + " " + GsAudio.Instance.clip + " " + GsSingleton<GsGame>.Made + " "
                + GsSingleton<GsAudio>.Made + " " + g.Self().Tag + " " + a.Self().Tag);
            new GsGame();
            Log("singleton made " + GsSingleton<GsGame>.Made + " " + GsSingleton<GsAudio>.Made + " "
                + (GsGame.Instance == g));
            GsSingleton<GsGame>.Reset();
            Log("singleton reset " + GsSingleton<GsGame>.Has + " " + GsSingleton<GsAudio>.Has);

            // Static class per closed type, with a static constructor.
            Log("cache inits " + GsLog.Inits);
            GsCache<int>.Put(3);
            GsCache<string>.Put("s");
            GsCache<GsVec>.Put(new GsVec(4, 5));
            GsCache<int>.Put(9);
            GsVec cv = GsCache<GsVec>.Value;
            Log("cache " + GsCache<int>.Value + " " + GsCache<string>.Value + " " + cv.x + "," + cv.y + " "
                + GsCache<int>.Hits + " " + GsCache<string>.Hits + " " + GsCache<GsVec>.Hits + " "
                + GsCache<int>.Count + " " + GsCache<string>.Count + " " + GsCache<GsVec>.Items.Count);
            Log("cache inits " + GsLog.Inits + " " + GsLog.Order + " " + GsCache<int>.Empty.Length + " "
                + ((object)GsCache<int>.Empty == (object)GsCache<string>.Empty));
            GsCache<uint>.Value = 4000000000u;
            GsCache<long>.Value = 1L << 40;
            GsCache<double>.Value = 2.5;
            GsCache<long>.Value += 2;
            Log("cache wide " + GsCache<uint>.Value + " " + GsCache<long>.Value + " "
                + (int)(GsCache<double>.Value * 10) + " " + GsCache<uint>.Hits);
            GsCache<GsVec>.Value.x = 40;
            Log("cache struct " + GsCache<GsVec>.Value.x + " " + GsCache<GsVec>.Get().y);

            // From a generic method.
            GsUse.Write(11);
            GsUse.Write("t");
            Log("generic method " + GsUse.Read<int>() + " " + GsUse.Read<string>() + " "
                + GsCache<int>.Hits + " " + GsCache<string>.Hits);

            // A static event.
            int ints = 0;
            string strings = "";
            Action<int> onInt = v => ints += v;
            GsBus<int>.Raised += onInt;
            GsBus<string>.Raised += v => strings += v;
            GsBus<int>.Raise(5);
            GsBus<string>.Raise("x");
            GsBus<int>.Raise(6);
            GsBus<int>.Raised -= onInt;
            GsBus<int>.Raise(100);
            GsBus<string>.Raise("y");
            Log("event " + ints + " " + strings);

            // Type ids.
            int idInt = GsTypeId<int>.Value;
            int idString = GsTypeId<string>.Value;
            int idVec = GsUse.Id<GsVec>();
            int idList = GsUse.ListId<int>();
            Log("type id " + idInt + " " + idString + " " + idVec + " " + idList + " " + GsTypeId<int>.Value
                + " " + GsUse.Id<string>() + " " + GsTypeId<List<int>>.Value + " " + GsTypeIds.Next);

            // A counter that has nothing to do with T.
            GsCounted<int> c1 = new GsCounted<int>(1);
            GsCounted<int> c2 = new GsCounted<int>(2);
            GsCounted<string> c3 = new GsCounted<string>("z");
            GsCounted<int> c4 = c2.Another(8);
            Func<int> reader = c3.Reader();
            Log("counted " + c1.id + " " + c2.id + " " + c3.id + " " + c4.id + " " + GsCounted<int>.Count
                + " " + GsCounted<string>.Count + " " + c1.Seen() + " " + c3.Seen() + " " + reader() + " "
                + c4.Reader()());

            // A nested type of a generic type.
            new GsOuter<int>.Inner().Bump();
            new GsOuter<int>.Inner().Bump();
            new GsOuter<string>.Inner().Bump();
            Log("nested " + GsOuter<int>.Inner.Mine + " " + GsOuter<string>.Inner.Mine + " "
                + GsOuter<int>.Total + " " + GsOuter<string>.Total);

            // A generic class deriving from one, and a closed class from that.
            GsMid<long> mid = new GsMid<long>(1L);
            GsLeaf leaf = new GsLeaf();
            GsMid<string>.Label = "other";
            Log("derived " + mid.Describe() + " " + leaf.Describe() + " " + leaf.id + " "
                + GsCounted<long>.Count + " " + new GsMid<string>("q").Describe());

            GsPool<int>.Factory = () => -1;
            GsPool<string>.Factory = () => "new";
            GsPool<int>.Return(4);
            GsPool<int>.Return(5);
            GsPool<string>.Return("a");
            Log("pool " + GsPool<int>.Free + " " + GsPool<string>.Free + " " + GsPool<int>.Take() + " "
                + GsPool<int>.Take() + " " + GsPool<int>.Take() + " " + GsPool<string>.Take() + " "
                + GsPool<string>.Take() + " " + GsPool<int>.Returned + " " + GsPool<string>.Returned);
            GsPool<int>.Return(7);
            GsPool<int>.Return(8);
            int sum = 0;
            foreach (int i in GsPool<int>.All()) sum += i;
            string joined = "";
            foreach (string s in GsPool<string>.All()) joined += s;
            Log("pool iterator " + sum + " [" + joined + "] " + GsPool<int>.CountWhere(x => x > 7) + " "
                + GsPool<string>.CountWhere(x => true));
            Action<int> ret = GsPool<int>.Returner();
            Action<string> rets = GsPool<string>.Returner();
            Action<string> direct = GsPool<string>.Return;
            ret(1);
            rets("p");
            direct("q");
            Func<int> ci = GsPool<int>.Counter();
            Func<int> cs = GsPool<string>.Counter();
            Log("pool delegates " + ci() + " " + cs() + " " + GsPool<int>.Free + " " + GsPool<string>.Free
                + " " + (ret == GsPool<int>.Returner()) + " " + ((object)ret == (object)rets));
            Log("pool typeof " + GsPool<int>.Name() + " " + GsPool<GsKind>.Name() + " " + GsPool<string>.Name()
                + " " + GsPool<double>.Name());
            Log("pool convert " + GsPool<int>.Convert(x => "#" + x) + " " + GsPool<string>.Returned + " "
                + GsPool<string>.Convert(x => x.Length) + " " + GsPool<int>.Returned);
            int by = 1;
            GsPool<int>.Bump(ref by);
            GsPool<string>.Bump(ref GsPool<int>.Returned);
            Log("pool ref " + by + " " + GsPool<int>.Returned);
            GsPool<GsKind>.Return(GsKind.High);
            GsPool<byte>.Return(200);
            Log("pool enum " + GsPool<GsKind>.Take() + " " + GsPool<byte>.Take() + " " + GsPool<GsKind>.Returned
                + " " + GsPool<byte>.Returned + " " + GsPool<sbyte>.Returned);

            GsWrapper<int> w = new GsWrapper<int>();
            Log("wrapper " + w.Returned() + " " + new GsWrapper<string>().Returned() + " "
                + GsWrapper<int>.Twice() + " " + w.Lifted());
            GsPool<List<int>>.Return(null);
            Log("wrapper lifted " + w.Lifted() + " " + GsWrapper<List<int>>.Twice());

            GsDerived<int> d1 = new GsDerived<int>();
            GsDerived<int> d2 = new GsDerived<int>();
            GsDerived<string> d3 = new GsDerived<string>();
            GsBase<int> b = d2;
            GsFinal f = new GsFinal();
            Log("virtual " + d1.Who() + " " + b.Who() + " " + d3.Who() + " " + f.Who() + " "
                + GsBase<int>.Made + " " + GsBase<string>.Made + " " + GsBase<GsVec>.Made + " " + GsBase<long>.Made
                + " " + GsBase<int>.Last + " " + GsBase<long>.Last);

            GsSlot<int> s1 = GsSlot<int>.Make(3);
            GsSlot<int>.Make(4);
            GsSlot<string> s2 = GsSlot<string>.Make("v");
            Log("struct " + s1.value + " " + s2.value + " " + GsSlot<int>.Count + " " + GsSlot<string>.Count);

            Log("generic method " + GsMath.Sum(1, 2) + " " + GsMath.Sum("b", "a") + " " + GsMath.Relay(5)
                + " " + GsPool<int>.Returned + " " + GsPool<string>.Returned);
            Func<int> later = GsMath.Later(9);
            Func<int> laters = GsMath.Later("z");
            Log("generic lambda " + later() + " " + laters() + " " + later() + " " + GsPool<int>.Free);

            // The runtime's own generic statics, which are Java statics.
            uint[] eu = Array.Empty<uint>();
            GsVec[] ev = Array.Empty<GsVec>();
            string[] es = Array.Empty<string>();
            Log("runtime empty " + (eu.Length + ev.Length + es.Length) + " "
                + es.Length + " " + eu.Concat(new uint[] { 4000000000u }).First());
        }

        // ---- string receivers ---------------------------------------------

        // An instance method of string called on null throws
        // NullReferenceException; the static string.Equals takes a null on
        // either side. ArgumentNullException is for a null argument only.
        private static void StringReceivers()
        {
            Section("string receivers");
            string none = null, abc = "abc";
            Log("instance " + Kind(() => "" + none.Equals((string)null)) + " " + Kind(() => "" + none.Equals((object)null))
                + " " + Kind(() => "" + none.Equals("abc")) + " " + Kind(() => "" + none.Equals((string)null, StringComparison.Ordinal)));
            Log("static " + string.Equals(none, null) + " " + string.Equals(none, abc) + " " + string.Equals(abc, none) + " "
                + string.Equals(none, null, StringComparison.Ordinal) + " " + string.Equals("ABC", abc, StringComparison.OrdinalIgnoreCase)
                + " " + string.Equals("ABC", abc, StringComparison.Ordinal) + " " + Kind(() => "" + string.Equals(abc, "x", (StringComparison)9)));
            Log("on text " + abc.Equals((string)null) + " " + abc.Equals((object)"abc") + " " + abc.Equals((object)1) + " "
                + abc.Equals("ABC", StringComparison.OrdinalIgnoreCase) + " " + abc.Equals("ABC", StringComparison.InvariantCultureIgnoreCase)
                + " " + abc.Equals("ABC", StringComparison.CurrentCulture));
            Log("other receivers " + Kind(() => "" + none.CompareTo((string)null)) + " " + Kind(() => "" + none.CompareTo("a")) + " "
                + Kind(() => "" + none.StartsWith("a")) + " " + Kind(() => "" + none.EndsWith("a")) + " " + Kind(() => "" + none.Contains("a"))
                + " " + Kind(() => "" + none.IndexOf("a")) + " " + Kind(() => "" + none.IndexOf('a')) + " " + Kind(() => "" + none.Length));
        }

        // ---- delegate type arguments --------------------------------------

        private static void TakesObject(object o)
        {
        }

        // Combine and Remove want one run-time delegate type, type arguments
        // and all, exactly: a delegate kept through variance is still of the
        // type it was made as.
        private static void DelegateTypeArguments()
        {
            Section("delegate type arguments");
            Action<int> ai = x => { };
            Action<int> ai2 = TakesInt;
            Action<string> text = x => { };
            Action<object> ao = TakesObject;
            Action<string> variant = ao;
            Try("combine int+string", () => "" + (Delegate.Combine(ai, text) != null));
            Try("combine int+int", () => "" + (Delegate.Combine(ai, ai2) != null));
            Try("combine object+string", () => "" + (Delegate.Combine(ao, text) != null));
            Try("combine variant+string", () => "" + (Delegate.Combine(variant, text) != null));
            Try("plus variant", () => { Action<string> e = text; e += variant; return "joined"; });
            Try("combine multi+string", () => "" + (Delegate.Combine(Delegate.Combine(ai, ai2), text) != null));
            Try("remove int-string", () => "" + (Delegate.Remove(ai, text) == (Delegate)ai));
            Try("remove int-int", () => "" + (Delegate.Remove(Delegate.Combine(ai, ai2), ai2) == (Delegate)ai));
            Try("equals object/variant", () => "" + ao.Equals(variant) + " " + ((Action<string>)TakesObject).Equals(variant));
            Try("generic int+string", () => "" + (Delegate.Combine(new DkKeyed<int>().Make(), new DkKeyed<string>().Make()) != null));
            Try("generic int+int", () => "" + (Delegate.Combine(new DkKeyed<int>().Make(), DkKeyed<int>.MakeStatic()) != null));
            Try("generic static+closed", () => "" + (Delegate.Combine(DkKeyed<int>.MakeStatic(), text) != null));
            Try("generic static+closed int", () => "" + (Delegate.Combine(DkKeyed<int>.MakeStatic(), ai) != null));
            Try("generic method int+string", () => "" + (Delegate.Combine(DkMethods.Make<int>(), DkMethods.Make<string>()) != null));
            Try("generic method int+closed", () => "" + (Delegate.Combine(DkMethods.Make<int>(), ai2) != null));
        }

        private static void ConstructedTypes()
        {
            Section("constructed types");
            Type a = typeof(List<int>);
            Type b = typeof(List<string>);
            Log("list " + (a == b) + " " + (a == typeof(List<int>)) + " " + a.Equals(b) + " " + a.Equals(typeof(List<int>))
                + " " + (a != b));
            Log(a.Name + " " + a + " | " + b);
            Type d = typeof(Dictionary<string, int>);
            Log(d.Name + " " + d + " " + (d == typeof(Dictionary<string, int>)) + " " + (d == typeof(Dictionary<int, string>)));
            Log(typeof(List<List<int>>) + " " + typeof(List<int[]>) + " " + typeof(List<int[]>).Name + " "
                + typeof(KeyValuePair<int, bool>) + " " + typeof(Action<int>) + " " + typeof(Func<int, string>).Name);
            Log(typeof(DkNest<int>.Inner).Name + " " + typeof(DkNest<int>.Inner) + " " + typeof(DkKeyed<GsVec>));
            Log("from generic code " + (DkKeyed<int>.ListType() == typeof(List<int>)) + " "
                + (DkKeyed<string>.ListType() == typeof(List<int>)) + " " + DkKeyed<string>.ListType() + " "
                + (DkMethods.ListOf<int>() == typeof(Dictionary<string, int>)) + " " + DkMethods.ListOf<bool>());
        }

        // ---- enumerator Reset and null params arrays -----------------------

        private static string Refusal(Func<string> f)
        {
            try
            {
                return f();
            }
            catch (ArgumentNullException)
            {
                return "ArgumentNull";
            }
            catch (ArgumentException)
            {
                return "Argument";
            }
            catch (InvalidOperationException)
            {
                return "InvalidOperation";
            }
            catch (NotSupportedException)
            {
                return "NotSupported";
            }
            catch (NullReferenceException)
            {
                return "NullReference";
            }
            catch (Exception)
            {
                return "another exception";
            }
        }

        private static string Rest(IEnumerator e)
        {
            string s = "";
            while (e.MoveNext())
            {
                s += e.Current + ",";
            }
            return s;
        }

        private static int Steps(IEnumerator e)
        {
            int n = 0;
            while (e.MoveNext())
            {
                n++;
            }
            return n;
        }

        // A collection's enumerator starts again on Reset, unless the
        // collection changed since the walk began; only an iterator method's
        // enumerator refuses with NotSupportedException.
        private static void EnumeratorReset()
        {
            Section("enumerator reset");
            List<int> list = new List<int> { 1, 2, 3 };
            IEnumerator le = list.GetEnumerator();
            le.MoveNext();
            le.MoveNext();
            Log("list " + Refusal(() => { le.Reset(); return Rest(le); }) + " " + Refusal(() => { le.Reset(); return Rest(le) + Rest(le); }));
            IEnumerator stale = list.GetEnumerator();
            stale.MoveNext();
            list.Add(4);
            Log("list changed " + Refusal(() => { stale.Reset(); return "reset"; }));
            Dictionary<string, int> d = new Dictionary<string, int> { { "a", 1 }, { "b", 2 } };
            IEnumerator de = d.GetEnumerator();
            de.MoveNext();
            IEnumerator ke = d.Keys.GetEnumerator();
            ke.MoveNext();
            ke.MoveNext();
            IEnumerator ve = d.Values.GetEnumerator();
            ve.MoveNext();
            Log("dictionary " + Refusal(() => { de.Reset(); return "" + Steps(de); }) + " " + Refusal(() => { ke.Reset(); return Rest(ke); })
                + " " + Refusal(() => { ve.Reset(); return Rest(ve); }));
            IEnumerator de2 = d.GetEnumerator();
            IEnumerator ke2 = d.Keys.GetEnumerator();
            IEnumerator ve2 = d.Values.GetEnumerator();
            d["c"] = 3;
            Log("dictionary changed " + Refusal(() => { de2.Reset(); return "reset"; }) + " " + Refusal(() => { ke2.Reset(); return "reset"; })
                + " " + Refusal(() => { ve2.Reset(); return "reset"; }));
            int[] arr = { 5, 6 };
            IEnumerator ae = arr.GetEnumerator();
            ae.MoveNext();
            Log("array " + Refusal(() => { ae.Reset(); return Rest(ae); }));
            IEnumerator ie = Countdown(2).GetEnumerator();
            ie.MoveNext();
            Log("iterator " + Refusal(() => { ie.Reset(); return "reset"; }));
        }

        private static IEnumerable<int> Countdown(int n)
        {
            while (n > 0)
            {
                yield return n--;
            }
        }

        // A null element is an empty string; a null array, or a null format,
        // is ArgumentNullException.
        private static void NullParamsArrays()
        {
            Section("null params arrays");
            Log("concat " + Refusal(() => string.Concat((object[])null)) + " " + Refusal(() => string.Concat((string[])null))
                + " [" + Refusal(() => string.Concat((object)null)) + "] [" + Refusal(() => string.Concat((string)null, (string)null))
                + "] " + Refusal(() => string.Concat(new object[] { null, 1 })) + " " + Refusal(() => string.Concat(new string[] { null, "x" })));
            Log("format " + Refusal(() => string.Format(null, 1)) + " " + Refusal(() => string.Format("{0}", (object[])null))
                + " [" + Refusal(() => string.Format("{0}", (object)null)) + "] " + Refusal(() => string.Format(null, 1, 2))
                + " " + Refusal(() => string.Format(null, 1, 2, 3)) + " " + Refusal(() => string.Format(null, new object[] { 1 }))
                + " " + Refusal(() => string.Format("x", null)));
            Log("join " + Refusal(() => string.Join(",", (string[])null)) + " " + Refusal(() => string.Join(null, new string[] { "a", "b" })));
        }

        private static int noisyWalks;

        private static IEnumerable<int> Noisy()
        {
            noisyWalks++;
            yield return 1;
        }

        // Array.Clear takes the array as an argument: a null one is
        // ArgumentNullException, whatever shape it would have had. LINQ checks
        // its source and its function before it asks for an element, so an
        // empty source with a null predicate still refuses, and a negative
        // ElementAt never starts the source.
        private static void ArgumentChecks()
        {
            Section("argument checks");
            Log("clear " + Refusal(() => { Array.Clear((int[])null, 0, 0); return "ok"; }) + " "
                + Refusal(() => { Array.Clear((Vec2[])null, 0, 0); return "ok"; }) + " "
                + Refusal(() => { Array.Clear((int[,])null, 0, 0); return "ok"; }) + " "
                + Refusal(() => { Array.Clear((string[])null, 0, 0); return "ok"; }));
            List<int> empty = new List<int>();
            Func<int, bool> np = null;
            Func<int, int> ns = null;
            Log("null predicate " + Refusal(() => "" + empty.Any(np)) + " " + Refusal(() => "" + empty.All(np)) + " "
                + Refusal(() => "" + empty.Count(np)) + " " + Refusal(() => "" + empty.First(np)) + " "
                + Refusal(() => "" + empty.FirstOrDefault(np)) + " " + Refusal(() => "" + empty.Where(np).Count()) + " "
                + Refusal(() => "" + empty.Select(ns).Count()));
            IEnumerable<int> none = null;
            Log("null source " + Refusal(() => "" + none.Any()) + " " + Refusal(() => "" + none.Count()) + " "
                + Refusal(() => "" + none.Concat(empty).Count()) + " " + Refusal(() => "" + empty.Concat(none).Count()) + " "
                + Refusal(() => "" + none.ElementAt(0)));
            noisyWalks = 0;
            Log("element at " + Refusal(() => "" + Noisy().ElementAt(-1)) + " walks=" + noisyWalks + " "
                + Refusal(() => "" + Noisy().ElementAt(5)) + " walks=" + noisyWalks + " "
                + Refusal(() => "" + Noisy().Where(np).Count()) + " walks=" + noisyWalks);
            Log("take skip [" + Rest(new int[] { 1, 2 }.Take(-1).GetEnumerator()) + "] [" + Rest(new int[] { 1, 2 }.Skip(-1).GetEnumerator()) + "]");
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
            EnumMethods();
            BaseLibrary();
            StructEquality();
            StructsThroughDelegates();
            ErasedStructValues();
            Angles();
            StaticInit();
            UnsignedBoxes();
            DelegateTypes();
            Powers();
            Rounding();
            ArraysAsEnumerables();
            StructArraysFromGenerics();
            BoxesAndReceivers();
            UnboxingEnums();
            GenericStatics();
            PrimitiveArraysToGenerics();
            StringArgumentErrors();
            ComparableArguments();
            ArraysAsCollections();
            StringReceivers();
            DelegateTypeArguments();
            ConstructedTypes();
            EnumeratorReset();
            NullParamsArrays();
            ArgumentChecks();
            Console.WriteLine("done");
        }
    }

    public struct HidePoint
    {
        public int X;
    }

    public class HideBase
    {
        public string Name() { return "base"; }
        public virtual string Virt() { return "base-virt"; }
        private string Helper() { return "base-helper"; }
        public string Ask() { return Helper(); }
    }

    public class HideDerived : HideBase
    {
        public new string Name() { return "derived"; }
        public new virtual string Virt() { return "derived-virt"; }
        private string Helper() { return "derived-helper"; }
        public string AskMine() { return Helper(); }
    }

    public class HideLeaf : HideDerived
    {
        public override string Virt() { return "leaf-virt"; }
    }
}
