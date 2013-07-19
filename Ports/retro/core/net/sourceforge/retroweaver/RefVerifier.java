package net.sourceforge.retroweaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sourceforge.retroweaver.event.VerifierListener;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;


/**
 * Reads through a class file searching for references to classes, methods, or
 * fields, which don't exist on the specified classpath. This is primarily
 * useful when trying to target one JDK while using the compiler for another.
 */
public class RefVerifier extends ClassAdapter {

	private final int target;

	private String currentclassName;

	private final RetroWeaverClassLoader classLoader;

	private final List<String> classPathArray;

	private Set<String> failedClasses;

	private final VerifierListener listener;

	private int warningCount;

	private final List<String> classes;

	private final Map<String, SoftReference<ClassReader>> classReaderCache = new HashMap<String, SoftReference<ClassReader>>();

	private static final String nl = System.getProperty("line.separator");

	public RefVerifier(int target, ClassVisitor cv, List<String> classPathArray, VerifierListener listener) {
		super(cv);
		classLoader = new RetroWeaverClassLoader();
		this.classPathArray = classPathArray;

		this.listener = listener;
		this.target = target;

		classes = new LinkedList<String>();
	}

	public void addClass(String className) {
		classes.add(className);
	}

	public void verifyJarFile(String jarFileName) throws IOException {
		JarFile jarFile = new JarFile(jarFileName);

		int count = classes.size();
		if (count > 0) {
			listener.verifyPathStarted("Verifying " + count + (count == 1?" class":" classes"));
		}
		classLoader.setClassPath(classPathArray);

		for (String name : classes) {
			JarEntry entry = jarFile.getJarEntry(name);
			InputStream is = jarFile.getInputStream(entry);
			verifyClass(is);
		}
	}

	public void verifyFiles() throws IOException {
		int count = classes.size();
		if (count > 0) {
			listener.verifyPathStarted("Verifying " + count + (count == 1?" class":" classes"));
		}
		classLoader.setClassPath(classPathArray);

		for (String sourcePath : classes) {
			verifyClass(new FileInputStream(sourcePath));			
		}
	}

	private void verifySingleClass(String classFileName) throws IOException {
		classLoader.setClassPath(classPathArray);

		verifyClass(new FileInputStream(classFileName));
	}

	private void verifyClass(InputStream sourceStream)
			throws IOException {

		failedClasses = new HashSet<String>();

        ClassReader cr = new ClassReader(sourceStream);
        cr.accept(this, 0);
	}

	private void unknowClassWarning(String className, String msg) {
		StringBuffer report = new StringBuffer().append(currentclassName)
			.append(": unknown class ").append(className);

		if (msg != null) {
			report.append(": ").append(msg);
		}

		warning(report);
	}

	private void unknownFieldWarning(String name, String desc, String msg) {
		StringBuffer report = new StringBuffer().append(currentclassName)
			.append(": unknown field ").append(name).append('/').append(desc.replace('/', '.'));

		if (msg != null) {
			report.append(", ").append(msg);
		}

		warning(report);
	}

	private void unknownMethodWarning(String name, String desc, String msg) {
		StringBuffer report = new StringBuffer().append(currentclassName)
			.append(": unknown method ").append(name).append('/').append(desc.replace('/', '.'));

		if (msg != null) {
			report.append(", ").append(msg);
		}

		warning(report);
	}

	private void invalidClassVersion(String className, int target, int version) {
		StringBuffer report = new StringBuffer().append(className)
			.append(": invalid class version ").append(version).append(", target is ").append(target);

		warning(report);
	}

	private void warning(StringBuffer report) {
		warningCount++;
		listener.acceptWarning(report.toString());
	}

	public void displaySummary() {
		if (warningCount != 0) {
			listener.displaySummary(warningCount);
		}
	}

	private ClassReader getClassReader(String className) throws ClassNotFoundException {
		ClassReader reader = null;
		SoftReference<ClassReader> ref = classReaderCache.get(className);
		if (ref != null) {
			reader = ref.get();
		}

		if (reader == null) {
			byte b[] = classLoader.getClassData(className);

			reader = new ClassReader(b);

			classReaderCache.put(className, new SoftReference<ClassReader>(reader));

			// class file version should not be higher than target
			int version = reader.readShort(6); // get major number only
			if (version > target) {
				invalidClassVersion(className.replace('/', '.'), target, version);
			}
		}
		return reader;		
	}

	public static String getUsage() {
		return "Usage: RefVerifier <options>" + nl + " Options: " + nl
				+ " -class <path to class to verify> (required) " + nl
				+ " -cp <classpath containing valid classes> (required)";
	}

	public static void main(String[] args) throws IOException {

		List<String> classpath = new ArrayList<String>();
		String classfile = null;

		for (int i = 0; i < args.length; ++i) {
			String command = args[i];
			++i;

			if ("-class".equals(command)) {
				classfile = args[i];
			} else if ("-cp".equals(command)) {
				String path = args[i];
				StringTokenizer st = new StringTokenizer(path,
						File.pathSeparator);
				while (st.hasMoreTokens()) {
					classpath.add(st.nextToken());
				}
			} else {
				System.out.println("I don't understand the command: " + command); // NOPMD by xlv
				System.out.println(); // NOPMD by xlv
				System.out.println(getUsage()); // NOPMD by xlv
				return;
			}
		}

		if (classfile == null) {
			System.out.println("Option \"-class\" is required."); // NOPMD by xlv
			System.out.println(); // NOPMD by xlv
			System.out.println(getUsage()); // NOPMD by xlv
			return;
		}

		RefVerifier vr = new RefVerifier(Weaver.VERSION_1_4, EMPTY_VISITOR, classpath,
				new DefaultListener(true));
		vr.verifySingleClass(classfile);
		vr.displaySummary();
	}
	
	private void checkClassName(String className) {
		Type t = Type.getType(className);
		String name;

		switch (t.getSort()) {
		case Type.ARRAY:
			t = t.getElementType();
			if (t.getSort() != Type.OBJECT) {
				return;
			}

			// fall through to object processing
		case Type.OBJECT:
			name = t.getClassName();
			break;
		default:
			return;
		}
		
		checkSimpleClassName(name);
	}

	private void checkClassNameInType(String className) {
		switch (className.charAt(0)) {
			case 'L':
				if (className.endsWith(";")) {
					checkClassName(className);
				} else {
					checkSimpleClassName(className);
				}
				break;
			case '[':
				checkClassName(className);
				break;
			default:
				checkSimpleClassName(className);
		}
	}

	private void checkSimpleClassName(String className) {
		String name = className.replace('.', '/');
		try {
			getClassReader(name);
		} catch (ClassNotFoundException e) {
			failedClasses.add(name);
			unknowClassWarning(name.replace('/', '.'), null);
		}
	}

	// visitor methods

    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
		listener.verifyClassStarted("Verifying " + name);

		currentclassName = name.replace('/', '.');

		if (superName != null) {
			checkSimpleClassName(superName);
		}
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; ++i) {
				checkSimpleClassName(interfaces[i]);
			}
		}

		cv.visit(version, access, name, signature, superName, interfaces);
    }

    public void visitOuterClass(
        final String owner,
        final String name,
        final String desc)
    {
    	checkSimpleClassName(owner);

    	cv.visitOuterClass(owner, name, desc);
    }

    public void visitInnerClass(
        final String name,
        final String outerName,
        final String innerName,
        final int access)
    {
        if (name != null) {
        	checkSimpleClassName(name);
        }
        if (outerName != null) {
        	checkSimpleClassName(outerName);
        }
        
        cv.visitInnerClass(name, outerName, innerName, access);
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        if (exceptions != null) {
            for (String s: exceptions) {
            	checkSimpleClassName(s);
            }
        }
    
        return new MethodVerifier(cv.visitMethod(access, name, desc, signature, exceptions));
    }

    public AnnotationVisitor visitAnnotation( final String desc, final boolean visible) {
        checkClassNameInType(desc);
        return new AnnotationVerifier(cv.visitAnnotation(desc, visible));
    }

    private class AnnotationVerifier implements AnnotationVisitor {

        private final AnnotationVisitor av;

        AnnotationVerifier(final AnnotationVisitor av) {
            this.av = av;
        }

        public void visit(final String name, final Object value) {
            av.visit(name, value);
        }

        public void visitEnum(final String name, final String desc, final String value) {
            checkClassNameInType(desc);
            av.visitEnum(name, desc, value);
        }

        public AnnotationVisitor visitAnnotation(final String name, final String desc) {
            checkClassNameInType(desc);
            return av.visitAnnotation(name, desc);
        }

        public AnnotationVisitor visitArray(final String name) {
            return av.visitArray(name);
        }

        public void visitEnd() {
            av.visitEnd();
        }

    }

    private class MethodVerifier extends MethodAdapter {

    	MethodVerifier(MethodVisitor mv) {
    		super(mv);
    	}

        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationVerifier(mv.visitAnnotationDefault());
        }

        public AnnotationVisitor visitAnnotation( final String desc, final boolean visible) {
            checkClassNameInType(desc);
            return new AnnotationVerifier(mv.visitAnnotation(desc, visible));
        }

    	public void visitTypeInsn(int opcode, String desc) {
    		checkClassNameInType(desc);
    		
    		mv.visitTypeInsn(opcode, desc);
    	}
 
    	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			// Don't report a field error, about a class for which we've
			// already shown an error
			if (!failedClasses.contains(owner)) {
				try {
					if (!findField(owner, name, desc)) {
						unknownFieldWarning(name,desc, "Field not found in " + owner.replace('/', '.'));
					}
				} catch (ClassNotFoundException e) {
						unknownFieldWarning(name,desc, "The class, " + owner.replace('/', '.')
						+ ", could not be located: " + e.getMessage());
				}
			}
			mv.visitFieldInsn(opcode, owner, name, desc);
    	}
    	
    	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if (!failedClasses.contains(owner) && owner.charAt(0) != '[') {
				// Don't report a method error, about a class for which we've
				// already shown an error.
				// We just ignore methods called on arrays, because we know
				// they must exist   		

				try {
					if (!findMethod(owner, name, desc)) {
						unknownMethodWarning(name, desc, "Method not found in " + owner.replace('/', '.'));						
					}
				} catch (ClassNotFoundException e) {
					unknownMethodWarning(name, desc, "The class, " + owner.replace('/', '.')
							+ ", could not be located: " + e.getMessage());
				}
			}

			mv.visitMethodInsn(opcode, owner, name, desc);
    	}

    	public void visitMultiANewArrayInsn(String desc, int dims) {
    		checkClassName(desc);
    		
    		mv.visitMultiANewArrayInsn(desc, dims);
    	}

    	public void visitLocalVariable(
	            String name,
	            String desc,
	            String signature,
	            Label start,
	            Label end,
	            int index) {
    		checkClassName(desc);
    		
    		mv.visitLocalVariable(name, desc, signature, start, end, index);
    	}

    }

	private boolean findField(String owner, final String name, final String c) throws ClassNotFoundException {
		String javaClassName = owner;
		while (true) {
			ClassReader reader = getClassReader(javaClassName);
			FindFieldOrMethodClassVisitor visitor = new FindFieldOrMethodClassVisitor(false, name, c);
		
			try {
				reader.accept(visitor, 0);
			} catch (Success s) {
				return true;
			}
			String[] is = visitor.classInterfaces;
			for (String i : is) {
				if (findField(i, name, c)) {
					return true;
				}
			}

			if ("java/lang/Object".equals(javaClassName)) {
				return false;
			}
			javaClassName = visitor.superClassName;
		}
	}

	private boolean findMethod(final String owner, final String name, final String desc) throws ClassNotFoundException {
		String javaClassName = owner;
		while (true) {
			ClassReader reader = getClassReader(javaClassName);
			FindFieldOrMethodClassVisitor visitor = new FindFieldOrMethodClassVisitor(true, name, desc);
			try {
				reader.accept(visitor, 0);
			} catch (Success s) {
				return true;
			}

			if (visitor.isInterface || visitor.isAbstract) {
				String[] is = visitor.classInterfaces;
				for (String i : is) {
					if (findMethod(i, name, desc)) {
						return true;
					}
				}
				if (visitor.isInterface) {
					return false;
				}
			}

			if ("java/lang/Object".equals(javaClassName)) {
				return false;
			}
			javaClassName = visitor.superClassName;
		}
	}

	private static final EmptyVisitor EMPTY_VISITOR = new EmptyVisitor();

	private static class Success extends RuntimeException {};

	// Visitor to search for fields or methods in supplier classes

	private static class FindFieldOrMethodClassVisitor implements ClassVisitor {
		FindFieldOrMethodClassVisitor(boolean methdodMatcher, final String name, final String desc) {
			this.searchedName = name;
			this.searchedDesc = desc;
			this.methdodMatcher = methdodMatcher;
		}
		private final boolean methdodMatcher;
		private final String searchedName;
		private final String searchedDesc;

		protected String classInterfaces[];
		protected String superClassName;
		protected boolean isInterface;
		protected boolean isAbstract;

	    public void visit(
	            final int version,
	            final int access,
	            final String name,
	            final String signature,
	            final String superName,
	            final String[] interfaces)
	        {
	    		classInterfaces = interfaces;
	    		superClassName = superName;
	    		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
	    		isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
	        }

	    public void visitSource(final String source, final String debug) {
	    }

	    public void visitOuterClass(
	        final String owner,
	        final String name,
	        final String desc)
	    {
	    }

	    public AnnotationVisitor visitAnnotation(
	        final String desc,
	        final boolean visible)
	    {
	        return EMPTY_VISITOR;
	    }

	    public void visitAttribute(final Attribute attr) {
	    }

	    public void visitInnerClass(
	        final String name,
	        final String outerName,
	        final String innerName,
	        final int access)
	    {
	    }

	    public FieldVisitor visitField(
	            final int access,
	            final String name,
	            final String desc,
	            final String signature,
	            final Object value) {
	    	if (!methdodMatcher && name.equals(searchedName) && desc.equals(searchedDesc)) {
		   		throw new Success();
		    }
	        return null;
	    }

		public MethodVisitor visitMethod(
	            int access,
	            String name,
	            String desc,
	            String signature,
	            String[] exceptions) {
			if (methdodMatcher && name.equals(searchedName) && desc.equals(searchedDesc)) {
				throw new Success();
			}
	        return null;
	   }

	    public void visitEnd() {
	    }
	}

	public static class DefaultListener implements VerifierListener {
	
		private final boolean verbose;
	
		DefaultListener(boolean verbose) {
			this.verbose = verbose;
		}
	
		public void verifyPathStarted(String msg) {
			System.out.println("[RefVerifier] " + msg); // NOPMD by xlv
		}
	
		public void verifyClassStarted(String msg) {
			if (verbose) {
				System.out.println("[RefVerifier] " + msg); // NOPMD by xlv
			}
		}
	
		public void acceptWarning(String msg) {
			System.out.println("[RefVerifier] " + msg); // NOPMD by xlv
		}
	
		public void displaySummary(int warningCount) {
			System.out.println("[RefVerifier] Verification complete, " + warningCount + " warning(s)."); // NOPMD by xlv
		}

	}

}
