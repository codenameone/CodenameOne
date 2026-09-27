/*
 * Copyright (c) 2017, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.translator;


/**
 * this contains a set of method signatures, which is used in the
 * less-usual case where a method uses more than one different
 * signature for the same function.  The design assumes what 
 * we know; that there are never very many different signatures
 * for the same function name.
 * 
 * in the more usual case, the single signature is its own set.
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

    @Override
    public SignatureSet nextSignature() {
        return null;
    }
}