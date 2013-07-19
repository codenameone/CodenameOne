package net.sourceforge.retroweaver.translator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class MethodsMirror implements Mirror {

	public MethodsMirror(Class class_) {
		this.class_ = class_;
		translatedName = class_.getName().replace('.', '/');
	}

	private final String translatedName;

	private final Class class_;

	public boolean exists() {
		return true;
	}

	public String getTranslatedName() {
		return translatedName;
	}

	public boolean isClassMirror() {
		return false;
	}

	/**
	 * Returns true if the mirror contains a method that mirrors
	 * <code>methodName</code>. The mirror method should be static, have the
	 * same name, and have the exact same arguments excepting an argument of
	 * type
	 */
	public boolean hasMethod(final String owner, final String methodName, final String methodDescriptor, final int opcode) {
		Type[] types = Type.getArgumentTypes(methodDescriptor);

		if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
			Type[] newTypes = new Type[types.length + 1];
			newTypes[0] = Type.getType("L" + owner + ";");
			System.arraycopy(types, 0, newTypes, 1, types.length);
			types = newTypes;
		}

	outer_loop:
		for (Method m : class_.getDeclaredMethods()) {
			final Type[] methodTypes = Type.getArgumentTypes(m);
			if (!m.getName().equals(methodName)
					|| methodTypes.length != types.length) {
				continue;
			}

			for (int i = 0; i < types.length; ++i) {
				if (!types[i].equals(methodTypes[i])) {
					continue outer_loop;
				}
			}

			return true;
		}

		return false;
	}

	public boolean hasStaticField(String fieldName, String fieldDescriptor) {
		try {
			Field f = class_.getDeclaredField(fieldName);
			if ((f.getModifiers() & Modifier.STATIC) == 0) {
				// not a static field
				return false;
			}
			Type fieldType = Type.getType(f.getType());
			Type argType = Type.getType(fieldDescriptor);

			return argType.equals(fieldType);
		} catch (NoSuchFieldException nsfe) {
			return false;
		}
	}

}
