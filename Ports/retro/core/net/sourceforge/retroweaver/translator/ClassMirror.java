package net.sourceforge.retroweaver.translator;

class ClassMirror implements Mirror {

	public ClassMirror(final Class class_) {
		translatedName = class_.getName().replace('.', '/');
	}

	private final String translatedName;

	public boolean isClassMirror() {
		return true;
	}

	public boolean hasMethod(final String owner, final String methodName, final String methodDescriptor, final int opcode) {
		return false;
	}

	public boolean hasStaticField(String fieldName, String fieldDescriptor) {
		return false;
	}

	public boolean exists() {
		return true;
	}

	public String getTranslatedName() {
		return translatedName;
	}

}
