package org.platon.p2p.plugins;

import org.platon.p2p.db.DB;
import org.platon.p2p.db.DBException;
import org.platon.p2p.proto.common.ResourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yangzhou
 * @create 2018-07-23 10:25
 */
public class ResourceMap {


    @Autowired
    DB db;
    private static Logger logger = LoggerFactory.getLogger(ResourceMap.class);


    private final Set<ResourceID> resourceCache = new HashSet<>();



    public void setDb(DB db) {
        this.db = db;
    }

    public boolean isExist(ResourceID resourceID) {
        if (resourceCache.contains(resourceID)) {
           return true;
        }


        try {

            if (db.get(resourceID.getId().toByteArray()) != null) {
                return true;
            }
            if (db.hgetAll(resourceID.getId().toByteArray()) != null) {
                return true;
            }
        } catch (DBException e) {
           logger.error(e.getMessage());
            return false;
        }
        return false;
    }
    public void add(ResourceID resourceID) {
        resourceCache.add(resourceID);
    }

    public boolean remove(ResourceID resourceID) {
        return resourceCache.remove(resourceID);
    }

}
