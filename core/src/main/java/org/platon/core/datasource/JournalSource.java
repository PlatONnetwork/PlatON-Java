package org.platon.core.datasource;

import org.platon.storage.datasource.AbstractChainedSource;
import org.platon.storage.datasource.HashedKeySource;
import org.platon.storage.datasource.SerializerIfc;
import org.platon.storage.datasource.Source;
import org.platon.storage.datasource.inmemory.HashMapDB;

import java.util.ArrayList;
import java.util.List;

public class JournalSource<V> extends AbstractChainedSource<byte[], V, byte[], V>
        implements HashedKeySource<byte[], V> {

    public static class Update {
        byte[] updateHash;
        List<byte[]> insertedKeys = new ArrayList<>();
        List<byte[]> deletedKeys = new ArrayList<>();

        public Update() {
        }

        public Update(byte[] bytes) {
            parse(bytes);
        }

        public byte[] serialize() {
            /*byte[][] insertedBytes = new byte[insertedKeys.size()][];
            for (int i = 0; i < insertedBytes.length; i++) {
                insertedBytes[i] = RLP.encodeElement(insertedKeys.get(i));
            }
            byte[][] deletedBytes = new byte[deletedKeys.size()][];
            for (int i = 0; i < deletedBytes.length; i++) {
                deletedBytes[i] = RLP.encodeElement(deletedKeys.get(i));
            }
            return RLP.encodeList(RLP.encodeElement(updateHash),
                    RLP.encodeList(insertedBytes), RLP.encodeList(deletedBytes));*/
            return null;
        }

        private void parse(byte[] encoded) {
            /*RLPList l = (RLPList) RLP.decode2(encoded).get(0);
            updateHash = l.get(0).getRLPData();

            for (RLPElement aRInserted : (RLPList) l.get(1)) {
                insertedKeys.add(aRInserted.getRLPData());
            }
            for (RLPElement aRDeleted : (RLPList) l.get(2)) {
                deletedKeys.add(aRDeleted.getRLPData());
            }*/
        }

        public List<byte[]> getInsertedKeys() {
            return insertedKeys;
        }

        public List<byte[]> getDeletedKeys() {
            return deletedKeys;
        }
    }

    private Update currentUpdate = new Update();

    Source<byte[], Update> journal = new HashMapDB<>();

    /**
     * Constructs instance with the underlying backing Source
     */
    public JournalSource(Source<byte[], V> src) {
        super(src);
    }

    public void setJournalStore(Source<byte[], byte[]> journalSource) {
        journal = new SourceCodec.BytesKey<>(journalSource,
                new SerializerIfc<Update, byte[]>() {
                    public byte[] serialize(Update object) {
                        return object.serialize();
                    }

                    public Update deserialize(byte[] stream) {
                        return stream == null ? null : new Update(stream);
                    }
                });
    }

    @Override
    public synchronized void put(byte[] key, V val) {
        if (val == null) {
            delete(key);
            return;
        }

        getSource().put(key, val);
        currentUpdate.insertedKeys.add(key);
    }

    /**
     * Deletes are not propagated to the backing Source immediately
     * but instead they are recorded to the current Update and
     * might be later persisted
     */
    @Override
    public synchronized void delete(byte[] key) {
        currentUpdate.deletedKeys.add(key);
    }

    @Override
    public synchronized V get(byte[] key) {
        return getSource().get(key);
    }

    /**
     * Records all the changes made prior to this call to a single chunk
     * with supplied hash.
     * Later those updates could be either persisted to backing Source (deletes only)
     * or reverted from the backing Source (inserts only)
     */
    public synchronized Update commitUpdates(byte[] updateHash) {
        currentUpdate.updateHash = updateHash;
        journal.put(updateHash, currentUpdate);
        Update committed = currentUpdate;
        currentUpdate = new Update();
        return committed;
    }

    public Source<byte[], Update> getJournal() {
        return journal;
    }

    @Override
    public synchronized boolean flushImpl() {
        journal.flush();
        return false;
    }
}
