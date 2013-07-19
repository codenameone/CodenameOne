package net.sourceforge.retroweaver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;

public class WeaveRunner {

	private final RetroWeaverClassLoader classLoader;

	public WeaveRunner(String classPath) {
		// use the current JVM version as the target
		String version = System.getProperty("java.version");
		int target;
		switch (version.charAt(2)) {
		case '2':
			target = Weaver.VERSION_1_2;
			break;
		case '3':
			target = Weaver.VERSION_1_3;
			break;
		case '4':
			target = Weaver.VERSION_1_4;
			break;
		case '5':
			target = Weaver.VERSION_1_5;
			break;
		case '6':
			target = Weaver.VERSION_1_6;
			break;
		default:
			throw new RetroWeaverException("Unsupported JVM version: " + version);
		}
		final RetroWeaver retroWeaver = new RetroWeaver(target);
		retroWeaver.setLazy(true);
		
		classLoader = new RetroWeaverClassLoader();
		classLoader.setClassPath(classPath);
		classLoader.setWeaver(retroWeaver);
	}

	public void run(String className, String[] args)
			throws ClassNotFoundException, NoSuchMethodException {
		ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			Class clazz = classLoader.loadClass(className);

			Method m = clazz.getMethod("main", new Class[] { args.getClass() });
			m.setAccessible(true);
			int mods = m.getModifiers();
			if (m.getReturnType() != void.class || !Modifier.isStatic(mods)
					|| !Modifier.isPublic(mods)) {
				throw new NoSuchMethodException("main");
			}
			try {
				m.invoke(null, new Object[] { args });
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException ite) {
				throw new RetroWeaverException(ite);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(previousContextClassLoader);
		}
	}

	public void executeJar(String jarFileName, String[] args)
			throws ClassNotFoundException, NoSuchMethodException {
		// add jar to class path
		classLoader.addJarClassPathElement(jarFileName);

		// get class name from MANIFEST
		String className = null;
		try {
			URL u = new URL("jar:file:" + jarFileName + "!/");
			JarURLConnection uc = (JarURLConnection) u.openConnection();
			Attributes attr = uc.getMainAttributes();

			if (attr != null) {
				className = attr.getValue(Attributes.Name.MAIN_CLASS);
			}
		} catch (IOException ioe) {
		}

		if (className == null) {
			System.err.println("No " + Attributes.Name.MAIN_CLASS + " specified in jar file " + jarFileName); // NOPMD by xlv
		} else {
			run(className, args);
		}
	}

	public static void main(String[] args) throws ClassNotFoundException,
			NoSuchMethodException {
		String classPath = null;
		String mainClass = null;
		String jarFileName = null;

		int argIndex = 0;
		while (argIndex < args.length) {
			String command = args[argIndex++];

			if (command.equals("-cp") || command.equals("-classpath")) {
				classPath = args[argIndex++];
			} else if (command.equals("-jar")) {
				jarFileName = args[argIndex++];
				break;
			} else {
				mainClass = command;
				break;
			}
		}
		if (jarFileName == null) {
			String errorMsg = null;

			if (classPath == null) {
				errorMsg = "Missing class path";
			}
			if (mainClass == null) {
				errorMsg = "Missing main class or jar option";
			}

			if (errorMsg != null) {
				System.out.println(errorMsg); // NOPMD by xlv
				System.out.println(); // NOPMD by xlv
				usage();
				return;
			}
		}

		String[] realArgs = new String[args.length - argIndex];
		System.arraycopy(args, argIndex, realArgs, 0, args.length - argIndex);

		WeaveRunner runner = new WeaveRunner(classPath);

		if (jarFileName != null) {
			runner.executeJar(jarFileName, realArgs);
		} else {
			runner.run(mainClass, realArgs);
		}
	}

	private static final String nl = System.getProperty("line.separator");

	private static void usage() {
		String msg = "Usage: WeaveRunner [-options] class [args...]"
				+ nl
				+ "\t\t(to execute a class)"
				+ nl
				+ "\tor WeaveRunner [-options] -jar jarfile [args...]"
				+ nl
				+ "\t\t(to execute a jar file)"
				+ nl
				+ nl
				+ "where options include:"
				+ nl
				+ "\t-cp <class search path of directories and zip/jar files>"
				+ nl
				+ "\t-classpath <class search path of directories and zip/jar files>"
				+ nl + "\t\tA " + File.pathSeparatorChar
				+ " separated list of directories, JAR archives," + nl
				+ "\t\tand ZIP archives to search for class files." + nl;
		System.out.println(msg); // NOPMD by xlv
	}

}
