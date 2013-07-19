package net.sourceforge.retroweaver;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class SignatureStripper extends ClassAdapter {

	public SignatureStripper(ClassVisitor cv) {
		super(cv);
	}

    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
        super.visit(version, access, name, null, superName, interfaces);
    }

    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        return super.visitField(access, name, desc, null, value);
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
    	MethodVisitor mv = super.visitMethod(access, name, desc, null, exceptions);
        return mv == null ? null : new MethodSignatureStripper(mv);
    }

    static class MethodSignatureStripper extends MethodAdapter {
    	MethodSignatureStripper(MethodVisitor mv) {
    		super(mv);
    	}
        public void visitLocalVariable(
                final String name,
                final String desc,
                final String signature,
                final Label start,
                final Label end,
                final int index)
            {
                super.visitLocalVariable(name, desc, null, start, end, index);
            }
    }

}
