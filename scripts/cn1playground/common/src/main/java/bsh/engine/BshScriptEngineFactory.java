package bsh.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import bsh.Interpreter;
import bsh.StringUtil;

import java.util.List;
import java.util.Arrays;

//  Adopted from http://ikayzo.org/svn/beanshell/BeanShell/engine/src/bsh/engine/BshScriptEngineFactory.java
public class BshScriptEngineFactory implements ScriptEngineFactory {
    // Begin impl ScriptEnginInfo

    final List<String> extensions = Arrays.asList("bsh");

    final List<String> mimeTypes = Arrays.asList("application/x-beanshell", "application/x-bsh");

    final List<String> names = Arrays.asList("beanshell", "bsh");


    public String getEngineName() {
        return "BeanShell Engine";
    }


    public String getEngineVersion() {
        return Interpreter.VERSION;
    }


    public List<String> getExtensions() {
        return extensions;
    }


    public List<String> getMimeTypes() {
        return mimeTypes;
    }


    public List<String> getNames() {
        return names;
    }


    public String getLanguageName() {
        return "BeanShell";
    }


    public String getLanguageVersion() {
        return Interpreter.VERSION;
    }


    public Object getParameter(String param) {
        if (param.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        }
        if (param.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        }
        if (param.equals(ScriptEngine.NAME)) {
            return getEngineName();
        }
        if (param.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        }
        if (param.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        }
        if (param.equals("THREADING")) {
            return "MULTITHREADED";
        }

        return null;
    }


    public String getMethodCallSyntax(String objectName, String methodName, String... args) {
        StringBuffer sb = new StringBuffer();
        if (objectName != null)
            sb.append(objectName).append('.');
        sb.append(StringUtil.methodString(methodName, args)).append(";");
        return sb.toString();
    }


    public String getOutputStatement(String message) {
        return "print(\"" + message + "\");";
    }


    public String getProgram(String... statements) {
        StringBuffer sb = new StringBuffer();
        for (final String statement : statements) {
            sb.append(statement);
            if ( ! statement.endsWith(";")) {
                sb.append(";");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // End impl ScriptEngineInfo

    // Begin impl ScriptEngineFactory


    public ScriptEngine getScriptEngine() {
        return new BshScriptEngine();
    }

    // End impl ScriptEngineFactory
}

