package net.sourceforge.retroweaver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifierClassVisitor;

public class ASMifier extends ASMifierClassVisitor {

	public static void main(final String[] args) throws IOException {
        int i = 0;
        boolean skipDebug = true;

        boolean ok = true;
        if (args.length < 1 || args.length > 2) {
            ok = false;
        }
        if (ok && args[0].equals("-debug")) {
            i = 1;
            skipDebug = false;
            if (args.length != 2) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("Prints the ASM code to generate the given class."); // NOPMD by xlv
            System.err.println("Usage: RetroweaverASMifier [-debug] " // NOPMD by xlv
                    + "<fully qualified class name or class file name>");
            return;
        }
        ClassReader cr;
        if (args[i].endsWith(".class") || args[i].indexOf('\\') > -1
                || args[i].indexOf('/') > -1) {
            cr = new ClassReader(new FileInputStream(args[i]));
        } else {
            cr = new ClassReader(args[i]);
        }
        cr.accept(new ASMifier(new PrintWriter(System.out)),
                getDefaultAttributes(),
                skipDebug?ClassReader.SKIP_DEBUG:0);
    }

	public ASMifier(final PrintWriter pw) {
		super(pw);
	}

	public static Attribute[] getDefaultAttributes() {
		return RetroWeaver.CUSTOM_ATTRIBUTES;
	}

}
