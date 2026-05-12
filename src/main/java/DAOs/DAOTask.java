package DAOs;

import models.*;
import exceptions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class DAOTask extends DAO<Task> {

    protected static final String TABLE_TASK  = "Task";
    protected static final String TABLE_ELAPSED = "ElapsedTimeOnTask";

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_DESCRIPTION = "description";
    protected static final String FIELD_ECHEANCE = "echeance";
    protected static final String FIELD_ID_STATE = "idState";
    protected static final String FIELD_CREATOR = "creator";
    protected static final String FIELD_START_TIME = "startTime";
    protected static final String FIELD_CURRENT_WORKER = "currentWorker";

    protected static final String FIELD_ID_TASK         = "idTask";
    protected static final String FIELD_ID_COLLABORATOR = "idCollaborator";
    protected static final String FIELD_ELAPSED_TIME    = "elapsedTime";

    @Override
    public Task find(int id) throws SQLException {
        Connection connection = Database.getConnection();
        String query = "SELECT * FROM " + TABLE_TASK + " WHERE " + FIELD_ID + " = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Task task = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();
            if (rs.next())
                task = getResult(rs);
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
        return task;
    }

    @Override
    public void create(Task objectToCreate) throws AlreadyExistsException, SQLException {
        if (checkAlreadyExists(objectToCreate) >= 0)
            throw new AlreadyExistsException("This task already exists in database.");

        Connection connection = Database.getConnection();
        String query = "INSERT INTO " + TABLE_TASK + " ("
                + FIELD_DESCRIPTION + ", " + FIELD_ECHEANCE + ", " + FIELD_ID_STATE + ", "
                + FIELD_CREATOR + ", " + FIELD_START_TIME + ", " + FIELD_CURRENT_WORKER
                + ") VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(query);
            fillTaskStatement(stmt, objectToCreate);

            stmt.executeUpdate();
            getNewId(objectToCreate);
        } finally {
            closeStatementAndResulSet(stmt, null);
        }

        for (Map.Entry<Collaborator, Long> entry : objectToCreate.getCollaboratorElapsedTime().entrySet())
            upsertElapsedTime(objectToCreate.getId(), entry.getKey().getId(), entry.getValue());
    }

    @Override
    public void update(Task objectToUpdate) throws SQLException, NoSuchElementException {
        if (find(objectToUpdate.getId()) == null)
            throw new NoSuchElementException("[ERROR] There is no Task with the id " + objectToUpdate.getId());

        Connection connection = Database.getConnection();
        String query = "UPDATE " + TABLE_TASK + " SET "
                + FIELD_DESCRIPTION + " = ?, " + FIELD_ECHEANCE + " = ?, " + FIELD_ID_STATE + " = ?, "
                + FIELD_CREATOR + " = ?, " + FIELD_START_TIME + " = ?, " + FIELD_CURRENT_WORKER + " = ?"
                + " WHERE " + FIELD_ID + " = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(query);
            fillTaskStatement(stmt, objectToUpdate);
            stmt.setInt(7, objectToUpdate.getId());

            stmt.executeUpdate();
        } finally {
            closeStatementAndResulSet(stmt, null);
        }

        for (Map.Entry<Collaborator, Long> entry : objectToUpdate.getCollaboratorElapsedTime().entrySet())
            upsertElapsedTime(objectToUpdate.getId(), entry.getKey().getId(), entry.getValue());
    }

    @Override
    public void delete(Task objectToDelete) throws SQLException, NoSuchElementException {
        Connection connection = Database.getConnection();
        String query = "DELETE FROM " + TABLE_TASK + " WHERE " + FIELD_ID + " = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, objectToDelete.getId());

            if (stmt.executeUpdate() == 0)
                throw new NoSuchElementException("[ERROR] There is no Task with the id " + objectToDelete.getId());
        } finally {
            closeStatementAndResulSet(stmt, null);
        }
    }

    @Override
    public Set<Task> findAll() throws SQLException {
        Connection connection = Database.getConnection();
        String query = "SELECT * FROM " + TABLE_TASK;
        Set<Task> tasks = new HashSet<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next())
                tasks.add(getResult(rs));
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
        return tasks;
    }

    /**
     * Bind the shared fields of a Task to a PreparedStatement (positions 1 to 6).
     * Used by both create (INSERT) and update (UPDATE) to avoid code duplication.
     * @param stmt the PreparedStatement to fill (must have at least 6 parameters)
     * @param task the Task whose fields are bound to the statement
     * @throws SQLException if the database could not be reached
     * @pre stmt is a valid PreparedStatement with parameters at positions 1..6
     * @post positions 1..6 of stmt are bound with task's description, echeance,
     *       state, creator, startTime and currentWorker
     */
    private void fillTaskStatement(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getDescription());
        stmt.setTimestamp(2, Timestamp.valueOf(task.getEcheance().withSecond(0).withNano(0)));
        stmt.setInt(3, task.getState().getValue());
        stmt.setInt(4, task.getCreator().getId());
        if (task.getStartTime() != null)
            stmt.setTimestamp(5, Timestamp.valueOf(task.getStartTime()));
        else
            stmt.setNull(5, Types.TIMESTAMP);

        if (task.getCurrentWorker() != null)
            stmt.setInt(6, task.getCurrentWorker().getId());
        else
            stmt.setNull(6, Types.INTEGER);
    }

    /**
     * Retrieve the id generated by the database after an INSERT and set it on the Task object
     * @param newTask the Task just inserted in the database
     * @throws SQLException if the database could not be reached
     * @pre the Task has been inserted in the database
     * @post newTask.getId() is set to the id generated by the database
     */
    private void getNewId(Task newTask) throws SQLException {
        Connection connection = Database.getConnection();
        String query = "SELECT " + FIELD_ID + " FROM " + TABLE_TASK
                + " WHERE " + FIELD_DESCRIPTION + " = ?"
                + " AND " + FIELD_ECHEANCE + " = ?"
                + " AND " + FIELD_CREATOR + " = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setString(1, newTask.getDescription());
            stmt.setTimestamp(2, Timestamp.valueOf(newTask.getEcheance().withSecond(0).withNano(0)));
            stmt.setInt(3, newTask.getCreator().getId());

            rs = stmt.executeQuery();
            if (rs.next())
                newTask.setId(rs.getInt(FIELD_ID));
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
    }

    /**
     * Insert or update the elapsed time for a (task, collaborator) pair in ElapsedTimeOnTask.
     * If the pair already exists the elapsed time is updated, otherwise a new row is inserted.
     * @param idTask the id of the Task
     * @param idCollaborator the id of the Collaborator
     * @param elapsedTime the elapsed time in minutes
     * @throws SQLException if the database could not be reached
     * @post the (idTask, idCollaborator) row in ElapsedTimeOnTask reflects elapsedTime
     */
    private void upsertElapsedTime(int idTask, int idCollaborator, long elapsedTime) throws SQLException {
        Connection connection = Database.getConnection();
        String checkQuery = "SELECT 1 FROM " + TABLE_ELAPSED
                + " WHERE " + FIELD_ID_TASK + " = ?"
                + " AND " + FIELD_ID_COLLABORATOR + " = ?";
        PreparedStatement checkStmt = null;
        ResultSet rs = null;
        boolean exists;
        try {
            checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setInt(1, idTask);
            checkStmt.setInt(2, idCollaborator);

            rs = checkStmt.executeQuery();
            exists = rs.next();
        } finally {
            closeStatementAndResulSet(checkStmt, rs);
        }

        String query;
        if (exists)
            query = "UPDATE " + TABLE_ELAPSED + " SET " + FIELD_ELAPSED_TIME + " = ?"
                    + " WHERE " + FIELD_ID_TASK + " = ? AND " + FIELD_ID_COLLABORATOR + " = ?";
        else
            query = "INSERT INTO " + TABLE_ELAPSED + " ("
                    + FIELD_ID_TASK + ", " + FIELD_ID_COLLABORATOR + ", " + FIELD_ELAPSED_TIME
                    + ") VALUES (?, ?, ?)";

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(query);
            if (exists) {
                stmt.setLong(1, elapsedTime);
                stmt.setInt(2, idTask);
                stmt.setInt(3, idCollaborator);
            } else {
                stmt.setInt(1, idTask);
                stmt.setInt(2, idCollaborator);
                stmt.setLong(3, elapsedTime);
            }
            stmt.executeUpdate();
        } finally {
            closeStatementAndResulSet(stmt, null);
        }
    }

    /**
     * Load all ElapsedTimeOnTask entries for a given Task and populate its collaboratorElapsedTime map
     * @param task the Task whose map must be populated
     * @throws SQLException if the database could not be reached
     * @pre task exists in the database
     * @post task.getCollaboratorElapsedTime() contains one entry per row in ElapsedTimeOnTask for this task
     */
    private void loadElapsedTimes(Task task) throws SQLException {
        Connection connection = Database.getConnection();
        String query = "SELECT * FROM " + TABLE_ELAPSED + " WHERE " + FIELD_ID_TASK + " = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, task.getId());

            rs = stmt.executeQuery();
            Map<Collaborator, Long> map = new HashMap<>();
            DAOCollaborator daoCollaborator = new DAOCollaborator();
            while (rs.next()) {
                Collaborator collaborator = daoCollaborator.find(rs.getInt(FIELD_ID_COLLABORATOR));
                map.put(collaborator, rs.getLong(FIELD_ELAPSED_TIME));
            }
            task.setCollaboratorElapsedTime(map);
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
    }

    /**
     * Check whether a Task with the same description, echeance and creator already exists in the database
     * @param objectToCheck the Task to check
     * @return the id of the found Task, or -1 if none exists
     * @throws SQLException if the database could not be reached
     */
    @Override
    protected int checkAlreadyExists(Task objectToCheck) throws SQLException {
        Connection connection = Database.getConnection();
        String query = "SELECT " + FIELD_ID + " FROM " + TABLE_TASK
                + " WHERE " + FIELD_DESCRIPTION + " = ?"
                + " AND " + FIELD_ECHEANCE + " = ?"
                + " AND " + FIELD_CREATOR + " = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setString(1, objectToCheck.getDescription());
            stmt.setTimestamp(2, Timestamp.valueOf(objectToCheck.getEcheance().withSecond(0).withNano(0)));
            stmt.setInt(3, objectToCheck.getCreator().getId());

            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(FIELD_ID);
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
        return -1;
    }

    /**
     * Build a Task object from the current row of a ResultSet and load its collaboratorElapsedTime map
     * @param result the ResultSet positioned on the row to read
     * @return the Task built from the row, with its collaboratorElapsedTime map populated
     * @throws SQLException if the database could not be reached
     */
    @Override
    protected Task getResult(ResultSet result) throws SQLException {
        DAOCollaborator daoCollaborator = new DAOCollaborator();

        Task task = new Task(
                result.getString(FIELD_DESCRIPTION),
                result.getTimestamp(FIELD_ECHEANCE).toLocalDateTime().withSecond(0).withNano(0),
                daoCollaborator.find(result.getInt(FIELD_CREATOR))
        );
        task.setId(result.getInt(FIELD_ID));
        task.setState(TaskState.fromValue(result.getInt(FIELD_ID_STATE)));

        Timestamp startTime = result.getTimestamp(FIELD_START_TIME);
        if (startTime != null)
            task.setStartTime(startTime.toLocalDateTime().withSecond(0).withNano(0));

        int idCurrentWorker = result.getInt(FIELD_CURRENT_WORKER);
        if (!result.wasNull())
            task.setCurrentWorker(daoCollaborator.find(idCurrentWorker));

        loadElapsedTimes(task);
        return task;
    }
}