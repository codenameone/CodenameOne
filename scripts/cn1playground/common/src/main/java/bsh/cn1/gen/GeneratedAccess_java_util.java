package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_java_util {
    private GeneratedAccess_java_util() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("AbstractCollection".equals(simpleName)) {
            return java.util.AbstractCollection.class;
        }
        if ("AbstractList".equals(simpleName)) {
            return java.util.AbstractList.class;
        }
        if ("AbstractMap".equals(simpleName)) {
            return java.util.AbstractMap.class;
        }
        if ("SimpleEntry".equals(simpleName)) {
            return java.util.AbstractMap.SimpleEntry.class;
        }
        if ("SimpleImmutableEntry".equals(simpleName)) {
            return java.util.AbstractMap.SimpleImmutableEntry.class;
        }
        if ("AbstractQueue".equals(simpleName)) {
            return java.util.AbstractQueue.class;
        }
        if ("AbstractSequentialList".equals(simpleName)) {
            return java.util.AbstractSequentialList.class;
        }
        if ("AbstractSet".equals(simpleName)) {
            return java.util.AbstractSet.class;
        }
        if ("ArrayDeque".equals(simpleName)) {
            return java.util.ArrayDeque.class;
        }
        if ("ArrayList".equals(simpleName)) {
            return java.util.ArrayList.class;
        }
        if ("Arrays".equals(simpleName)) {
            return java.util.Arrays.class;
        }
        if ("BitSet".equals(simpleName)) {
            return java.util.BitSet.class;
        }
        if ("Calendar".equals(simpleName)) {
            return java.util.Calendar.class;
        }
        if ("Collection".equals(simpleName)) {
            return java.util.Collection.class;
        }
        if ("Collections".equals(simpleName)) {
            return java.util.Collections.class;
        }
        if ("Comparator".equals(simpleName)) {
            return java.util.Comparator.class;
        }
        if ("ConcurrentModificationException".equals(simpleName)) {
            return java.util.ConcurrentModificationException.class;
        }
        if ("Date".equals(simpleName)) {
            return java.util.Date.class;
        }
        if ("Deque".equals(simpleName)) {
            return java.util.Deque.class;
        }
        if ("Dictionary".equals(simpleName)) {
            return java.util.Dictionary.class;
        }
        if ("EmptyStackException".equals(simpleName)) {
            return java.util.EmptyStackException.class;
        }
        if ("Enumeration".equals(simpleName)) {
            return java.util.Enumeration.class;
        }
        if ("EventListener".equals(simpleName)) {
            return java.util.EventListener.class;
        }
        if ("HashMap".equals(simpleName)) {
            return java.util.HashMap.class;
        }
        if ("HashSet".equals(simpleName)) {
            return java.util.HashSet.class;
        }
        if ("Hashtable".equals(simpleName)) {
            return java.util.Hashtable.class;
        }
        if ("IdentityHashMap".equals(simpleName)) {
            return java.util.IdentityHashMap.class;
        }
        if ("Iterator".equals(simpleName)) {
            return java.util.Iterator.class;
        }
        if ("LinkedHashMap".equals(simpleName)) {
            return java.util.LinkedHashMap.class;
        }
        if ("LinkedHashSet".equals(simpleName)) {
            return java.util.LinkedHashSet.class;
        }
        if ("LinkedList".equals(simpleName)) {
            return java.util.LinkedList.class;
        }
        if ("List".equals(simpleName)) {
            return java.util.List.class;
        }
        if ("ListIterator".equals(simpleName)) {
            return java.util.ListIterator.class;
        }
        if ("Locale".equals(simpleName)) {
            return java.util.Locale.class;
        }
        if ("Map".equals(simpleName)) {
            return java.util.Map.class;
        }
        if ("Entry".equals(simpleName)) {
            return java.util.Map.Entry.class;
        }
        if ("NavigableMap".equals(simpleName)) {
            return java.util.NavigableMap.class;
        }
        if ("NavigableSet".equals(simpleName)) {
            return java.util.NavigableSet.class;
        }
        if ("NoSuchElementException".equals(simpleName)) {
            return java.util.NoSuchElementException.class;
        }
        if ("Objects".equals(simpleName)) {
            return java.util.Objects.class;
        }
        if ("Observable".equals(simpleName)) {
            return java.util.Observable.class;
        }
        if ("Observer".equals(simpleName)) {
            return java.util.Observer.class;
        }
        if ("PriorityQueue".equals(simpleName)) {
            return java.util.PriorityQueue.class;
        }
        if ("Queue".equals(simpleName)) {
            return java.util.Queue.class;
        }
        if ("Random".equals(simpleName)) {
            return java.util.Random.class;
        }
        if ("RandomAccess".equals(simpleName)) {
            return java.util.RandomAccess.class;
        }
        if ("Set".equals(simpleName)) {
            return java.util.Set.class;
        }
        if ("SortedMap".equals(simpleName)) {
            return java.util.SortedMap.class;
        }
        if ("SortedSet".equals(simpleName)) {
            return java.util.SortedSet.class;
        }
        if ("Stack".equals(simpleName)) {
            return java.util.Stack.class;
        }
        if ("StringTokenizer".equals(simpleName)) {
            return java.util.StringTokenizer.class;
        }
        if ("TimeZone".equals(simpleName)) {
            return java.util.TimeZone.class;
        }
        if ("Timer".equals(simpleName)) {
            return java.util.Timer.class;
        }
        if ("TimerTask".equals(simpleName)) {
            return java.util.TimerTask.class;
        }
        if ("TreeMap".equals(simpleName)) {
            return java.util.TreeMap.class;
        }
        if ("TreeSet".equals(simpleName)) {
            return java.util.TreeSet.class;
        }
        if ("Vector".equals(simpleName)) {
            return java.util.Vector.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == java.util.AbstractMap.SimpleEntry.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.Entry.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.Entry.class}, false);
                return new java.util.AbstractMap.SimpleEntry((java.util.Map.Entry) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return new java.util.AbstractMap.SimpleEntry((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if (type == java.util.AbstractMap.SimpleImmutableEntry.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.Entry.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.Entry.class}, false);
                return new java.util.AbstractMap.SimpleImmutableEntry((java.util.Map.Entry) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return new java.util.AbstractMap.SimpleImmutableEntry((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if (type == java.util.ArrayDeque.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.ArrayDeque();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.ArrayDeque(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.ArrayDeque((java.util.Collection) adaptedArgs[0]);
            }
        }
        if (type == java.util.ArrayList.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.ArrayList();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.ArrayList(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.ArrayList((java.util.Collection) adaptedArgs[0]);
            }
        }
        if (type == java.util.BitSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.BitSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.BitSet(toIntValue(adaptedArgs[0]));
            }
        }
        if (type == java.util.ConcurrentModificationException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.ConcurrentModificationException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.util.ConcurrentModificationException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.util.Date.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Date();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return new java.util.Date(((Number) adaptedArgs[0]).longValue());
            }
        }
        if (type == java.util.EmptyStackException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.EmptyStackException();
            }
        }
        if (type == java.util.HashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.HashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.HashMap(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new java.util.HashMap((java.util.Map) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return new java.util.HashMap(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == java.util.HashSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.HashSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.HashSet(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.HashSet((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return new java.util.HashSet(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == java.util.Hashtable.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Hashtable();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.Hashtable(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new java.util.Hashtable((java.util.Map) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return new java.util.Hashtable(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == java.util.IdentityHashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.IdentityHashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.IdentityHashMap(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new java.util.IdentityHashMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if (type == java.util.LinkedHashMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.LinkedHashMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.LinkedHashMap(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new java.util.LinkedHashMap((java.util.Map) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return new java.util.LinkedHashMap(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class, java.lang.Boolean.class}, false);
                return new java.util.LinkedHashMap(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue(), ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if (type == java.util.LinkedHashSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.LinkedHashSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.LinkedHashSet(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.LinkedHashSet((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                return new java.util.LinkedHashSet(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == java.util.LinkedList.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.LinkedList();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.LinkedList((java.util.Collection) adaptedArgs[0]);
            }
        }
        if (type == java.util.Locale.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new java.util.Locale((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == java.util.NoSuchElementException.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.NoSuchElementException();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.util.NoSuchElementException((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == java.util.Observable.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Observable();
            }
        }
        if (type == java.util.PriorityQueue.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.PriorityQueue();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.PriorityQueue(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.PriorityQueue.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.PriorityQueue.class}, false);
                return new java.util.PriorityQueue((java.util.PriorityQueue) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false);
                return new java.util.PriorityQueue((java.util.SortedSet) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.PriorityQueue((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Comparator.class}, false);
                return new java.util.PriorityQueue(toIntValue(adaptedArgs[0]), (java.util.Comparator) adaptedArgs[1]);
            }
        }
        if (type == java.util.Random.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Random();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return new java.util.Random(((Number) adaptedArgs[0]).longValue());
            }
        }
        if (type == java.util.Stack.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Stack();
            }
        }
        if (type == java.util.StringTokenizer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new java.util.StringTokenizer((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new java.util.StringTokenizer((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false);
                return new java.util.StringTokenizer((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue());
            }
        }
        if (type == java.util.Timer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Timer();
            }
        }
        if (type == java.util.TreeMap.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.TreeMap();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Comparator.class}, false);
                return new java.util.TreeMap((java.util.Comparator) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedMap.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.SortedMap.class}, false);
                return new java.util.TreeMap((java.util.SortedMap) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return new java.util.TreeMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if (type == java.util.TreeSet.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.TreeSet();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.TreeSet((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Comparator.class}, false);
                return new java.util.TreeSet((java.util.Comparator) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.SortedSet.class}, false);
                return new java.util.TreeSet((java.util.SortedSet) adaptedArgs[0]);
            }
        }
        if (type == java.util.Vector.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new java.util.Vector();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new java.util.Vector(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return new java.util.Vector((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new java.util.Vector(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
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
        if ("asList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) adaptedArgs[i];
                }
                return java.util.Arrays.asList(varArgs);
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false);
                return java.util.Arrays.binarySearch((byte[]) adaptedArgs[0], (byte) toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false);
                return java.util.Arrays.binarySearch((char[]) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false);
                return java.util.Arrays.binarySearch((double[]) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false);
                return java.util.Arrays.binarySearch((float[]) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.binarySearch((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false);
                return java.util.Arrays.binarySearch((java.lang.Object[]) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false);
                return java.util.Arrays.binarySearch((long[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false);
                return java.util.Arrays.binarySearch((short[]) adaptedArgs[0], (short) toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class, java.util.Comparator.class}, false);
                return java.util.Arrays.binarySearch((java.lang.Object[]) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.util.Comparator) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false);
                return java.util.Arrays.binarySearch((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (byte) toIntValue(adaptedArgs[3]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false);
                return java.util.Arrays.binarySearch((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Character) adaptedArgs[3]).charValue());
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false);
                return java.util.Arrays.binarySearch((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return java.util.Arrays.binarySearch((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.binarySearch((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false);
                return java.util.Arrays.binarySearch((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.lang.Object) adaptedArgs[3]);
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false);
                return java.util.Arrays.binarySearch((long[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue());
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false);
                return java.util.Arrays.binarySearch((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (short) toIntValue(adaptedArgs[3]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.util.Comparator.class}, false);
                return java.util.Arrays.binarySearch((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.lang.Object) adaptedArgs[3], (java.util.Comparator) adaptedArgs[4]);
            }
        }
        if ("copyOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((boolean[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((long[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOf((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Class.class}, false);
                return java.util.Arrays.copyOf((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.Class) adaptedArgs[2]);
            }
        }
        if ("copyOfRange".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((boolean[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((long[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return java.util.Arrays.copyOfRange((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Class.class}, false);
                return java.util.Arrays.copyOfRange((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.lang.Class) adaptedArgs[3]);
            }
        }
        if ("deepEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false);
                return java.util.Arrays.deepEquals((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1]);
            }
        }
        if ("deepHashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return java.util.Arrays.deepHashCode((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("deepToString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return java.util.Arrays.deepToString((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, boolean[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class, boolean[].class}, false);
                return java.util.Arrays.equals((boolean[]) adaptedArgs[0], (boolean[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, byte[].class}, false);
                return java.util.Arrays.equals((byte[]) adaptedArgs[0], (byte[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, char[].class}, false);
                return java.util.Arrays.equals((char[]) adaptedArgs[0], (char[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, double[].class}, false);
                return java.util.Arrays.equals((double[]) adaptedArgs[0], (double[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, float[].class}, false);
                return java.util.Arrays.equals((float[]) adaptedArgs[0], (float[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                return java.util.Arrays.equals((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object[].class}, false);
                return java.util.Arrays.equals((java.lang.Object[]) adaptedArgs[0], (java.lang.Object[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, long[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, long[].class}, false);
                return java.util.Arrays.equals((long[]) adaptedArgs[0], (long[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, short[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, short[].class}, false);
                return java.util.Arrays.equals((short[]) adaptedArgs[0], (short[]) adaptedArgs[1]);
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class, java.lang.Boolean.class}, false);
                java.util.Arrays.fill((boolean[]) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Byte.class}, false);
                java.util.Arrays.fill((byte[]) adaptedArgs[0], (byte) toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Character.class}, false);
                java.util.Arrays.fill((char[]) adaptedArgs[0], ((Character) adaptedArgs[1]).charValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Double.class}, false);
                java.util.Arrays.fill((double[]) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Float.class}, false);
                java.util.Arrays.fill((float[]) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class}, false);
                java.util.Arrays.fill((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Object.class}, false);
                java.util.Arrays.fill((java.lang.Object[]) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Long.class}, false);
                java.util.Arrays.fill((long[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Short.class}, false);
                java.util.Arrays.fill((short[]) adaptedArgs[0], (short) toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                java.util.Arrays.fill((boolean[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Byte.class}, false);
                java.util.Arrays.fill((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (byte) toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Character.class}, false);
                java.util.Arrays.fill((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Character) adaptedArgs[3]).charValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class}, false);
                java.util.Arrays.fill((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).doubleValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                java.util.Arrays.fill((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).floatValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.fill((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class}, false);
                java.util.Arrays.fill((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.lang.Object) adaptedArgs[3]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false);
                java.util.Arrays.fill((long[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Short.class}, false);
                java.util.Arrays.fill((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (short) toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class}, false);
                return java.util.Arrays.hashCode((boolean[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return java.util.Arrays.hashCode((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return java.util.Arrays.hashCode((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return java.util.Arrays.hashCode((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                return java.util.Arrays.hashCode((float[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                return java.util.Arrays.hashCode((int[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return java.util.Arrays.hashCode((java.lang.Object[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class}, false);
                return java.util.Arrays.hashCode((long[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class}, false);
                return java.util.Arrays.hashCode((short[]) adaptedArgs[0]);
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                java.util.Arrays.sort((byte[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                java.util.Arrays.sort((char[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                java.util.Arrays.sort((double[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                java.util.Arrays.sort((float[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                java.util.Arrays.sort((int[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                java.util.Arrays.sort((java.lang.Object[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class}, false);
                java.util.Arrays.sort((long[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class}, false);
                java.util.Arrays.sort((short[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.util.Comparator.class}, false);
                java.util.Arrays.sort((java.lang.Object[]) adaptedArgs[0], (java.util.Comparator) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((char[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((double[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((long[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Arrays.sort((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class, java.lang.Integer.class, java.lang.Integer.class, java.util.Comparator.class}, false);
                java.util.Arrays.sort((java.lang.Object[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (java.util.Comparator) adaptedArgs[3]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{boolean[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{boolean[].class}, false);
                return java.util.Arrays.toString((boolean[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return java.util.Arrays.toString((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{char[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{char[].class}, false);
                return java.util.Arrays.toString((char[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{double[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{double[].class}, false);
                return java.util.Arrays.toString((double[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                return java.util.Arrays.toString((float[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, false);
                return java.util.Arrays.toString((int[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return java.util.Arrays.toString((java.lang.Object[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class}, false);
                return java.util.Arrays.toString((long[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{short[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class}, false);
                return java.util.Arrays.toString((short[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Arrays.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Calendar.getInstance();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false);
                return java.util.Calendar.getInstance((java.util.TimeZone) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Calendar.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.Object) adaptedArgs[i];
                }
                return java.util.Collections.addAll((java.util.Collection) adaptedArgs[0], varArgs);
            }
        }
        if ("binarySearch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class}, false);
                return java.util.Collections.binarySearch((java.util.List) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class, java.util.Comparator.class}, false);
                return java.util.Collections.binarySearch((java.util.List) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.util.Comparator) adaptedArgs[2]);
            }
        }
        if ("checkedCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Class.class}, false);
                return java.util.Collections.checkedCollection((java.util.Collection) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
        }
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false);
                java.util.Collections.copy((java.util.List) adaptedArgs[0], (java.util.List) adaptedArgs[1]); return null;
            }
        }
        if ("disjoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Collection.class}, false);
                return java.util.Collections.disjoint((java.util.Collection) adaptedArgs[0], (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("emptyList".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Collections.emptyList();
            }
        }
        if ("emptyMap".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Collections.emptyMap();
            }
        }
        if ("emptySet".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Collections.emptySet();
            }
        }
        if ("enumeration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return java.util.Collections.enumeration((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("fill".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class}, false);
                java.util.Collections.fill((java.util.List) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("frequency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.lang.Object.class}, false);
                return java.util.Collections.frequency((java.util.Collection) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("indexOfSubList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false);
                return java.util.Collections.indexOfSubList((java.util.List) adaptedArgs[0], (java.util.List) adaptedArgs[1]);
            }
        }
        if ("lastIndexOfSubList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.List.class}, false);
                return java.util.Collections.lastIndexOfSubList((java.util.List) adaptedArgs[0], (java.util.List) adaptedArgs[1]);
            }
        }
        if ("list".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Enumeration.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Enumeration.class}, false);
                return java.util.Collections.list((java.util.Enumeration) adaptedArgs[0]);
            }
        }
        if ("max".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return java.util.Collections.max((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Comparator.class}, false);
                return java.util.Collections.max((java.util.Collection) adaptedArgs[0], (java.util.Comparator) adaptedArgs[1]);
            }
        }
        if ("min".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return java.util.Collections.min((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class, java.util.Comparator.class}, false);
                return java.util.Collections.min((java.util.Collection) adaptedArgs[0], (java.util.Comparator) adaptedArgs[1]);
            }
        }
        if ("nCopies".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return java.util.Collections.nCopies(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("newSetFromMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return java.util.Collections.newSetFromMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("replaceAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Object.class, java.lang.Object.class}, false);
                return java.util.Collections.replaceAll((java.util.List) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.lang.Object) adaptedArgs[2]);
            }
        }
        if ("reverse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                java.util.Collections.reverse((java.util.List) adaptedArgs[0]); return null;
            }
        }
        if ("reverseOrder".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Collections.reverseOrder();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Comparator.class}, false);
                return java.util.Collections.reverseOrder((java.util.Comparator) adaptedArgs[0]);
            }
        }
        if ("rotate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class}, false);
                java.util.Collections.rotate((java.util.List) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("shuffle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                java.util.Collections.shuffle((java.util.List) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Random.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.Random.class}, false);
                java.util.Collections.shuffle((java.util.List) adaptedArgs[0], (java.util.Random) adaptedArgs[1]); return null;
            }
        }
        if ("singleton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Collections.singleton((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("singletonList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Collections.singletonList((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("singletonMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return java.util.Collections.singletonMap((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("sort".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                java.util.Collections.sort((java.util.List) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.util.Comparator.class}, false);
                java.util.Collections.sort((java.util.List) adaptedArgs[0], (java.util.Comparator) adaptedArgs[1]); return null;
            }
        }
        if ("swap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                java.util.Collections.swap((java.util.List) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("synchronizedCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return java.util.Collections.synchronizedCollection((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("synchronizedList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return java.util.Collections.synchronizedList((java.util.List) adaptedArgs[0]);
            }
        }
        if ("synchronizedMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return java.util.Collections.synchronizedMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("synchronizedSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Set.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Set.class}, false);
                return java.util.Collections.synchronizedSet((java.util.Set) adaptedArgs[0]);
            }
        }
        if ("unmodifiableCollection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return java.util.Collections.unmodifiableCollection((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("unmodifiableList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return java.util.Collections.unmodifiableList((java.util.List) adaptedArgs[0]);
            }
        }
        if ("unmodifiableMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return java.util.Collections.unmodifiableMap((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("unmodifiableSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Set.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Set.class}, false);
                return java.util.Collections.unmodifiableSet((java.util.Set) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(java.util.Collections.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("getDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.Locale.getDefault();
            }
        }
        if ("setDefault".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Locale.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Locale.class}, false);
                java.util.Locale.setDefault((java.util.Locale) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(java.util.Locale.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.util.Comparator.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class, java.util.Comparator.class}, false);
                return java.util.Objects.compare((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1], (java.util.Comparator) adaptedArgs[2]);
            }
        }
        if ("deepEquals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return java.util.Objects.deepEquals((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return java.util.Objects.equals((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("hash".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, true);
                java.lang.Object[] varArgs = new java.lang.Object[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.Object) adaptedArgs[i];
                }
                return java.util.Objects.hash(varArgs);
            }
        }
        if ("hashCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Objects.hashCode((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("nonNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Objects.nonNull((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("requireNonNull".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Objects.requireNonNull((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return java.util.Objects.requireNonNull((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return java.util.Objects.toString((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return java.util.Objects.toString((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(java.util.Objects.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("getAvailableIDs".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.TimeZone.getAvailableIDs();
            }
        }
        if ("getDefault".equals(name)) {
            if (safeArgs.length == 0) {
                return java.util.TimeZone.getDefault();
            }
        }
        if ("getTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return java.util.TimeZone.getTimeZone((java.lang.String) adaptedArgs[0]);
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
        if (target instanceof java.util.AbstractMap.SimpleEntry) {
            try {
                return invoke20((java.util.AbstractMap.SimpleEntry) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.AbstractMap.SimpleImmutableEntry) {
            try {
                return invoke21((java.util.AbstractMap.SimpleImmutableEntry) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.BitSet) {
            try {
                return invoke22((java.util.BitSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Calendar) {
            try {
                return invoke23((java.util.Calendar) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Date) {
            try {
                return invoke24((java.util.Date) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Dictionary) {
            try {
                return invoke25((java.util.Dictionary) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.EmptyStackException) {
            try {
                return invoke26((java.util.EmptyStackException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Locale) {
            try {
                return invoke27((java.util.Locale) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NoSuchElementException) {
            try {
                return invoke28((java.util.NoSuchElementException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Observable) {
            try {
                return invoke29((java.util.Observable) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Random) {
            try {
                return invoke30((java.util.Random) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.StringTokenizer) {
            try {
                return invoke31((java.util.StringTokenizer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TimeZone) {
            try {
                return invoke32((java.util.TimeZone) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Timer) {
            try {
                return invoke33((java.util.Timer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.TimerTask) {
            try {
                return invoke34((java.util.TimerTask) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Collection) {
            try {
                return invoke35((java.util.Collection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Comparator) {
            try {
                return invoke36((java.util.Comparator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Deque) {
            try {
                return invoke37((java.util.Deque) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Enumeration) {
            try {
                return invoke38((java.util.Enumeration) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Iterator) {
            try {
                return invoke39((java.util.Iterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.List) {
            try {
                return invoke40((java.util.List) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.ListIterator) {
            try {
                return invoke41((java.util.ListIterator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Map) {
            try {
                return invoke42((java.util.Map) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Map.Entry) {
            try {
                return invoke43((java.util.Map.Entry) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NavigableMap) {
            try {
                return invoke44((java.util.NavigableMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.NavigableSet) {
            try {
                return invoke45((java.util.NavigableSet) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Observer) {
            try {
                return invoke46((java.util.Observer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Queue) {
            try {
                return invoke47((java.util.Queue) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.Set) {
            try {
                return invoke48((java.util.Set) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.SortedMap) {
            try {
                return invoke49((java.util.SortedMap) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof java.util.SortedSet) {
            try {
                return invoke50((java.util.SortedSet) target, name, safeArgs);
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(java.util.LinkedList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("addFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addFirst((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("addLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addLast((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingIterator();
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirst();
            }
        }
        if ("getLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLast();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("offer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerFirst((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerLast((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("peekFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekFirst();
            }
        }
        if ("peekLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekLast();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("pollFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirst();
            }
        }
        if ("pollLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLast();
            }
        }
        if ("pop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pop();
            }
        }
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.push((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeFirst();
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeFirstOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeLast();
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeLastOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(java.util.Stack typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("capacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.capacity();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("copyInto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                typedTarget.copyInto((java.lang.Object[]) adaptedArgs[0]); return null;
            }
        }
        if ("elementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.elementAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("empty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.empty();
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.ensureCapacity(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("firstElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstElement();
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastElement();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("pop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pop();
            }
        }
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.push((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeAllElements".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllElements(); return null;
            }
        }
        if ("removeElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeElement((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeElementAt(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("search".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.search((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(java.util.AbstractSequentialList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(java.util.ArrayList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.ensureCapacity(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(java.util.HashSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(java.util.LinkedHashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(java.util.PriorityQueue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("offer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(java.util.TreeSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("ceiling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceiling((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingIterator();
            }
        }
        if ("descendingSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingSet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("first".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.first();
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floor((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headSet((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.headSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("higher".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higher((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("last".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.last();
            }
        }
        if ("lower".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lower((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("pollFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirst();
            }
        }
        if ("pollLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLast();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subSet((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.subSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Object) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("tailSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailSet((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.tailSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(java.util.Vector typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("addElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addElement((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("capacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.capacity();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("copyInto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                typedTarget.copyInto((java.lang.Object[]) adaptedArgs[0]); return null;
            }
        }
        if ("elementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.elementAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("ensureCapacity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.ensureCapacity(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("firstElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstElement();
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("insertElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.insertElementAt((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastElement();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeAllElements".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAllElements(); return null;
            }
        }
        if ("removeElement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeElement((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeElementAt(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setElementAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Integer.class}, false);
                typedTarget.setElementAt((java.lang.Object) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSize(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("trimToSize".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.trimToSize(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(java.util.AbstractList typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(java.util.AbstractQueue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(java.util.AbstractSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(java.util.ArrayDeque typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("addFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addFirst((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("addLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addLast((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingIterator();
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirst();
            }
        }
        if ("getLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLast();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("offer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerFirst((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerLast((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("peekFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekFirst();
            }
        }
        if ("peekLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekLast();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("pollFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirst();
            }
        }
        if ("pollLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLast();
            }
        }
        if ("pop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pop();
            }
        }
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.push((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeFirst();
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeFirstOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeLast();
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeLastOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(java.util.HashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(java.util.Hashtable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("keys".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keys();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(java.util.IdentityHashMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(java.util.TreeMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("ceilingEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceilingEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("ceilingKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceilingKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("descendingKeySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingKeySet();
            }
        }
        if ("descendingMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingMap();
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("firstEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstEntry();
            }
        }
        if ("firstKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstKey();
            }
        }
        if ("floorEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floorEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floorKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floorKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headMap((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.headMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("higherEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higherEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("higherKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higherKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("lastEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastEntry();
            }
        }
        if ("lastKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastKey();
            }
        }
        if ("lowerEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lowerEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("lowerKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lowerKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("navigableKeySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.navigableKeySet();
            }
        }
        if ("pollFirstEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirstEntry();
            }
        }
        if ("pollLastEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLastEntry();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subMap((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.subMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Object) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("tailMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailMap((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.tailMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(java.util.AbstractCollection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(java.util.AbstractMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(java.util.AbstractMap.SimpleEntry typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKey();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.setValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(java.util.AbstractMap.SimpleImmutableEntry typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKey();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.setValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(java.util.BitSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("and".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.BitSet.class}, false);
                typedTarget.and((java.util.BitSet) adaptedArgs[0]); return null;
            }
        }
        if ("andNot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.BitSet.class}, false);
                typedTarget.andNot((java.util.BitSet) adaptedArgs[0]); return null;
            }
        }
        if ("cardinality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.cardinality();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.clear(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.clear(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("flip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.flip(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.flip(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("intersects".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.BitSet.class}, false);
                return typedTarget.intersects((java.util.BitSet) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("length".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.length();
            }
        }
        if ("nextClearBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.nextClearBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("nextSetBit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.nextSetBit(toIntValue(adaptedArgs[0]));
            }
        }
        if ("or".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.BitSet.class}, false);
                typedTarget.or((java.util.BitSet) adaptedArgs[0]); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.set(toIntValue(adaptedArgs[0])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.set(toIntValue(adaptedArgs[0]), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.set(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.set(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("xor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.BitSet.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.BitSet.class}, false);
                typedTarget.xor((java.util.BitSet) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(java.util.Calendar typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("after".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.after((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("before".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.before((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getTimeZone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeZone();
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.set(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setTime((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setTimeZone".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimeZone.class}, false);
                typedTarget.setTimeZone((java.util.TimeZone) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(java.util.Date typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compareTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                return typedTarget.compareTo((java.util.Date) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setTime(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(java.util.Dictionary typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("elements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.elements();
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keys".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keys();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(java.util.EmptyStackException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.addSuppressed((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.initCause((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false);
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(java.util.Locale typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCountry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCountry();
            }
        }
        if ("getLanguage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke28(java.util.NoSuchElementException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.addSuppressed((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuppressed();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.initCause((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false);
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke29(java.util.Observable typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.addObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("countObservers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countObservers();
            }
        }
        if ("deleteObserver".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observer.class}, false);
                typedTarget.deleteObserver((java.util.Observer) adaptedArgs[0]); return null;
            }
        }
        if ("deleteObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.deleteObservers(); return null;
            }
        }
        if ("hasChanged".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasChanged();
            }
        }
        if ("notifyObservers".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.notifyObservers(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.notifyObservers((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke30(java.util.Random typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("nextBoolean".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextBoolean();
            }
        }
        if ("nextBytes".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.nextBytes((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("nextDouble".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextDouble();
            }
        }
        if ("nextFloat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextFloat();
            }
        }
        if ("nextInt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextInt();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.nextInt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("nextLong".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextLong();
            }
        }
        if ("setSeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setSeed(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke31(java.util.StringTokenizer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("countTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.countTokens();
            }
        }
        if ("hasMoreElements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasMoreElements();
            }
        }
        if ("hasMoreTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasMoreTokens();
            }
        }
        if ("nextElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextElement();
            }
        }
        if ("nextToken".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextToken();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.nextToken((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke32(java.util.TimeZone typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getID();
            }
        }
        if ("getOffset".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getOffset(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]));
            }
        }
        if ("getRawOffset".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawOffset();
            }
        }
        if ("useDaylightTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.useDaylightTime();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke33(java.util.Timer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cancel(); return null;
            }
        }
        if ("schedule".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class}, false);
                typedTarget.schedule((java.util.TimerTask) adaptedArgs[0], (java.util.Date) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class}, false);
                typedTarget.schedule((java.util.TimerTask) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false);
                typedTarget.schedule((java.util.TimerTask) adaptedArgs[0], (java.util.Date) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false);
                typedTarget.schedule((java.util.TimerTask) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue()); return null;
            }
        }
        if ("scheduleAtFixedRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.util.Date.class, java.lang.Long.class}, false);
                typedTarget.scheduleAtFixedRate((java.util.TimerTask) adaptedArgs[0], (java.util.Date) adaptedArgs[1], ((Number) adaptedArgs[2]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.TimerTask.class, java.lang.Long.class, java.lang.Long.class}, false);
                typedTarget.scheduleAtFixedRate((java.util.TimerTask) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke34(java.util.TimerTask typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cancel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.cancel();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke35(java.util.Collection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke36(java.util.Comparator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("compare".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.compare((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke37(java.util.Deque typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("addFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addFirst((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("addLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addLast((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingIterator();
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirst();
            }
        }
        if ("getLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLast();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("offer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerFirst".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerFirst((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("offerLast".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offerLast((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("peekFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekFirst();
            }
        }
        if ("peekLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peekLast();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("pollFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirst();
            }
        }
        if ("pollLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLast();
            }
        }
        if ("pop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pop();
            }
        }
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.push((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("removeFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeFirst();
            }
        }
        if ("removeFirstOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeFirstOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.removeLast();
            }
        }
        if ("removeLastOccurrence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.removeLastOccurrence((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke38(java.util.Enumeration typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("hasMoreElements".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasMoreElements();
            }
        }
        if ("nextElement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextElement();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke39(java.util.Iterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("hasNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasNext();
            }
        }
        if ("next".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.next();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke40(java.util.List typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                typedTarget.add(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.util.Collection.class}, false);
                return typedTarget.addAll(toIntValue(adaptedArgs[0]), (java.util.Collection) adaptedArgs[1]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.get(toIntValue(adaptedArgs[0]));
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("indexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.indexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("lastIndexOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lastIndexOf((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("listIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.listIterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.listIterator(toIntValue(adaptedArgs[0]));
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.remove(toIntValue(adaptedArgs[0]));
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class}, false);
                return typedTarget.set(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.subList(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke41(java.util.ListIterator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.add((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("hasNext".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasNext();
            }
        }
        if ("hasPrevious".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasPrevious();
            }
        }
        if ("next".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.next();
            }
        }
        if ("nextIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.nextIndex();
            }
        }
        if ("previous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.previous();
            }
        }
        if ("previousIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.previousIndex();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("set".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.set((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke42(java.util.Map typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.put((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke43(java.util.Map.Entry typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKey();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.setValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke44(java.util.NavigableMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("ceilingEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceilingEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("ceilingKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceilingKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("descendingKeySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingKeySet();
            }
        }
        if ("descendingMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingMap();
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("firstEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstEntry();
            }
        }
        if ("firstKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstKey();
            }
        }
        if ("floorEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floorEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("floorKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floorKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headMap((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.headMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("higherEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higherEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("higherKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higherKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("lastEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastEntry();
            }
        }
        if ("lastKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastKey();
            }
        }
        if ("lowerEntry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lowerEntry((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("lowerKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lowerKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("navigableKeySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.navigableKeySet();
            }
        }
        if ("pollFirstEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirstEntry();
            }
        }
        if ("pollLastEntry".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLastEntry();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subMap((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.subMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Object) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("tailMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailMap((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.tailMap((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke45(java.util.NavigableSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("ceiling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.ceiling((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("descendingIterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingIterator();
            }
        }
        if ("descendingSet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.descendingSet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("first".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.first();
            }
        }
        if ("floor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.floor((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headSet((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.headSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("higher".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.higher((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("last".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.last();
            }
        }
        if ("lower".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.lower((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("pollFirst".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollFirst();
            }
        }
        if ("pollLast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pollLast();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subSet((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.subSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Object) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue());
            }
        }
        if ("tailSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailSet((java.lang.Object) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Boolean.class}, false);
                return typedTarget.tailSet((java.lang.Object) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke46(java.util.Observer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Observable.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Observable.class, java.lang.Object.class}, false);
                typedTarget.update((java.util.Observable) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke47(java.util.Queue typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("element".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.element();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("offer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.offer((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("peek".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.peek();
            }
        }
        if ("poll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.poll();
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.remove();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke48(java.util.Set typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke49(java.util.SortedMap typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("containsKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsKey((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.containsValue((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("entrySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.entrySet();
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("firstKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.firstKey();
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.get((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headMap((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("keySet".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.keySet();
            }
        }
        if ("lastKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.lastKey();
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                typedTarget.putAll((java.util.Map) adaptedArgs[0]); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subMap((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("tailMap".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailMap((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("values".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.values();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke50(java.util.SortedSet typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.addAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("comparator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.comparator();
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.contains((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("containsAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.containsAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("first".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.first();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("headSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.headSet((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("last".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.last();
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.remove((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.removeAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("retainAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                return typedTarget.retainAll((java.util.Collection) adaptedArgs[0]);
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("subSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Object.class}, false);
                return typedTarget.subSet((java.lang.Object) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("tailSet".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.tailSet((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("toArray".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toArray();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object[].class}, false);
                return typedTarget.toArray((java.lang.Object[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == java.util.Calendar.class) return getStaticField0(name);
        if (type == java.util.Collections.class) return getStaticField1(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
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
        throw unsupportedStaticField(java.util.Calendar.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("EMPTY_LIST".equals(name)) return java.util.Collections.EMPTY_LIST;
        if ("EMPTY_MAP".equals(name)) return java.util.Collections.EMPTY_MAP;
        if ("EMPTY_SET".equals(name)) return java.util.Collections.EMPTY_SET;
        throw unsupportedStaticField(java.util.Collections.class, name);
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

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
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
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
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
