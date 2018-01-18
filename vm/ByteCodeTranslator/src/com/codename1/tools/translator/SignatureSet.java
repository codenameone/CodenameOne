package com.codename1.tools.translator;

public interface SignatureSet
{
	public boolean containsSignature(SignatureSet sig);
	public String getSignature();
	public String getMethodName();
	// next signature in the set is null by default.
	default public SignatureSet nextSignature() { 
		return null;
	}
}
