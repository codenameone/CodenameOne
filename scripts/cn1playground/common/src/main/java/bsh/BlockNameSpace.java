package bsh;

/**
 * Reduced block namespace without concurrent/reference-cache machinery.
 */
class BlockNameSpace extends NameSpace {
    public static int blockCount;

    public static NameSpace getInstance(NameSpace parent, int blockId) {
        return new BlockNameSpace(parent, blockId);
    }

    public BlockNameSpace(NameSpace parent, int blockId) {
        super(parent, parent.getName() + "/BlockNameSpace" + blockId);
        this.isMethod = parent.isMethod;
    }

    public Variable setVariable(String name, Object value, boolean strictJava, boolean recurse)
            throws UtilEvalError {
        if (weHaveVar(name)) {
            return super.setVariable(name, value, strictJava, false);
        }
        return getParent().setVariable(name, value, strictJava, recurse);
    }

    public void setBlockVariable(String name, Object value) throws UtilEvalError {
        super.setVariable(name, value, false, false);
    }

    private boolean weHaveVar(String name) {
        try {
            return super.getVariableImpl(name, false) != null;
        } catch (UtilEvalError e) {
            return false;
        }
    }

    private NameSpace getNonBlockParent() {
        NameSpace parent = super.getParent();
        if (parent instanceof BlockNameSpace) {
            return ((BlockNameSpace) parent).getNonBlockParent();
        }
        return parent;
    }

    public This getThis(Interpreter declaringInterpreter) {
        return getNonBlockParent().getThis(declaringInterpreter);
    }

    public This getSuper(Interpreter declaringInterpreter) {
        return getNonBlockParent().getSuper(declaringInterpreter);
    }

    public void importClass(String name) {
        getParent().importClass(name);
    }

    public void importPackage(String name) {
        getParent().importPackage(name);
    }

    public void setMethod(BshMethod method) {
        getParent().setMethod(method);
    }
}
