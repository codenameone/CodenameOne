package net.sourceforge.retroweaver;

import java.util.Map;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifiable;
import org.objectweb.asm.util.Traceable;

public class RetroWeaverAttribute extends Attribute implements ASMifiable, Traceable {

	private static final String RETROWEAVER_ATTRIBUTE_NAME = "net.sourceforge.Retroweaver";

	private final int retroweaverBuildNumber;

	private final int originalClassVersion;

	private final long timestamp;

	public RetroWeaverAttribute(int retroweaverBuildNumber, int originalClassVersion) {
		this(retroweaverBuildNumber, originalClassVersion, System.currentTimeMillis());
	}

	public RetroWeaverAttribute(int retroweaverBuildNumber, int originalClassVersion, long timestamp) {
		super(RETROWEAVER_ATTRIBUTE_NAME);
		this.retroweaverBuildNumber = retroweaverBuildNumber;
		this.originalClassVersion = originalClassVersion;
		this.timestamp = timestamp;
	}

	public int getRetroweaverBuildNumber() {
		return retroweaverBuildNumber;
	}
	
	public int getOriginalClassVersion() {
		return originalClassVersion;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isUnknown() {
		return false;
	}

    protected Attribute read(
        final ClassReader cr,
        final int off,
        final int len,
        final char[] buf,
        final int codeOff,
        final Label[] labels)
    {
        return new RetroWeaverAttribute(
        		cr.readInt(off),
        		cr.readInt(off+4),
        		cr.readLong(off+8));
    }

    protected ByteVector write(
        final ClassWriter cw,
        final byte[] code,
        final int len,
        final int maxStack,
        final int maxLocals)
    {
    	ByteVector bv = new ByteVector();

        bv.putInt(retroweaverBuildNumber);
        bv.putInt(originalClassVersion);
        bv.putLong(timestamp);

        return bv;
    }

    public void asmify(
        final StringBuffer buf,
        final String varName,
        final Map labelNames)
    {
        buf.append("Attribute ")
            .append(varName)
            .append(" = new net.sourceforge.retroweaver.RetroweaverAttribute(")
            .append(retroweaverBuildNumber)
            .append(", ")
            .append(originalClassVersion)
            .append(", ")
            .append(timestamp)
            .append(" /*").append(new java.util.Date(timestamp)).append("*/);\n");
    }

    public void trace(final StringBuffer buf, final Map labelNames) {
    	buf.append(retroweaverBuildNumber)
	        .append(' ')
	        .append(originalClassVersion)
	        .append(' ')
	        .append(timestamp)
	        .append(" (").append(new java.util.Date(timestamp)).append(")\n");
    }

}


