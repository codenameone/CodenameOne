package net.sourceforge.retroweaver.translator;

class NoMirror implements Mirror {

	protected NoMirror() {
		// private constructor
	}

	public boolean exists() {
		return false;
	}

	public String getTranslatedName() {
		throw new UnsupportedOperationException();
	}

	public boolean isClassMirror() {
		return false;
	}

	public boolean hasMethod(final String owner, final String methodName, final String methodDescriptor, final int opcode) {
		return false;
	}

	public boolean hasStaticField(String fieldName, String fieldDescriptor) {
		return false;
	}

}
