package DAOs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static Connection connexion = null;

    private static String URL = "jdbc:oracle:thin:@labinfo.hers.be:1521:xe";
    private static String USER = "XXXX";
    private static String PASSWORD = "XXXX";
    
    private Database(){

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base de données établie avec succès");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch(SQLException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException{
        if(connexion == null){
            new Database();
        }   
        return connexion;
    }

    public static void closeConnection(){
        if(connexion != null){
            try {
                connexion.close();
                connexion = null;
                System.out.println("Connexion fermée");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

