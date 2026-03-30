package bsh;

import bsh.cn1.CN1AccessRegistry;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

/**
 * Minimal class manager for the reduced CN1 BeanShell runtime.
 */
public class BshClassManager {
    static final Map<Class<?>, MemberCache> memberCache = new Hashtable<Class<?>, MemberCache>();

    static final class MemberCache {
        public Invocable findMethod(String name, Object... args) { return null; }
        public Invocable findMethod(String name, Class<?>... types) { return null; }
        public Invocable findStaticMethod(String name) { return null; }
        public Invocable findGetter(String propName) { return null; }
        public Invocable findSetter(String propName) { return null; }
        public int findMemberIndex(String name, Class<?>[] types) { return -1; }
        public int memberCount(String name) { return 0; }
        public java.util.List<Invocable> members(String name) { return java.util.Collections.emptyList(); }
        public boolean hasMember(String name) { return false; }
        public boolean hasField(String name) { return false; }
        public Invocable findField(String name) { return null; }
    }

    private Interpreter declaringInterpreter;
    protected final transient Map<String, Class<?>> absoluteClassCache = new Hashtable<String, Class<?>>();
    protected final transient Map<String, Class<?>> associatedClasses = new Hashtable<String, Class<?>>();
    protected ClassLoader externalClassLoader;

    public static BshClassManager createClassManager(Interpreter interpreter) {
        BshClassManager manager = new BshClassManager();
        manager.declaringInterpreter = interpreter;
        return manager;
    }

    public boolean getStrictJava() {
        return declaringInterpreter != null && declaringInterpreter.getStrictJava();
    }

    public boolean classExists(String name) {
        return classForName(name) != null;
    }

    public Class<?> classForName(String name) {
        if (absoluteClassCache.containsKey(name)) {
            return absoluteClassCache.get(name);
        }
        Class<?> found = CN1AccessRegistry.getInstance().findClass(name);
        if (found == null) {
            try {
                found = plainClassForName(name);
            } catch (ClassNotFoundException ex) {
                found = null;
            }
        }
        if (found != null) {
            cacheClassInfo(name, found);
        }
        return found;
    }

    protected Class<?> loadSourceClass(String name) {
        return null;
    }

    public Class<?> plainClassForName(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public Object getResource(String path) {
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        return null;
    }

    public void cacheClassInfo(String name, Class<?> value) {
        if (value != null) {
            absoluteClassCache.put(name, value);
        }
    }

    public void associateClass(Class<?> clas) {
        if (clas != null) {
            associatedClasses.put(clas.getName(), clas);
        }
    }

    public Class<?> getAssociatedClass(String name) {
        return associatedClasses.get(name);
    }

    protected void clearCaches() {
        absoluteClassCache.clear();
        associatedClasses.clear();
        memberCache.clear();
    }

    public void setClassLoader(ClassLoader externalCL) {
        externalClassLoader = externalCL;
        classLoaderChanged();
    }

    public void addClassPath(Object path) {}
    public void reset() { clearCaches(); }
    public void setClassPath(Object[] cp) throws UtilEvalError { throw cmUnavailable(); }
    public void reloadAllClasses() throws UtilEvalError { throw cmUnavailable(); }
    public void reloadClasses(String[] classNames) throws UtilEvalError { throw cmUnavailable(); }
    public void reloadPackage(String pack) throws UtilEvalError { throw cmUnavailable(); }
    protected void doSuperImport() throws UtilEvalError { throw cmUnavailable(); }
    protected boolean hasSuperImport() { return false; }
    protected String getClassNameByUnqName(String name) throws UtilEvalError { throw cmUnavailable(); }
    public void addListener(Listener l) {}
    public void removeListener(Listener l) {}
    public void dump(PrintWriter pw) { pw.println("BshClassManager: reduced CN1 runtime."); }
    public Class<?> defineClass(String name, byte[] code) { throw new InterpreterError("Class generation is disabled."); }
    protected void classLoaderChanged() {}

    protected static UtilEvalError cmUnavailable() {
        return new Capabilities.Unavailable("ClassLoading features unavailable.");
    }

    public static interface Listener {
        void classLoaderChanged();
    }
}
