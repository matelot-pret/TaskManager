package DAOs;

import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class DAO<T> {

    public abstract T find(int id)throws SQLException;

    public abstract T create(T objectToCreate)throws AlreadyBoundException, IllegalArgumentException, SQLException;

    public abstract T update(T objectToUpdate)throws SQLException, NoSuchElementException;

    public abstract T delete(T objectToDele)throws SQLException, NoSuchElementException;

    public abstract Set<T> findAll()throws SQLException;
}
