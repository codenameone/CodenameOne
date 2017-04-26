package com.codename1.tools.translator;


/**
 * this contains a set of method signatures, which is used in the
 * less-usual case where a method uses more than one different
 * signature for the same function.  The design assumes what 
 * we know; that there are never very many different signatures
 * for the same function name.
 * 
 * in the more usual case, the single signature is its own set.
 * 
 * @author Ddyer
 *
 */
class MultipleSignatureSet implements SignatureSet
{	// structure the signatures as a list with a singleton at the end.
	SignatureSet contents;	// will never be null
	SignatureSet next;		// will never be null
	
	public MultipleSignatureSet(SignatureSet thisSet,SignatureSet nextSet)
	{
		contents = thisSet;
		next = nextSet;
	}
	
	public boolean containsSignature(SignatureSet sig)
	{	// linear search, ok in this case because we know
		// there will only be a few signatures for any
		// given method name.
		return(contents.containsSignature(sig)
				|| next.containsSignature(sig));
	}
	
	public String getSignature() {
		throw new Error("Multiple signatures, shouldn't call this");
	}
	public String getMethodName() {
		return(contents.getMethodName());
	}
}