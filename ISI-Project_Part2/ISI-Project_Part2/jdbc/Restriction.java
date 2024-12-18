package jdbc;

import java.sql.*;

public class Restriction {

  public static void checkValidTravel(int client, int scooter, int station) throws SQLException {
    // verify if the scooter is in the dock and available
    final String GET_SCOOTER = """
      SELECT *
      FROM dock
      WHERE scooter = ? AND station = ? AND state = 'occupy'
    """;

    //check if client has a travel in progress
    final String GET_TRAVEL = """
      SELECT *
      FROM travel
      WHERE client = ? AND dtfinal IS NULL
    """;

    try (
      Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
      PreparedStatement pstmtGetScooter = conn.prepareStatement(GET_SCOOTER);
      PreparedStatement pstmtGetTravel = conn.prepareStatement(GET_TRAVEL)
    ) {
      pstmtGetScooter.setInt(1, scooter);
      pstmtGetScooter.setInt(2, station);

      ResultSet rsGetScooter = pstmtGetScooter.executeQuery();
      if (!rsGetScooter.next()) {
        throw new SQLException("Scooter is not in the dock or is not available");
      }

      pstmtGetTravel.setInt(1, client);
      ResultSet rsGetTravel = pstmtGetTravel.executeQuery();
      if (rsGetTravel.next()) {
        throw new SQLException("Client already has a travel in progress");
      }
    }
  };

  public static void updateCreditStart(int client, int fstation) throws SQLException {

    // get the client's credit
    final String GET_CREDIT = """
      SELECT credit
      FROM card
      WHERE client = ?
    """;

    // get the cost of unlocking the scooter
    final String GET_COST = """
      SELECT unlock
      FROM servicecost
    """;

    // update the credit
    final String UPDATE_CREDIT = """
      UPDATE card
      SET credit = ?
      WHERE client = ?
    """;

    try (
      Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
      PreparedStatement pstmtGetCredit = conn.prepareStatement(GET_CREDIT);
      PreparedStatement pstmtGetCost = conn.prepareStatement(GET_COST);
      PreparedStatement pstmtUpdateCredit = conn.prepareStatement(UPDATE_CREDIT)
    ) {
      conn.setAutoCommit(false);

      pstmtGetCredit.setInt(1, client);

      ResultSet rsGetCredit = pstmtGetCredit.executeQuery();
      if (!rsGetCredit.next()) {
        throw new SQLException("Client does not have a card");
      }

      int credit = rsGetCredit.getInt("credit");

      ResultSet rsGetCost = pstmtGetCost.executeQuery();
      if (!rsGetCost.next()) {
        throw new SQLException("Service cost not found");
      }

      int cost = rsGetCost.getInt("unlock");

      if (credit < cost) {
        throw new SQLException("Insufficient credit");
      }

      pstmtUpdateCredit.setInt(1, credit - cost);
      pstmtUpdateCredit.setInt(2, client);

      int affectedRowsCredit = pstmtUpdateCredit.executeUpdate();
      if (affectedRowsCredit == 0) {
        throw new SQLException("Failed to update credit");
      }

      conn.commit();
      conn.setAutoCommit(true);
    }
  };

  public static void updateCreditStop(int client, int scooter, int stfinal, Timestamp dtfinal) throws SQLException {
  
    // verify if travel exists
    final String GET_TRAVEL = """
      SELECT *
      FROM travel
      WHERE client = ? AND dtfinal IS NULL
    """;

    // get the cost of using the scooter
    final String GET_COST = """
      SELECT usable
      FROM servicecost
    """;

    // update the credit
    final String UPDATE_CREDIT = """
      UPDATE card
      SET credit = CASE WHEN credit < ? THEN 0 ELSE credit - ? END
      WHERE client = ?
    """;

    try (
      Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
      PreparedStatement pstmtGetTravel = conn.prepareStatement(GET_TRAVEL);
      PreparedStatement pstmtGetCost = conn.prepareStatement(GET_COST);
      PreparedStatement pstmtUpdateCredit = conn.prepareStatement(UPDATE_CREDIT)
    ) {
      conn.setAutoCommit(false);

      pstmtGetTravel.setInt(1, client);

      ResultSet rsGetTravel = pstmtGetTravel.executeQuery();
      if (!rsGetTravel.next()) {
        throw new SQLException("Travel not found");
      }

      ResultSet rsGetCost = pstmtGetCost.executeQuery();
      if (!rsGetCost.next()) {
        throw new SQLException("Service cost not found");
      }

      Timestamp dtinitial = rsGetTravel.getTimestamp("dtinitial");

      //get the time difference in minutes
      long diff = (dtfinal.getTime() - dtinitial.getTime()) / 60000;
      long cost = rsGetCost.getInt("usable") * diff;

      pstmtUpdateCredit.setLong(1, cost);
      pstmtUpdateCredit.setLong(2, cost);
      pstmtUpdateCredit.setInt(3, client);

      int affectedRowsCredit = pstmtUpdateCredit.executeUpdate();
      if (affectedRowsCredit == 0) {
        throw new SQLException("Failed to update credit");
      }

      conn.commit();
      conn.setAutoCommit(true);
    }
  }

}