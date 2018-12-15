package org.platon.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.util.encoders.Hex;
import org.platon.core.proto.PermissionAddrProto;
import org.platon.storage.trie.SecureTrie;
import org.platon.storage.datasource.CachedSource;
import org.platon.storage.datasource.Source;
import org.platon.storage.datasource.WriteCache;
import org.platon.storage.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Permission
 *
 * @author yanze
 * @desc Account's permission
 * @create 2018-07-27 15:55
 **/
public class Permission {

    private static final Logger logger = LoggerFactory.getLogger(Permission.class);

    private byte[] permissionRoot;

    private boolean dirty;

    private CachedSource.BytesKey<byte[]> trieCache;
    private Trie<byte[]> permissionTrie;

    public Permission(Source<byte[], byte[]> stateDS, byte[] permissionRoot){

        trieCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        permissionTrie = new SecureTrie(trieCache, permissionRoot);

        this.permissionRoot = permissionRoot;
        this.dirty = false;
    }

    /**
     * auth have this permission
     * @param address auth address
     * @param url permission url
     * @return -true:auth access  -false:auth fail
     */
    public boolean auth(byte[] address,byte[] url){
        return  auth(address,url,true);
    }

    private boolean auth(byte[] address,byte[] url,boolean isCycle){
        try {
            PermissionAddrProto permissionAddrProto = PermissionAddrProto.parseFrom(permissionTrie.get(url));
            List<ByteString> addressList = permissionAddrProto.getAddressList().getEleList();
            List<ByteString> urlList = permissionAddrProto.getUrlList().getEleList();
            //check address is contain
            for(ByteString addressBStr : addressList){
                if(Arrays.equals(address,addressBStr.toByteArray())){
                    return true;
                }
            }
            if(isCycle){
                //check url is contain
                for(ByteString urlBStr : urlList){
                    //isCycle is false for prevent infinite loops
                    auth(address,urlBStr.toByteArray(),false);
                }
            }
            return false;
        } catch (InvalidProtocolBufferException e) {
            logger.error("auth error,address:"+Hex.toHexString(address),e);
            return false;
        }
    }

}
