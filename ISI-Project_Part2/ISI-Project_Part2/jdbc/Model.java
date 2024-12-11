package jdbc;

import java.util.Scanner;
import java.io.IOException;
import java.sql.*;

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
                Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
                PreparedStatement pstmtPerson = conn.prepareStatement(INSERT_PERSON, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement pstmtCard = conn.prepareStatement(INSERT_CARD);
                PreparedStatement pstmtUser = conn.prepareStatement(INSERT_USER);) {
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
            
            
            
            // CONTINUE




            conn.commit();
            if (pstmtUser != null)
                pstmtUser.close();
            if (pstmtCard != null)
                pstmtCard.close();
            if (pstmtPerson != null)
                pstmtPerson.close();
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
     * To implement from this point forward. Do not need to change the code above.
     * -------------------------------------------------------------------------------
     * IMPORTANT:
     * --- DO NOT MOVE IN THE CODE ABOVE. JUST HAVE TO IMPLEMENT THE METHODS BELOW
     * ---
     * -------------------------------------------------------------------------------
     **/

    static void listOrders(String[] orders) {
       /**
         * Lists orders based on specified criteria
         * 
         * @param orders Criteria for listing orders
         * @throws SQLException if database operation fails
         */
        final String VALUE_CMD = "SELECT * FROM replacementorder WHERE time_interval = ? AND station_number = ?";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(VALUE_CMD);
        ) {
            pstmt.setString(1, orders[0]);
            pstmt.setString(2, orders[1]);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Order ID: " + rs.getInt("id"));
                // Print other order details as needed
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public static void listReplacementOrders(int stationId, Timestamp startDate, Timestamp endDate) throws SQLException {
         /**
         * Lists replacement orders for a specific station in a given time period
         * @param stationId Station ID
         * @param startDate Start date for period
         * @param endDate End date for period
         * @throws SQLException if database operation fails
         */
        final String QUERY = "SELECT * FROM replacement_orders WHERE station_id = ? AND order_date BETWEEN ? AND ?";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(QUERY);
        ) {
            pstmt.setInt(1, stationId);
            pstmt.setTimestamp(2, startDate);
            pstmt.setTimestamp(3, endDate);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Order ID: " + rs.getInt("id"));
                // Print other order details as needed
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void travel(String[] values){
            /**
         * Processes a travel operation (start or stop)
         * @param values Array containing [operation, name, station, scooter]
         * @throws SQLException if database operation fails
         */
        final String START_TRAVEL = "INSERT INTO travels (client_id, scooter_id, station_id, start_time) VALUES (?, ?, ?, ?)";
        final String STOP_TRAVEL = "UPDATE travels SET end_time = ? WHERE client_id = ? AND scooter_id = ? AND station_id = ? AND end_time IS NULL";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
        ) {
            conn.setAutoCommit(false);
            if (values[0].equalsIgnoreCase("start")) {
                try (PreparedStatement pstmt = conn.prepareStatement(START_TRAVEL)) {
                    pstmt.setInt(1, getClientId(values[1]));
                    pstmt.setInt(2, Integer.parseInt(values[3]));
                    pstmt.setInt(3, Integer.parseInt(values[2]));
                    pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    pstmt.executeUpdate();
                }
            } else if (values[0].equalsIgnoreCase("stop")) {
                try (PreparedStatement pstmt = conn.prepareStatement(STOP_TRAVEL)) {
                    pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    pstmt.setInt(2, getClientId(values[1]));
                    pstmt.setInt(3, Integer.parseInt(values[3]));
                    pstmt.setInt(4, Integer.parseInt(values[2]));
                    pstmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static int getClientId(String name) throws SQLException {
        /** Auxiliar method -- if you want
         * Gets client ID by name from database
         * @param name The name of the client
         * @return client ID or -1 if not found
         * @throws SQLException if database operation fails
         */
        final String QUERY = "SELECT id FROM client WHERE name = ?";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(QUERY);
        ) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void startTravel(int clientId, int scooterId, int stationId) throws SQLException {
        /**
         * Starts a new travel
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Station ID
         * @throws SQLException if database operation fails
         */
        final String START_TRAVEL = "INSERT INTO travels (client_id, scooter_id, station_id, start_time) VALUES (?, ?, ?, ?)";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(START_TRAVEL);
        ) {
            pstmt.setInt(1, clientId);
            pstmt.setInt(2, scooterId);
            pstmt.setInt(3, stationId);
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public static void stopTravel(int clientId, int scooterId, int stationId) throws SQLException {
        /**
         * Stops an ongoing travel
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Destination station ID
         * @throws SQLException if database operation fails
         */
        final String STOP_TRAVEL = "UPDATE travels SET end_time = ? WHERE client_id = ? AND scooter_id = ? AND station_id = ? AND end_time IS NULL";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(STOP_TRAVEL);
        ) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(2, clientId);
            pstmt.setInt(3, scooterId);
            pstmt.setInt(4, stationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateDocks(int dockId, String newState) {
        /**
         * Updates the state of a dock
         * @param dockId ID of the dock
         * @param newState New state of the dock
         * @throws SQLException if database operation fails
         */
        final String UPDATE_DOCK = "UPDATE docks SET state = ? WHERE id = ?";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(UPDATE_DOCK);
        ) {
            pstmt.setString(1, newState);
            pstmt.setInt(2, dockId);
            pstmt.executeUpdate();
            System.out.println("Dock updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void userSatisfaction(int userId, int rating) {
        /**
         * Records user satisfaction rating
         * @param userId ID of the user
         * @param rating Satisfaction rating
         * @throws SQLException if database operation fails
         */
        final String INSERT_RATING = "INSERT INTO user_satisfaction (user_id, rating) VALUES (?, ?)";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(INSERT_RATING);
        ) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, rating);
            pstmt.executeUpdate();
            System.out.println("User satisfaction recorded successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void occupationStation(int stationId) {
        /**
         * Retrieves and prints the occupation data of a station
         * @param stationId ID of the station
         * @throws SQLException if database operation fails
         */
        final String QUERY_OCCUPATION = "SELECT * FROM station_occupation WHERE station_id = ?";
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(QUERY_OCCUPATION);
        ) {
            pstmt.setInt(1, stationId);
            ResultSet rs = pstmt.executeQuery();
            UI.printResults(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}