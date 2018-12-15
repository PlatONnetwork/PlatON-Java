package org.platon.core.db;

import org.platon.common.wrapper.DataWord;
import org.platon.core.Account;
import org.platon.core.Repository;
import org.platon.core.datasource.Serializers;
import org.platon.core.datasource.SourceCodec;
import org.platon.storage.trie.SecureTrie;
import org.platon.storage.trie.Trie;
import org.platon.storage.trie.TrieImpl;
import org.platon.storage.datasource.*;

public class RepositoryRoot extends RepositoryImpl {

    private static class StorageCache extends ReadWriteCache<DataWord, DataWord> {

        Trie<byte[]> trie;

        public StorageCache(Trie<byte[]> trie) {
            super(new SourceCodec<>(trie, Serializers.StorageKeySerializer,
                    Serializers.StorageValueSerializer), WriteCache.CacheType.SIMPLE);
            this.trie = trie;
        }
    }

    private class MultiStorageCache extends MultiCache<StorageCache> {

        public MultiStorageCache() {
            super(null);
        }

        @Override
        protected synchronized StorageCache create(byte[] key, StorageCache srcCache) {
            Account account = accountCache.get(key);
            SerializerIfc<byte[], byte[]> keyCompositor = new NodeKeyCompositor(key);
            Source<byte[], byte[]> composingSrc = new SourceCodec.KeyOnly<>(trieCache, keyCompositor);
            TrieImpl storageTrie = createTrie(composingSrc, account == null ? null : account.getStorageRoot());
            return new StorageCache(storageTrie);
        }

        @Override
        protected synchronized boolean flushChild(byte[] key, StorageCache childCache) {
            if (super.flushChild(key, childCache)) {
                if (childCache != null) {
                    Account storageOwnerAccount = accountCache.get(key);
                    // need to update account storage root
                    childCache.trie.flush();
                    byte[] rootHash = childCache.trie.getRootHash();
                    storageOwnerAccount.setStorageRoot(rootHash);
                    accountCache.put(key, storageOwnerAccount);
                    return true;
                } else {
                    // account was deleted
                    return true;
                }
            } else {
                // no storage changes
                return false;
            }
        }
    }

    private Source<byte[], byte[]> stateDS;
    private CachedSource.BytesKey<byte[]> trieCache;
    private Trie<byte[]> stateTrie;

    public RepositoryRoot(Source<byte[], byte[]> stateDS) {
        this(stateDS, null);
    }

    /**
     * Building the following structure for snapshot Repository:
     *
     * stateDS --> trieCache --> stateTrie --> accountStateCodec --> accountStateCache
     *  \                 \
     *   \                 \-->>> storageKeyCompositor --> contractStorageTrie --> storageCodec --> storageCache
     *    \--> codeCache
     */
    public RepositoryRoot(final Source<byte[], byte[]> stateDS, byte[] root) {
        this.stateDS = stateDS;

        trieCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        stateTrie = new SecureTrie(trieCache, root);

        SourceCodec.BytesKey<Account, byte[]> accountCodec = new SourceCodec.BytesKey<>(stateTrie, Serializers.AccountStateSerializer);
        final ReadWriteCache.BytesKey<Account> accountCache = new ReadWriteCache.BytesKey<>(accountCodec, WriteCache.CacheType.SIMPLE);

        final MultiCache<StorageCache> storageCache = new MultiStorageCache();

        // counting as there can be 2 contracts with the same code, 1 can suicide
        Source<byte[], byte[]> codeCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        init(accountCache, codeCache, storageCache);
    }

    @Override
    public synchronized void commit() {
        super.commit();
        stateTrie.flush();
        trieCache.flush();
    }

    @Override
    public synchronized byte[] getRoot() {
        storageCache.flush();
        accountCache.flush();

        return stateTrie.getRootHash();
    }

    @Override
    public synchronized void flush() {
        commit();
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        return new RepositoryRoot(stateDS, root);
    }

    @Override
    public synchronized void syncToRoot(byte[] root) {
        stateTrie.setRoot(root);
    }

    protected TrieImpl createTrie(Source<byte[], byte[]> trieCache, byte[] root) {
        return new SecureTrie(trieCache, root);
    }

    @Override
    public synchronized String dumpStateTrie() {
        return ((TrieImpl) stateTrie).dumpTrie();
    }

}
