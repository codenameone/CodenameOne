package bsh;

import java.util.ArrayList;
import java.util.List;

/** It's a specific error that is throwed when try to execute something that mustn't be executed */
class SecurityError extends UtilEvalError {

    SecurityError(String msg) {
        super("SecurityError: " + msg);
    }

    @Override
    public EvalError toEvalError(String msg, Node node, CallStack callstack) {
        return new EvalError(this.getMessage(), node, callstack);
    }

    @Override
    public EvalError toEvalError(Node node, CallStack callstack) {
        return new EvalError(this.getMessage(), node, callstack);
    }

    /** This method basically return the types of args at a concatened String by ", " */
    private static String argsTypesString(Object[] args) {
        List<String> typesString = new ArrayList<String>();
        for (Class<?> typeClass: Types.getTypes(args))
            typesString.add(typeClass != null ? Types.prettyName(typeClass) : "null");
        return String.join(", ", typesString);
    }

    /** Create a error for when can't construct a instance */
    static SecurityError cantConstruct(Class<?> _class, Object[] args) {
        String msg = String.format("Can't call this construct: new %s(%s)", _class.getName(), argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't construct a instance using reflection */
    static SecurityError reflectCantConstruct(Class<?> _class, Object[] args) {
        String msg = String.format("Can't call this construct using reflection: new %s(%s)", _class.getName(), argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a static method */
    static SecurityError cantInvokeStaticMethod(Class<?> _class, String methodName, Object[] args) {
        String className = Types.prettyName(_class);
        String msg = String.format("Can't invoke this static method: %s.%s(%s)", className, methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a static method using reflection */
    static SecurityError reflectCantInvokeStaticMethod(Class<?> _class, String methodName, Object[] args) {
        String className = Types.prettyName(_class);
        String msg = String.format("Can't invoke this static method using reflection: %s.%s(%s)", className, methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a method */
    static SecurityError cantInvokeMethod(Object thisArg, String methodName, Object[] args) {
        String className = Types.prettyName(thisArg.getClass());
        String msg = String.format("Can't invoke this method: %s.%s(%s)", className, methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a method using reflection */
    static SecurityError reflectCantInvokeMethod(Object thisArg, String methodName, Object[] args) {
        String className = Types.prettyName(thisArg.getClass());
        String msg = String.format("Can't invoke this method using reflection: %s.%s(%s)", className, methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a local method ( aka commands ) */
    static SecurityError cantInvokeLocalMethod(String methodName, Object[] args) {
        String msg = String.format("Can't invoke this local method: %s(%s)", methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't invoke a super method */
    static SecurityError cantInvokeSuperMethod(Class<?> superClass, String methodName, Object[] args) {
        String superClassName = Types.prettyName(superClass);
        String msg = String.format("Can't invoke this super method: %s.%s(%s)", superClassName, methodName, argsTypesString(args));
        return new SecurityError(msg);
    }

    /** Create a error for when can't get a field */
    static SecurityError cantGetField(Object thisArg, String fieldName) {
        String className = Types.prettyName(thisArg.getClass());
        String msg = String.format("Can't get this field: %s.%s", className, fieldName);
        return new SecurityError(msg);
    }

    /** Create a error for when can't get a field */
    static SecurityError reflectCantGetField(Object thisArg, String fieldName) {
        String className = Types.prettyName(thisArg.getClass());
        String msg = String.format("Can't get this field using reflection: %s.%s", className, fieldName);
        return new SecurityError(msg);
    }

    /** Create a error for when can't get a field */
    static SecurityError cantGetStaticField(Class<?> _class, String fieldName) {
        String className = Types.prettyName(_class);
        String msg = String.format("Can't get this static field: %s.%s", className, fieldName);
        return new SecurityError(msg);
    }

    /** Create a error for when can't get a field */
    static SecurityError reflectCantGetStaticField(Class<?> _class, String fieldName) {
        String className = Types.prettyName(_class);
        String msg = String.format("Can't get this static field using reflection: %s.%s", className, fieldName);
        return new SecurityError(msg);
    }

    /** Create a error for when a class can't extends another class */
    static SecurityError cantExtends(Class<?> superClass) {
        String msg = String.format("This class can't be extended: %s", superClass.getName());
        return new SecurityError(msg);
    }

    /** Create a error for when a class can't implements an interface */
    static SecurityError cantImplements(Class<?> _interface) {
        String msg = String.format("This interface can't be implemented: %s", _interface.getName());
        return new SecurityError(msg);
    }

}
