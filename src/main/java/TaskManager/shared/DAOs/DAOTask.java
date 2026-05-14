package TaskManager.shared.DAOs;

import TaskManager.shared.exceptions.AlreadyExistsException;
import TaskManager.shared.models.Collaborator;
import TaskManager.shared.models.Task;
import TaskManager.shared.models.TaskState;

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

    protected static final String TABLE_TASK           = "Task";
    protected static final String TABLE_ELAPSED        = "ElapsedTimeOnTask";

    protected static final String FIELD_ID             = "id";
    protected static final String FIELD_DESCRIPTION    = "description";
    protected static final String FIELD_ECHEANCE       = "echeance";
    protected static final String FIELD_ID_STATE       = "idState";
    protected static final String FIELD_CREATOR        = "creator";
    protected static final String FIELD_START_TIME     = "startTime";
    protected static final String FIELD_CURRENT_WORKER = "currentWorker";

    protected static final String FIELD_ID_TASK         = "idTask";
    protected static final String FIELD_ID_COLLABORATOR = "idCollaborator";
    protected static final String FIELD_ELAPSED_TIME    = "elapsedTime";

    /**
     * SQL query that loads a task and all related data in a single JOIN.
     * Returns one row per ElapsedTimeOnTask entry (or one row if none).
     * Columns prefixed with c_ = creator, w_ = currentWorker, e_ = elapsed collaborator.
     */
    private static final String SELECT_FULL =
            "SELECT t.id, t.description, t.echeance, t.idState, t.startTime, " +
                    "       c.id AS c_id, c.login AS c_login, c.firstName AS c_firstName, c.lastName AS c_lastName, " +
                    "       w.id AS w_id, w.login AS w_login, w.firstName AS w_firstName, w.lastName AS w_lastName, " +
                    "       e.idCollaborator AS e_id, e.elapsedTime AS e_elapsed, " +
                    "       ec.login AS e_login, ec.firstName AS e_firstName, ec.lastName AS e_lastName " +
                    "FROM Task t " +
                    "JOIN Collaborator c ON t.creator = c.id " +
                    "LEFT JOIN Collaborator w ON t.currentWorker = w.id " +
                    "LEFT JOIN ElapsedTimeOnTask e ON t.id = e.idTask " +
                    "LEFT JOIN Collaborator ec ON e.idCollaborator = ec.id ";

    @Override
    public Task find(int id) throws SQLException {
        Connection connection = Database.getConnection();
        String query = SELECT_FULL + "WHERE t.id = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            Map<Integer, Task> map = buildTasksFromResultSet(rs);
            return map.get(id);
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
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
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(SELECT_FULL);
            rs = stmt.executeQuery();
            return new HashSet<>(buildTasksFromResultSet(rs).values());
        } finally {
            closeStatementAndResulSet(stmt, rs);
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    /**
     * Build a map of taskId -> Task from a ResultSet produced by SELECT_FULL.
     * The JOIN produces multiple rows per task (one per ElapsedTimeOnTask entry).
     * This method groups them back into Task objects with their full elapsed time map.
     * @param rs the ResultSet to read
     * @return a map of taskId -> Task with collaboratorElapsedTime populated
     * @throws SQLException if the ResultSet cannot be read
     */
    private Map<Integer, Task> buildTasksFromResultSet(ResultSet rs) throws SQLException {
        Map<Integer, Task> tasks = new HashMap<>();
        while (rs.next()) {
            int taskId = rs.getInt("id");
            Task task = tasks.get(taskId);

            if (task == null) {
                // Construire le créateur
                Collaborator creator = new Collaborator(
                        rs.getInt("c_id"),
                        rs.getString("c_login"),
                        rs.getString("c_firstName"),
                        rs.getString("c_lastName")
                );

                // Construire la tâche
                task = new Task(
                        rs.getString(FIELD_DESCRIPTION),
                        rs.getTimestamp(FIELD_ECHEANCE).toLocalDateTime().withSecond(0).withNano(0),
                        creator
                );
                task.setId(taskId);
                task.setState(TaskState.fromValue(rs.getInt(FIELD_ID_STATE)));

                Timestamp startTime = rs.getTimestamp(FIELD_START_TIME);
                if (startTime != null)
                    task.setStartTime(startTime.toLocalDateTime().withSecond(0).withNano(0));

                // Construire le currentWorker si présent
                int workerId = rs.getInt("w_id");
                if (!rs.wasNull()) {
                    Collaborator worker = new Collaborator(
                            workerId,
                            rs.getString("w_login"),
                            rs.getString("w_firstName"),
                            rs.getString("w_lastName")
                    );
                    task.setCurrentWorker(worker);
                }

                tasks.put(taskId, task);
            }

            // Ajouter l'entrée ElapsedTimeOnTask si présente
            int elapsedCollabId = rs.getInt("e_id");
            if (!rs.wasNull()) {
                Collaborator elapsedCollab = new Collaborator(
                        elapsedCollabId,
                        rs.getString("e_login"),
                        rs.getString("e_firstName"),
                        rs.getString("e_lastName")
                );
                task.getCollaboratorElapsedTime().put(
                        elapsedCollab, rs.getLong("e_elapsed"));
            }
        }
        return tasks;
    }

    /**
     * Bind the shared fields of a Task to a PreparedStatement (positions 1 to 6).
     * @param stmt the PreparedStatement with at least 6 parameters
     * @param task the Task whose fields are bound
     * @throws SQLException if the binding fails
     * @pre positions 1..6 are available in stmt
     * @post positions 1..6 are bound with description, echeance, state, creator, startTime, currentWorker
     */
    private void fillTaskStatement(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getDescription());
        stmt.setTimestamp(2, Timestamp.valueOf(task.getEcheance().withSecond(0).withNano(0)));
        stmt.setInt(3, task.getState().getValue());
        stmt.setInt(4, task.getCreator().getId());
        if (task.getStartTime() != null)
            stmt.setTimestamp(5, Timestamp.valueOf(task.getStartTime().withSecond(0).withNano(0)));
        else
            stmt.setNull(5, Types.TIMESTAMP);
        if (task.getCurrentWorker() != null)
            stmt.setInt(6, task.getCurrentWorker().getId());
        else
            stmt.setNull(6, Types.INTEGER);
    }

    /**
     * Retrieve the id generated by the database after an INSERT and set it on the Task.
     * @param newTask the Task just inserted
     * @throws SQLException if the database could not be reached
     * @pre the Task has been inserted
     * @post newTask.getId() is set to the generated id
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
     * Insert or update the elapsed time for a (task, collaborator) pair.
     * @param idTask the task id
     * @param idCollaborator the collaborator id
     * @param elapsedTime the elapsed time in minutes
     * @throws SQLException if the database could not be reached
     * @post the row in ElapsedTimeOnTask reflects elapsedTime
     */
    private void upsertElapsedTime(int idTask, int idCollaborator, long elapsedTime) throws SQLException {
        Connection connection = Database.getConnection();
        String checkQuery = "SELECT 1 FROM " + TABLE_ELAPSED
                + " WHERE " + FIELD_ID_TASK + " = ? AND " + FIELD_ID_COLLABORATOR + " = ?";
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

        String query = exists
                ? "UPDATE " + TABLE_ELAPSED + " SET " + FIELD_ELAPSED_TIME + " = ?"
                  + " WHERE " + FIELD_ID_TASK + " = ? AND " + FIELD_ID_COLLABORATOR + " = ?"
                : "INSERT INTO " + TABLE_ELAPSED + " ("
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

    @Override
    protected Task getResult(ResultSet result) throws SQLException {
        // Non utilisé — remplacé par buildTasksFromResultSet avec JOIN
        throw new UnsupportedOperationException("Use buildTasksFromResultSet instead");
    }
}