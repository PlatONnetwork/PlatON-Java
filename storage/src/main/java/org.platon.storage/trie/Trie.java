package org.platon.storage.trie;

import org.platon.storage.datasource.Source;

public interface Trie<V> extends Source<byte[], V> {

    byte[] getRootHash();

    void setRoot(byte[] root);

    void clear();
}
