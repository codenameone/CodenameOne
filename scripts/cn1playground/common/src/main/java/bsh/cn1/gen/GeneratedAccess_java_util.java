package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_util {
    private GeneratedAccess_java_util() {
    }

    public static Class<?> findClass(String name) {
        if ("java.util.AbstractCollection".equals(name)) return java.util.AbstractCollection.class;
        if ("java.util.AbstractList".equals(name)) return java.util.AbstractList.class;
        if ("java.util.AbstractMap".equals(name)) return java.util.AbstractMap.class;
        if ("java.util.AbstractQueue".equals(name)) return java.util.AbstractQueue.class;
        if ("java.util.AbstractSequentialList".equals(name)) return java.util.AbstractSequentialList.class;
        if ("java.util.AbstractSet".equals(name)) return java.util.AbstractSet.class;
        if ("java.util.ArrayDeque".equals(name)) return java.util.ArrayDeque.class;
        if ("java.util.ArrayList".equals(name)) return java.util.ArrayList.class;
        if ("java.util.Arrays".equals(name)) return java.util.Arrays.class;
        if ("java.util.BitSet".equals(name)) return java.util.BitSet.class;
        if ("java.util.Calendar".equals(name)) return java.util.Calendar.class;
        if ("java.util.Collection".equals(name)) return java.util.Collection.class;
        if ("java.util.Collections".equals(name)) return java.util.Collections.class;
        if ("java.util.Comparator".equals(name)) return java.util.Comparator.class;
        if ("java.util.ConcurrentModificationException".equals(name)) return java.util.ConcurrentModificationException.class;
        if ("java.util.Date".equals(name)) return java.util.Date.class;
        if ("java.util.Deque".equals(name)) return java.util.Deque.class;
        if ("java.util.Dictionary".equals(name)) return java.util.Dictionary.class;
        if ("java.util.EmptyStackException".equals(name)) return java.util.EmptyStackException.class;
        if ("java.util.Enumeration".equals(name)) return java.util.Enumeration.class;
        if ("java.util.EventListener".equals(name)) return java.util.EventListener.class;
        if ("java.util.EventListenerProxy".equals(name)) return java.util.EventListenerProxy.class;
        if ("java.util.HashMap".equals(name)) return java.util.HashMap.class;
        if ("java.util.HashSet".equals(name)) return java.util.HashSet.class;
        if ("java.util.Hashtable".equals(name)) return java.util.Hashtable.class;
        if ("java.util.IdentityHashMap".equals(name)) return java.util.IdentityHashMap.class;
        if ("java.util.Iterator".equals(name)) return java.util.Iterator.class;
        if ("java.util.LinkedHashMap".equals(name)) return java.util.LinkedHashMap.class;
        if ("java.util.LinkedHashSet".equals(name)) return java.util.LinkedHashSet.class;
        if ("java.util.LinkedList".equals(name)) return java.util.LinkedList.class;
        if ("java.util.List".equals(name)) return java.util.List.class;
        if ("java.util.ListIterator".equals(name)) return java.util.ListIterator.class;
        if ("java.util.Locale".equals(name)) return java.util.Locale.class;
        if ("java.util.Map".equals(name)) return java.util.Map.class;
        if ("java.util.NavigableMap".equals(name)) return java.util.NavigableMap.class;
        if ("java.util.NavigableSet".equals(name)) return java.util.NavigableSet.class;
        if ("java.util.NoSuchElementException".equals(name)) return java.util.NoSuchElementException.class;
        if ("java.util.Objects".equals(name)) return java.util.Objects.class;
        if ("java.util.Observable".equals(name)) return java.util.Observable.class;
        if ("java.util.Observer".equals(name)) return java.util.Observer.class;
        if ("java.util.PriorityQueue".equals(name)) return java.util.PriorityQueue.class;
        if ("java.util.Queue".equals(name)) return java.util.Queue.class;
        if ("java.util.Random".equals(name)) return java.util.Random.class;
        if ("java.util.RandomAccess".equals(name)) return java.util.RandomAccess.class;
        if ("java.util.Set".equals(name)) return java.util.Set.class;
        if ("java.util.SortedMap".equals(name)) return java.util.SortedMap.class;
        if ("java.util.SortedSet".equals(name)) return java.util.SortedSet.class;
        if ("java.util.Stack".equals(name)) return java.util.Stack.class;
        if ("java.util.StringTokenizer".equals(name)) return java.util.StringTokenizer.class;
        if ("java.util.TimeZone".equals(name)) return java.util.TimeZone.class;
        if ("java.util.Timer".equals(name)) return java.util.Timer.class;
        if ("java.util.TimerTask".equals(name)) return java.util.TimerTask.class;
        if ("java.util.TreeMap".equals(name)) return java.util.TreeMap.class;
        if ("java.util.TreeSet".equals(name)) return java.util.TreeSet.class;
        if ("java.util.Vector".equals(name)) return java.util.Vector.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.util.ArrayDeque.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.ArrayDeque();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.ArrayDeque(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.ArrayDeque((java.util.Collection) safeArgs[0]);
            }
        }
        if (type == java.util.ArrayList.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.ArrayList();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.ArrayList(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.ArrayList((java.util.Collection) safeArgs[0]);
            }
        }
        if (type == java.util.BitSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.BitSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.BitSet(((Number) safeArgs[0]).intValue());
            }
        }
        if (type == java.util.ConcurrentModificationException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.ConcurrentModificationException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.util.ConcurrentModificationException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.util.Date.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Date();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return new java.util.Date(((Number) safeArgs[0]).longValue());
            }
        }
        if (type == java.util.EmptyStackException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.EmptyStackException();
            }
        }
        if (type == java.util.HashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.HashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.HashMap(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return new java.util.HashMap((java.util.Map) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return new java.util.HashMap(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if (type == java.util.HashSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.HashSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.HashSet(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.HashSet((java.util.Collection) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return new java.util.HashSet(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if (type == java.util.Hashtable.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Hashtable();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.Hashtable(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return new java.util.Hashtable((java.util.Map) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return new java.util.Hashtable(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if (type == java.util.IdentityHashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.IdentityHashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.IdentityHashMap(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return new java.util.IdentityHashMap((java.util.Map) safeArgs[0]);
            }
        }
        if (type == java.util.LinkedHashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.LinkedHashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.LinkedHashMap(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return new java.util.LinkedHashMap((java.util.Map) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return new java.util.LinkedHashMap(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                return new java.util.LinkedHashMap(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue(), ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if (type == java.util.LinkedHashSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.LinkedHashSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.LinkedHashSet(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.LinkedHashSet((java.util.Collection) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                return new java.util.LinkedHashSet(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).floatValue());
            }
        }
        if (type == java.util.LinkedList.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.LinkedList();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.LinkedList((java.util.Collection) safeArgs[0]);
            }
        }
        if (type == java.util.Locale.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new java.util.Locale((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        if (type == java.util.NoSuchElementException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.NoSuchElementException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.util.NoSuchElementException((java.lang.String) safeArgs[0]);
            }
        }
        if (type == java.util.Observable.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Observable();
            }
        }
        if (type == java.util.PriorityQueue.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.PriorityQueue();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.PriorityQueue(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.PriorityQueue((java.util.Collection) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.PriorityQueue.class}, false)) {
                return new java.util.PriorityQueue((java.util.PriorityQueue) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                return new java.util.PriorityQueue((java.util.SortedSet) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Comparator.class}, false)) {
                return new java.util.PriorityQueue(((Number) safeArgs[0]).intValue(), (java.util.Comparator) safeArgs[1]);
            }
        }
        if (type == java.util.Random.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Random();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return new java.util.Random(((Number) safeArgs[0]).longValue());
            }
        }
        if (type == java.util.Stack.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Stack();
            }
        }
        if (type == java.util.StringTokenizer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new java.util.StringTokenizer((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new java.util.StringTokenizer((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return new java.util.StringTokenizer((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if (type == java.util.Timer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Timer();
            }
        }
        if (type == java.util.TreeMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.TreeMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                return new java.util.TreeMap((java.util.Comparator) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return new java.util.TreeMap((java.util.Map) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedMap.class}, false)) {
                return new java.util.TreeMap((java.util.SortedMap) safeArgs[0]);
            }
        }
        if (type == java.util.TreeSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.TreeSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.TreeSet((java.util.Collection) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                return new java.util.TreeSet((java.util.Comparator) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                return new java.util.TreeSet((java.util.SortedSet) safeArgs[0]);
            }
        }
        if (type == java.util.Vector.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new java.util.Vector();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return new java.util.Vector(((Number) safeArgs[0]).intValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return new java.util.Vector((java.util.Collection) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return new java.util.Vector(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.util.Arrays.class) return invokeStatic0(name, safeArgs);
        if (type == java.util.Calendar.class) return invokeStatic1(name, safeArgs);
        if (type == java.util.Collections.class) return invokeStatic2(name, safeArgs);
        if (type == java.util.Locale.class) return invokeStatic3(name, safeArgs);
        if (type == java.util.Objects.class) return invokeStatic4(name, safeArgs);
        if (type == java.util.TimeZone.class) return invokeStatic5(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false)) {
                return java.util.Arrays.binarySearch((byte[]) safeArgs[0], ((Number) safeArgs[1]).byteValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false)) {
                return java.util.Arrays.binarySearch((char[]) safeArgs[0], ((Character) safeArgs[1]).charValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false)) {
                return java.util.Arrays.binarySearch((double[]) safeArgs[0], ((Number) safeArgs[1]).doubleValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false)) {
                return java.util.Arrays.binarySearch((float[]) safeArgs[0], ((Number) safeArgs[1]).floatValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.binarySearch((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                return java.util.Arrays.binarySearch((java.lang.Object[]) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false)) {
                return java.util.Arrays.binarySearch((long[]) safeArgs[0], ((Number) safeArgs[1]).longValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false)) {
                return java.util.Arrays.binarySearch((short[]) safeArgs[0], ((Number) safeArgs[1]).shortValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false)) {
                return java.util.Arrays.binarySearch((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).byteValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false)) {
                return java.util.Arrays.binarySearch((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Character) safeArgs[3]).charValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false)) {
                return java.util.Arrays.binarySearch((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).doubleValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                return java.util.Arrays.binarySearch((float[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).floatValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.binarySearch((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                return java.util.Arrays.binarySearch((java.lang.Object[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), (java.lang.Object) safeArgs[3]);
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                return java.util.Arrays.binarySearch((long[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).longValue());
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false)) {
                return java.util.Arrays.binarySearch((short[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).shortValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((boolean[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((float[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((long[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOf((short[]) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((boolean[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((float[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((long[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return java.util.Arrays.copyOfRange((short[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue());
            }
        }
        if ("deepEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false)) {
                return java.util.Arrays.deepEquals((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1]);
            }
        }
        if ("deepHashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                return java.util.Arrays.deepHashCode((java.lang.Object[]) safeArgs[0]);
            }
        }
        if ("deepToString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                return java.util.Arrays.deepToString((java.lang.Object[]) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, boolean[].class}, false)) {
                return java.util.Arrays.equals((boolean[]) safeArgs[0], (boolean[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                return java.util.Arrays.equals((byte[]) safeArgs[0], (byte[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, char[].class}, false)) {
                return java.util.Arrays.equals((char[]) safeArgs[0], (char[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, double[].class}, false)) {
                return java.util.Arrays.equals((double[]) safeArgs[0], (double[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, float[].class}, false)) {
                return java.util.Arrays.equals((float[]) safeArgs[0], (float[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                return java.util.Arrays.equals((int[]) safeArgs[0], (int[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false)) {
                return java.util.Arrays.equals((java.lang.Object[]) safeArgs[0], (java.lang.Object[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, long[].class}, false)) {
                return java.util.Arrays.equals((long[]) safeArgs[0], (long[]) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, short[].class}, false)) {
                return java.util.Arrays.equals((short[]) safeArgs[0], (short[]) safeArgs[1]);
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Boolean.class}, false)) {
                java.util.Arrays.fill((boolean[]) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false)) {
                java.util.Arrays.fill((byte[]) safeArgs[0], ((Number) safeArgs[1]).byteValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false)) {
                java.util.Arrays.fill((char[]) safeArgs[0], ((Character) safeArgs[1]).charValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false)) {
                java.util.Arrays.fill((double[]) safeArgs[0], ((Number) safeArgs[1]).doubleValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false)) {
                java.util.Arrays.fill((float[]) safeArgs[0], ((Number) safeArgs[1]).floatValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                java.util.Arrays.fill((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                java.util.Arrays.fill((java.lang.Object[]) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false)) {
                java.util.Arrays.fill((long[]) safeArgs[0], ((Number) safeArgs[1]).longValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false)) {
                java.util.Arrays.fill((short[]) safeArgs[0], ((Number) safeArgs[1]).shortValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                java.util.Arrays.fill((boolean[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Boolean) safeArgs[3]).booleanValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false)) {
                java.util.Arrays.fill((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).byteValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false)) {
                java.util.Arrays.fill((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Character) safeArgs[3]).charValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false)) {
                java.util.Arrays.fill((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).doubleValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                java.util.Arrays.fill((float[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).floatValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.fill((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                java.util.Arrays.fill((java.lang.Object[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), (java.lang.Object) safeArgs[3]); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                java.util.Arrays.fill((long[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).longValue()); return null;
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false)) {
                java.util.Arrays.fill((short[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).shortValue()); return null;
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class}, false)) {
                return java.util.Arrays.hashCode((boolean[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return java.util.Arrays.hashCode((byte[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return java.util.Arrays.hashCode((char[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return java.util.Arrays.hashCode((double[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                return java.util.Arrays.hashCode((float[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                return java.util.Arrays.hashCode((int[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                return java.util.Arrays.hashCode((java.lang.Object[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                return java.util.Arrays.hashCode((long[]) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                return java.util.Arrays.hashCode((short[]) safeArgs[0]);
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                java.util.Arrays.sort((byte[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                java.util.Arrays.sort((char[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                java.util.Arrays.sort((double[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                java.util.Arrays.sort((float[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                java.util.Arrays.sort((int[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                java.util.Arrays.sort((java.lang.Object[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                java.util.Arrays.sort((long[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                java.util.Arrays.sort((short[]) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((byte[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((char[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((double[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((float[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((int[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((java.lang.Object[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((long[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Arrays.sort((short[]) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class}, false)) {
                return java.util.Arrays.toString((boolean[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                return java.util.Arrays.toString((byte[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                return java.util.Arrays.toString((char[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                return java.util.Arrays.toString((double[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                return java.util.Arrays.toString((float[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                return java.util.Arrays.toString((int[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                return java.util.Arrays.toString((java.lang.Object[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                return java.util.Arrays.toString((long[]) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                return java.util.Arrays.toString((short[]) safeArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Arrays.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Calendar.getInstance();
            }
        }
        if ("getInstance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                return java.util.Calendar.getInstance((java.util.TimeZone) safeArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Calendar.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("asLifoQueue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Deque.class}, false)) {
                return java.util.Collections.asLifoQueue((java.util.Deque) safeArgs[0]);
            }
        }
        if ("checkedCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Class.class}, false)) {
                return java.util.Collections.checkedCollection((java.util.Collection) safeArgs[0], (java.lang.Class) safeArgs[1]);
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                java.util.Collections.copy((java.util.List) safeArgs[0], (java.util.List) safeArgs[1]); return null;
            }
        }
        if ("disjoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Collection.class}, false)) {
                return java.util.Collections.disjoint((java.util.Collection) safeArgs[0], (java.util.Collection) safeArgs[1]);
            }
        }
        if ("emptyList".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Collections.emptyList();
            }
        }
        if ("emptyMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Collections.emptyMap();
            }
        }
        if ("emptySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Collections.emptySet();
            }
        }
        if ("enumeration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return java.util.Collections.enumeration((java.util.Collection) safeArgs[0]);
            }
        }
        if ("frequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Object.class}, false)) {
                return java.util.Collections.frequency((java.util.Collection) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("indexOfSubList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                return java.util.Collections.indexOfSubList((java.util.List) safeArgs[0], (java.util.List) safeArgs[1]);
            }
        }
        if ("lastIndexOfSubList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                return java.util.Collections.lastIndexOfSubList((java.util.List) safeArgs[0], (java.util.List) safeArgs[1]);
            }
        }
        if ("list".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Enumeration.class}, false)) {
                return java.util.Collections.list((java.util.Enumeration) safeArgs[0]);
            }
        }
        if ("newSetFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return java.util.Collections.newSetFromMap((java.util.Map) safeArgs[0]);
            }
        }
        if ("reverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                java.util.Collections.reverse((java.util.List) safeArgs[0]); return null;
            }
        }
        if ("reverseOrder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Collections.reverseOrder();
            }
        }
        if ("reverseOrder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                return java.util.Collections.reverseOrder((java.util.Comparator) safeArgs[0]);
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class}, false)) {
                java.util.Collections.rotate((java.util.List) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("shuffle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                java.util.Collections.shuffle((java.util.List) safeArgs[0]); return null;
            }
        }
        if ("shuffle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Random.class}, false)) {
                java.util.Collections.shuffle((java.util.List) safeArgs[0], (java.util.Random) safeArgs[1]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                java.util.Collections.sort((java.util.List) safeArgs[0]); return null;
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Comparator.class}, false)) {
                java.util.Collections.sort((java.util.List) safeArgs[0], (java.util.Comparator) safeArgs[1]); return null;
            }
        }
        if ("swap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                java.util.Collections.swap((java.util.List) safeArgs[0], ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue()); return null;
            }
        }
        if ("synchronizedCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return java.util.Collections.synchronizedCollection((java.util.Collection) safeArgs[0]);
            }
        }
        if ("synchronizedList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                return java.util.Collections.synchronizedList((java.util.List) safeArgs[0]);
            }
        }
        if ("synchronizedMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return java.util.Collections.synchronizedMap((java.util.Map) safeArgs[0]);
            }
        }
        if ("synchronizedSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Set.class}, false)) {
                return java.util.Collections.synchronizedSet((java.util.Set) safeArgs[0]);
            }
        }
        if ("synchronizedSortedMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.SortedMap.class}, false)) {
                return java.util.Collections.synchronizedSortedMap((java.util.SortedMap) safeArgs[0]);
            }
        }
        if ("synchronizedSortedSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                return java.util.Collections.synchronizedSortedSet((java.util.SortedSet) safeArgs[0]);
            }
        }
        if ("unmodifiableCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return java.util.Collections.unmodifiableCollection((java.util.Collection) safeArgs[0]);
            }
        }
        if ("unmodifiableList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                return java.util.Collections.unmodifiableList((java.util.List) safeArgs[0]);
            }
        }
        if ("unmodifiableMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                return java.util.Collections.unmodifiableMap((java.util.Map) safeArgs[0]);
            }
        }
        if ("unmodifiableSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Set.class}, false)) {
                return java.util.Collections.unmodifiableSet((java.util.Set) safeArgs[0]);
            }
        }
        if ("unmodifiableSortedMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.SortedMap.class}, false)) {
                return java.util.Collections.unmodifiableSortedMap((java.util.SortedMap) safeArgs[0]);
            }
        }
        if ("unmodifiableSortedSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                return java.util.Collections.unmodifiableSortedSet((java.util.SortedSet) safeArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Collections.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.Locale.getDefault();
            }
        }
        if ("setDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Locale.class}, false)) {
                java.util.Locale.setDefault((java.util.Locale) safeArgs[0]); return null;
            }
        }
        throw unsupportedStatic(java.util.Locale.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("deepEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                return java.util.Objects.deepEquals((java.lang.Object) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                return java.util.Objects.equals((java.lang.Object) safeArgs[0], (java.lang.Object) safeArgs[1]);
            }
        }
        if ("hash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                java.lang.Object[] varArgs = new java.lang.Object[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) safeArgs[i];
                }
                return java.util.Objects.hash(varArgs);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return java.util.Objects.hashCode((java.lang.Object) safeArgs[0]);
            }
        }
        if ("nonNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return java.util.Objects.nonNull((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return java.util.Objects.toString((java.lang.Object) safeArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                return java.util.Objects.toString((java.lang.Object) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        throw unsupportedStatic(java.util.Objects.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getAvailableIDs".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.TimeZone.getAvailableIDs();
            }
        }
        if ("getDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return java.util.TimeZone.getDefault();
            }
        }
        if ("getTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return java.util.TimeZone.getTimeZone((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.TimeZone.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof java.util.LinkedHashSet) {
            try {
                return invoke0((java.util.LinkedHashSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.LinkedList) {
            try {
                return invoke1((java.util.LinkedList) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Stack) {
            try {
                return invoke2((java.util.Stack) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractSequentialList) {
            try {
                return invoke3((java.util.AbstractSequentialList) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.ArrayList) {
            try {
                return invoke4((java.util.ArrayList) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.HashSet) {
            try {
                return invoke5((java.util.HashSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.LinkedHashMap) {
            try {
                return invoke6((java.util.LinkedHashMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.PriorityQueue) {
            try {
                return invoke7((java.util.PriorityQueue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TreeSet) {
            try {
                return invoke8((java.util.TreeSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Vector) {
            try {
                return invoke9((java.util.Vector) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractList) {
            try {
                return invoke10((java.util.AbstractList) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractQueue) {
            try {
                return invoke11((java.util.AbstractQueue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractSet) {
            try {
                return invoke12((java.util.AbstractSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.ArrayDeque) {
            try {
                return invoke13((java.util.ArrayDeque) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.HashMap) {
            try {
                return invoke14((java.util.HashMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Hashtable) {
            try {
                return invoke15((java.util.Hashtable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.IdentityHashMap) {
            try {
                return invoke16((java.util.IdentityHashMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TreeMap) {
            try {
                return invoke17((java.util.TreeMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractCollection) {
            try {
                return invoke18((java.util.AbstractCollection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractMap) {
            try {
                return invoke19((java.util.AbstractMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.BitSet) {
            try {
                return invoke20((java.util.BitSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Calendar) {
            try {
                return invoke21((java.util.Calendar) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Date) {
            try {
                return invoke22((java.util.Date) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Dictionary) {
            try {
                return invoke23((java.util.Dictionary) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.EmptyStackException) {
            try {
                return invoke24((java.util.EmptyStackException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.EventListenerProxy) {
            try {
                return invoke25((java.util.EventListenerProxy) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Locale) {
            try {
                return invoke26((java.util.Locale) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NoSuchElementException) {
            try {
                return invoke27((java.util.NoSuchElementException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Observable) {
            try {
                return invoke28((java.util.Observable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Random) {
            try {
                return invoke29((java.util.Random) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.StringTokenizer) {
            try {
                return invoke30((java.util.StringTokenizer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TimeZone) {
            try {
                return invoke31((java.util.TimeZone) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Timer) {
            try {
                return invoke32((java.util.Timer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TimerTask) {
            try {
                return invoke33((java.util.TimerTask) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Collection) {
            try {
                return invoke34((java.util.Collection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Comparator) {
            try {
                return invoke35((java.util.Comparator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Deque) {
            try {
                return invoke36((java.util.Deque) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Enumeration) {
            try {
                return invoke37((java.util.Enumeration) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Iterator) {
            try {
                return invoke38((java.util.Iterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.List) {
            try {
                return invoke39((java.util.List) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.ListIterator) {
            try {
                return invoke40((java.util.ListIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Map) {
            try {
                return invoke41((java.util.Map) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NavigableMap) {
            try {
                return invoke42((java.util.NavigableMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NavigableSet) {
            try {
                return invoke43((java.util.NavigableSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Observer) {
            try {
                return invoke44((java.util.Observer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Queue) {
            try {
                return invoke45((java.util.Queue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Set) {
            try {
                return invoke46((java.util.Set) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.SortedMap) {
            try {
                return invoke47((java.util.SortedMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.SortedSet) {
            try {
                return invoke48((java.util.SortedSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(java.util.LinkedHashSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.util.LinkedList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingIterator();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeFirstOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeLastOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(java.util.Stack typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("capacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.capacity();
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("copyInto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                typedTarget.copyInto((java.lang.Object[]) safeArgs[0]); return null;
            }
        }
        if ("elements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.elements();
            }
        }
        if ("empty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.empty();
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.ensureCapacity(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("removeAllElements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllElements(); return null;
            }
        }
        if ("removeElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeElement((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeElementAt(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("search".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.search((java.lang.Object) safeArgs[0]);
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.util.AbstractSequentialList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(java.util.ArrayList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.ensureCapacity(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(java.util.HashSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(java.util.LinkedHashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(java.util.PriorityQueue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(java.util.TreeSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingIterator();
            }
        }
        if ("descendingSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingSet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(java.util.Vector typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("capacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.capacity();
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("copyInto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                typedTarget.copyInto((java.lang.Object[]) safeArgs[0]); return null;
            }
        }
        if ("elements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.elements();
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.ensureCapacity(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0], ((Number) safeArgs[1]).intValue());
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("removeAllElements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAllElements(); return null;
            }
        }
        if ("removeElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeElement((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeElementAt(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSize(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(java.util.AbstractList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(java.util.AbstractQueue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(java.util.AbstractSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(java.util.ArrayDeque typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingIterator();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeFirstOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeLastOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(java.util.HashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(java.util.Hashtable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("elements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.elements();
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("keys".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keys();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(java.util.IdentityHashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(java.util.TreeMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("descendingKeySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingKeySet();
            }
        }
        if ("descendingMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingMap();
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("firstEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.firstEntry();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("lastEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.lastEntry();
            }
        }
        if ("navigableKeySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.navigableKeySet();
            }
        }
        if ("pollFirstEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pollFirstEntry();
            }
        }
        if ("pollLastEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pollLastEntry();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(java.util.AbstractCollection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(java.util.AbstractMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(java.util.BitSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("and".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                typedTarget.and((java.util.BitSet) safeArgs[0]); return null;
            }
        }
        if ("andNot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                typedTarget.andNot((java.util.BitSet) safeArgs[0]); return null;
            }
        }
        if ("cardinality".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.cardinality();
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.clear(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.clear(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("flip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.flip(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("flip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.flip(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.get(((Number) safeArgs[0]).intValue());
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.get(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                return typedTarget.intersects((java.util.BitSet) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("length".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.length();
            }
        }
        if ("nextClearBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.nextClearBit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("nextSetBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.nextSetBit(((Number) safeArgs[0]).intValue());
            }
        }
        if ("or".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                typedTarget.or((java.util.BitSet) safeArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), ((Boolean) safeArgs[1]).booleanValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Boolean) safeArgs[2]).booleanValue()); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        if ("xor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                typedTarget.xor((java.util.BitSet) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(java.util.Calendar typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.add(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("after".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.after((java.lang.Object) safeArgs[0]);
            }
        }
        if ("before".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.before((java.lang.Object) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.get(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTime();
            }
        }
        if ("getTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimeZone();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                typedTarget.set(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                typedTarget.setTime((java.util.Date) safeArgs[0]); return null;
            }
        }
        if ("setTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                typedTarget.setTimeZone((java.util.TimeZone) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(java.util.Date typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                return typedTarget.compareTo((java.util.Date) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("getTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTime();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setTime(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(java.util.Dictionary typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("elements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.elements();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keys".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keys();
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(java.util.EmptyStackException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.addSuppressed((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return typedTarget.initCause((java.lang.Throwable) safeArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(java.util.EventListenerProxy typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getListener();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(java.util.Locale typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCountry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCountry();
            }
        }
        if ("getLanguage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLanguage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(java.util.NoSuchElementException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                typedTarget.addSuppressed((java.lang.Throwable) safeArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                return typedTarget.initCause((java.lang.Throwable) safeArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(java.util.Observable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                typedTarget.addObserver((java.util.Observer) safeArgs[0]); return null;
            }
        }
        if ("countObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                typedTarget.deleteObserver((java.util.Observer) safeArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("hasChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasChanged();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.notifyObservers(); return null;
            }
        }
        if ("notifyObservers".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.notifyObservers((java.lang.Object) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(java.util.Random typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("nextBoolean".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextBoolean();
            }
        }
        if ("nextBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                typedTarget.nextBytes((byte[]) safeArgs[0]); return null;
            }
        }
        if ("nextDouble".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextDouble();
            }
        }
        if ("nextFloat".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextFloat();
            }
        }
        if ("nextInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextInt();
            }
        }
        if ("nextInt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.nextInt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("nextLong".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextLong();
            }
        }
        if ("setSeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setSeed(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke30(java.util.StringTokenizer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("countTokens".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.countTokens();
            }
        }
        if ("hasMoreElements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasMoreElements();
            }
        }
        if ("hasMoreTokens".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasMoreTokens();
            }
        }
        if ("nextElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextElement();
            }
        }
        if ("nextToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextToken();
            }
        }
        if ("nextToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.nextToken((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke31(java.util.TimeZone typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getID".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getID();
            }
        }
        if ("getOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.getOffset(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue(), ((Number) safeArgs[2]).intValue(), ((Number) safeArgs[3]).intValue(), ((Number) safeArgs[4]).intValue(), ((Number) safeArgs[5]).intValue());
            }
        }
        if ("getRawOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRawOffset();
            }
        }
        if ("useDaylightTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.useDaylightTime();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke32(java.util.Timer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.cancel(); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class}, false)) {
                typedTarget.schedule((java.util.TimerTask) safeArgs[0], (java.util.Date) safeArgs[1]); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class}, false)) {
                typedTarget.schedule((java.util.TimerTask) safeArgs[0], ((Number) safeArgs[1]).longValue()); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false)) {
                typedTarget.schedule((java.util.TimerTask) safeArgs[0], (java.util.Date) safeArgs[1], ((Number) safeArgs[2]).longValue()); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                typedTarget.schedule((java.util.TimerTask) safeArgs[0], ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue()); return null;
            }
        }
        if ("scheduleAtFixedRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false)) {
                typedTarget.scheduleAtFixedRate((java.util.TimerTask) safeArgs[0], (java.util.Date) safeArgs[1], ((Number) safeArgs[2]).longValue()); return null;
            }
        }
        if ("scheduleAtFixedRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                typedTarget.scheduleAtFixedRate((java.util.TimerTask) safeArgs[0], ((Number) safeArgs[1]).longValue(), ((Number) safeArgs[2]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke33(java.util.TimerTask typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.cancel();
            }
        }
        if ("scheduledExecutionTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.scheduledExecutionTime();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke34(java.util.Collection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke35(java.util.Comparator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke36(java.util.Deque typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingIterator();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeFirstOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.removeLastOccurrence((java.lang.Object) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke37(java.util.Enumeration typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("hasMoreElements".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasMoreElements();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke38(java.util.Iterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("hasNext".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasNext();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.remove(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke39(java.util.List typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                return typedTarget.addAll(((Number) safeArgs[0]).intValue(), (java.util.Collection) safeArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.indexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.lastIndexOf((java.lang.Object) safeArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.listIterator();
            }
        }
        if ("listIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.listIterator(((Number) safeArgs[0]).intValue());
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                return typedTarget.subList(((Number) safeArgs[0]).intValue(), ((Number) safeArgs[1]).intValue());
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke40(java.util.ListIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("hasNext".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasNext();
            }
        }
        if ("hasPrevious".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hasPrevious();
            }
        }
        if ("nextIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.nextIndex();
            }
        }
        if ("previousIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.previousIndex();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.remove(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke41(java.util.Map typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke42(java.util.NavigableMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("descendingKeySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingKeySet();
            }
        }
        if ("descendingMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingMap();
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("firstEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.firstEntry();
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("lastEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.lastEntry();
            }
        }
        if ("navigableKeySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.navigableKeySet();
            }
        }
        if ("pollFirstEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pollFirstEntry();
            }
        }
        if ("pollLastEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.pollLastEntry();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke43(java.util.NavigableSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingIterator();
            }
        }
        if ("descendingSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.descendingSet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke44(java.util.Observer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observable.class, java.lang.Object.class}, false)) {
                typedTarget.update((java.util.Observable) safeArgs[0], (java.lang.Object) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke45(java.util.Queue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke46(java.util.Set typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke47(java.util.SortedMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsKey((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.containsValue((java.lang.Object) safeArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                typedTarget.putAll((java.util.Map) safeArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("values".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke48(java.util.SortedSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.addAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.contains((java.lang.Object) safeArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.containsAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.equals((java.lang.Object) safeArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                return typedTarget.remove((java.lang.Object) safeArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.removeAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                return typedTarget.retainAll((java.util.Collection) safeArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toArray();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.util.Calendar.class) {
            if ("AM".equals(name)) return java.util.Calendar.AM;
            if ("AM_PM".equals(name)) return java.util.Calendar.AM_PM;
            if ("APRIL".equals(name)) return java.util.Calendar.APRIL;
            if ("AUGUST".equals(name)) return java.util.Calendar.AUGUST;
            if ("DATE".equals(name)) return java.util.Calendar.DATE;
            if ("DAY_OF_MONTH".equals(name)) return java.util.Calendar.DAY_OF_MONTH;
            if ("DAY_OF_WEEK".equals(name)) return java.util.Calendar.DAY_OF_WEEK;
            if ("DAY_OF_WEEK_IN_MONTH".equals(name)) return java.util.Calendar.DAY_OF_WEEK_IN_MONTH;
            if ("DECEMBER".equals(name)) return java.util.Calendar.DECEMBER;
            if ("FEBRUARY".equals(name)) return java.util.Calendar.FEBRUARY;
            if ("FRIDAY".equals(name)) return java.util.Calendar.FRIDAY;
            if ("HOUR".equals(name)) return java.util.Calendar.HOUR;
            if ("HOUR_OF_DAY".equals(name)) return java.util.Calendar.HOUR_OF_DAY;
            if ("JANUARY".equals(name)) return java.util.Calendar.JANUARY;
            if ("JULY".equals(name)) return java.util.Calendar.JULY;
            if ("JUNE".equals(name)) return java.util.Calendar.JUNE;
            if ("MARCH".equals(name)) return java.util.Calendar.MARCH;
            if ("MAY".equals(name)) return java.util.Calendar.MAY;
            if ("MILLISECOND".equals(name)) return java.util.Calendar.MILLISECOND;
            if ("MINUTE".equals(name)) return java.util.Calendar.MINUTE;
            if ("MONDAY".equals(name)) return java.util.Calendar.MONDAY;
            if ("MONTH".equals(name)) return java.util.Calendar.MONTH;
            if ("NOVEMBER".equals(name)) return java.util.Calendar.NOVEMBER;
            if ("OCTOBER".equals(name)) return java.util.Calendar.OCTOBER;
            if ("PM".equals(name)) return java.util.Calendar.PM;
            if ("SATURDAY".equals(name)) return java.util.Calendar.SATURDAY;
            if ("SECOND".equals(name)) return java.util.Calendar.SECOND;
            if ("SEPTEMBER".equals(name)) return java.util.Calendar.SEPTEMBER;
            if ("SUNDAY".equals(name)) return java.util.Calendar.SUNDAY;
            if ("THURSDAY".equals(name)) return java.util.Calendar.THURSDAY;
            if ("TUESDAY".equals(name)) return java.util.Calendar.TUESDAY;
            if ("WEDNESDAY".equals(name)) return java.util.Calendar.WEDNESDAY;
            if ("WEEK_OF_MONTH".equals(name)) return java.util.Calendar.WEEK_OF_MONTH;
            if ("WEEK_OF_YEAR".equals(name)) return java.util.Calendar.WEEK_OF_YEAR;
            if ("YEAR".equals(name)) return java.util.Calendar.YEAR;
        }
        if (type == java.util.Collections.class) {
            if ("EMPTY_LIST".equals(name)) return java.util.Collections.EMPTY_LIST;
            if ("EMPTY_MAP".equals(name)) return java.util.Collections.EMPTY_MAP;
            if ("EMPTY_SET".equals(name)) return java.util.Collections.EMPTY_SET;
        }
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            return value instanceof Number;
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
