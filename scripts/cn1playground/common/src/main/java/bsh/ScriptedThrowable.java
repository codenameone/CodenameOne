/*****************************************************************************
 * Codename One Playground BeanShell fork.                                    *
 *****************************************************************************/

package bsh;

/**
 * Runtime wrapper that lets a {@link ScriptedInstance} flow through Java
 * throw/catch. {@link BSHThrowStatement} wraps a ScriptedInstance here
 * when it's thrown so the JVM's exception machinery can carry it; BSH's
 * catch-matching unwraps to compare the declared catch type against the
 * wrapped instance's {@link ScriptedClass}.
 */
public final class ScriptedThrowable extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ScriptedInstance scriptedInstance;

    public ScriptedThrowable(ScriptedInstance scriptedInstance) {
        super(messageFromDelegate(scriptedInstance),
                causeFromDelegate(scriptedInstance));
        this.scriptedInstance = scriptedInstance;
    }

    public ScriptedInstance getScriptedInstance() {
        return scriptedInstance;
    }

    private static String messageFromDelegate(ScriptedInstance si) {
        if (si == null) return null;
        Object d = si.getJavaDelegate();
        if (d instanceof Throwable) {
            return ((Throwable) d).getMessage();
        }
        return si.toString();
    }

    private static Throwable causeFromDelegate(ScriptedInstance si) {
        if (si == null) return null;
        Object d = si.getJavaDelegate();
        if (d instanceof Throwable) {
            return ((Throwable) d).getCause();
        }
        return null;
    }
}
