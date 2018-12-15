package org.platon.p2p.handler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.platon.common.utils.ByteUtil;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.attach.LinkService;
import org.platon.p2p.proto.attach.AttachMessage;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Body;
import org.platon.p2p.proto.platon.Header;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author lvxy
 * @version 0.0.1
 * @date 2018/8/30 17:16
 */
public class PlatonMessageHandlerContext {

    private static final Logger logger = LoggerFactory.getLogger(PlatonMessageHandlerContext.class);

    private static Map<String, String> beanMap = new HashMap<>();
    private static Map<String, Method> methodMap = new HashMap<>();

    public Object getHandler(String messageName){
        return SpringContextUtil.getBean(beanMap.get(messageName));
    }

    public Method getMethod(String messageName){
        return methodMap.get(messageName);
    }

    private PlatonMessageHandlerContext() {
        Reflections reflections =new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.platon.p2p.handler"))
                .setScanners(new MethodAnnotationsScanner()));

        Set<Method> methods = reflections.getMethodsAnnotatedWith(PlatonMessageType.class);

        for (Method method : methods) {
            String className = method.getDeclaringClass().getName();
            String classSimpleName = method.getDeclaringClass().getSimpleName();

            PlatonMessageType platonMessageType = method.getAnnotation(PlatonMessageType.class);

            Component component = method.getDeclaringClass().getAnnotation(Component.class);
            if(component==null){
                logger.warn("Ignore the annotation:{} in {}", platonMessageType.value(), className);
                continue;
            }else{
                methodMap.put(platonMessageType.value(), method);

                beanMap.put(platonMessageType.value(), StringUtils.isBlank(component.value())? WordUtils.uncapitalize(classSimpleName) : component.value());
            }
        }
    }

    public static PlatonMessageHandlerContext getInstance() {
        return PlatonMessageHandlerContext.SingletonContainer.instance;
    }

    private static class SingletonContainer {
        private static PlatonMessageHandlerContext instance = new PlatonMessageHandlerContext();
    }



    public static void main(String[] args){


        RoutableID destID = RoutableID.newBuilder().setId(ByteString.copyFromUtf8("1122")).setType(RoutableID.DestinationType.NODEIDTYPE).build();
        NodeID viaID = NodeID.newBuilder().setId(ByteString.copyFromUtf8("3344")).build();

        Header header = Header.newBuilder().setTxId("txId").setTtl(10).addDest(destID).addVia(viaID).setMsgType("Ping").build();

        AttachMessage attachMessage = AttachMessage.newBuilder().setNodeId(viaID).build();

        Body message = Body.newBuilder().setData(Any.pack(attachMessage)).build();

        PlatonMessage platonMessage = PlatonMessage.newBuilder().setHeader(header).setBody(message).build();


        Any any =  platonMessage.getBody().getData();




        try {
            Class clz = Class.forName("org.platon.p2p.proto.attach.AttachMessage");

            Object msg = any.unpack(clz);

            Object test = new LinkService ();
            Method method = PlatonMessageHandlerContext.getInstance().getMethod("AttachMessage");

            Integer result =  (Integer)method.invoke(test, header, msg);
            System.out.println("result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] a = ByteUtil.hostToBytes("127.0.0.1");
        byte[] b = ByteUtil.hostToBytes("127.0.1.1");

        System.out.println( ByteUtils.equals(a, b));

    }

}
