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
        final String VALUE_CMD = """
            SELECT *
            FROM replacementorder
            WHERE dtorder BETWEEN ? AND ? AND station = ?
        """;
        try (
            Connection conn = DriverManager.getConnection(UI.getInstance().getConnectionString());
            PreparedStatement pstmt = conn.prepareStatement(VALUE_CMD);
        ) {
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
        // TO BE DONE  
    }
    
    public static int getClientId(String name) throws SQLException {
        /** Auxiliar method -- if you want
         * Gets client ID by name from database
         * @param name The name of the client
         * @return client ID or -1 if not found
         * @throws SQLException if database operation fails
         */
        
    }

    public static void startTravel(int clientId, int scooterId, int stationId) throws SQLException {
        /**
         * Starts a new travel
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Station ID
         * @throws SQLException if database operation fails
         */
        System.out.print("EMPTY")
    }

    
    public static void stopTravel(int clientId, int scooterId, int stationId) throws SQLException {
        /**
         * Stops an ongoing travel
         * @param clientId Client ID
         * @param scooterId Scooter ID
         * @param stationId Destination station ID
         * @throws SQLException if database operation fails
         */
        System.out.print("EMPTY")
    }

    public static void updateDocks(/*FILL WITH PARAMETERS */) {
        // TODO
        System.out.println("updateDocks()");
    }

    public static void userSatisfaction(/*FILL WITH PARAMETERS */) {
        // TODO
        System.out.println("userSatisfaction()");
    }

    public static void occupationStation(/*FILL WITH PARAMETERS */) {
        // TODO
        System.out.println("occupationStation()");
    }    
}