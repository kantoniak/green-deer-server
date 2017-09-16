package com.kantoniak.greendeer.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;
import com.google.protobuf.Timestamp;
import com.kantoniak.greendeer.proto.Run;
import com.kantoniak.greendeer.proto.RunList;
import com.kantoniak.greendeer.proto.RunServiceGrpc;
import com.kantoniak.greendeer.proto.GetListResponse;
import com.kantoniak.greendeer.proto.GetListRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * Server that manages startup/shutdown of a {@code RunServer} server.
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

    private static final SimpleDateFormat datetimeFormatter = new SimpleDateFormat("dd.MM.yyyy");

    private Run createRun(int meters, int seconds, String date, float weight) {

      long timeFinishedAsSeconds = -1;
      try {
        timeFinishedAsSeconds = datetimeFormatter.parse(date).getTime() / 1000;
      } catch (ParseException e) {
        // Ignore
      }

      return Run.newBuilder()
        .setMeters(meters).setTimeInSeconds(seconds)
        .setTimeFinished(Timestamp.newBuilder().setSeconds(timeFinishedAsSeconds))
        .setWeight(weight)
        .build();

    }

    @Override
    public void getList(GetListRequest req, StreamObserver<GetListResponse> responseObserver) {

      SimpleDateFormat dtFormat = new SimpleDateFormat("dd.MM.yyyy");

      GetListResponse reply = GetListResponse.newBuilder()
          .setRunList(RunList.newBuilder().addAllRuns(Arrays.asList(
                  createRun(7000, 2620, "31.07.2017", 84.7f),
                  createRun(3100, 991, "02.08.2017", 83.1f),
                  createRun(3100, 921, "04.08.2017", 82.7f)
              )).build())
          .build();
      
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}
