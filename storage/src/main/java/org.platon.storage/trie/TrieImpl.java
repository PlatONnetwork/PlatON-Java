package org.platon.storage.trie;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;
import org.platon.core.proto.EmptyBytesMessage;
import org.platon.crypto.HashUtil;
import org.platon.storage.datasource.Source;
import org.platon.storage.datasource.inmemory.HashMapDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrieImpl implements Trie<byte[]> {

    public static final byte[] EMPTY_TRIE_HASH ;

    private final static Object NULL_NODE = new Object();
    private final byte ENCODEISHASH = 't';
    private final byte ENCODEISNOTHASH = 'f';

    static {
        EMPTY_TRIE_HASH = HashUtil.sha3(EmptyBytesMessage.newBuilder().setData(ByteString.EMPTY).build().toByteArray());
    }

    public class Node {
        protected byte[] hash;
        protected byte[] key;
        protected byte[] lazyParse;
        protected boolean dirty;

        
        public byte[] encode() {
            return encode(true);
        }


        private byte[] encode(boolean forceHash) {
            int len;
            byte isHash;
            byte[] ret;

            if (!dirty) {
                if (provable || this == root) {
                    return hash;
                } else {
                    if (hash == null && lazyParse != null && lazyParse.length > 0 && lazyParse.length < byteSplitThreshold) {
                        len = lazyParse.length;
                        ret = new byte[len + 1];
                        System.arraycopy(lazyParse, 0, ret, 1, len);
                        ret[0] = ENCODEISNOTHASH;
                        return ret;
                    } else if (hash != null) {
                        len = hash.length;
                        ret = new byte[len + 1];
                        System.arraycopy(hash, 0, ret, 1, len);
                        ret[0] = ENCODEISHASH;
                        return ret;
                    } else {
                        throw new RuntimeException("Node status error");
                    }
                }
            }

            TrieProto.NodeBase.Builder builder = TrieProto.NodeBase.newBuilder();

            if (key != null && key.length > 0) {
                builder.setKey(ByteString.copyFrom(key));
            }

            if (this instanceof ValueNode) {
                ValueNode vNode = (ValueNode) this;
                if (vNode.value != null && vNode.value.length > 0) {
                    builder.addValueOrNodeHash(ByteString.copyFrom(vNode.value));
                }
            } else if (this instanceof BranchNode) {
                BranchNode bNode = (BranchNode) this;
                for (Node node : bNode.childNodes) {
                    node.parse();
                    if (node != null) {
                        builder.addValueOrNodeHash(ByteString.copyFrom(node.encode(false)));
                    } else {
                        throw new RuntimeException("Should not get null child node!");
                    }
                }
            } else {
                throw new RuntimeException("Should not use Node instance to call any method!");
            }

            if (hash != null) {
                deleteHash(hash);
                hash = null;
            }
            dirty = false;

            TrieProto.NodeBase nodeBase = builder.build();
            byte[] byteArr = nodeBase.toByteArray();

            if (provable || this == root) {
                hash = bcSHA3Digest256(byteArr);
                addHash(hash, byteArr);
                return hash;
            } else {

                if (byteArr.length < byteSplitThreshold && !forceHash) {
                    len = byteArr.length;
                    isHash = ENCODEISNOTHASH;
                    lazyParse = byteArr;
                } else {
                    hash = bcSHA3Digest256(byteArr);
                    addHash(hash, byteArr);
                    len = hash.length;
                    isHash = ENCODEISHASH;
                    byteArr = hash;
                }
                ret = new byte[len + 1];
                ret[0] = isHash;
                System.arraycopy(byteArr, 0, ret, 1, len);
                return ret;
            }
        }

        private boolean resolveCheck() {
            if (lazyParse != null || hash == null) {
                return true;
            }

            lazyParse = getHash(hash);
            return lazyParse != null;
        }

        public void resolve() {
            if (!resolveCheck()) {
                throw new RuntimeException("Invalid Trie state, can't resolve hash " + new String(hash));
            }
        }

        public void dispose() {
            if (hash != null) {
                deleteHash(hash);
            }
        }

        public void parse() {
            parse(0);
        }

        private void parse(int depth) {
            if (this instanceof ValueNode) {
                ValueNode vNode = (ValueNode) this;
                if (vNode.value != null) {
                    return;
                }
            } else if (this instanceof BranchNode) {
                BranchNode bNode = (BranchNode) this;
                if (bNode.childNodes != null && bNode.childNodes.size() > 1) {
                    return;
                }
            } else {
                throw new RuntimeException("Unexpected Node instance Node");
            }

            if (hash == null && lazyParse == null) {
                throw new RuntimeException("Could not parse:hash=null, lazyParse=null");
            } else if (hash != null && lazyParse == null) {
                resolve();
            } else {
            }

            try {
                TrieProto.NodeBase nodeBase = TrieProto.NodeBase.parseFrom(lazyParse);

                int cnt = nodeBase.getValueOrNodeHashCount();
                assert cnt > 0 && cnt < 257;
                key = nodeBase.getKey().toByteArray();


                if (cnt == 1) {
                    ValueNode vNode = (ValueNode) this;
                    vNode.value = nodeBase.getValueOrNodeHash(0).toByteArray();
                } else {
                    if (depth >= 1) {
                        return;
                    }

                    int index;
                    byte isHash;
                    BranchNode bNode = (BranchNode) this;
                    for (index = 0; index < cnt; index++) {

                        byte[] hashOrValue = nodeBase.getValueOrNodeHash(index).toByteArray();
                        assert hashOrValue.length > 1;
                        byte[] hOrV = new byte[hashOrValue.length - 1];
                        byte[] childLazyParse;

                        if (!provable) {
                            isHash = hashOrValue[0];
                            System.arraycopy(hashOrValue, 1, hOrV, 0, hashOrValue.length - 1);

                            if (isHash == ENCODEISNOTHASH) {
                                childLazyParse = hOrV;
                            } else {
                                childLazyParse = getHash(hOrV);
                            }
                        } else {
                            childLazyParse = getHash(hashOrValue);
                        }

                        int childrenOfChild = TrieImpl.parseNodeType(childLazyParse);

                        Node childNode;
                        if (childrenOfChild == 1) {
                            childNode = new ValueNode();
                        } else if (childrenOfChild >= 2) {
                            childNode = new BranchNode();
                        } else {
                            throw new RuntimeException("Invalid encoded Trie ");
                        }
                        childNode.lazyParse = childLazyParse;
                        childNode.parse(depth + 1);
                        bNode.childNodes.add(childNode);
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        
        public String getType() {
            if (this instanceof ValueNode) {
                return new String("V:");
            } else if (this instanceof BranchNode) {
                return new String("B:");
            } else {
                return new String("N:");
            }
        }

        public String dumpStruct(String indent, String prefix) {
            String ret;
            String hashStr = "";
            String keyStr = "";
            final int SUBLENGTHLIMIT = 8;
            if (hash != null) {
                hashStr = Hex.toHexString(hash);
            }
            if (key != null) {
                keyStr = Hex.toHexString(key);
            }
            if (this instanceof ValueNode) {
                ValueNode vThis = (ValueNode) this;
                String valudeStr = Hex.toHexString(vThis.value);
                ret = indent + prefix + "ValueNode" + (dirty ? " *" : "") +
                        (hash == null ? "" : "(hash: " + hashStr.substring(0, SUBLENGTHLIMIT) + ")");
                ret += " [" + keyStr.substring(0, Math.min(SUBLENGTHLIMIT, keyStr.length())) + "] = " + valudeStr.substring(0, Math.min(SUBLENGTHLIMIT, valudeStr.length())) + "\n";
            } else if (this instanceof BranchNode) {
                BranchNode bThis = (BranchNode) this;
                bThis.parse();
                ret = indent + prefix + "BranchNode" + (dirty ? " *" : "") +
                        (hash == null ? "" : "(hash: " + hashStr.substring(0, Math.min(SUBLENGTHLIMIT, hashStr.length())) + ")") +
                        (key == null ? "" : "(key: " + keyStr.substring(0, Math.min(SUBLENGTHLIMIT, keyStr.length())) + ")");

                for (int i = 0; i < bThis.childNodes.size(); i++) {
                    Node child = bThis.getChildNodeByIndex(i);
                    if (child != null) {
                        ret += child.dumpStruct(indent + "  ", "[" + i + "] ");
                    }
                }
            } else {
                ret = indent + prefix + "Node" + (dirty ? " *" : "") +
                        (hash == null ? "" : "(hash: " + hashStr.substring(0, SUBLENGTHLIMIT) + ")");
            }
            return ret;
        }

        public List<String> dumpTrieNode(boolean compact) {
            List<String> ret = new ArrayList<>();
            if (hash != null) {
                ret.add(hash2str(hash, compact) + " ==> " + dumpContent(false, compact));
            }

            if (this instanceof BranchNode) {
                BranchNode bThis = (BranchNode) this;
                bThis.parse();
                for (int i = 0; i < bThis.childNodes.size(); i++) {
                    Node child = bThis.getChildNodeByIndex(i);
                    if (child != null) {
                        ret.addAll(child.dumpTrieNode(compact));
                    }
                }
            } else if (this instanceof ValueNode) {

            }
            return ret;
        }

        private String dumpContent(boolean recursion, boolean compact) {
            if (recursion && hash != null) {
                return hash2str(hash, compact);
            }

            String ret;
            if (this instanceof BranchNode) {
                ret = "[";
                BranchNode bThis = (BranchNode) this;
                bThis.parse();
                for (int i = 0; i < bThis.childNodes.size(); i++) {
                    Node child = bThis.getChildNodeByIndex(i);
                    ret += i == 0 ? "" : ",";
                    ret += child == null ? "" : child.dumpContent(true, compact);
                }
                ret += "]";
            } else if (this instanceof BranchNode) {
                ValueNode vThis = (ValueNode) this;
                ret = "[<ValudeNode>, " + val2str(vThis.value, compact) + "]";
            } else {
                ret = "[<Node>]";
            }
            return ret;
        }
    }

    public final class ValueNode extends Node {
        protected byte[] value;

        public ValueNode() {
            dirty = true;
        }

        public ValueNode(byte[] hash) {
            this.hash = hash;
            dirty = true;
        }

        public ValueNode(byte[] key, byte[] value) {
            this.key = key;
            this.value = value;
            dirty = true;
        }

        public byte[] getValue() {
            parse();
            return value;
        }

        public Node setValue(byte[] value) {
            parse();
            assert value != null;
            this.value = value;
            dirty = true;
            return this;
        }
    }

    public final class BranchNode extends Node {

        protected ArrayList<Node> childNodes;

        public BranchNode(byte[] key) {
            this.key = key;
            this.childNodes = new ArrayList<Node>();
            dirty = true;
        }

        public BranchNode() {
            this.childNodes = new ArrayList<Node>();
            dirty = true;
        }

        public Node getChildNodeByIndex(int index) {
            parse();
            return childNodes.get(index);
        }

        public int getChildPositionBy1stByte(int searchByte) {
            parse();
            Object n = NULL_NODE;
            int left = 0;
            int right = childNodes.size() - 1;
            int mid, midNodeByte;

            while (left <= right) {
                mid = (left + right) / 2;
                midNodeByte = childNodes.get(mid).key[0] & 0xff;
                if (searchByte < midNodeByte) {
                    right = mid - 1;
                } else if (midNodeByte < searchByte) {
                    left = mid + 1;
                } else {
                    return mid;
                }
            }

            return -1;
        }

        public int getByteInsertPosition(int searchByte) {
            parse();
            Object n = NULL_NODE;
            int left = 0;
            int right = childNodes.size() - 1;
            int mid, midNodeByte, pos = -1;
            if (childNodes.size() == 0) {
                return 0;
            } else if (childNodes.size() == 1) {
                return searchByte < (childNodes.get(0).key[0] & 0xff) ? 0 : 1;
            }


            mid = (left + right) / 2;
            while (left < right) {
                mid = (left + right) / 2;
                midNodeByte = childNodes.get(mid).key[0] & 0xff;
                if (searchByte < midNodeByte) {
                    right = mid;
                } else if (searchByte > midNodeByte) {
                    left = mid;
                } else {

                    throw new RuntimeException("Should not exist!");
                }
                if (left == right - 1) {
                    break;
                }
            }


            if (searchByte < (childNodes.get(left).key[0] & 0xff)) {
                return left;
            } else if (searchByte > (childNodes.get(right).key[0] & 0xff)) {
                return right + 1;
            } else {
                return left + 1;
            }
        }
    }

    private boolean provable = false;
    private int byteSplitThreshold = 32;

    private Node root;
    private Source<byte[], byte[]> cache;


    public TrieImpl() {
        this.cache = new HashMapDB();
        provable = true;
    }


    public TrieImpl(boolean useSPV, int threshold) {
        this.cache = new HashMapDB();
        provable = useSPV;
        byteSplitThreshold = threshold;
    }

    public TrieImpl(Source cache, boolean useSPV, int threshold) {
        this.cache = cache;
        provable = useSPV;
        byteSplitThreshold = threshold;
    }

    public TrieImpl(Source cache, boolean useSPV, int threshold, byte[] root) {
        this.cache = cache;
        provable = useSPV;
        byteSplitThreshold = threshold;
        setRoot(root);
    }

    public Source<byte[], byte[]> getCache() {
        return cache;
    }


    @Override
    public void setRoot(byte[] rootHash) {
        if (rootHash != null) {
            byte[] ret = getHash(rootHash);
            int cnt = TrieImpl.parseNodeType(ret);
            if (cnt == 1) {
                this.root = new ValueNode(rootHash);
                this.root.lazyParse = ret;
                this.root.parse();
            } else if (cnt >= 2) {
                this.root = new BranchNode();
                this.root.hash = rootHash;
                this.root.lazyParse = ret;
                this.root.parse();
            } else {
                this.root = null;
            }
        } else {
            this.root = null;
        }
    }

    @Override
    public void clear() {
        throw new RuntimeException("Not Unsupported.");
    }

    private void encode() {
        if (root != null) {
            root.encode();
        }
    }

    private boolean hasRoot() {
        return root != null && root.resolveCheck();
    }

    @Override
    public byte[] getRootHash() {
        encode();
        if (hasRoot()) {
            return root.hash;
        } else {
            return HashUtil.EMPTY_HASH;
        }
    }

    public void setRootHash(byte[] hash) {
        root.hash = hash;
        root.dirty = true;
    }

    private int calcCommonPrefix(byte[] k1, byte[] k2) {
        int len, commLen = 0;

        if (k1 == null || k2 == null) {
            return 0;
        }

        len = k1.length <= k2.length ? k1.length : k2.length;
        for (int i = 0; i < len; i++) {
            if (k1[i] == k2[i])
                commLen++;
            else
                return commLen;
        }
        return commLen;
    }

    public byte[] spvEncodeHashList(byte[] spvKey) {
        if (!provable || spvKey == null) {
            return null;
        }

        if (get(spvKey) == null) {
            return null;
        }

        return spvEncode(root, bcSHA3Digest256(spvKey), true);
    }

    public byte[] spvEncodeHashList(byte[] spvKey, byte[] spvValue) {
        if (!provable || spvKey == null || spvValue == null) {
            return null;
        }

        if (!Arrays.equals(get(spvKey), spvValue))
        {
            return null;
        }

        return spvEncode(root, bcSHA3Digest256(spvKey), true);
    }

    private byte[] spvEncode(Node n, byte[] k, boolean hasCommon) {

        if (!hasCommon) {
            return n.encode();
        }

        if (n instanceof ValueNode) {
            assert Arrays.equals(n.key, k);

            ValueNode vThis = (ValueNode) n;
            TrieProto.NodeBase.Builder builder = TrieProto.NodeBase.newBuilder();
            builder.setKey(ByteString.copyFrom(n.key));
            builder.addValueOrNodeHash(ByteString.copyFrom(vThis.getValue()));
            builder.setHash(ByteString.copyFrom(n.encode()));

            TrieProto.NodeBase nodeBase = builder.build();
            return nodeBase.toByteArray();
        } else if (n instanceof BranchNode) {
            int commPrefix = calcCommonPrefix(n.key, k);
            byte[] kLeft = new byte[k.length - commPrefix];
            System.arraycopy(k, commPrefix, kLeft, 0, k.length - commPrefix);

            BranchNode bThis = (BranchNode) n;
            int cnt = bThis.childNodes.size();
            TrieProto.NodeBase.Builder builder = TrieProto.NodeBase.newBuilder();
            for (int i = 0; i < cnt; i++) {
                byte[] childRet;
                Node child = bThis.getChildNodeByIndex(i);
                child.parse();
                if (child.key[0] == kLeft[0]) {
                    childRet = spvEncode(child, kLeft, true);
                    TrieProto.NodeBase childNb;
                    try {
                        childNb = TrieProto.NodeBase.parseFrom(childRet);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Decode proto error:!" + new String(childRet));
                    }

                    builder.setChildBase(childNb);
                    builder.setChildBasePos(i);
                    builder.setChildEncode(ByteString.copyFrom(childRet));
                } else {
                    childRet = spvEncode(child, kLeft, false);
                    builder.addValueOrNodeHash(ByteString.copyFrom(childRet));
                }
            }

            builder.setKey(ByteString.copyFrom(n.key));
            builder.setHash(ByteString.copyFrom(n.encode()));

            TrieProto.NodeBase nodeBase = builder.build();
            return nodeBase.toByteArray();
        } else {
            throw new RuntimeException("Invalid Node type");
        }
    }

    public static boolean spvVerifyHashList(byte[] spvHash, byte[] data) {
        try {
            TrieProto.NodeBase nodeBase = TrieProto.NodeBase.parseFrom(data);

            TrieProto.NodeBase.Builder builder = TrieProto.NodeBase.newBuilder();
            builder.setKey(nodeBase.getKey());
            byte[] nodeHash = nodeBase.getHash().toByteArray();

            int pos;
            if (nodeBase.getChildBase() == TrieProto.NodeBase.getDefaultInstance()) {
                if (nodeBase.getValueOrNodeHashCount() != 1) {
                    throw new RuntimeException("Data decode error ");
                } else {
                    builder.addValueOrNodeHash(nodeBase.getValueOrNodeHash(0));
                    TrieProto.NodeBase childNode = builder.build();
                    byte[] childByteArr = childNode.toByteArray();
                    byte[] caculNodeHash = bcSHA3Digest256(childByteArr);


                    byte[] caculedSPVHash = bcSHA3Digest256(nodeBase.getValueOrNodeHash(0).toByteArray());
                    return Arrays.equals(caculedSPVHash, spvHash) && Arrays.equals(nodeHash, caculNodeHash);
                }
            } else {
                pos = nodeBase.getChildBasePos();
                int cnt = nodeBase.getValueOrNodeHashCount();
                for (int i = 0; i < pos; i++) {
                    builder.addValueOrNodeHash(nodeBase.getValueOrNodeHash(i));
                }

                TrieProto.NodeBase childBase = nodeBase.getChildBase();
                byte[] childHash = childBase.getHash().toByteArray();
                builder.addValueOrNodeHash(ByteString.copyFrom(childHash));
                for (int i = 0; i < cnt - pos; i++) {
                    builder.addValueOrNodeHash(nodeBase.getValueOrNodeHash(i + pos));
                }
            }

            TrieProto.NodeBase thisNode = builder.build();
            byte[] byteArr = thisNode.toByteArray();
            if (!Arrays.equals(bcSHA3Digest256(byteArr), nodeHash)) {
                return false;
            }

            byte[] childData = nodeBase.getChildEncode().toByteArray();

            return spvVerifyHashList(spvHash, childData);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            throw new RuntimeException("Decode spv data error ");
        }

    }


    public static byte[] bcSHA3Digest256(byte[] value) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
        return digestSHA3.digest(value);
    }

    public byte[] get(byte[] key) {
        if (hasRoot()) {
            return get(root, bcSHA3Digest256(key));
        }
        return null;
    }

    public byte[] get(byte[] key, boolean isHash) {
        if (hasRoot()) {
            if (isHash && key != null && key.length == 32) {
                return get(root, key);
            } else {
                return get(root, bcSHA3Digest256(key));
            }
        }
        return null;
    }

    private byte[] get(Node n, byte[] k) {
        if (n == null || k == null) {
            return null;
        }

        if (n instanceof ValueNode) {
            if (Arrays.equals(n.key, k)) {
                ValueNode vNode = (ValueNode) n;
                return vNode.getValue();
            } else {
                return null;
            }
        } else if (n instanceof BranchNode) {
            int commPrefix = calcCommonPrefix(n.key, k);
            if (commPrefix == k.length) {
                return null;
            } else {
                int index = k[commPrefix] & 0xff;
                byte[] kLeft = new byte[k.length - commPrefix];
                System.arraycopy(k, commPrefix, kLeft, 0, k.length - commPrefix);
                BranchNode bNode = (BranchNode) n;
                int pos = bNode.getChildPositionBy1stByte(index);
                if (pos == -1) {
                    return null;
                }
                return get(bNode.getChildNodeByIndex(pos), kLeft);
            }
        } else {
            throw new RuntimeException("Invalid Trie type, can't resolve type ");
        }
    }

    public void put(byte[] key, byte[] value) {
        if (key != null && key.length > 0 && value != null && value.length > 0) {
            if (root == null) {
                root = new ValueNode(bcSHA3Digest256(key), value);
            } else {
                root = insert(root, bcSHA3Digest256(key), value);
            }
        } else {

        }
    }

    public void put(byte[] key, byte[] value, boolean isHash) {
        if (key != null && key.length > 0 && value != null && value.length > 0) {
            byte[] kNew;
            if (isHash && key != null && key.length == 32) {
                kNew = key;
            } else {
                kNew = bcSHA3Digest256(key);
            }

            if (root == null) {
                root = new ValueNode(kNew, value);
            } else {
                root = insert(root, kNew, value);
            }
        } else {

        }
    }

    private Node insert(Node n, byte[] k, byte[] v) {
        if (n == null || k == null || v == null) {

            return null;
        }

        int commPrefix = calcCommonPrefix(n.key, k);

        if (n instanceof ValueNode) {
            if (commPrefix == n.key.length) {
                ValueNode vNode = (ValueNode) n;
                return vNode.setValue(v);
            } else if (commPrefix == k.length) {
                throw new RuntimeException("Invalid key length:" + new String(k));
            } else {
                byte[] commKey = new byte[commPrefix];
                System.arraycopy(k, 0, commKey, 0, commPrefix);
                BranchNode newBranchNode = new BranchNode(commKey);

                byte[] nKeyNew = new byte[n.key.length - commPrefix];
                System.arraycopy(n.key, commPrefix, nKeyNew, 0, n.key.length - commPrefix);
                ValueNode nNode = (ValueNode) n;
                ValueNode nNewNode = new ValueNode(nKeyNew, nNode.value);
                int posN = nKeyNew[0] & 0xff;

                byte[] kNew = new byte[k.length - commPrefix];
                System.arraycopy(k, commPrefix, kNew, 0, k.length - commPrefix);
                ValueNode newKVNode = new ValueNode(kNew, v);
                int posK = kNew[0] & 0xff;

                if (posN < posK) {
                    newBranchNode.childNodes.add(nNewNode);
                    newBranchNode.childNodes.add(newKVNode);
                } else {
                    newBranchNode.childNodes.add(newKVNode);
                    newBranchNode.childNodes.add(nNewNode);
                }

                return newBranchNode;
            }
        } else if (n instanceof BranchNode) {
            if (n.key.length == 0 || commPrefix == n.key.length) {
                byte[] kNew = new byte[k.length - commPrefix];
                System.arraycopy(k, commPrefix, kNew, 0, k.length - commPrefix);

                int index = kNew[0] & 0xff;
                BranchNode b = (BranchNode) n;
                int pos = b.getChildPositionBy1stByte(index);
                if (pos == -1) {
                    ValueNode newKVNode = new ValueNode(kNew, v);
                    int posInsert = b.getByteInsertPosition(index);
                    b.childNodes.add(posInsert, newKVNode);
                } else {
                    Node child = b.getChildNodeByIndex(pos);
                    Node retNode = insert(child, kNew, v);
                    if (retNode != child) {
                        b.childNodes.remove(pos);
                        b.childNodes.add(pos, retNode);
                    }
                }
                return b;
            } else {
                byte[] commKey = new byte[commPrefix];
                System.arraycopy(k, 0, commKey, 0, commPrefix);
                BranchNode newBranchNode = new BranchNode(commKey);

                byte[] kNew = new byte[k.length - commPrefix];
                System.arraycopy(k, commPrefix, kNew, 0, k.length - commPrefix);
                ValueNode newKVNode = new ValueNode(kNew, v);
                int posK = kNew[0] & 0xff;

                byte[] nKeyNew = new byte[n.key.length - commPrefix];
                System.arraycopy(n.key, commPrefix, nKeyNew, 0, n.key.length - commPrefix);
                BranchNode nNewNode = (BranchNode) n;
                nNewNode.key = nKeyNew;
                int posN = nKeyNew[0] & 0xff;

                if (posN < posK) {
                    newBranchNode.childNodes.add(nNewNode);
                    newBranchNode.childNodes.add(newKVNode);
                } else {
                    newBranchNode.childNodes.add(newKVNode);
                    newBranchNode.childNodes.add(nNewNode);
                }

                return newBranchNode;
            }
        } else {
            throw new RuntimeException("Invalid Trie type, can't resolve type ");
        }
    }

    public void delete(byte[] key) {
        if (root != null) {
            root = delete(root, bcSHA3Digest256(key));
        }
    }

    public void delete(byte[] key, boolean isHash) {
        if (root != null) {
            if (isHash && key != null && key.length == 32) {
                root = delete(root, key);
            } else {
                root = delete(root, bcSHA3Digest256(key));
            }
        }
    }

    private Node delete(Node n, byte[] k) {
        if (n instanceof ValueNode) {
            if (Arrays.equals(n.key, k)) {
                n.dispose();
                if (root.hash != null && root.hash.length > 0 && Arrays.equals(n.hash, root.hash)) {
                    root = null;
                }

                return null;
            } else {
                return n;
            }
        } else if (n instanceof BranchNode) {
            int commPrefix = calcCommonPrefix(n.key, k);

            assert k.length > commPrefix;

            byte[] kNew = new byte[k.length - commPrefix];
            System.arraycopy(k, commPrefix, kNew, 0, k.length - commPrefix);
            int index = kNew[0] & 0xff;
            BranchNode bNode = (BranchNode) n;
            int posNode = bNode.getChildPositionBy1stByte(index);
            if (posNode == -1) {
                return n;
            } else {
                Node child = bNode.getChildNodeByIndex(posNode);
                Node newChild = delete(child, kNew);
                if (child == newChild) {
                    return n;
                } else if (newChild != null) {
                    bNode.childNodes.remove(posNode);
                    bNode.childNodes.add(posNode, newChild);
                    return bNode;
                } else {
                    bNode.childNodes.remove(posNode);
                }

                int pos = 0;
                int childNum = 0;
                BranchNode bnNode = (BranchNode) n;
                int i = -1;
                for (Node children : bNode.childNodes) {
                    i++;
                    if (children instanceof ValueNode) {
                        ValueNode vChild = (ValueNode) children;
                        if (vChild != null && vChild.value != null) {
                            childNum++;
                            pos = i;
                        }
                    } else if (children instanceof BranchNode) {
                        BranchNode bChild = (BranchNode) children;
                        if (bChild != null && bChild.childNodes != null && bChild.childNodes.size() > 1) {
                            childNum++;
                            pos = i;
                        }
                    }

                    if (childNum >= 2) {

                        return n;
                    }
                }

                if (childNum == 0) {
                    n.dispose();
                    return null;
                } else {
                    Node childNew = bnNode.childNodes.get(pos);
                    byte[] keyNew = new byte[n.key.length + childNew.key.length];
                    System.arraycopy(n.key, 0, keyNew, 0, n.key.length);
                    System.arraycopy(childNew.key, 0, keyNew, n.key.length, childNew.key.length);
                    childNew.key = keyNew;
                    childNew.dirty = true;

                    n.dispose();
                    remove(n);
                    return childNew;
                }
            }
        } else {
            throw new RuntimeException("Invalid Trie type, can't resolve type ");
        }
    }

    private void remove(Node n) {
        n.hash = null;
        n.key = null;
        n.lazyParse = null;

        if (n instanceof ValueNode) {
            ValueNode vn = (ValueNode) n;
            vn.value = null;
        } else if (n instanceof BranchNode) {
            BranchNode bn = (BranchNode) n;
            bn.childNodes = null;
        } else {
            throw new RuntimeException("Invalid Trie type, can't resolve type ");
        }
        n = null;
    }

    public boolean flush() {
        if (root != null && root.dirty) {

            encode();

            if (root instanceof ValueNode) {
                root = new ValueNode(root.hash);
            } else if (root instanceof BranchNode) {
                byte[] oldHash = root.hash;
                root = new BranchNode();
                root.hash = oldHash;
            } else {
                throw new RuntimeException("Invalid Node type");
            }

            return true;
        } else {
            return false;
        }
    }

    private void addHash(byte[] hash, byte[] value) {
        cache.put(hash, value);
    }

    private byte[] getHash(byte[] hash) {
        return cache.get(hash);
    }

    private void deleteHash(byte[] hash) {
        cache.delete(hash);
    }


    public String dumpStructure() {
        return root == null ? "<empty>" : root.dumpStruct("", "");
    }

    public String dumpTrie() {
        return dumpTrie(true);
    }

    public String dumpTrie(boolean compact) {
        if (root == null) return "<empty>";
        encode();
        StringBuilder ret = new StringBuilder();
        List<String> strings = root.dumpTrieNode(compact);
        ret.append("Root: " + hash2str(getRootHash(), compact) + "\n");
        for (String s : strings) {
            ret.append(s).append('\n');
        }
        return ret.toString();
    }

    private static String hash2str(byte[] hash, boolean shortHash) {
        String ret = new String(hash);
        return "0x" + (shortHash ? ret.substring(0, 8) : ret);
    }

    private static String val2str(byte[] val, boolean shortHash) {
        String ret = new String(val);
        if (val.length > 16) {
            ret = ret.substring(0, 10) + "... len " + val.length;
        }
        return "\"" + ret + "\"";
    }

    private static int parseNodeType(byte[] codedData) {
        int cnt = -1;
        if (codedData != null && codedData.length > 0) {
            try {
                TrieProto.NodeBase nodeBase = TrieProto.NodeBase.parseFrom(codedData);
                cnt = nodeBase.getValueOrNodeHashCount();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                throw new RuntimeException("Fail while decode" + new String(codedData));
            }
        } else {
            cnt = -2;
        }
        return cnt;
    }
}
