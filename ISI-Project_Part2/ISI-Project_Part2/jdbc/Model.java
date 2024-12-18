package jdbc;

import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

/*
* 
* @author MP
* @version 1.0
* @since 2024-11-07
 */
public class Model {

    static String inputData(String str) throws IOException {
        // IMPLEMENTED
        /*
         * Gets input data from user
         * 
         * @param str Description of required input values
         * 
         * @return String containing comma-separated values
         */
        Scanner key = new Scanner(System.in); // Scanner closes System.in if you call close(). Don't do it
        System.out.println("Enter corresponding values, separated by commas of:");
        System.out.println(str);
        return key.nextLine();
    }

    static void addUser(User userData, Card cardData) {
        // PARCIALLY IMPLEMENTED
        /**
         * Adds a new user with associated card to the database
         *
         * @param userData User information
         * @param cardData Card information
         * @throws SQLException if database operation fails
         */
        final String INSERT_PERSON = "INSERT INTO person(email, taxnumber, name) VALUES (?,?,?)";
        final String INSERT_CARD = "INSERT INTO card(credit, typeof, client) VALUES (?,?,?)";
        final String INSERT_USER = "INSERT INTO client(person, dtregister) VALUES (?,?)";

        try (
                Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmtPerson = conn.prepareStatement(INSERT_PERSON, Statement.RETURN_GENERATED_KEYS); PreparedStatement pstmtCard = conn.prepareStatement(INSERT_CARD); PreparedStatement pstmtUser = conn.prepareStatement(INSERT_USER);) {
            conn.setAutoCommit(false);

            // Insert person
            pstmtPerson.setString(1, userData.getEmail());
            pstmtPerson.setInt(2, userData.getTaxNumber());
            pstmtPerson.setString(3, userData.getName());

            int affectedRows = pstmtPerson.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Creating person failed, no rows affected.");
            }

            int personId;
            try (ResultSet generatedKeys = pstmtPerson.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    personId = generatedKeys.getInt(1);
                } else {
                    throw new RuntimeException("Creating person failed, no ID obtained.");
                }
            }

            // Insert User
            pstmtUser.setInt(1, personId);
            pstmtUser.setTimestamp(2, userData.getRegistrationDate());

            int affectedRowsUser = pstmtUser.executeUpdate();
            if (affectedRowsUser == 0) {
                throw new RuntimeException("Creating user failed, no rows affected.");
            }

            // Insert card
            pstmtCard.setDouble(1, cardData.getCredit());
            pstmtCard.setString(2, cardData.getReference());
            pstmtCard.setInt(3, personId);

            int affectedRowsCard = pstmtCard.executeUpdate();
            if (affectedRowsCard == 0) {
                throw new RuntimeException("Creating card failed, no rows affected.");
            }

            conn.commit();
            if (pstmtUser != null) {
                pstmtUser.close();
            }
            if (pstmtCard != null) {
                pstmtCard.close();
            }
            if (pstmtPerson != null) {
                pstmtPerson.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Error on insert values");
            // e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * To implement from this point forward. Do not need to change the code
     * above.
     * -------------------------------------------------------------------------------
     * IMPORTANT: --- DO NOT MOVE IN THE CODE ABOVE. JUST HAVE TO IMPLEMENT THE
     * METHODS BELOW ---
     * -------------------------------------------------------------------------------
     *
     */
    static void listOrders(String[] orders) {
        /**
         * Lists orders based on specified criteria
         *
         * @param orders Criteria for listing orders
         * @throws SQLException if database operation fails
         */
        final String VALUE_CMD = """
            SELECT *
            FROM replacementorder
            WHERE dtorder BETWEEN ? AND ? AND station = ?
        """;
        try (
                Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmt = conn.prepareStatement(VALUE_CMD);) {
            Date start;
            Date end;

            try {
                start = Date.valueOf(orders[0]);
                end = Date.valueOf(orders[1]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid date format. Use yyyy-mm-dd.");
            }

            int station;
            try {
                station = Integer.parseInt(orders[2]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid station ID.");
            }

            pstmt.setDate(1, start);
            pstmt.setDate(2, end);
            pstmt.setInt(3, station);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Station ID: " + rs.getInt("station"));
                System.out.println("    Order Date: " + rs.getTimestamp("dtorder"));
                System.out.println("    Occupation rate: " + rs.getInt("roccupation"));
            }

            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void travel(String[] values) {
        try {
            int operation = Integer.parseInt(values[0]);
            int station = Integer.parseInt(values[2]);
            int scooter = Integer.parseInt(values[3]);
            int clientId = getClientId(values[1]);

            if (clientId == -1) {
                throw new IllegalArgumentException("Invalid client.");
            }

            switch (operation) {
                case 1 ->
                    startTravel(clientId, scooter, station);
                case 2 ->
                    stopTravel(clientId, scooter, station);
                default ->
                    throw new IllegalArgumentException("Invalid operation. Use 1 for start and 2 for stop.");
            }
        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getClientId(String name) throws SQLException {
        /**
         * Auxiliar method -- if you want Gets client ID by name from database
         *
         * @param name The name of the client
         * @return client ID or -1 if not found
         * @throws SQLException if database operation fails
         */

        final String GET_CLIENT = "SELECT id FROM person WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmt = conn.prepareStatement(GET_CLIENT);) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                return -1;
            }
        }
    }

    public static void startTravel(int clientId, int scooterId, int stationId) throws SQLException {
        final String INSERT_TRAVEL = """
            INSERT INTO travel (dtinitial, client, scooter, stinitial)
            VALUES (?,?,?,?)
        """;

        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmtInsertTravel = conn.prepareStatement(INSERT_TRAVEL);
        ) {
            Restriction.checkValidTravel(clientId, scooterId, stationId);
            Restriction.updateCreditStart(clientId, scooterId);

            Timestamp now = new Timestamp(System.currentTimeMillis());
            pstmtInsertTravel.setTimestamp(1, now);
            pstmtInsertTravel.setInt(2, clientId);
            pstmtInsertTravel.setInt(3, scooterId);
            pstmtInsertTravel.setInt(4, stationId);

            int affectedRows = pstmtInsertTravel.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Error inserting travel.");
            }
        }
        
    }

    public static void stopTravel(int clientId, int scooterId, int stationId) throws SQLException {
        final String UPDATE_TRAVEL = """
            UPDATE travel
            SET stfinal = ?, dtfinal = ?
            WHERE client = ? AND scooter = ? AND dtfinal IS NULL
        """;
        
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmtUpdateTravel = conn.prepareStatement(UPDATE_TRAVEL);
        ) {
            Timestamp dtfinal = new Timestamp(System.currentTimeMillis());
            Restriction.updateCreditStop(clientId, scooterId, stationId, dtfinal);

            pstmtUpdateTravel.setInt(1, stationId);
            pstmtUpdateTravel.setTimestamp(2, dtfinal);
            pstmtUpdateTravel.setInt(3, clientId);
            pstmtUpdateTravel.setInt(4, scooterId);

            int affectedRows = pstmtUpdateTravel.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Error updating travel.");
            }
        }
    }

    public static void updateDocks()  {

        // get all the replacements that haven't been done yet and order them by date for each station
        final String GET_REPLACEMENTS = """
            SELECT o.dtorder, o.station, r.dtreplacement, r.action
            FROM replacementorder as o
            JOIN replacement as r
            ON o.dtorder = r.dtreporder AND o.station = r.repstation
            WHERE o.dtreplacement IS NULL
        """;

        final String UPDATE_REPLACEMENT_ORDERS = """
            UPDATE replacementorder as o
            SET dtreplacement = r.dtreplacement
            FROM (
                SELECT dtreporder, repstation, MAX(dtreplacement) as dtreplacement
                FROM replacement
                WHERE dtreplacement IS NOT NULL
                GROUP BY dtreporder, repstation
                ORDER BY dtreplacement
            ) as r
            WHERE r.dtreplacement IS NOT NULL AND r.dtreporder = o.dtorder AND r.repstation = o.station
        """;

        final String GET_DOCK = """
            SELECT *
            FROM dock
            WHERE station = ? AND state = ?
        """;

        final String GET_FREE_SCOOTERS = """
            SELECT *
            FROM scooter
            WHERE id NOT IN (
                SELECT scooter
                FROM dock d
                WHERE d.scooter IS NOT NULL
            )
        """;

        final String UPDATE_DOCK = """
            UPDATE dock
            SET state = ?, scooter = ?
            WHERE number = ?
        """;

        final String GET_TRAVELS = """
            SELECT *
            FROM travel
        """;

        final String GET_DOCK_BY_SCOOTER = """
            SELECT scooter, number
            FROM dock
            WHERE state = ? AND station = ? AND scooter = ?
        """;

        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmtGetReplacements = conn.prepareStatement(GET_REPLACEMENTS);
            PreparedStatement pstmtGetDock = conn.prepareStatement(GET_DOCK);
            PreparedStatement pstmtGetFreeScooters = conn.prepareStatement(GET_FREE_SCOOTERS);
            PreparedStatement pstmtGetTravels = conn.prepareStatement(GET_TRAVELS);
            PreparedStatement pstmtUpdateDock = conn.prepareStatement(UPDATE_DOCK);
            PreparedStatement pstmtGetDockByScooter = conn.prepareStatement(GET_DOCK_BY_SCOOTER);
            PreparedStatement pstmtUpdateReplacementOrders = conn.prepareStatement(UPDATE_REPLACEMENT_ORDERS);
        ) {
            conn.setAutoCommit(false);

            ResultSet rsReplacements = pstmtGetReplacements.executeQuery();
            while (rsReplacements.next()) {
                int station = rsReplacements.getInt("station");
                String action = rsReplacements.getString("action");

                if (action.equals("inplace")) {
                    pstmtGetDock.setInt(1, station);
                    pstmtGetDock.setString(2, "free");

                    ResultSet rsDock = pstmtGetDock.executeQuery();
                    if (!rsDock.next()) {
                        throw new IllegalArgumentException("No free docks available.");
                    }

                    ResultSet rsFreeScooters = pstmtGetFreeScooters.executeQuery();
                    if (!rsFreeScooters.next()) {
                        throw new IllegalArgumentException("No free scooters available.");
                    }

                    int dockId = rsDock.getInt("number");
                    int scooterId = rsFreeScooters.getInt("id");

                    pstmtUpdateDock.setString(1, "occupy");
                    pstmtUpdateDock.setInt(2, scooterId);
                    pstmtUpdateDock.setInt(3, dockId);

                    int affectedRows = pstmtUpdateDock.executeUpdate();
                    if (affectedRows == 0) {
                        throw new IllegalArgumentException("Error updating dock.");
                    }
                } else {
                    pstmtGetDock.setInt(1, station);
                    pstmtGetDock.setString(2, "occupy");

                    ResultSet rsDock = pstmtGetDock.executeQuery();
                    if (!rsDock.next()) {
                        throw new IllegalArgumentException("No occupied docks available.");
                    }

                    int dockId = rsDock.getInt("number");

                    pstmtUpdateDock.setString(1, "free");
                    pstmtUpdateDock.setNull(2, Types.INTEGER);
                    pstmtUpdateDock.setInt(3, dockId);

                    int affectedRows = pstmtUpdateDock.executeUpdate();
                    if (affectedRows == 0) {
                        throw new IllegalArgumentException("Error updating dock.");
                    }
                }
            }

            int affectedRows = pstmtUpdateReplacementOrders.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Error updating replacement orders.");
            }

            ResultSet rsTravels = pstmtGetTravels.executeQuery();
            while(rsTravels.next()) {
                int scooter = rsTravels.getInt("scooter");
                int stfinal = rsTravels.getInt("stfinal");
                
                if (!rsTravels.wasNull()) {
                    pstmtGetDockByScooter.setString(1, "occupy");
                    pstmtGetDockByScooter.setInt(2, stfinal);
                    pstmtGetDockByScooter.setInt(3, scooter);

                    ResultSet rsScooterDock = pstmtGetDockByScooter.executeQuery();
                    if (!rsScooterDock.next()) {
                        pstmtGetDock.setInt(1, stfinal);
                        pstmtGetDock.setString(2, "free");

                        ResultSet rsDock = pstmtGetDock.executeQuery();
                        if (!rsDock.next()) {
                            throw new IllegalArgumentException("No free docks available.");
                        }

                        int dockId = rsDock.getInt("number");

                        pstmtUpdateDock.setString(1, "occupy");
                        pstmtUpdateDock.setInt(2, scooter);
                        pstmtUpdateDock.setInt(3, dockId);

                        int affectedRowsDock = pstmtUpdateDock.executeUpdate();
                        if (affectedRowsDock == 0) {
                            throw new IllegalArgumentException("Error updating dock.");
                        }
                    }
                }
                
                int stinitial = rsTravels.getInt("stinitial");

                pstmtGetDockByScooter.setString(1, "occupy");
                pstmtGetDockByScooter.setInt(2, stinitial);
                pstmtGetDockByScooter.setInt(3, scooter);

                ResultSet rsScooterDock = pstmtGetDockByScooter.executeQuery();
                if (rsScooterDock.next()) {
                    int dockId = rsScooterDock.getInt("number");

                    pstmtUpdateDock.setString(1, "free");
                    pstmtUpdateDock.setNull(2, Types.INTEGER);
                    pstmtUpdateDock.setInt(3, dockId);

                    int affectedRowsDock = pstmtUpdateDock.executeUpdate();
                    if (affectedRowsDock == 0) {
                        throw new IllegalArgumentException("Error updating dock.");
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void userSatisfaction(String client) {
        final String STATEMENT = """
            SELECT 
                s.model,
                AVG(t.evaluation) avg_rating,
                COUNT(*) num_ratings,
                COUNT(CASE WHEN t.evaluation >= 4 THEN 1 ELSE NULL END) / COUNT(*) * 100 satisfaction_rating
            FROM travel t, scooter s
            WHERE t.client = ? AND t.scooter = s.id AND t.evaluation IS NOT NULL
            GROUP BY s.model
            ORDER BY avg_rating DESC
        """;

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmt = conn.prepareStatement(STATEMENT);) {
            int clientId = getClientId(client);

            if (clientId == -1) {
                throw new IllegalArgumentException("Invalid client.");
            }

            pstmt.setInt(1, clientId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Model: " + rs.getString("model"));
                System.out.println("    Average rating: " + rs.getDouble("avg_rating"));
                System.out.println("    Number of ratings: " + rs.getInt("num_ratings"));
                System.out.println("    Satisfaction rating: " + rs.getDouble("satisfaction_rating") + "%");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void occupationStation() {
        String STATEMENT = """
            SELECT
                s.id,
                COUNT(CASE WHEN d.state = 'occupy' THEN 1 ELSE NULL END) * 100 / COUNT(*) occupation_rate
            FROM dock d, station s
            WHERE d.station = s.id
            GROUP BY s.id
            ORDER BY occupation_rate DESC
        """;

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmt = conn.prepareStatement(STATEMENT);) {
            pstmt.setMaxRows(3);
            ResultSet rs = pstmt.executeQuery();
            // print the three rows with the highest occupation rate
            while (rs.next()) {
                System.out.println("Station ID: " + rs.getInt("id"));
                System.out.println("    Occupation rate: " + rs.getDouble("occupation_rate") + "%");
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }
}