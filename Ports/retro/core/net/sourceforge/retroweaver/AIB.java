
package net.sourceforge.retroweaver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.retroweaver.runtime.java.lang.annotation.Annotation;
import net.sourceforge.retroweaver.runtime.java.lang.annotation.AnnotationFormatError;
import net.sourceforge.retroweaver.runtime.java.lang.Enum;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * The Annotation Information Block.
 *
 * This is the runtime data structure that holds all of the annotation data in
 * a form that Retroweaver's runtime can use easily. At weave time, we create 
 * a public static transient field named [ANNOTATIONS_FIELD] of this type. At
 * runtime, we parse the class file, read the annotation data, and populate this
 * data structure.
 *
 * ( Method parameter annotations appear in the same order as method parameters,
 *  and each parameter gets its own list of Annotations )
 *
 * @author Toby Reyelts
 *
 */
public class AIB implements ClassVisitor {

  private Class class_;
  private Map<String,Annotation> classAnnotations;
  private Map<String,Map<String,Annotation>> methodAnnotations;
  private Map<String,ArrayList<Map<String,Annotation>>> methodParameterAnnotations;
  private Map<String,Map<String,Annotation>> fieldAnnotations;
  private Map<String,Annotation> inheritedClassAnnotations;
  private Map<String,Object> cachedMethodDefaults;

  private AIB(Class c) {
    classAnnotations = new HashMap<String,Annotation>();
    methodAnnotations = new HashMap<String,Map<String,Annotation>>();
    methodParameterAnnotations = new HashMap<String,ArrayList<Map<String,Annotation>>>();
    fieldAnnotations = new HashMap<String,Map<String,Annotation>>();
    inheritedClassAnnotations = new HashMap<String,Annotation>();

    this.class_ = c;

    readClassStream(c.getName(), this);
  }

	private void readClassStream(final String name, final ClassVisitor cv) {
		String resource = "/" + name.replace('.', '/') + ".class";
		InputStream classStream = class_.getResourceAsStream(resource);
		try {
			ClassReader r = new ClassReader(classStream);
			r.accept(cv, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG
					+ ClassReader.SKIP_FRAMES);

			Class parent = class_.getSuperclass();
			if (parent != null) {
				AIB parentAib = getAib(parent);
				for(Map.Entry<String, Annotation> entry: parentAib.inheritedClassAnnotations.entrySet()) {
					inheritedClassAnnotations.put(entry.getKey(), entry.getValue());
				}
			}
			// add the local annotations
			for(Map.Entry<String, Annotation> entry: classAnnotations.entrySet()) {
				inheritedClassAnnotations.put(entry.getKey(), entry.getValue());
			}
		} catch (IOException e) {
			// Shouldn't generally happen
			throw new AnnotationFormatError(
					"[Retroweaver] Unable to read annotation data for: " + name, e);
		} finally {
			try {
				if (classStream != null) {
					classStream.close();
				}
			} catch (IOException e) { // NOPMD by xlv
			}
		}
	}

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[]{};

  private static final Annotation[][] EMPTY_ANNOTATION_ARRAY_ARRAY = new Annotation[][]{};

  public Annotation[] getClassAnnotations() {
	  return EMPTY_ANNOTATION_ARRAY;//inheritedClassAnnotations.values().toArray( EMPTY_ANNOTATION_ARRAY );
  }

  public Annotation[] getDeclaredClassAnnotations() {
	  return EMPTY_ANNOTATION_ARRAY;//classAnnotations.values().toArray( EMPTY_ANNOTATION_ARRAY );
  }

  public <T extends Annotation> T getClassAnnotation(final Class<T> annotationType) {
	  //return (T) inheritedClassAnnotations.get(annotationType.getName());
      return null;
  }

  public Annotation[] getFieldAnnotations(final String fieldName) {
	  final Map<String,Annotation> annotations = fieldAnnotations.get( fieldName );
      return EMPTY_ANNOTATION_ARRAY; //annotations.values().toArray( EMPTY_ANNOTATION_ARRAY );
  }

  public <T extends Annotation> T getFieldAnnotation(final String fieldName, final Class<T> annotationType) {
	  //final Map<String,Annotation> annotations = fieldAnnotations.get( fieldName );
	  //return (T) annotations.get(annotationType.getName());
      return null;
  }

  private String getMethodIdentifier(final String methodName, final Class[] parameterTypes, final Class returnType) {
	    final StringBuilder b = new StringBuilder(methodName);
	    b.append('(');
	    for (Class c: parameterTypes) {
	    	b.append(Type.getDescriptor(c));
	    }
	    b.append(')').append(Type.getDescriptor(returnType));
	    return b.toString();
	  
  }
  public Annotation[] getMethodAnnotations(final String methodName, final Class[] parameterTypes, final Class returnType) {
	  /*final Map<String,Annotation> annotations = methodAnnotations.get(getMethodIdentifier(methodName, parameterTypes, returnType));
	  if (annotations == null) {
		  return EMPTY_ANNOTATION_ARRAY;
	  }*/
      return EMPTY_ANNOTATION_ARRAY; //annotations.values().toArray( EMPTY_ANNOTATION_ARRAY );	  
  }

  public <T extends Annotation> T getMethodAnnotation(final String methodName, final Class[] parameterTypes, final Class returnType, final Class<T> annotationType) {
	  /*final Map<String,Annotation> annotations = methodAnnotations.get(getMethodIdentifier(methodName, parameterTypes, returnType));
	  if (annotations == null) {
		  return null;
	  }
	  return (T) annotations.get(annotationType.getName());*/
      return null;
  }

  private Map<String, Object> getMethodDefaults() {
	  assert(class_.isAnnotation());

	  if (cachedMethodDefaults == null) {
		DefaultValueVisitor v = new DefaultValueVisitor();
		cachedMethodDefaults = v.parseAttributes(class_.getName());
	  }

	  return cachedMethodDefaults;
  }

  private static final Method cloneMethod;

  static {
	try {
		cloneMethod = Object.class.getDeclaredMethod("clone", new Class[0]);
		cloneMethod.setAccessible(true);
	} catch (NoSuchMethodException e) {
		throw new RuntimeException(e.getMessage());
	}
  }

  public Object getDefaultValue(final String methodName) {
    assert (class_.isAnnotation());

	Object o = getMethodDefaults().get(methodName);
	if (o == null) {
		return null;
	} else if (o.getClass().isArray()) {
		try {
			o = cloneMethod.invoke(o);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	return o;
  }

  public Annotation[][] getMethodParameterAnnotations(final String methodName, final Class[] parameterTypes, final Class returnType) {
	  ArrayList<Map<String, Annotation>> annotations = methodParameterAnnotations.get(getMethodIdentifier(methodName, parameterTypes, returnType));

	  if (annotations == null) {
		  return EMPTY_ANNOTATION_ARRAY_ARRAY; // NOPMD by xlv
	  }

	  if (annotations.size() != parameterTypes.length) {
		  throw new AnnotationFormatError("inconsistent parameter count");
	  }

	  Annotation[][] a = new Annotation[parameterTypes.length][];

	  for(int i = 0; i < parameterTypes.length; i++) {
		  Map<String, Annotation> map = annotations.get(i);
		  
		  a[i] = map.values().toArray(EMPTY_ANNOTATION_ARRAY);
	  }
	  
	  return a;
  }

	private static final Map<Class, AIB> classDescriptors = new HashMap<Class, AIB>();

	/**
	 * Returns the AIB for the class.
	 */
	public static AIB getAib(final Class c) {
		synchronized (c) {
			AIB aib = classDescriptors.get(c);
			if (aib == null) {
				aib = new AIB(c);
				classDescriptors.put(c, aib);
			}
			return aib;
		}
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (!visible) { return EMPTY_VISITOR; }

		return new TopLevelAnnotation(desc, classAnnotations);
	}

	public FieldVisitor visitField(final int access, final String fieldName,
			final String desc, final String signature, final Object value) {
		return new FieldVisitor() {

			Map<String,Annotation> annotations = new HashMap<String,Annotation>();
			
			public AnnotationVisitor visitAnnotation(String desc,
					boolean visible) {
				if (!visible) { return EMPTY_VISITOR; }

				return new TopLevelAnnotation(desc, annotations);
			}

			public void visitAttribute(Attribute attr) {
				// EMPTY
			}

			public void visitEnd() {
				fieldAnnotations.put(fieldName, annotations);
			}
		};
	}

	public MethodVisitor visitMethod(final int access, final String methodName,
			final String desc, final String signature, final String[] exceptions) {
		return new MethodAdapter(EMPTY_VISITOR) {

			Map<String,Annotation> ma = new HashMap<String,Annotation>();
			ArrayList<Map<String, Annotation>> pa = new ArrayList<Map<String, Annotation>>();

			public AnnotationVisitor visitAnnotationDefault() {
				return EMPTY_VISITOR;
			}

			public AnnotationVisitor visitAnnotation(String desc,
					boolean visible) {
				if (!visible) { return EMPTY_VISITOR; }

				return new TopLevelAnnotation(desc, ma);
			}

			public AnnotationVisitor visitParameterAnnotation(int parameter,
					String desc, boolean visible) {
				if (!visible) { return EMPTY_VISITOR; }

				Map<String, Annotation> map;
				if (parameter < pa.size()) {
					map = pa.get(parameter);
				} else {
					map = new HashMap<String, Annotation>();
					pa.add(parameter, map);
				}

				return new TopLevelAnnotation(desc, map);
			}

			public void visitEnd() {
				String name = methodName + desc;
				methodAnnotations.put(name, ma);
				methodParameterAnnotations.put(name, pa);
			}
		};
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		// EMPTY
	}

	public void visitSource(String source, String debug) {
		// EMPTY
	}

	public void visitOuterClass(String owner, String name, String desc) {
		// EMPTY
	}

	public void visitAttribute(Attribute attr) {
		// EMPTY
	}

	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		// EMPTY
	}

	public void visitEnd() {
		// EMPTY
	}

	class DefaultValueVisitor implements ClassVisitor {
 
		private final Map<String,Object> attributes = new HashMap<String,Object>();

		Map<String,Object> parseAttributes(String className) {
			readClassStream(className, this);

			return attributes;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return EMPTY_VISITOR;
		}

		public FieldVisitor visitField(final int access, final String name,
				final String desc, final String signature, final Object value) {
			return EMPTY_VISITOR;
		}

		public MethodVisitor visitMethod(final int access, final String methodName,
				final String desc, final String signature, final String[] exceptions) {
			return new MethodAdapter(EMPTY_VISITOR) {

				public AnnotationVisitor visitAnnotationDefault() {
					// remove leading ()
					String type = desc.substring(2);

					return new DefaultAnnotation(methodName, type, attributes);
				}

				public AnnotationVisitor visitAnnotation(String desc,
						boolean visible) {
					return EMPTY_VISITOR;
				}

				public AnnotationVisitor visitParameterAnnotation(int parameter,
						String desc, boolean visible) {
					return EMPTY_VISITOR;
				}
			};
		}

		public void visit(int version, int access, String name, String signature,
				String superName, String[] interfaces) {
			// EMPTY
		}

		public void visitSource(String source, String debug) {
			// EMPTY
		}

		public void visitOuterClass(String owner, String name, String desc) {
			// EMPTY
		}

		public void visitAttribute(Attribute attr) {
			// EMPTY
		}

		public void visitInnerClass(String name, String outerName,
				String innerName, int access) {
			// EMPTY
		}

		public void visitEnd() {
			// EMPTY
		}
	}

	abstract class AbstractAnnotationVisitor implements AnnotationVisitor {

		protected final AbstractAnnotationVisitor parent;

		protected final String className;

		AbstractAnnotationVisitor(AbstractAnnotationVisitor parent, String className) {
			this.parent = parent;
			this.className = className;
		}

		protected Class getClass(String name) {
			try {
				return Class.forName(name, true, class_.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new AnnotationFormatError(
						"[Retroweaver] Unable to find class: " + name, e);
			}
		}

		protected Annotation createAnnotation(String className, Map<String, Object>attributes) {
			

			return null;
		}

		private Object getEnumValue(final String desc, final String value) {
			String name = Type.getType(desc).getClassName();
			Class c = getClass(name);
			return Enum.valueOf(c, value);
		}

		abstract void insertValue(String name, Object value);

		public void visit(String name, Object value) {
			Object v;
			if (value instanceof Type) {
				Type t = (Type) value;
				v = getClass(t.getClassName());
			} else {
				v = value;
			}

			insertValue(name, v);
		}

		public void visitEnum(String name, String desc, String value) {
			insertValue(name, getEnumValue(desc, value));
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return new NestedAnnotation(this, desc);
		}

	}

	private class ArrayAnnotation extends AbstractAnnotationVisitor {

		ArrayAnnotation(AbstractAnnotationVisitor parent, String className, String type) {
			super(parent, className);
			this.type = type;
		}

		private final String type;

		private final List<Object> values = new LinkedList<Object>();

		void insertValue(String name, Object value) {
			values.add(value);
		}

		public AnnotationVisitor visitArray(String name) {
			throw new UnsupportedOperationException("Nested arrays are not allowed");
		}

		public void visitEnd() {
			Class c = getClass(type.replace('/', '.'));
			c = c.getComponentType();

			Object a = Array.newInstance(c, values.size());
			if (!values.isEmpty()) {
				a = values.toArray((Object[]) a);
			}

			parent.insertValue(className, a);
		}
	}

	private class NestedAnnotation extends AbstractAnnotationVisitor {		

		NestedAnnotation(AbstractAnnotationVisitor parent, String className) {
			super(parent, className);
			
			// get default values
			String type = Type.getType(className).getClassName();
			attributes = new HashMap<String, Object>(getAib(getClass(type)).getMethodDefaults());
		}

		private final Map<String, Object> attributes;

		void insertValue(String name, Object value) {
			attributes.put(name, value);
		}

		public AnnotationVisitor visitArray(String name) {
			throw new UnsupportedOperationException();
		}

		public void visitEnd() {
			Annotation annotation = createAnnotation(className, attributes);

			parent.insertValue(className, annotation);
		}

	}

	private class DefaultAnnotation extends AbstractAnnotationVisitor {
		
		DefaultAnnotation(String className, String type, Map<String, Object> attributes) {
			super(null, className);
			this.type = type;
			this.attributes = attributes;
		}

		private final String type;

		private final Map<String, Object> attributes;
		
		void insertValue(String name, Object value) {
			attributes.put(className, value);
		}

		public AnnotationVisitor visitArray(String name) {
			return new ArrayAnnotation(this, className, type);
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return new NestedAnnotation(this, className);
		}

		public void visitEnd() {
		}

	}

	private class TopLevelAnnotation extends AbstractAnnotationVisitor {

		TopLevelAnnotation(String className, Map<String,Annotation> annotations) {
			super(null, className);

			this.annotations = annotations;

			// first get default values
			// then visitor methods are used to fill the custom settings
			String type = Type.getType(className).getClassName();
			attributes = new HashMap<String, Object>(getAib(getClass(type)).getMethodDefaults());
		}

		private final Map<String,Annotation> annotations;

		private final Map<String, Object> attributes;

		private String getClassNameFromInternalName(final String name) {
			if (name.charAt(0) != 'L') {
				return name;
			}

			return name.replace('/', '.').substring(1, name.length()-1);
		}

		void insertValue(String name, Object value) {
			String key = getClassNameFromInternalName(name);
			attributes.put(key, value);
		}

		public AnnotationVisitor visitArray(String name) {
			try {
				String type = Type.getType(className).getClassName();
				Method m = Class.forName(type).getMethod(name, new Class[0]);
				type = m.getReturnType().getName();
				return new ArrayAnnotation(this, name, type);
			} catch (Exception e) {
				throw new AnnotationFormatError(e);
			}
		}

		public void visitEnd() {
			Annotation annotation = createAnnotation(className, attributes);

			String key = getClassNameFromInternalName(className);
			annotations.put(key, annotation);
		}

	}
	
	public static final AIBEmptyVisitor EMPTY_VISITOR = new AIBEmptyVisitor();

	public static final class AIBEmptyVisitor implements ClassVisitor, FieldVisitor,
			MethodVisitor, AnnotationVisitor {

		public void visit(final int version, final int access,
				final String name, final String signature,
				final String superName, final String[] interfaces) {
		}

		public void visitSource(final String source, final String debug) {
		}

		public void visitOuterClass(final String owner, final String name,
				final String desc) {
		}

		public AnnotationVisitor visitAnnotation(final String desc,
				final boolean visible) {
			return this;
		}

		public void visitAttribute(final Attribute attr) {
		}

		public void visitInnerClass(final String name, final String outerName,
				final String innerName, final int access) {
		}

		public FieldVisitor visitField(final int access, final String name,
				final String desc, final String signature, final Object value) {
			return this;
		}

		public MethodVisitor visitMethod(final int access, final String name,
				final String desc, final String signature,
				final String[] exceptions) {
			return this;
		}

		public void visitEnd() {
		}

		public AnnotationVisitor visitAnnotationDefault() {
			return this;
		}

		public AnnotationVisitor visitParameterAnnotation(final int parameter,
				final String desc, final boolean visible) {
			return this;
		}

		public void visitCode() {
		}

		public void visitFrame(final int type, final int nLocal,
				final Object[] local, final int nStack, final Object[] stack) {
		}

		public void visitInsn(final int opcode) {
		}

		public void visitIntInsn(final int opcode, final int operand) {
		}

		public void visitVarInsn(final int opcode, final int var) {
		}

		public void visitTypeInsn(final int opcode, final String desc) {
		}

		public void visitFieldInsn(final int opcode, final String owner,
				final String name, final String desc) {
		}

		public void visitMethodInsn(final int opcode, final String owner,
				final String name, final String desc) {
		}

		public void visitJumpInsn(final int opcode, final Label label) {
		}

		public void visitLabel(final Label label) {
		}

		public void visitLdcInsn(final Object cst) {
		}

		public void visitIincInsn(final int var, final int increment) {
		}

		public void visitTableSwitchInsn(final int min, final int max,
				final Label dflt, final Label labels[]) {
		}

		public void visitLookupSwitchInsn(final Label dflt, final int keys[],
				final Label labels[]) {
		}

		public void visitMultiANewArrayInsn(final String desc, final int dims) {
		}

		public void visitTryCatchBlock(final Label start, final Label end,
				final Label handler, final String type) {
		}

		public void visitLocalVariable(final String name, final String desc,
				final String signature, final Label start, final Label end,
				final int index) {
		}

		public void visitLineNumber(final int line, final Label start) {
		}

		public void visitMaxs(final int maxStack, final int maxLocals) {
		}

		public void visit(final String name, final Object value) {
		}

		public void visitEnum(final String name, final String desc,
				final String value) {
		}

		public AnnotationVisitor visitAnnotation(final String name,
				final String desc) {
			return this;
		}

		public AnnotationVisitor visitArray(final String name) {
			return this;
		}
	}

}

