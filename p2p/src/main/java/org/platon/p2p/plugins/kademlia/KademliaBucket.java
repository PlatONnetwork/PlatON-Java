package org.platon.p2p.plugins.kademlia;

import org.apache.commons.codec.binary.Hex;
import org.platon.p2p.common.KadPluginConfig;
import org.platon.p2p.proto.common.NodeID;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;



public class KademliaBucket
{

    
    private final int depth;

    
    private final TreeSet<Contact> contacts;

    
    private final TreeSet<Contact> replacementCache;


    {
        contacts = new TreeSet<>();
        replacementCache = new TreeSet<>();
    }

    
    public KademliaBucket(int depth)
    {
        this.depth = depth;

    }


    public synchronized void insert(Contact c)
    {
        if (this.contacts.contains(c))
        {
            
            this.removeFromContacts(c.getNode());
            c.setSeenNow();
            c.resetStaleCount();
            this.contacts.add(c);
        }
        else
        {
            
            if (contacts.size() >= KadPluginConfig.getInstance().getKValue())
            {
                
                Contact stalest = null;
                for (Contact tmp : this.contacts)
                {
                    if (tmp.staleCount() >= KadPluginConfig.getInstance().getStaleTimes())
                    {
                        
                        if (stalest == null)
                        {
                            stalest = tmp;
                        }
                        else if (tmp.staleCount() > stalest.staleCount())
                        {
                            stalest = tmp;
                        }
                    }
                }

                
                if (stalest != null)
                {
                    this.contacts.remove(stalest);
                    this.contacts.add(c);
                }
                else
                {
                    
                    this.insertIntoReplacementCache(c);
                }
            }
            else
            {
                this.contacts.add(c);
            }
        }
    }

    public synchronized void insert(NodeID n)
    {
        this.insert(new Contact(n));
    }

    public synchronized boolean containsContact(Contact c)
    {
        return this.contacts.contains(c);
    }

    public synchronized boolean containsNode(NodeID n)
    {
        return this.containsContact(new Contact(n));
    }

    public synchronized boolean removeContact(Contact c)
    {
        
        if (!this.contacts.contains(c))
        {
            return false;
        }

        
        if (!this.replacementCache.isEmpty())
        {
            
            this.contacts.remove(c);
            Contact replacement = this.replacementCache.first();
            this.contacts.add(replacement);
            this.replacementCache.remove(replacement);
        }
        else
        {
            
            this.getFromContacts(c.getNode()).incrementStaleCount();
        }

        return true;
    }

    private synchronized Contact getFromContacts(NodeID n)
    {
        for (Contact c : this.contacts)
        {
            if (c.getNode().equals(n))
            {
                return c;
            }
        }

        
        throw new NoSuchElementException("The contact does not exist in the contacts list.");
    }

    public synchronized Contact removeFromContacts(NodeID n)
    {
        for (Contact c : this.contacts)
        {
            if (c.getNode().getId().equals(n.getId()))
            {
                this.contacts.remove(c);
                return c;
            }
        }

        
        throw new NoSuchElementException("Node does not exist in the replacement cache. ");
    }

    public synchronized boolean removeNode(NodeID n)
    {
        return this.removeContact(new Contact(n));
    }

    public synchronized int numContacts()
    {
        return this.contacts.size();
    }

    public synchronized int getDepth()
    {
        return this.depth;
    }

    public synchronized List<Contact> getContacts()
    {
        final ArrayList<Contact> ret = new ArrayList<>();

        
        if (this.contacts.isEmpty())
        {
            return ret;
        }

        
        for (Contact c : this.contacts)
        {
            ret.add(c);
        }

        return ret;
    }

    
    private synchronized void insertIntoReplacementCache(Contact c)
    {
        
        if (this.replacementCache.contains(c))
        {
            
            Contact tmp = this.removeFromReplacementCache(c.getNode());
            tmp.setNode(c.getNode());
            tmp.setSeenNow();
            this.replacementCache.add(tmp);
        }
        else if (this.replacementCache.size() > KadPluginConfig.getInstance().getKValue())
        {
            
            this.replacementCache.remove(this.replacementCache.last());
            this.replacementCache.add(c);
        }
        else
        {
            this.replacementCache.add(c);
        }
    }

    private synchronized Contact removeFromReplacementCache(NodeID n)
    {
        for (Contact c : this.replacementCache)
        {
            if (c.getNode().equals(n))
            {
                this.replacementCache.remove(c);
                return c;
            }
        }

        
        throw new NoSuchElementException("Node does not exist in the replacement cache. ");
    }

    public synchronized String toString()
    {
        StringBuilder sb = new StringBuilder("Bucket at depth: ");
        sb.append(this.depth);
        sb.append("\n Nodes: \n");
        for (Contact n : this.contacts)
        {
            sb.append("Node: ");
            sb.append("nodeId:" + Hex.encodeHexString(n.getNode().getId().toByteArray()) + "  localNodeEndpoint:" + n.getNode().getEndpoint());
            sb.append(" (stale: ");
            sb.append(n.staleCount());
            sb.append(")");
            sb.append("\n");
        }

        return sb.toString();
    }
}
