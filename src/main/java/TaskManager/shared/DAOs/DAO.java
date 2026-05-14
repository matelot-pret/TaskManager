package TaskManager.shared.DAOs;
import TaskManager.shared.exceptions.AlreadyExistsException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class DAO<T> {

    public abstract T find(int id)throws SQLException;

    public abstract void create(T objectToCreate)throws AlreadyExistsException, SQLException;

    public abstract void update(T objectToUpdate)throws SQLException, NoSuchElementException;

    public abstract void delete(T objectToDele)throws SQLException, NoSuchElementException;

    public abstract Set<T> findAll()throws SQLException;

    protected abstract int checkAlreadyExists(T objectToCheck) throws SQLException;

    protected abstract T getResult(ResultSet result) throws SQLException;

    /**
     * Close a Statement and a ResultSet
     * @param statement the Statement to close (can be null)
     * @param resultSet the ResultSet to close (can be null)
     */
    protected void closeStatementAndResulSet(Statement statement, ResultSet resultSet) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
