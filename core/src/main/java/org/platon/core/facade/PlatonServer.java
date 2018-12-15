package org.platon.core.facade;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.platon.core.config.CoreConfig;
import org.platon.core.rpc.servant.AtpGrpcServiceImpl;

import java.io.IOException;
import java.net.URISyntaxException;

public class PlatonServer {

    private static Server server;

    public static void main(String args[]) throws IOException, URISyntaxException {



        final CoreConfig config = CoreConfig.getInstance();

        PlatonImpl platon = (PlatonImpl) PlatonFactory.createPlaton();


        server = ServerBuilder
                .forPort(11001)
                .addService(new AtpGrpcServiceImpl())

                .build().start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("shutdown...");
               server.shutdown();
            }
        }));

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
