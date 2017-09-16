package com.kantoniak.greendeer.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;
import com.kantoniak.greendeer.proto.Run;
import com.kantoniak.greendeer.proto.RunList;
import com.kantoniak.greendeer.proto.RunServiceGrpc;
import com.kantoniak.greendeer.proto.GetListResponse;
import com.kantoniak.greendeer.proto.GetListRequest;

import java.util.Arrays;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class RunServer {
  private static final Logger logger = Logger.getLogger(RunServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 50051;
    server = ServerBuilder.forPort(port)
        .addService(new RunServiceImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        RunServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final RunServer server = new RunServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class RunServiceImpl extends RunServiceGrpc.RunServiceImplBase {

    @Override
    public void getList(GetListRequest req, StreamObserver<GetListResponse> responseObserver) {
      logger.info("Server received getList() call");

      GetListResponse reply = GetListResponse.newBuilder()
          .setRunList(RunList.newBuilder().addAllRuns(Arrays.asList(
                  Run.newBuilder().setMeters(7000).setTimeInSeconds(2620).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(991).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(921).build(),
                  Run.newBuilder().setMeters(7000).setTimeInSeconds(2620).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(991).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(921).build(),
                  Run.newBuilder().setMeters(7000).setTimeInSeconds(2620).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(991).build(),
                  Run.newBuilder().setMeters(3100).setTimeInSeconds(921).build())
              ).build())
          .build();
      
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}
