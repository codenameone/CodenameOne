package bsh.engine;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import bsh.EvalError;
import bsh.ExternalNameSpace;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.ParseException;
import bsh.PreparsedScript;
import bsh.TargetError;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/*
    Adopted from http://ikayzo.org/svn/beanshell/BeanShell/engine/src/bsh/engine/BshScriptEngine.java
    Notes
    This engine supports open-ended pluggable scriptcontexts
*/

public class BshScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {
    // The BeanShell global namespace for the interpreter is stored in the
    // engine scope map under this key.
    static final String engineNameSpaceKey = "org_beanshell_engine_namespace";

    private BshScriptEngineFactory factory;
    private Interpreter interpreter;


    public BshScriptEngine() {
        this(null);
    }


    public BshScriptEngine(BshScriptEngineFactory factory) {
        this.factory = factory;
        getInterpreter(); // go ahead and prime the interpreter now
    }


    protected Interpreter getInterpreter() {
        if (interpreter == null) {
            this.interpreter = new Interpreter();
            interpreter.setNameSpace(null); // should always be set by context
        }

        return interpreter;
    }


    @Override
    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        return evalSource(script, scriptContext);
    }


    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        return evalSource(reader, scriptContext);
    }

    /*
        This is the primary implementation method.
        We respect the String/Reader difference here in BeanShell because
        BeanShell will do a few extra things in the string case... e.g.
        tack on a trailing ";" semicolon if necessary.
    */


    private Object evalSource(Object source, ScriptContext scriptContext) throws ScriptException {
        try {
            Interpreter bsh = getInterpreter();
            NameSpace contextNameSpace = getEngineNameSpace(scriptContext);
            bsh.setNameSpace(contextNameSpace);

            bsh.setOut(toPrintStream(scriptContext.getWriter()));
            bsh.setErr(toPrintStream(scriptContext.getErrorWriter()));


            if (source instanceof Reader) {
                return bsh.eval((Reader) source);
            } else {
                return bsh.eval((String) source);
            }
        } catch (ParseException e) {
            // explicit parsing error
            throw new ScriptException(e.toString(), e.getErrorSourceFile(), e.getErrorLineNumber());
        } catch (TargetError e) {
            // The script threw an application level exception
            ScriptException se = new ScriptException(e.toString(), e.getErrorSourceFile(), e.getErrorLineNumber());
            se.initCause(e.getTarget());
            throw se;
        } catch (EvalError e) {
            // The script couldn't be evaluated properly
            throw new ScriptException(e.toString(), e.getErrorSourceFile(), e.getErrorLineNumber());
        } catch (IOException e) {
            throw new ScriptException(e.toString());
        }
    }


    private PrintStream toPrintStream(final Writer writer)
            throws UnsupportedEncodingException {
        // This is a big hack, convert writer to PrintStream
        return new PrintStream(new WriterOutputStream(writer), true, "UTF-8");
    }


    /*
        Check the context for an existing global namespace embedded
        in the script context engine scope.  If none exists, ininitialize the
        context with one.
    */


    private static NameSpace getEngineNameSpace(ScriptContext scriptContext) {
        Map<String, Object> engineView = new ScriptContextEngineView(scriptContext);
        NameSpace ns = (NameSpace) engineView.get(engineNameSpaceKey);

        if (!engineView.containsKey(engineNameSpaceKey)) {
            // Create a global namespace for the interpreter
            ns = new ExternalNameSpace(null/*parent*/, "javax_script_context", engineView);
            engineView.put(engineNameSpaceKey, ns);
        }

        return ns;
    }


    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }


    @Override
    public ScriptEngineFactory getFactory() {
        if (factory == null) {
            factory = new BshScriptEngineFactory();
        }
        return factory;
    }


    /**
     * Compiles the script (source represented as a {@code String}) for later
     * execution.
     *
     * @param script The source of the script, represented as a {@code String}.
     * @return An subclass of {@code CompiledScript} to be executed later
     *         using one of the {@code eval} methods of {@code CompiledScript}.
     * @throws ScriptException    if compilation fails.
     * @throws NullPointerException if the argument is null.
     */

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        try {
            final PreparsedScript preparsed = new PreparsedScript(script);
            return new CompiledScript() {

                @Override
                public Object eval(ScriptContext context) throws ScriptException {
                    try {
                        preparsed.setOut(toPrintStream(context.getWriter()));
                        preparsed.setErr(toPrintStream(context.getErrorWriter()));
                        return preparsed.invoke(new ScriptContextEngineView(context));
                    } catch (final UnsupportedEncodingException e) {
                        throw new ScriptException(e.toString(), script, -1);
                    } catch (final EvalError e) {
                        throw constructScriptException(e);
                    }
                }

                @Override
                public ScriptEngine getEngine() {
                    return BshScriptEngine.this;
                }

            };
        } catch (final EvalError e) {
            throw constructScriptException(e);
        }
    }


    private ScriptException constructScriptException(final EvalError e) {
        return new ScriptException(e.getMessage(), e.getErrorSourceFile(), e.getErrorLineNumber());
    }


    private static String convertToString(Reader reader) throws IOException {
        final StringBuffer buffer = new StringBuffer(64);
        char[] cb = new char[64];
        int len;
        while ((len = reader.read(cb)) != -1) {
            buffer.append(cb, 0, len);
        }
        reader.close();
        return buffer.toString();
    }


    /**
     * Compiles the script (source read from {@code Reader}) for later
     * execution.  Functionality is identical to {@code compile(String)} other
     * than the way in which the source is passed.
     *
     * @param script The reader from which the script source is obtained.
     * @return An implementation of {@code CompiledScript} to be executed
     *         later using one of its {@code eval} methods of
     *         {@code CompiledScript}.
     * @throws ScriptException    if compilation fails.
     * @throws NullPointerException if argument is null.
     */
    @Override
    public CompiledScript compile(Reader script) throws ScriptException {
        try {
            return compile(convertToString(script));
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }


    /**
     * Calls a procedure compiled during a previous script execution, which is
     * retained in the state of the {@code ScriptEngine{@code .
     *
     * @param name The name of the procedure to be called.
     * @param thiz If the procedure is a member  of a class defined in the script
     *             and thiz is an instance of that class returned by a previous execution or
     *             invocation, the named method is called through that instance. If classes are
     *             not supported in the scripting language or if the procedure is not a member
     *             function of any class, the argument must be {@code null}.
     * @param args Arguments to pass to the procedure.  The rules for converting
     *             the arguments to scripting variables are implementation-specific.
     * @return The value returned by the procedure.  The rules for converting the
     *         scripting variable returned by the procedure to a Java Object are
     *         implementation-specific.
     * @throws javax.script.ScriptException if an error occurrs during invocation
     *                                      of the method.
     * @throws NoSuchMethodException        if method with given name or matching argument
     *                                      types cannot be found.
     * @throws NullPointerException      if method name is null.
     */
    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (!(thiz instanceof bsh.This)) {
            throw new ScriptException("Illegal object type: " + (null == thiz ? "null" : thiz.getClass()));
        }

        bsh.This bshObject = (bsh.This) thiz;

        try {
            return bshObject.invokeMethod(name, args);
        } catch (TargetError e) {
            // The script threw an application level exception
            // set it as the cause ?
            ScriptException se = new ScriptException(e.toString(), e.getErrorSourceFile(), e.getErrorLineNumber());
            se.initCause(e.getTarget());
            throw se;
        } catch (EvalError e) {
            // The script couldn't be evaluated properly
            throw new ScriptException(e.toString(), e.getErrorSourceFile(), e.getErrorLineNumber());
        }
    }


    /**
     * Same as invoke(Object, String, Object...) with {@code null} as the
     * first argument.  Used to call top-level procedures defined in scripts.
     *
     * @param args Arguments to pass to the procedure
     * @return The value returned by the procedure
     * @throws javax.script.ScriptException if an error occurrs during invocation
     *                                      of the method.
     * @throws NoSuchMethodException        if method with given name or matching
     *                                      argument types cannot be found.
     * @throws NullPointerException      if method name is null.
     */
    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return invokeMethod(getGlobal(), name, args);
    }


    /**
     * Returns an implementation of an interface using procedures compiled in the
     * interpreter. The methods of the interface may be implemented using the
     * {@code invoke} method.
     *
     * @param clasz The {@code Class} object of the interface to return.
     * @return An instance of requested interface - null if the requested interface
     *         is unavailable, i. e. if compiled methods in the
     *         {@code ScriptEngine} cannot be found matching the ones in the
     *         requested interface.
     * @throws IllegalArgumentException if the specified {@code Class} object
     *                                  does not exist or is not an interface.
     */
    @Override
    public <T> T getInterface(Class<T> clasz) {
        return clasz.cast(getGlobal().getInterface(clasz));
    }


    /**
     * Returns an implementation of an interface using member functions of a
     * scripting object compiled in the interpreter. The methods of the interface
     * may be implemented using invoke(Object, String, Object...) method.
     *
     * @param thiz  The scripting object whose member functions are used to
     *              implement the methods of the interface.
     * @param clasz The {@code Class} object of the interface to return.
     * @return An instance of requested interface - null if the requested
     *         interface is unavailable, i. e. if compiled methods in the
     *         {@code ScriptEngine} cannot be found matching the ones in the
     *         requested interface.
     * @throws IllegalArgumentException if the specified {@code Class} object
     *                                  does not exist or is not an interface, or if the specified Object is null
     *                                  or does not represent a scripting object.
     */
    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (!(thiz instanceof bsh.This)) {
            throw new IllegalArgumentException("Illegal object type: " + (null == thiz ? "null" : thiz.getClass()));
        }

        bsh.This bshThis = (bsh.This) thiz;
        return clasz.cast(bshThis.getInterface(clasz));
    }


    private bsh.This getGlobal() {
        // requires 2.0b5 to make getThis() public
        return getEngineNameSpace(getContext()).getThis(getInterpreter());
    }

    /*
        This is a total hack.  We need to introduce a writer to the
        Interpreter.
    */

    class WriterOutputStream extends OutputStream {

        private final ByteBuffer input;
        private final CharBuffer output;
        private final Writer writer;
        private final CharsetDecoder decoder;

        WriterOutputStream(Writer writer) {
            this.writer = writer;
            this.decoder = StandardCharsets.UTF_8.newDecoder();
            int size = 16384;
            this.input = ByteBuffer.allocate(size);
            this.output = CharBuffer.allocate(size);
        }

        @Override
        public void write(int data) throws IOException {
            this.write(new byte[] {(byte) data}, 0, 1);
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            this.write(buffer, 0, buffer.length);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            while (length > 0) {
                final int done = Math.min(length, this.input.remaining());
                this.input.put(buffer, offset, done);
                this.input.flip();
                while (true) {
                    final CoderResult result = this.decoder.decode(
                        this.input, this.output, false);
                    if (result.isError())
                        result.throwException();
                    this.writer.write(
                        this.output.array(), 0, this.output.position()
                    );
                    this.writer.flush();
                    this.output.rewind();
                    if (result.isUnderflow())
                        break;
                }
                this.input.compact();
                offset += done;
                length -= done;
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

}
