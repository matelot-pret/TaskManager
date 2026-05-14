package TaskManager.server;

import TaskManager.shared.DAOs.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

@SpringBootApplication
public class ServerMain {
    public static void main(String[] args) {

        SpringApplication.run(ServerMain.class, args);
        try{
            Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}