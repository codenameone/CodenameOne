package net.sourceforge.retroweaver.translator;

interface Mirror {

	boolean exists();

	String getTranslatedName();

	boolean isClassMirror();

	boolean hasMethod(String owner, String methodName, String methodDescriptor, int opcode);

	boolean hasStaticField(String fieldName, String fieldDescriptor);

}
