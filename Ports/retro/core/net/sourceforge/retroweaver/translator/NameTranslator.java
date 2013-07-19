
package net.sourceforge.retroweaver.translator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.retroweaver.RetroWeaverException;

import org.objectweb.asm.Type;

/**
 * Substitutes JDK 1.5 classes for their mirrors.
 *
 * Out of the box, Retroweaver supports:
 *   all new language features and their associated runtime
 *     (autoboxing, generics, annotations, extended for loop, static import, varargs)
 *   java.util.concurrent
 *   TODO: full list of what is supported
 *
 * Additional runtime support can be added to Retroweaver by writing a mirror class and adding it 
 * to the class path. A mirror class can be one of two types: class or methods mirror. 
 *
 * 1) Class mirror: Retroweaver replaces every single reference to the JDK 1.5 class directly with the mirror
 * class.
 *
 * 2) Methods mirror: Retroweaver replaces calls to the JDK 1.5 class with static calls to the mirror
 * class. Mirrors for instance calls are different from mirrors from static calls in that Retroweaver 
 * adds the JDK 1.5 object as the first parameter to the mirror. For example, a static call to 
 * java.lang.Integer.valueOf( int ) is replaced directly by net.sourceforge.retroweaver.runtime.java.lang.Integer_.valueOf( int ).
 * However, an instance call to Class.getAnnotations() is replaced with a static call to 
 * net.sourceforge.retroweaver.runtime.java.lang.Class_.getAnnotations( Class ). Notice how the receiver (Class) is
 * added as the first parameter to the mirror call.
 *
 * In order for Retroweaver to find your mirror classes, you must place them in Retroweaver's class path and
 * register their mirror namespace. A mirror namespace defines the prefix of the package being 
 * replaced and the prefix of the package doing the replacing. For example, the namespace for the 
 * java.util.concurrent backport mirrors is "java.util.concurrent/edu.emory.mathcs.backport.java.util.concurrent". 
 * Retroweaver has a default mirror namespace "/net.sourceforge.retroweaver.runtime". 
 * As an example, java.lang.annotation.Annotation is replaced with 
 * net.sourceforge.retroweaver.runtime.java.lang.annotation.Annotation. In order to differentiate class mirrors
 * from methods mirrors, a methods mirror must have exactly one trailing underscore in its class name:
 * for example, net.sourceforge.retroweaver.runtime.java.lang.Class_
 *
 */

public class NameTranslator {

	private static final NameSpace defaultNamespace = new NameSpace("", "net.sourceforge.retroweaver.runtime");

	private static final NameSpace concurrentNamespace = new NameSpace("java.util.concurrent", "edu.emory.mathcs.backport.java.util.concurrent");

	private static final NameSpace harmonyNamespace = new NameSpace("", "net.sourceforge.retroweaver.harmony.runtime");

	/**
	 * Only select classes from the java.util concurrent package can be mirrored
	 */
	private static final String[] javaUtilClasses = new String[] {
			"AbstractQueue",
			"ArrayDeque",
			"Deque",
			"NavigableMap",
			"NavigableSet",
			"PriorityQueue",
			"Queue"
	};

	private static final Mirror noMirror = new NoMirror();

	private final List<NameSpace> namespaces = new LinkedList<NameSpace>();

	private final Map<String, Mirror> mirrors = new HashMap<String, Mirror>();

	private static final NameTranslator generalTranslator ;

	public static final NameTranslator getGeneralTranslator() {
		return generalTranslator;
	}

	private static final NameTranslator harmonyTranslator;

	public static final NameTranslator getHarmonyTranslator() {
		return harmonyTranslator;
	}

	private static final NameTranslator stringBuilderTranslator;

	public static final NameTranslator getStringBuilderTranslator() {
		return stringBuilderTranslator;
	}

	private String name;

	private NameTranslator(String name) {
		// private constructor
		this.name = name;
	}

	static {
		generalTranslator = new NameTranslator("general");
		generalTranslator.addNameSpace(defaultNamespace);
		generalTranslator.addNameSpace(concurrentNamespace);
		for (String s: javaUtilClasses) {
			NameSpace n = new NameSpace("java.util." + s, "edu.emory.mathcs.backport.java.util." + s);
			generalTranslator.addNameSpace(n);
		}

		harmonyTranslator = new NameTranslator("harmony");
		harmonyTranslator.addNameSpace(harmonyNamespace);

		// special rule around StringBuilder
		stringBuilderTranslator = new NameTranslator("StringBuilder");
		stringBuilderTranslator.mirrors.put("java/lang/StringBuilder", new ClassMirror(StringBuffer.class));
	}

	/**
	 * Adds a new runtime subsystem for the name translation. For instance
	 * the concurrency backport translation is done with the NameSpace
	 * "java.util.concurrent", "edu.emory.mathcs.backport.java.util.concurrent"
	 */
	public void addNameSpace(NameSpace nameSpace) {
		namespaces.add(nameSpace);
	}

	/**
	 * Returns either a class or methods mirror
	 * Returns noMirror if there is no match
	 */
	protected Mirror getMirror(final String class_) {
		if (class_ == null) {
			return noMirror;
		}

		// See if we can find an existing mirror 
		final Mirror cachedMirror = mirrors.get(class_);

		if (cachedMirror != null) {
			return cachedMirror;
		}

		// Perform the lookup (on both class and methods if necessary)
		for (NameSpace n : namespaces) {
			String mirrorClass = n.getMirrorClassName(class_);

			if (mirrorClass == null) {
				continue;
			}

			mirrorClass = mirrorClass.replace('/', '.');

			// Attempt class mirror first
			try {
				final Class clazz = Class.forName(mirrorClass);
				final Mirror mirror = new ClassMirror(clazz);
				mirrors.put(class_, mirror);
				return mirror;
			} catch (ClassNotFoundException e) { // NOPMD by xlv
			}

			// Attempt methods mirror 
			mirrorClass += '_';
			try {
				final Class clazz = Class.forName(mirrorClass);
				final Mirror mirror = new MethodsMirror(clazz);
				mirrors.put(class_, mirror);

				return mirror;
			} catch (ClassNotFoundException e) { // NOPMD by xlv
			}
		}

		// No matches in any of the namespaces
		mirrors.put(class_, noMirror);
		return noMirror;
	}

	/**
	 * Translate an id or a method signature into its retroweaver runtime or
	 * concurrent backport equivalent.
	 * 
	 * @param name The <code>String</code> to translate.
	 *
	 * @return the translated name
	 */
	protected String translate(final String name) {
		if (name == null) {
			return null;
		}

		final StringBuffer buffer = new StringBuffer();
		translate(false, name, buffer, 0, name.length());

		return buffer.toString();
	}

	/**
	 * Translates the name only if it has a mirror.
	 */
	private String getMirrorTranslation(final String name) {
		final Mirror mirror = getMirror(name);
		return mirror.exists() ? mirror.getTranslatedName() : name;
	}

	/**
	 * Translates the name only if it represents a fully mirrored class.
	 */
	public String getClassMirrorTranslation(final String name) {
		final Mirror mirror = getMirror(name);
		return mirror.isClassMirror() ? mirror.getTranslatedName() : name;
	}

	/**
	 * Translates the name only if it represents a fully mirrored class.
	 */
	public String getClassMirrorTranslationDescriptor(final String name) {
		if (name == null) {
			return null;
		}

		final StringBuffer buffer = new StringBuffer();
		translate(true, name, buffer, 0, name.length());

		return buffer.toString();
	}

	private void translate(final boolean classMirrorsOnly, final String in, final StringBuffer out, final int start, final int end) {
		if (start >= end) {
			return;
		}

		final char firstChar = in.charAt(start);

		switch (firstChar) {
		case 'Z': // boolean
		case 'B': // byte
		case 'C': // char
		case 'S': // short
		case 'I': // int
		case 'J': // long
		case 'F': // float
		case 'D': // double
		case '[': // type[]
		case 'V': // void
			out.append(firstChar);
			translate(classMirrorsOnly, in, out, start + 1, end);
			break;
		case 'L': // L fully-qualified-class;
			final int endName = in.indexOf(';', start + 1);
			if (endName == -1) {
				// false positive: it's an id, translate the entire string
				final String name = in.substring(start, end);
				final String newName = classMirrorsOnly?getClassMirrorTranslation(name):getMirrorTranslation(name);
				out.append(newName);
			} else {
				final String className = in.substring(start + 1, endName);
				final String newClassName = classMirrorsOnly?getClassMirrorTranslation(className):getMirrorTranslation(className);

				out.append('L').append(newClassName).append(';');
				translate(classMirrorsOnly, in, out, endName + 1, end);
			}
			break;
		case '(': // ( arg-types ) ret-type
			final int endArgs = in.indexOf(')', start + 1);
			if (endArgs == -1) {
				throw new RetroWeaverException("Class name parsing error: missing ')' in " + in);
			}

			out.append('(');
			if (endArgs != start + 1) {
				translate(classMirrorsOnly, in, out, start + 1, endArgs);
			}
			out.append(')');
			translate(classMirrorsOnly, in, out, endArgs + 1, end);
			break;
		default:
			// translate the entire string
			final String name = in.substring(start, end);
			final String newName = classMirrorsOnly?getClassMirrorTranslation(name):getMirrorTranslation(name);
			out.append(newName);
		}
	}


	/**
	 * Translates a descriptor, specifically. Only translates names in the
	 * descriptor, if they are represented by class mirrors.
	 *
	 */
	protected String translateMethodDescriptor(final String descriptor) {
		Type[] argTypes = Type.getArgumentTypes(descriptor);

		for (int i = 0; i < argTypes.length; ++i) {
			argTypes[i] = getMirrorType(argTypes[i]);
		}

		final Type returnType = getMirrorType(Type.getReturnType(descriptor));

		return Type.getMethodDescriptor(returnType, argTypes);
	}

	/**
	 * Translates a simple type descriptor, specifically. Only translates names in the
	 * descriptor, if they are represented by class mirrors.
	 *
	 */
	protected String translateDescriptor(final String descriptor) {
		Type type = Type.getType(descriptor);

		type = getMirrorType(type);

		return type.getDescriptor();
	}

	private Type getMirrorType(final Type type) {
		int numDimensions = 0;
		final Type basicType;

		if (type.getSort() == Type.ARRAY) {
			numDimensions = type.getDimensions();
			basicType = type.getElementType();
		} else {
			basicType = type;
		}

		if (basicType.getSort() != Type.OBJECT) {
			return type;
		}

		final Mirror mirror = getMirror(basicType.getInternalName());

		if (mirror.isClassMirror()) {
			final StringBuilder name = new StringBuilder();

			for (int i = 0; i < numDimensions; ++i) {
				name.append('[');
			}
			name.append('L').append(mirror.getTranslatedName()).append(';');

			return Type.getType(name.toString());
		}

		return type;
	}

}
