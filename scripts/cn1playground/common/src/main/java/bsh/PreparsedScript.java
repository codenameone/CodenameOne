package bsh;

import java.io.PrintStream;
import java.io.StringReader;
import java.util.Map;

/** With this class the script source is only parsed once and the resulting
 * AST is used for {@link #invoke(java.util.Map) every invocation}. This class
 * is designed to be thread-safe. */
public class PreparsedScript {

    /** Prepared script reference. */
    private final BshMethod prepared;
    /** Local interpreter reference. */
    private final Interpreter interpreter;

    /** Constructor without a supplied class loader.
     * Delegate to overloaded constructor while attempting to locate a
     * default class loader.
     * @param source to be prepared.
     * @throws EvalError thrown on script error.*/
    public PreparsedScript(final String source) throws EvalError {
        this(source, getDefaultClassLoader());
    }

    /** Instance of a prepared script for the supplied source and class loader.
     * @param source to be prepared.
     * @param classLoader
     * @throws EvalError thrown on script error.*/
    public PreparsedScript(final String source, final ClassLoader classLoader) throws EvalError {
        interpreter = new Interpreter();
        interpreter.setClassLoader(classLoader);
        try {
            final This callable = (This) interpreter.eval(new StringReader(
                    "__execute() {"
                    + interpreter.terminatedScript(source)
                    + "} return this;"),
                interpreter.globalNameSpace,
                interpreter.showEvalString("pre-parsed script", source));
            prepared = callable.getNameSpace()
                        .getMethod("__execute", Reflect.ZERO_TYPES, false);
        } catch (final UtilEvalError e) {
            throw new IllegalStateException(e);
        }
    }

    /** Find the default system class loader.
     * @return a class loader or null. */
    private static ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = null;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (final SecurityException e) { }
        // fall through
        if ( null == classLoader )
            classLoader = PreparsedScript.class.getClassLoader();
        if ( null == classLoader )
            return ClassLoader.getSystemClassLoader();
        return classLoader;
    }

    /** Invoke the prepared script with the supplied arguments exposed.
     * @param context map of arguments
     * @return of the script execution
     * @throws EvalError thrown on script error.*/
    public Object invoke(final Map<String,?> context) throws EvalError {
        final NameSpace scope = new NameSpace(interpreter.globalNameSpace,
                interpreter.getClassManager(), "BeanshellExecutable");
        scope.isMethod = true;
        final Interpreter local = new Interpreter(scope, interpreter);

        for ( final Map.Entry<String,?> entry : context.entrySet() )
            local.set(entry.getKey(), entry.getValue());

        return Primitive.unwrap(prepared.invoke(Reflect.ZERO_ARGS,
                local, new CallStack(scope), Node.JAVACODE, true));
    }

    /** Attach a standard output stream.
     * @param out the stream. */
    public void setOut(final PrintStream out) {
        interpreter.setOut(out);
    }


    /** Attach a standard error stream.
     * @param err the stream. */
    public void setErr(final PrintStream err) {
        interpreter.setErr(err);
    }

}
