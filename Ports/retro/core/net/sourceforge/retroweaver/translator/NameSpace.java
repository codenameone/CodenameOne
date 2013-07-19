package net.sourceforge.retroweaver.translator;

public class NameSpace {

	public NameSpace(String oldPrefix, String newPrefix) {
		if (oldPrefix == null || newPrefix == null) {
			throw new IllegalArgumentException();
		}

		this.oldPrefix = oldPrefix.replace('.', '/');
		this.newPrefix = newPrefix.replace('.', '/');
	}

	private final String oldPrefix;

	private final String newPrefix;

	public String getOldPrefix() {
		return oldPrefix;
	}

	public String getNewPrefix() {
		return newPrefix;
	}

	public String toString() {
		return "[" + oldPrefix + ", " + newPrefix + "]";
	}

	/**
	 * Returns the translated mirror class name for <code>class_</code> or
	 * null if the namespace is not applicable
	 * 
	 * @param class_ the class name to translate
	 * @return the translated name or null
	 */
	public String getMirrorClassName(final String class_) {
		if (oldPrefix.length() == 0) {
			return newPrefix + '/' + class_;
		}

		if (!class_.startsWith(oldPrefix)) {
			return null;
		}

		return class_.replaceFirst(oldPrefix, newPrefix);
	}

}
