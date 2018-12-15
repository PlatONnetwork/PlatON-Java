package org.platon.core.db;

import org.platon.common.AppenderName;
import org.platon.core.block.BlockHeader;
import org.platon.core.datasource.Serializers;
import org.platon.core.datasource.DataSourceArray;
import org.platon.core.datasource.ObjectDataSource;
import org.platon.storage.datasource.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HeaderStore {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    Source<byte[], byte[]> indexDS;
    DataSourceArray<byte[]> index;
    Source<byte[], byte[]> headersDS;
    ObjectDataSource<BlockHeader> headers;

    public HeaderStore() {
    }

    public void init(Source<byte[], byte[]> index, Source<byte[], byte[]> headers) {
        indexDS = index;
        this.index = new DataSourceArray<>(
                new ObjectDataSource<>(index,Serializers.AsIsSerializer, 2048));
        this.headersDS = headers;
        this.headers = new ObjectDataSource<>(headers, Serializers.BlockHeaderSerializer, 512);
    }


    public synchronized BlockHeader getBestHeader() {

        long maxNumber = getMaxNumber();
        if (maxNumber < 0) return null;

        return getHeaderByNumber(maxNumber);
    }

    public synchronized void flush() {
        headers.flush();
        index.flush();
        headersDS.flush();
        indexDS.flush();
    }

    public synchronized void saveHeader(BlockHeader header) {
        index.set((int) header.getNumber(), header.getHash());
        headers.put(header.getHash(), header);
    }

    public synchronized BlockHeader getHeaderByNumber(long number) {
        if (number < 0 || number >= index.size()) {
            return null;
        }

        byte[] hash = index.get((int) number);
        if (hash == null) {
            return null;
        }

        return headers.get(hash);
    }

    public synchronized int size() {
        return index.size();
    }

    public synchronized BlockHeader getHeaderByHash(byte[] hash) {
        return headers.get(hash);
    }

    public synchronized long getMaxNumber(){
        if (index.size() > 0) {
            return (long) index.size() - 1;
        } else {
            return -1;
        }
    }
}
