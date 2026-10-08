package com.codename1.compat.jdk;

import java.io.IOException;

/// `java.io.Externalizable` for the Codename One runtime, so that a class
/// written to save itself through Java serialization compiles and loads. See
/// [ObjectOutput] for what calls it on a device: nothing.
public interface Externalizable extends java.io.Serializable {

    void writeExternal(ObjectOutput out) throws IOException;

    void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
}
