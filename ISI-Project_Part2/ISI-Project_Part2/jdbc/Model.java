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
        final String INSERT_PERSON = "INSERT INTO person(email, taxnumber, name) VALUES (?,?,?) RETURNING id";
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
                // Print other order details as needed
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
        /**
         * Processes a travel operation (start or stop)
         *
         * @param values Array containing [operation, name, station, scooter]
         * @throws SQLException if database operation fails
         */
        // TO BE DONE

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
        /**
         * Starts a new travel
         *
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Station ID
         * @throws SQLException if database operation fails
         */

        final String GET_DOCK = "SELECT * FROM dock WHERE scooter = ? AND station = ?";
        final String GET_TRAVEL = "SELECT * FROM travel WHERE client = ? AND dtfinal IS NULL";
        final String INSERT_TRAVEL = "INSERT INTO travel(dtinitial, client, scooter, stinitial) VALUES (?,?,?,?)";
        final String UPDATE_CARD = "UPDATE card SET credit = credit - (SELECT unlock FROM servicecost) WHERE client = ?";

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmtGetDock = conn.prepareStatement(GET_DOCK); PreparedStatement pstmtGetTravel = conn.prepareStatement(GET_TRAVEL); PreparedStatement pstmtInsertTravel = conn.prepareStatement(INSERT_TRAVEL); PreparedStatement pstmtUpdateCard = conn.prepareStatement(UPDATE_CARD);) {
            conn.setAutoCommit(false);

            pstmtGetTravel.setInt(1, clientId);

            ResultSet rsGetTravel = pstmtGetTravel.executeQuery();
            if (rsGetTravel.next()) {
                throw new IllegalArgumentException("Client already has an ongoing travel.");
            }

            pstmtGetDock.setInt(1, scooterId);
            pstmtGetDock.setInt(2, stationId);

            ResultSet rsDock = pstmtGetDock.executeQuery();
            if (!rsDock.next()) {
                throw new IllegalArgumentException("Scooter not found in dock.");
            }

            if (rsDock.getString(2) != "occupy") {
                throw new IllegalArgumentException("Scooter is not available.");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            pstmtInsertTravel.setTimestamp(1, now);
            pstmtInsertTravel.setInt(2, clientId);
            pstmtInsertTravel.setInt(3, scooterId);
            pstmtInsertTravel.setInt(4, stationId);

            ResultSet rsInsert = pstmtInsertTravel.executeQuery();
            if (rsInsert.next()) {
                throw new IllegalArgumentException("Error inserting travel.");
            }

            pstmtUpdateCard.setInt(1, clientId);

            int affectedRowsCard = pstmtUpdateCard.executeUpdate();
            if (affectedRowsCard == 0) {
                throw new IllegalArgumentException("Error updating card.");
            }

            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    public static void stopTravel(int clientId, int scooterId, int stationId) throws SQLException {
        /**
         * Stops an ongoing travel
         *
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Destination station ID
         * @throws SQLException if database operation fails
         */

        final String GET_TRAVEL = "SELECT * FROM travel WHERE client = ? AND stfinal IS NULL";
        final String GET_DOCK = "SELECT * FROM dock WHERE station = ? AND state = ?";
        final String UPDATE_TRAVEL = "UPDATE travel SET stfinal = ? WHERE dtinitial = ?";
        final String UPDATE_CARD = "UPDATE card SET credit = credit - (? * (SELECT usable FROM servicecost)) WHERE client = ?";

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmtGetTravel = conn.prepareStatement(GET_TRAVEL); PreparedStatement pstmtGetDock = conn.prepareStatement(GET_DOCK); PreparedStatement pstmtUpdateTravel = conn.prepareStatement(UPDATE_TRAVEL); PreparedStatement pstmtUpdateCard = conn.prepareStatement(UPDATE_CARD)) {

            conn.setAutoCommit(false);

            //Check if there is a travel
            pstmtGetTravel.setInt(1, clientId);

            ResultSet rsGetTravel = pstmtGetTravel.executeQuery();
            if (!rsGetTravel.next()) {
                throw new IllegalArgumentException("Client does not have an ongoing travel.");
            }

            pstmtGetDock.setInt(1, stationId);
            pstmtGetDock.setString(2, "free");

            ResultSet rsDock = pstmtGetDock.executeQuery();
            if (!rsDock.next()) {
                throw new IllegalArgumentException("Station is not available.");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            pstmtUpdateTravel.setTimestamp(1, now);
            pstmtUpdateTravel.setInt(2, stationId);
            pstmtUpdateTravel.setTimestamp(3, rsGetTravel.getTimestamp(1));

            int affectedRowsTravel = pstmtUpdateTravel.executeUpdate();
            if (affectedRowsTravel == 0) {
                throw new IllegalArgumentException("Error updating travel.");
            }

            long ms = rsGetTravel.getTimestamp(1).getTime() - now.getTime();
            long minutes = ms / 60000;

            pstmtUpdateCard.setLong(1, minutes);

            int affectedRowsCard = pstmtUpdateCard.executeUpdate();
            if (affectedRowsCard == 0) {
                throw new IllegalArgumentException("Error updating card.");
            }

            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    public static void updateDocks() {

        final String GET_REPLACEMENT_ORDERS = """
            SELECT *
            FROM replacementorder
        """;
        final String GET_REPLACEMENT = """
            SELECT *
            FROM replacement
            WHERE dtreporder = ? and repstation = ? and dtreplacement > ? or ? IS NULL
            ORDER BY dtreplacement
        """;
        final String GET_FREE_DOCK = """
            SELECT *
            FROM dock
            WHERE station = ? AND state = 'free'
        """;
        final String GET_OCCUPIED_DOCK = """
            SELECT *
            FROM dock
            WHERE station = ? AND state = 'occupy'
        """;
        final String GET_FREE_SCOOTERS = """
            SELECT *
            FROM scooter
            WHERE id NOT IN
                (SELECT scooter
                FROM dock
                WHERE state = 'occupy')
        """;
        final String UPDATE_DOCK = """
            UPDATE dock
            SET state = ?, scooter = ?
            WHERE id = ?
        """;
        final String UPDATE_REPLACEMENTORDER = """
            UPDATE replacementorder
            SET dtreplacement = ?
            WHERE number = ? and station = ?
        """;
        final String GET_TRAVELS = """
            SELECT *
            FROM travel
            WHERE dtfinal IS NULL
        """;
        final String UPDATE_DOCK_BY_SCOOTER = """
            UPDATE dock
            SET state = ?, scooter = ?
            WHERE station = ? AND scooter = ?
        """;
        final String UPDATE_TRAVEL = """
            UPDATE travel
            SET dtfinal = ?
            WHERE dtinitial = ?
        """;

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmtGetFreeScooters = conn.prepareStatement(GET_FREE_SCOOTERS); PreparedStatement pstmtGetReplacementOrders = conn.prepareStatement(GET_REPLACEMENT_ORDERS); PreparedStatement pstmtGetReplacement = conn.prepareStatement(GET_REPLACEMENT); PreparedStatement pstmtGetFreeDock = conn.prepareStatement(GET_FREE_DOCK); PreparedStatement pstmtUpdateDock = conn.prepareStatement(UPDATE_DOCK); PreparedStatement pstmtGetOccupiedDock = conn.prepareStatement(GET_OCCUPIED_DOCK); PreparedStatement pstmtGetTravels = conn.prepareStatement(GET_TRAVELS); PreparedStatement pstmtUpdateReplacementOrder = conn.prepareStatement(UPDATE_REPLACEMENTORDER); PreparedStatement pstmtUpdateDockByScooter = conn.prepareStatement(UPDATE_DOCK_BY_SCOOTER); PreparedStatement pstmUpdateTravel = conn.prepareStatement(UPDATE_TRAVEL)) {

            ResultSet rsOrders = pstmtGetReplacementOrders.executeQuery();
            while (rsOrders.next()) {
                Timestamp dtorder = rsOrders.getTimestamp("dtorder");
                Timestamp dtreplacement = rsOrders.getTimestamp("dtreplacement");
                int station = rsOrders.getInt("station");

                pstmtGetReplacement.setTimestamp(1, dtorder);
                pstmtGetReplacement.setInt(2, station);
                pstmtGetReplacement.setTimestamp(3, dtreplacement);
                if (dtreplacement == null) {
                    pstmtGetReplacement.setNull(4, Types.TIMESTAMP);
                } else {
                    pstmtGetReplacement.setTimestamp(4, dtreplacement);
                }

                ResultSet rsReplacement = pstmtGetReplacement.executeQuery();
                Timestamp lastReplacement = null;
                while (rsReplacement.next()) {
                    String action = rsReplacement.getString("action");

                    if (action.equals("inplace")) {
                        pstmtGetFreeDock.setInt(1, station);

                        ResultSet rsDock = pstmtGetFreeDock.executeQuery();
                        if (rsDock.next()) {
                            ResultSet rsFreeScooter = pstmtGetFreeScooters.executeQuery();
                            if (!rsFreeScooter.next()) {
                                throw new IllegalArgumentException("No free scooters available. Create a new scooter in order to add it to the dock.");
                            }

                            int id = rsDock.getInt("id");
                            int scooter = rsFreeScooter.getInt("id");

                            pstmtUpdateDock.setString(1, "occupy");
                            pstmtUpdateDock.setInt(2, scooter);
                            pstmtUpdateDock.setInt(3, id);

                            int affectedRows = pstmtUpdateDock.executeUpdate();
                            if (affectedRows == 0) {
                                throw new IllegalArgumentException("Error updating dock.");
                            }
                        }

                    } else {
                        pstmtGetOccupiedDock.setInt(1, station);

                        ResultSet rsDock = pstmtGetOccupiedDock.executeQuery();
                        if (rsDock.next()) {
                            int id = rsDock.getInt("id");

                            pstmtUpdateDock.setString(1, "free");
                            pstmtUpdateDock.setNull(2, Types.INTEGER);
                            pstmtUpdateDock.setInt(3, id);

                            int affectedRows = pstmtUpdateDock.executeUpdate();
                            if (affectedRows == 0) {
                                throw new IllegalArgumentException("Error updating dock.");
                            }
                        }
                    }

                    lastReplacement = rsReplacement.getTimestamp("dtreplacement");
                }

                if (lastReplacement == null) {
                    continue;
                }

                pstmtUpdateReplacementOrder.setTimestamp(1, lastReplacement);
                pstmtUpdateReplacementOrder.setInt(2, rsOrders.getInt("number"));
                pstmtUpdateReplacementOrder.setInt(3, station);

                int affectedRows = pstmtUpdateReplacementOrder.executeUpdate();
                if (affectedRows == 0) {
                    throw new IllegalArgumentException("Error updating replacement order.");
                }
            }

            ResultSet rsGetTravels = pstmtGetTravels.executeQuery();
            while (rsGetTravels.next()) {
                int scooter = rsGetTravels.getInt("scooter");
                int stfinal = rsGetTravels.getInt("stfinal");

                if (rsGetTravels.wasNull()) {
                    int stinitial = rsGetTravels.getInt("stinitial");

                    pstmtUpdateDockByScooter.setString(1, "free");
                    pstmtUpdateDockByScooter.setNull(2, Types.INTEGER);
                    pstmtUpdateDockByScooter.setInt(3, stinitial);
                    pstmtUpdateDockByScooter.setInt(4, scooter);

                    ResultSet rsDock = pstmtUpdateDockByScooter.executeQuery();
                    if (rsDock.next()) {
                        throw new IllegalArgumentException("Error updating dock.");
                    }
                } else {
                    pstmtGetFreeDock.setInt(1, stfinal);

                    ResultSet rsDock = pstmtGetFreeDock.executeQuery();
                    if (!rsDock.next()) {
                        throw new IllegalArgumentException("No free docks available.");
                    }

                    int id = rsDock.getInt("id");

                    pstmtUpdateDock.setString(1, "occupy");
                    pstmtUpdateDock.setInt(2, scooter);
                    pstmtUpdateDock.setInt(3, id);

                    int affectedRows = pstmtUpdateDock.executeUpdate();
                    if (affectedRows == 0) {
                        throw new IllegalArgumentException("Error updating dock.");
                    }

                    Timestamp dtinitial = rsGetTravels.getTimestamp("dtinitial");

                    pstmUpdateTravel.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    pstmUpdateTravel.setTimestamp(2, dtinitial);

                    int affectedRowsTravel = pstmUpdateTravel.executeUpdate();
                    if (affectedRowsTravel == 0) {
                        throw new IllegalArgumentException("Error updating travel.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void userSatisfaction(String client) {
        final String STATEMENT = """
            SELECT 
                s.model,
                AVG(t.rating) avg_rating,
                COUNT(*) num_ratings,
                COUNT(CASE WHEN t.rating >= 4 THEN 1 ELSE NULL END) / COUNT(*) * 100 satisfaction_rating
            FROM travel t, scooter s
            WHERE t.client = ? AND t.scooter = s.id
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
                COUNT(CASE WHEN d.state = 'occupy' THEN 1 ELSE NULL END) / COUNT(*) * 100 occupation_rate
            FROM dock d, station s
            WHERE d.station = s.id
            GROUP BY s.id
            ORDER BY occupation_rate DESC
        """;

        try (Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString()); PreparedStatement pstmt = conn.prepareStatement(STATEMENT);) {
            ResultSet rs = pstmt.executeQuery();
            // print the three rows with the highest occupation rate
            while (rs.next() && rs.getRow() <= 3) {
                System.out.println("Station ID: " + rs.getInt("id"));
                System.out.println("    Occupation rate: " + rs.getDouble("occupation_rate") + "%");
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }
}
