package com.kantoniak.greendeer.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;
import com.google.protobuf.Timestamp;
import com.kantoniak.greendeer.proto.AddRunsResponse;
import com.kantoniak.greendeer.proto.AddRunsRequest;
import com.kantoniak.greendeer.proto.GetListResponse;
import com.kantoniak.greendeer.proto.GetListRequest;
import com.kantoniak.greendeer.proto.GetStatsResponse;
import com.kantoniak.greendeer.proto.GetStatsRequest;
import com.kantoniak.greendeer.proto.Run;
import com.kantoniak.greendeer.proto.RunList;
import com.kantoniak.greendeer.proto.RunServiceGrpc;
import com.kantoniak.greendeer.proto.Stats;

import java.lang.System;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Properties;

/**
 * Server that manages startup/shutdown of a {@code RunServer} server.
 */
public class RunServer {
  private static final Logger logger = Logger.getLogger(RunServer.class.getName());

  private Server server;
  private Connection connection;

  private void start(String username, String password) throws IOException {

    this.connection = connectToDatabase(username, password);
    if (null == this.connection) {
      logger.severe("Could not connect to database. Aborting.");
      System.exit(1);
    }

    /* The port on which the server should run */
    int port = 50051;
    server = ServerBuilder.forPort(port)
        .addService(new RunServiceImpl(connection))
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          connection.close();
        } catch (SQLException e) {
          e.printStackTrace();
          logger.severe("Could not close database connection.");
        } 
        RunServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private Connection connectToDatabase(String username, String password) {
    try {
      Class.forName("org.postgresql.Driver");

      String url = "jdbc:postgresql://localhost/green-deer";
      Properties props = new Properties();
      props.setProperty("user", username);
      props.setProperty("password", password);
      props.setProperty("ssl", "false");
      return DriverManager.getConnection(url, props);
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      return null;
    }
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
    server.start(args[0], args[1]);
    server.blockUntilShutdown();
  }

  static class RunServiceImpl extends RunServiceGrpc.RunServiceImplBase {

    private final Connection connection;

    RunServiceImpl(Connection connection) {
      this.connection = connection;
    }

    @Override
    public void getList(GetListRequest req, StreamObserver<GetListResponse> responseObserver) {

      try {
        ResultSet results = connection.createStatement().executeQuery("SELECT time_created, distance, time, weight FROM runs WHERE user_id=1");
        RunList.Builder runsBuilder = RunList.newBuilder();

        while(results.next()) {
          runsBuilder.addRuns(Run.newBuilder()
            .setTimeFinished(Timestamp.newBuilder().setSeconds(results.getTimestamp("time_created").getTime() / 1000))
            .setMeters(results.getInt("distance"))
            .setTimeInSeconds(results.getInt("time"))
            .setWeight(results.getFloat("weight"))
            .build());
        }

        GetListResponse reply = GetListResponse.newBuilder()
            .setRunList(runsBuilder)
            .build();
        
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      } catch (SQLException e) {
        e.printStackTrace();
        responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
      }
      
    }

    @Override
    public void getStats(GetStatsRequest req, StreamObserver<GetStatsResponse> responseObserver) {

      try {
        Stats.Builder statsBuilder = Stats.newBuilder();

        ResultSet currentResults = connection.createStatement().executeQuery("SELECT SUM(distance) AS distance_total, MIN(weight) AS weight_min FROM runs WHERE user_id=1");
        while(currentResults.next()) {
          statsBuilder.setMetersSum(currentResults.getInt("distance_total"));
          statsBuilder.setWeightLowest(currentResults.getFloat("weight_min"));
        }

        ResultSet goalResults = connection.createStatement().executeQuery("SELECT distance, weight FROM goals WHERE user_id=1");
        while(goalResults.next()) {
          statsBuilder.setMetersGoal(goalResults.getInt("distance"));
          statsBuilder.setWeightGoal(goalResults.getFloat("weight"));
        }
        
        GetStatsResponse reply = GetStatsResponse.newBuilder().setStats(statsBuilder).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      } catch (SQLException e) {
        e.printStackTrace();
        responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
      }

    }

    @Override
    public void addRuns(AddRunsRequest req, StreamObserver<AddRunsResponse> responseObserver) {

      AddRunsResponse reply = AddRunsResponse.newBuilder()
          .addAllAddedRuns(req.getRunsToAddList())
          .build();
      
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}
