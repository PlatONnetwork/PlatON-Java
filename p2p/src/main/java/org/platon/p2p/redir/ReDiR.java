package org.platon.p2p.redir;

import com.google.protobuf.Message;
import org.platon.p2p.NodeContext;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.db.DB;
import org.platon.p2p.proto.common.ResourceID;
import org.platon.p2p.proto.redir.*;
import org.platon.p2p.pubsub.PubSub;
import org.platon.p2p.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * @author yangzhou
 * @create 2018-04-28 11:15
 */
@Component("reDiR")
public class ReDiR {

    private static Logger logger = LoggerFactory.getLogger(ReDiR.class);



    @Autowired
    PubSub pubSub;
    @Autowired
    DB db;
    @Autowired
    MessageRouter messageRouter;

    public void setPubSub(PubSub pubSub) {
        this.pubSub = pubSub;
    }

    public void setDb(DB db) {
        this.db = db;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    
    public void registerService(ServiceEntry entry, KeySelector selector) {


        ReDiRMessage.Builder redirMessageBuilder = ReDiRMessage.newBuilder();

        ResourceID resourceId = selector.getResourceID(selector.getStartLevel(), entry.getSourceKey());
        redirMessageBuilder.setEntry(entry);
        redirMessageBuilder.setResourceId(resourceId);

        ReDiRMessage reDiRMessage = redirMessageBuilder.build();

        ServiceDiscoveryUtil.storeLocalMessage(db, pubSub, reDiRMessage, System.currentTimeMillis());

        logger.debug("registerService resourceId:{}, message:{}", resourceId, reDiRMessage.toString());


        redirMessageBuilder.setEntry(entry);



        CompletableFuture<Message> fut = messageRouter.sendRequest(reDiRMessage,
                Collections.singletonList(NodeUtils.toRoutableID(resourceId)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        fut.thenAcceptAsync(futMsg -> {
            addDownRegister(futMsg, reDiRMessage, selector.getStartLevel(), selector);
        }, NodeContext.executor).exceptionally(throwable -> {
            logger.warn(throwable.getMessage());
            return null;
        });

        fut.thenAcceptAsync(futMsg -> {
            addUpRegister(futMsg, reDiRMessage,  selector.getStartLevel(), selector);
        }, NodeContext.executor).exceptionally(throwable -> {
            logger.warn(throwable.getMessage());
            return null;
        });
    }




    
    private void addUpRegister(Message msg, ReDiRMessage send, Integer level, KeySelector selector) {
        logger.trace("current level:{}", level);


        if (level == 1) {
            return;
        }

        logger.trace("up register level:{}", level-1);
        ServiceEntry entry = send.getEntry();

        List<BigInteger> sortedList = generateSortedKey(((ReDiRRespMessage) msg).getKeyList(), selector);
        BigInteger me = selector.generateKey(entry.getSourceKey());

        ReDiRMessage.Builder redirMessageBuilder = ReDiRMessage.newBuilder();
        redirMessageBuilder.setEntry(entry);



        if (sortedList.isEmpty()
                || (sortedList.get(0).compareTo(me) >= 0
                || sortedList.get(sortedList.size() - 1).compareTo(me) <= 0)
                || (level > 0 && selector.isBoundary(level, me))) {

            logger.trace("up register level:{}", level -1);
            try {

                ResourceID resourceId = selector.getResourceID(level - 1, entry.getSourceKey());

                redirMessageBuilder.setResourceId(resourceId);
                logger.trace("up register resourceid:{}", NodeUtils.getNodeIdString(resourceId.getId()));

                final ReDiRMessage reDiRMessage = redirMessageBuilder.build();

                CompletableFuture<Message> fut = messageRouter.sendRequest(reDiRMessage,
                        Collections.singletonList(NodeUtils.toRoutableID(resourceId)),
                        MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

                fut.thenAcceptAsync(futMsg -> {
                    addUpRegister(futMsg, reDiRMessage,  level-1, selector);
                }, NodeContext.executor);

            }catch (Exception e) {
                logger.warn("error:", e);
            }
        } else {
            logger.trace("isn't need up register level:{}", level-1);
        }

    }


    
    private void addDownRegister(Message msg, ReDiRMessage send,  Integer level, KeySelector selector) {
        ServiceEntry entry = send.getEntry();

        logger.trace("down register level:{} total level:{}", level, selector.getLevel());

        if (level == selector.getLevel()) {
            return;
        }

        List<BigInteger> sortedList = generateSortedKey(((ReDiRRespMessage) msg).getKeyList(), selector);
        BigInteger me = selector.generateKey(entry.getSourceKey());

        logger.trace("sortedList.size:{} get(0): me:", sortedList.size());

        ReDiRMessage.Builder redirMessageBuilder = ReDiRMessage.newBuilder();



        if ((sortedList.size() == 1 && sortedList.get(0).compareTo(me) == 0)
                || (selector.isBoundary(level + 1, me))) {

            logger.trace("down register level:{}", level + 1);
            ResourceID resourceId = selector.getResourceID(level + 1, entry.getSourceKey());

            redirMessageBuilder.setResourceId(resourceId);

            ReDiRMessage reDiRMessage = redirMessageBuilder.build();
            CompletableFuture<Message> fut = messageRouter.sendRequest(reDiRMessage,
                    Collections.singletonList(NodeUtils.toRoutableID(resourceId)),
                    MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

            fut.thenAcceptAsync(futMsg -> {
                addDownRegister(futMsg, reDiRMessage,  level+1, selector);
            }, NodeContext.executor);
        } else {
            logger.trace("isn't down register level:{}", level + 1);
        }
    }

    
    private <T> List<BigInteger> generateSortedKey(List<T> keyList, KeySelector selector) {
        List<BigInteger> sortedList = new ArrayList<>();
        if (keyList == null) {
            return sortedList;
        }

        for (T entry : keyList) {

            if (entry instanceof String) {
                sortedList.add(selector.generateKey((String) entry));
            } else if (entry instanceof ServiceEntry) {
                sortedList.add(selector.generateKey(((ServiceEntry) entry).getSourceKey()));
            } else {
                throw new RuntimeException("unknown keyList type :" + entry.getClass().getName());
            }
        }
        Collections.sort(sortedList);
        return sortedList;
    }




    
    public CompletableFuture<List<ServiceEntry>> findService(String key, KeySelector selector) {
        logger.debug("find service key:{}", selector.generateKey(key));
        CompletableFuture<List<ServiceEntry>> res = new CompletableFuture<List<ServiceEntry>>();

        ResourceID resourceId = selector.getResourceID(selector.getStartLevel(), key);

        ReDiRFindMessage.Builder reDiRFindMessageBuilder = ReDiRFindMessage.newBuilder();
        reDiRFindMessageBuilder.setResourceId(resourceId);
        reDiRFindMessageBuilder.setFindKey(key);

        ReDiRFindMessage reDiRFindMessage = reDiRFindMessageBuilder.build();


        CompletableFuture<Message> fut = messageRouter.sendRequest(reDiRFindMessage,
                Collections.singletonList(NodeUtils.toRoutableID(resourceId)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        fut.thenAcceptAsync(futMsg -> {
            findService(futMsg, reDiRFindMessage, selector.getStartLevel(), res, selector);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });

        return res;
    }



    
    private void findService(Message msg, ReDiRFindMessage send, Integer level,  CompletableFuture<List<ServiceEntry>> res, KeySelector selector) {
        String key = send.getFindKey();
        logger.debug("find service key:{} level:{}", selector.generateKey(key), level);

        ReDiRFindRespMessage serviceMsg = (ReDiRFindRespMessage) msg;



        if (serviceMsg.getEntryList() != null && !serviceMsg.getEntryList().isEmpty()) {
            logger.debug("find key:{}", key);
            res.complete(serviceMsg.getEntryList());
            return;
        }


        if (serviceMsg.getEntryList() == null || serviceMsg.getKeyList().isEmpty()) {
            logger.warn("not found resource in level:{} key:{}", level, selector.generateKey(key));
            res.completeExceptionally(new ReDiRExecption("not found resource"));
            return;
        }

        List<BigInteger> keyList = generateSortedKey(serviceMsg.getKeyList(), selector);
        BigInteger me = selector.generateKey(key);



        if (keyList.isEmpty() || (keyList.get(keyList.size() - 1).compareTo(me) < 0)) {
            if (level > selector.getStartLevel()) {
                logger.warn("not found resource in level:{} key:{}", level, key.toString());
                res.completeExceptionally(new ReDiRExecption("not found resource"));
                return;
            }
            level -= 1;

        } else if (selector.isBoundary(level + 1, me)) {
            logger.warn("not found resource in level:{} key:{}", level + 1, key.toString());
            res.completeExceptionally(new ReDiRExecption("not found resource"));
            return;
        } else {
            if (level >= selector.getLevel()) {
                logger.warn("not found resource key:{} level:{}", key.toString(), level);
                res.completeExceptionally(new ReDiRExecption("not found resource"));
                return;
            }
            level += 1;
        }

        ResourceID resourceId = selector.getResourceID(level, key);

        CompletableFuture<Message> fut = messageRouter.sendRequest(send,
                Collections.singletonList(NodeUtils.toRoutableID(resourceId)),
                MessageRouter.ForwardingOptionType.FORWARD_CONNECTION, true);

        final Integer currentLevel = level;
        fut.thenAcceptAsync(futMsg -> {
            findService(futMsg, send, currentLevel, res, selector);
        }, NodeContext.executor).exceptionally(throwable -> {
            res.completeExceptionally(throwable);
            return null;
        });

    }

}
