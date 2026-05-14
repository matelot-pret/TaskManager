package TaskManager.server.services;

import TaskManager.shared.DAOs.DAOCollaborator;
import TaskManager.shared.exceptions.AlreadyExistsException;
import TaskManager.shared.models.Collaborator;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Set;

@Service
public class CollaboratorService {

    private final DAOCollaborator daoCollaborator = new DAOCollaborator();

    /**
     * Attempt to find a collaborator by login.
     * @param login the login to search
     * @return the Collaborator found, or null if no collaborator has this login
     * @throws SQLException if the database could not be reached
     */
    public Collaborator login(String login) throws SQLException {
        return daoCollaborator.findByLogin(login);
    }

    /**
     * Register a new collaborator.
     * @param login the login of the new collaborator
     * @param firstName the first name of the new collaborator
     * @param lastName the last name of the new collaborator
     * @return the created Collaborator with its generated id
     * @throws AlreadyExistsException if a collaborator with the same login, firstName and lastName already exists
     * @throws SQLException if the database could not be reached
     */
    public Collaborator register(String login, String firstName, String lastName)
            throws AlreadyExistsException, SQLException {
        Collaborator collaborator = new Collaborator(login, firstName, lastName);
        daoCollaborator.create(collaborator);
        return collaborator;
    }

    /**
     * Retrieve all collaborators from the database.
     * @return a Set of all Collaborator objects
     * @throws SQLException if the database could not be reached
     */
    public Set<Collaborator> findAll() throws SQLException {
        return daoCollaborator.findAll();
    }
}