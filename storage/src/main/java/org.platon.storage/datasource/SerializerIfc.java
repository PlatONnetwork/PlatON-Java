package org.platon.storage.datasource;

public interface SerializerIfc<T, S> {

    S serialize(T object);

    T deserialize(S stream);
}
