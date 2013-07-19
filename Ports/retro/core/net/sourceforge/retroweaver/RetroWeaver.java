package net.sourceforge.retroweaver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import net.sourceforge.retroweaver.event.WeaveListener;
import net.sourceforge.retroweaver.optimizer.ClassConstantsCollector;
import net.sourceforge.retroweaver.optimizer.Constant;
import net.sourceforge.retroweaver.optimizer.ConstantComparator;
import net.sourceforge.retroweaver.optimizer.ConstantPool;
import net.sourceforge.retroweaver.translator.NameSpace;
import net.sourceforge.retroweaver.translator.NameTranslator;
import net.sourceforge.retroweaver.translator.NameTranslatorClassVisitor;
import net.sourceforge.retroweaver.translator.TranslatorException;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A bytecode enhancer that translates Java 1.5 class files into Java 1.4 class
 * files. The enhancer performs primarily two tasks: 1) Reverses changes made to
 * the class file format in 1.5 to the former 1.4 format. 2) Replaces compiler
 * generated calls into the new 1.5 runtime with calls into RetroWeaver's
 * replacement runtime.
 */
public class RetroWeaver {

	private final int target;

	private boolean lazy;

	/**
	 * Indicates whether the generic signatures should be stripped. Default to <code>false</code>.
	 */
	private boolean stripSignatures;

	/**
	 * Indicates whether the custom retroweaver attributes should be stripped. Default to <code>false</code>.
	 */
	private boolean stripAttributes;

	private int weavedClassCount;

	private WeaveListener listener;

	private RefVerifier verifier;

	private static final String newLine = System.getProperty("line.separator");

	public RetroWeaver(int target) {
		this.target = target;
	}

	protected static final FileFilter classFilter = new FileFilter() {
		public boolean accept(File f) {
			return f.getName().endsWith(".class");
		}
	};

	protected static final FileFilter subdirFilter = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory();
		}
	};

	protected static void buildFileSets(ArrayList<File[]> fileSets, File path) {
		File[] files = path.listFiles(classFilter);
		if (files != null) {
			fileSets.add(files);
		}

		File[] subdirs = path.listFiles(subdirFilter);
		if (subdirs != null) {
			for (File subdir : subdirs) {
				buildFileSets(fileSets, subdir);
			}
		}
	}

	private void displayStartMessage(int n) {
		if (n > 0) {
			listener.weavingStarted("Processing " + n + (n == 1?" class":" classes"));
		}
	}

	private void displayEndMessage() {
		if (weavedClassCount > 0) {
			listener.weavingCompleted(Integer.toString(weavedClassCount) + (weavedClassCount == 1?" class":" classes") + " weaved.");
		}
	}

	public void weave(File path) throws IOException {
		ArrayList<File[]> fileSets = new ArrayList<File[]>();

		buildFileSets(fileSets, path);

		int n = 0;
		for (File[] set : fileSets) {
			n += set.length;
		}
		displayStartMessage(n);

		for (int i = 0; i < fileSets.size(); i++) {
			for (File file : fileSets.get(i)) {
				String sourcePath = file.getCanonicalPath();
				weave(sourcePath, null);
			}
		}
		displayEndMessage();

		if (verifier != null) {
			verifier.verifyFiles();
			verifier.displaySummary();
		}
	}

	public void weave(File[] baseDirs, String[][] fileSets, File outputDir)
			throws IOException {
		int n = 0;
		for (String[] set : fileSets) {
			n += set.length;
		}
		displayStartMessage(n);

		Set<String> weaved = new HashSet<String>();
		for (int i = 0; i < fileSets.length; i++) {
			for (String fileName : fileSets[i]) {
				File file = new File(baseDirs[i], fileName);
				String sourcePath = file.getCanonicalPath();
				String outputPath = null;
				if (outputDir != null) {
					outputPath = new File(outputDir, fileName)
							.getCanonicalPath();
				}
				// Weave it unless already weaved.
				if (!weaved.contains(sourcePath)) {
					weave(sourcePath, outputPath);
					weaved.add(sourcePath);
				}
			}
		}
		displayEndMessage();

		if (verifier != null) {
			verifier.verifyFiles();
			verifier.displaySummary();
		}
	}

	public void weaveJarFile(String sourceJarFileName, String destJarFileName)
			throws IOException {
		JarFile jarFile = new JarFile(sourceJarFileName);
		ArrayList<JarEntry> entries = Collections.list(jarFile.entries());

		OutputStream os = new FileOutputStream(destJarFileName);
		JarOutputStream out = new JarOutputStream(os);

		int n = 0;
		for (JarEntry entry : entries) {
			if (entry.getName().endsWith(".class")) {
				n++;
			}
		}
		displayStartMessage(n);

		for (JarEntry entry : entries) {
			String name = entry.getName();
			InputStream dataStream = null;
			if (name.endsWith(".class")) {
				// weave class
				InputStream is = jarFile.getInputStream(entry);
				ByteArrayOutputStream classStream = new ByteArrayOutputStream();
				if (weave(is, name, classStream)) {
					// class file was modified
					weavedClassCount++;

					dataStream = new ByteArrayInputStream(classStream
							.toByteArray());

					// create new entry
					entry = new JarEntry(name);
					recordFileForVerifier(name);
				}
			}

			if (dataStream == null) {
				// not a class file or class wasn't no
				dataStream = jarFile.getInputStream(entry);
			}
			// writing entry
			out.putNextEntry(new JarEntry(name));

			// writing data
			int len;
			final byte[] buf = new byte[1024];
			while ((len = dataStream.read(buf)) >= 0) {
				out.write(buf, 0, len);
			}
		}
		out.close();
		jarFile.close();

		displayEndMessage();

		if (verifier != null) {
			verifier.verifyJarFile(destJarFileName);
			verifier.displaySummary();
		}
	}

	public void weave(String sourcePath, String outputPath) throws IOException {
		InputStream is = new FileInputStream(sourcePath);
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			if (weave(is, sourcePath, bos)) {
				// new class was generated
				weavedClassCount++;

				String path;

				if (outputPath == null) {
					path = sourcePath;
				} else {
					path = outputPath;
					// create parent dir if necessary
					File parentDir = new File(path).getParentFile();
					if (parentDir != null) {
						parentDir.mkdirs();
					}
				}
				FileOutputStream fos = new FileOutputStream(path);
				fos.write(bos.toByteArray());
				fos.close();
				
				recordFileForVerifier(path);
			} else {
				// We're lazy and the class already has the target version.

				if (outputPath == null) {
					// weaving in place
					return;
				}

				File dir = new File(outputPath).getParentFile();
				if (dir != null) {
					dir.mkdirs();
				}

				File sf = new File(sourcePath);
				File of = new File(outputPath);

				if (!of.isFile()
						|| !of.getCanonicalPath().equals(sf.getCanonicalPath())) {
					// Target doesn't exist or is different from source so copy
					// the file and transfer utime.
					FileInputStream fis = new FileInputStream(sf);
					byte[] bytes = new byte[(int) sf.length()];
					fis.read(bytes);
					fis.close();
					FileOutputStream fos = new FileOutputStream(of);
					fos.write(bytes);
					fos.close();
					of.setLastModified(sf.lastModified());
				}
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) { // NOPMD by xlv
			}
		}
	}

	private void recordFileForVerifier(String fileName) {
		if (verifier != null) {
			verifier.addClass(fileName);
		}
	}

	private static final boolean COMPACT_CONSTANTS = true;

	protected static final Attribute[] CUSTOM_ATTRIBUTES = {
		new RetroWeaverAttribute(Weaver.getBuildNumber(), Weaver.VERSION_1_5)
	};

	private boolean classpathChecked;
	
	private boolean isRuntimeInClassPath() {
		if (!classpathChecked) {
			try {
				Class.forName("net.sourceforge.retroweaver.AIB");
				classpathChecked = true;
			} catch (ClassNotFoundException e) {
				listener.weavingError("Error: the retroweaver runtime must be in the classpath");
				return false;
			}			
		}
                return true;
	}

	protected boolean weave(InputStream sourceStream, String fileName, ByteArrayOutputStream bos)
			throws IOException {

		if (!isRuntimeInClassPath()) {
			return false;
		}

        ClassReader cr = new ClassReader(sourceStream);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        try {
        	// chain class visitors
        	ClassVisitor classVisitor = cw;
        	ConstantPool cp;
            if (COMPACT_CONSTANTS) {
                cp = new ConstantPool();
                classVisitor = new ClassConstantsCollector(classVisitor, cp);
            }
            classVisitor = new NameTranslatorClassVisitor(classVisitor, NameTranslator.getGeneralTranslator());
        	classVisitor = new ClassWeaver(classVisitor,
        								lazy, stripAttributes, target, listener);

            classVisitor = new NameTranslatorClassVisitor(classVisitor, NameTranslator.getHarmonyTranslator());

        	// StringBuilder translation will be done before general weaving and
        	// mirror translation: trimToSize() calls will be processed correctly
        	// and no need to do translations in general weaving process
            classVisitor = new NameTranslatorClassVisitor(classVisitor, NameTranslator.getStringBuilderTranslator());

        	if (stripSignatures) {
        		classVisitor = new SignatureStripper(classVisitor);
        	}

        	cr.accept(classVisitor, CUSTOM_ATTRIBUTES, ClassReader.EXPAND_FRAMES);      	

            if (COMPACT_CONSTANTS) {
            	Set<Constant> constants = new TreeSet<Constant>(new ConstantComparator());
            	constants.addAll(cp.values());

            	cr = new ClassReader(cw.toByteArray());
                cw = new ClassWriter(0);
                for(Constant c: constants) {
                	c.write(cw);
                }
                cr.accept(cw, 0);
            }

        	bos.write(cw.toByteArray());
        	return true;
        } catch (TranslatorException te) {
        	listener.weavingError(te.getMessage());
        	return false;
        } catch (LazyException e) {
        	return false;
        }
 	}

	public void setListener(WeaveListener listener) {
		this.listener = listener;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setVerifier(RefVerifier verifier) {
		this.verifier = verifier;
	}

	public static String getUsage() {
		return "Usage: RetroWeaver " + newLine + " <source path>" + newLine
				+ " [<output path>]";
	}

	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.println(getUsage()); // NOPMD by xlv
			return;
		}

		String sourcePath = args[0];
		String outputPath = null;

		if (args.length > 1) {
			outputPath = args[1];
		}

		try {
			RetroWeaver weaver = new RetroWeaver(Weaver.VERSION_1_4);
			weaver.setListener(new DefaultWeaveListener(false));
			weaver.weave(sourcePath, outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param stripSignatures The stripSignatures to set.
	 */
	public void setStripSignatures(boolean stripSignatures) {
		this.stripSignatures = stripSignatures;
	}

	/**
	 * @param stripAttributes the stripAttributes to set
	 */
	public void setStripAttributes(boolean stripAttributes) {
		this.stripAttributes = stripAttributes;
	}
	
	public void addNameSpaces(List<NameSpace> nameSpaces) {
		NameTranslator translator = NameTranslator.getGeneralTranslator();
		for(NameSpace n: nameSpaces) {
			translator.addNameSpace(n);
		}
	}

}

class LazyException extends RuntimeException {
}

class ClassWeaver extends ClassAdapter implements Opcodes {

    private final boolean lazy;
    private final boolean stripAttributes;
    private final int target;
    private int originalClassVersion;
    private final WeaveListener listener;

    private String className;

    private boolean isEnum;
    private boolean isInterface;

    private final Set<String> classLiteralCalls = new HashSet<String>();

    public ClassWeaver(final ClassVisitor cv, boolean lazy, boolean stripAttributes, int target, WeaveListener listener) {
        super(cv);
        this.lazy = lazy;
        this.stripAttributes = stripAttributes;
        this.target = target;
        this.listener = listener;
    }

    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
    	if (lazy && (version <= target)) {
        	// abort all visitors
    		throw new LazyException();
    	}

		if (listener != null) {
			listener.weavingPath(name);
		}

		className = name;
        isEnum = superName != null && superName.equals("java/lang/Enum");
        isInterface = (access & ACC_INTERFACE) == ACC_INTERFACE;
        originalClassVersion = version;

        cv.visit(target, // Changes the format of the class file from 1.5 to the target value.
                access,
                name,
                signature,
                superName,
                interfaces);
    }

    public void visitInnerClass(
        final String name,
        final String outerName,
        final String innerName,
        final int access)
    {
        cv.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        return cv.visitField(access, name, desc, signature, value);
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        int newAccess;
        if ((access&(ACC_SYNTHETIC|ACC_BRIDGE)) == (ACC_SYNTHETIC|ACC_BRIDGE)) {
            /*
            bridge methods for generic create problems with RMIC code in 1.4.
            It's a known bug with 1.4, see SUN's bug database at:
                http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4811083
                http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5035300

            Problem found when implementing Comparable<E>, with bridge method
                compareTo(Ljava/lang/Object;)I;
            */

            // Workaround disabled so that isSynthethic() and isBridge() can be implemented
            //newAccess = access & ~ACC_SYNTHETIC & ~ACC_BRIDGE;
            newAccess = access;
            
            if (name.equals(APPEND_METHOD) && 
            		(desc.equals(APPENDABLE_APPEND_SIGNATURE1) ||
            		desc.equals(APPENDABLE_APPEND_SIGNATURE2) ||
            		desc.equals(APPENDABLE_APPEND_SIGNATURE3))) {
            	/* remove bridge methods for Appendable, see Writer test case */
            	return null;
            }
        } else {
            newAccess = access;
        }

        String[] newExceptions;
        if (exceptions != null) {
        	newExceptions = new String[exceptions.length];
        	for (int i = 0; i < exceptions.length; i++) {
        		newExceptions[i] = NameTranslator.getGeneralTranslator().getClassMirrorTranslation(exceptions[i]);
        	}
        } else {
        	newExceptions = exceptions;
        }
        if(isEnum && name.equals("values")) {
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "values", desc, null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, className, "$VALUES", "[L" + className+ ";");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();

            return null;
        }
        MethodVisitor mv = new MethodWeaver(super.visitMethod(newAccess,
                    name,
                    desc,
                    signature,
                    newExceptions));
    	
    	if (!isEnum || !"<clinit>".equals(name)) {
    		return mv;
    	}

    	return new EnumMethodWeaver(mv);
    }
    
    /*public void visitSource(String source, String debug) {
        // remove debug info...
    }*/
    public void visitAttribute(final Attribute attr) {
    	if (attr instanceof RetroWeaverAttribute) {
    		// make sure the original version is kept if class file
    		// is weaved more than once
    		RetroWeaverAttribute ra = (RetroWeaverAttribute) attr;
    		originalClassVersion = ra.getOriginalClassVersion();
    	} else {
    		cv.visitAttribute(attr);
    	}
    }

    public void visitEnd() {
        if (isEnum) {
        	cv.visitField(ACC_PRIVATE + ACC_STATIC + ACC_FINAL + ACC_SYNTHETIC,
        			SERIAL_ID_FIELD,
        			SERIAL_ID_SIGNATURE,
        			null, new Long(0L));
        }
        if (!classLiteralCalls.isEmpty()) {
    		// generate synthetic fields and class$ method
    		for(String fieldName: classLiteralCalls) {
    			FieldVisitor fv = visitField(ACC_STATIC + ACC_SYNTHETIC
    					+ (isInterface?ACC_PUBLIC:ACC_PRIVATE),
	    					fieldName,
	    					CLASS_FIELD_DESC,
	    					null, null);
    			fv.visitEnd();
    		}
                if (!isInterface) {
                     // "class$" method
	    		MethodVisitor mv = cv.visitMethod(ACC_STATIC+ACC_SYNTHETIC,
                                                            "class$",
                                                            "(Ljava/lang/String;)Ljava/lang/Class;",
                                                            null, null);
	
	    		/*mv.visitCode();

                    Label beginTry = new Label();
                    Label endTry = new Label();
                    Label catchBlock = new Label();
                    Label finished = new Label();
                    mv.visitTryCatchBlock(beginTry, endTry, catchBlock, "java/lang/Exception");
                    mv.visitLabel(beginTry);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");        
                    mv.visitLabel(endTry);
                    mv.visitJumpInsn(GOTO, finished);
                    mv.visitLabel(catchBlock);
                    mv.visitInsn(POP);
                    mv.visitLabel(finished);

                    mv.visitInsn(ARETURN);
	    
	            mv.visitMaxs(0, 0);
	            mv.visitEnd();*/
                    mv.visitCode();
                    Label l0 = new Label();
                    Label l1 = new Label();
                    Label l2 = new Label();
                    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
                    mv.visitLabel(l0);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                    mv.visitLabel(l1);
                    mv.visitInsn(ARETURN);
                    mv.visitLabel(l2);
                    mv.visitVarInsn(ASTORE, 1);
                    mv.visitInsn(ACONST_NULL);
                    mv.visitInsn(ARETURN);
                    /*mv.visitTypeInsn(NEW, "java/lang/NoClassDefFoundError");
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "()V");
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NoClassDefFoundError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
                    mv.visitInsn(ATHROW);*/
                    mv.visitMaxs(2, 2);
                    mv.visitEnd();

    		}                
    	}

        if (!stripAttributes) {
        	RetroWeaverAttribute a = new RetroWeaverAttribute(Weaver.getBuildNumber(), originalClassVersion);        
        	cv.visitAttribute(a);
        }

        cv.visitEnd();
    }

    /**
     * Generate the byte code equivalent to ".class"
     * 
     * @param mv method visitor to use
     * @param cls name of class
     */
    private void generateClassCall(MethodVisitor mv) {
    	/* 
    	 * generate the code equivalent to ".class"
    	 * 

			new cls[0].getClass().getComponentType()
    	 */

        // converted this to a simpler .class without the array nonsense
        
        //mv.visitLdcInsn(Type.getType("L" + cls + ";"));
    	/*mv.visitInsn (ICONST_0);
    	mv.visitTypeInsn (ANEWARRAY, cls);    	
    	mv.visitMethodInsn (INVOKEVIRTUAL, JAVA_LANG_OBJECT, GET_CLASS_METHOD, GET_CLASS_SIGNATURE);
    	mv.visitMethodInsn (INVOKEVIRTUAL, JAVA_LANG_CLASS, GET_COMPONENT_TYPE_METHOD, GET_COMPONENT_TYPE_SIGNATURE);*/
            
            //mv.visitLdcInsn(cls.replace('/', '.'));

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");        
        
    }

    private class EnumMethodWeaver extends MethodAdapter implements Opcodes {
    	public EnumMethodWeaver(final MethodVisitor mv) {
    		super(mv);
    	}

        public void visitInsn(final int opcode) {
        	if (opcode == RETURN) {
            	// add call to setEnumValues(Object[] values, Class c)

            	String owner = className.replace('.', '/');
            	String fullName = 'L' + owner + ';';
            	Type t = Type.getType(fullName);

            	mv.visitMethodInsn(INVOKESTATIC, owner, "values", "()[" + fullName);
            	mv.visitLdcInsn(t);
            	mv.visitMethodInsn( INVOKESTATIC, RETROWEAVER_ENUM,
            			"setEnumValues", "([Ljava/lang/Object;Ljava/lang/Class;)V" );
        	}
            mv.visitInsn(opcode);
        }

    }

    private static final String JAVA_LANG_CLASS = "java/lang/Class";

    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String GET_CLASS_METHOD = "getClass";
    private static final String GET_CLASS_SIGNATURE = "()Ljava/lang/Class;";
    private static final String GET_COMPONENT_TYPE_METHOD = "getComponentType";
    private static final String GET_COMPONENT_TYPE_SIGNATURE = "()Ljava/lang/Class;";

    private static final String SERIAL_ID_FIELD = "serialVersionUID";
    private static final String SERIAL_ID_SIGNATURE = "J";

	private static final String CLASS_FIELD_DESC = "Ljava/lang/Class;";

	private static final String ITERABLE_CLASS = "java/lang/Iterable";
	private static final String ITERATOR_METHOD = "iterator";
	private static final String ITERATOR_SIGNATURE = "()Ljava/util/Iterator;";
	private static final String ITERABLE_METHODS_CLASS = "net/sourceforge/retroweaver/runtime/java/lang/Iterable_";
	private static final String ITERABLE_METHODS_ITERATOR_SIGNATURE = "(Ljava/lang/Object;)Ljava/util/Iterator;";

	private static final String APPEND_METHOD = "append";
	private static final String APPENDABLE_APPEND_SIGNATURE1 = "(C)Ljava/lang/Appendable;";
	private static final String APPENDABLE_APPEND_SIGNATURE2 = "(Ljava/lang/CharSequence;II)Ljava/lang/Appendable;";
	private static final String APPENDABLE_APPEND_SIGNATURE3 = "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;";

	private static final String RETROWEAVER_ENUM = "net/sourceforge/retroweaver/runtime/java/lang/Enum";

	private static final String REENTRANTREADWRITELOCK_CLASS = "java/util/concurrent/locks/ReentrantReadWriteLock";
	private static final String REENTRANTREADWRITELOCK_READLOCK_CLASS = "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock";
	private static final String REENTRANTREADWRITELOCK_WRITELOCK_CLASS = "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock";
	private static final String READLOCK_METHOD = "readLock";
	private static final String WRITELOCK_METHOD = "writeLock";
	private static final String REENTRANTREADWRITELOCK_READLOCK_SIGNATURE = "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;";
	private static final String REENTRANTREADWRITELOCK_WRITELOCK_SIGNATURE = "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$WriteLock;";
	private static final String REENTRANTREADWRITELOCK_READLOCK_NEW_SIGNATURE = "()Ljava/util/concurrent/locks/Lock;";
	private static final String REENTRANTREADWRITELOCK_WRITELOCK_NEW_SIGNATURE = "()Ljava/util/concurrent/locks/Lock;";

	class MethodWeaver extends MethodAdapter implements Opcodes {
		
	public MethodWeaver(final MethodVisitor mv) {
		super(mv);
	}

    public void visitMethodInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
    	if (opcode == INVOKEINTERFACE &&
    				owner.equals(ITERABLE_CLASS) &&
    				name.equals(ITERATOR_METHOD) &&
    				desc.equals(ITERATOR_SIGNATURE)) {
    		super.visitMethodInsn(INVOKESTATIC,
    				ITERABLE_METHODS_CLASS,
    				ITERATOR_METHOD,
    				ITERABLE_METHODS_ITERATOR_SIGNATURE);
    		return;
		} else if (opcode == INVOKEVIRTUAL &&
				owner.equals(REENTRANTREADWRITELOCK_CLASS)) {
			// workaround for ReentrantReadWriteLock readLock() and writeLock() incompatible return types
			if (name.equals(READLOCK_METHOD) && desc.equals(REENTRANTREADWRITELOCK_READLOCK_SIGNATURE)) {
				super.visitMethodInsn(opcode, owner, name, REENTRANTREADWRITELOCK_READLOCK_NEW_SIGNATURE);
				super.visitTypeInsn(CHECKCAST, REENTRANTREADWRITELOCK_READLOCK_CLASS);
				return;
			} else if (name.equals(WRITELOCK_METHOD) && desc.equals(REENTRANTREADWRITELOCK_WRITELOCK_SIGNATURE)) {
				super.visitMethodInsn(opcode, owner, name, REENTRANTREADWRITELOCK_WRITELOCK_NEW_SIGNATURE);
				super.visitTypeInsn(CHECKCAST, REENTRANTREADWRITELOCK_WRITELOCK_CLASS);
				return;
			}
		}

    	// not a special case, use default implementation
    	super.visitMethodInsn(opcode, owner, name, desc);
	}


    public void visitLdcInsn(final Object cst) {
    	if (cst instanceof Type) {
    		/**
    		 * Fix class literals. The 1.5 VM has had its ldc* instructions updated so
    		 * that it knows how to deal with CONSTANT_Class in addition to the other
    		 * types. So, we have to search for uses of ldc* that point to a
    		 * CONSTANT_Class and replace them with synthetic field access the way
    		 * it was generated in 1.4.
    		 */

    		// LDC or LDC_W with a class as argument

    		Type t = (Type) cst;
    		String fieldName = getClassLiteralFieldName(t);

    		classLiteralCalls.add(fieldName);

    		mv.visitFieldInsn(GETSTATIC, className, fieldName, CLASS_FIELD_DESC);
    		Label nonNullLabel = new Label();
    		mv.visitJumpInsn(IFNONNULL, nonNullLabel);
    		String s;
    		if (t.getSort() == Type.OBJECT) {
    			s = t.getInternalName();
    		} else {
    			s = t.getDescriptor();
    		}
    		
    		/* convert retroweaver runtime classes:
    		 * 		Enum into net.sourceforge.retroweaver.runtime.Enum_
    		 *		concurrent classes into their backport equivalent
    		 *		...
    		 */
    		s = NameTranslator.getGeneralTranslator().getClassMirrorTranslationDescriptor(s);
    		s = NameTranslator.getStringBuilderTranslator().getClassMirrorTranslationDescriptor(s);
    		s = NameTranslator.getHarmonyTranslator().getClassMirrorTranslationDescriptor(s);

    		//generateClassCall(mv, s);
                
                mv.visitLdcInsn(s.replace('/', '.'));
                if (isInterface) {
                    Label beginTry = new Label();
                    Label endTry = new Label();
                    Label catchBlock = new Label();
                    mv.visitTryCatchBlock(beginTry, endTry, catchBlock, "java/lang/Exception");
                    mv.visitLabel(beginTry);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");        
                    mv.visitFieldInsn(PUTSTATIC, className, fieldName, CLASS_FIELD_DESC);
                    mv.visitLabel(endTry);
                    mv.visitJumpInsn(GOTO, nonNullLabel);
                    mv.visitLabel(catchBlock);
                    mv.visitInsn(POP);
                } else {
                    mv.visitMethodInsn(INVOKESTATIC, className, "class$", "(Ljava/lang/String;)Ljava/lang/Class;");
                    mv.visitFieldInsn(PUTSTATIC, className, fieldName, CLASS_FIELD_DESC);
                }
                
    		mv.visitLabel(nonNullLabel);
    		mv.visitFieldInsn(GETSTATIC, className, fieldName, CLASS_FIELD_DESC);
                
                
                /*
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
            mv.visitLabel(l0);

            String s;
            if (t.getSort() == Type.OBJECT) {
                    s = t.getInternalName();
            } else {
                    s = t.getDescriptor();
            }
            s = NameTranslator.getGeneralTranslator().getClassMirrorTranslationDescriptor(s);
            s = NameTranslator.getStringBuilderTranslator().getClassMirrorTranslationDescriptor(s);
            s = NameTranslator.getHarmonyTranslator().getClassMirrorTranslationDescriptor(s);

            
            mv.visitLdcInsn(s.replace('/', '.'));
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            mv.visitLabel(l1);
            mv.visitLabel(l2);

                 */
    	} else {
    		super.visitLdcInsn(cst);
    	}
    }

    private String getClassLiteralFieldName(Type type) {
    	String fieldName;
    	if (type.getSort() == Type.ARRAY) {
    		fieldName = "array" + type.getDescriptor().replace('[', '$');
    		if (fieldName.charAt(fieldName.length()-1) == ';') {
    			fieldName = fieldName.substring(0, fieldName.length()-1);
    		}
    	} else {
    		fieldName = "class$" + type.getInternalName();
    	}
    	fieldName = fieldName.replace('/', '$');

    	return fieldName;
    }

}

}

class DefaultWeaveListener implements WeaveListener {

	private final boolean verbose;

	DefaultWeaveListener(boolean verbose) {
		this.verbose = verbose;
	}

	public void weavingStarted(String msg) {
		System.out.println("[RetroWeaver] " + msg); // NOPMD by xlv
	}

	public void weavingCompleted(String msg) {
		System.out.println("[RetroWeaver] " + msg); // NOPMD by xlv
	}

	public void weavingError(String msg) {
		System.out.println("[RetroWeaver] " + msg); // NOPMD by xlv
	}

	public void weavingPath(String sourcePath) {
		if (verbose) {
			System.out.println("[RetroWeaver] Weaving " + sourcePath); // NOPMD by xlv
		}
                if(sourcePath.equals("com/mycompany/myapp/MyApplication")) {
                    System.out.println("Stop here");
                }                
	}
}

