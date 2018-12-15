package org.platon.core;

import org.platon.crypto.HashUtil;
import org.platon.storage.trie.Trie;
import org.platon.storage.trie.TrieImpl;

import java.util.List;

/**
 * Created by alliswell on 2018/8/6.
 */
public class TrieHash {

	/**
	 * Calculate the rootHash of a list of bytes
	 * note: this function does NOT save the k-v into DB, just calculate the hash
	 */
	public static byte[] getTrieRoot(List<byte[]> values) {
		Trie trie = new TrieImpl();

		if (values == null || values.isEmpty())
			return HashUtil.EMPTY_HASH;

		for (int i = 0; i < values.size(); i++) {
			trie.put(new byte[] {(byte)((i >> 24) & 0xFF), (byte)((i >> 16) & 0xFF), (byte)((i >> 8) & 0xFF),(byte)(i & 0xFF)},
					values.get(i));
		}
		return trie.getRootHash();
	}

}
