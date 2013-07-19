package net.sourceforge.retroweaver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RetroWeaverClassLoader extends ClassLoader {

	private RetroWeaver retroWeaver;
	
	private List<ClassPathElement> classPathElements;

	protected void setWeaver(RetroWeaver retroWeaver) {
		this.retroWeaver = retroWeaver;
	}

	protected void setClassPath(List<String> classPath) {
		classPathElements = new LinkedList<ClassPathElement>();

		for(String pathEntry: classPath) {
			File f = new File(pathEntry);
			if (f.exists()) {
				if (f.isDirectory()) {
					addDirectoryClassPathElement(pathEntry);
				} else {
					addJarClassPathElement(pathEntry);
				}
			}
		}
	}

	protected void setClassPath(String classPath) {
		List<String> l = new LinkedList<String>();
		
		if (classPath != null) {
			StringTokenizer t = new StringTokenizer(classPath, File.pathSeparator);
			while (t.hasMoreTokens()) {
				l.add(t.nextToken());
			}
		}

		setClassPath(l);
	}

	protected void addDirectoryClassPathElement(String dirName) {
		DirectoryElement e = new DirectoryElement(dirName);
		classPathElements.add(e);
	}

	protected void addJarClassPathElement(String jarName) {
		try {
			JarElement e = new JarElement(jarName);
			classPathElements.add(e);
		} catch (IOException ioe) {
		}
	}

	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String resourceName = name.replace('.', '/') + ".class";
		for (ClassPathElement e : classPathElements) {
			if (e.hasResource(resourceName)) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				InputStream is = e.getResourceStream(resourceName);

				byte b[];
				boolean weaved;
				try {
					weaved = retroWeaver.weave(is, name, bos);
				} catch (IOException ioe) {
					throw new RetroWeaverException("Problem weaving class " + name
							+ ": " + ioe.getMessage());
				}
				if (weaved) {
					b = bos.toByteArray();
				} else {
					b = e.getResourceData(resourceName);
				}

				Class clazz = defineClass(name.replace('/', '.'), b, 0, b.length);

				return clazz;
			}
		}

		throw new ClassNotFoundException(name);
	}

	protected byte[] getClassData(String name) throws ClassNotFoundException {
		String resourceName = name.replace('.', '/') + ".class";
		for (ClassPathElement e : classPathElements) {
			if (e.hasResource(resourceName)) {
				byte b[] = e.getResourceData(resourceName);

				return b;
			}
		}

		throw new ClassNotFoundException(name);
	}

	protected URL findResource(String name) {
		for (ClassPathElement e : classPathElements) {
			if (e.hasResource(name)) {
				return e.getResourceURL(name);
			}
		}
		return null;
	}

	protected Enumeration<URL> findResources(String name) throws IOException {
		ArrayList<URL> l = new ArrayList<URL>();
		for (ClassPathElement e : classPathElements) {
			if (e.hasResource(name)) {
				l.add(e.getResourceURL(name));
			}
		}
		return Collections.enumeration(l);
	}

	private static abstract class ClassPathElement {
	
		protected abstract boolean hasResource(String name);
	
		protected abstract URL getResourceURL(String name);
	
		protected abstract InputStream getResourceStream(String name);
	
		protected byte[] getResourceData(String name) {
			assert (hasResource(name));
	
			InputStream is = getResourceStream(name);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	
			DataInputStream ds = new DataInputStream(is);
	
			byte b[] = new byte[2048];
			int i;
			try {
				while((i = ds.read(b)) != -1) {
					bos.write(b, 0, i);
				}
				return bos.toByteArray();
			} catch (IOException e) {
				return null;
			} finally {
				try {
					ds.close();
				} catch (IOException e) {
				}
			}
		}
	
	}
	
	private static class DirectoryElement extends ClassPathElement {
	
		private final String dirName;
	
		DirectoryElement(String dirName) {
			super();
			this.dirName = dirName;
		}
	
		protected boolean hasResource(String name) {
			String fullPath = dirName + File.separatorChar + name;
	
			File f = new File(fullPath);
	
			return f.exists() && f.isFile();
		}
	
		protected URL getResourceURL(String name) {
			assert (hasResource(name));
	
			String fullPath = dirName + File.separatorChar + name;
	
			try {
				return new URL("file:" + fullPath);
			} catch (MalformedURLException e) {
				return null;
			}
		}
	
		protected InputStream getResourceStream(String name) {
			assert (hasResource(name));
	
			try {
				File f = new File(dirName + File.separatorChar + name);
				return new FileInputStream(f);
			} catch (IOException ioe) {
				return null;
			}
		}
	
	}
	
	private static class JarElement extends ClassPathElement {
	
		private final String jarName;
	
		private final ZipFile jarFile;
	
		JarElement(String jarName) throws IOException {
			super();
			this.jarName = jarName;
			jarFile = new ZipFile(jarName);
		}
	
		protected boolean hasResource(String name) {
			ZipEntry entry = jarFile.getEntry(name);
	
			return entry != null;
		}
	
		protected URL getResourceURL(String name) {
			assert (hasResource(name));
	
			try {
				return new URL("jar:file:" + jarName + "!/" + name);
			} catch (MalformedURLException e) {
				return null;
			}
		}
	
		protected InputStream getResourceStream(String name) {
			assert (hasResource(name));
	
			try {
				ZipEntry entry = jarFile.getEntry(name);
				return jarFile.getInputStream(entry);
			} catch (IOException ioe) {
				return null;
			}
		}
	
	}

}