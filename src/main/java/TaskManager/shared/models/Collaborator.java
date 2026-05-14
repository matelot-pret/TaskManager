package TaskManager.shared.models;

import java.util.Objects;

public class Collaborator {
    private int id;
    private String login;
    private String firstName;
    private String LastName;

    public Collaborator(int id, String login, String firstName, String lastName){
        this.id = id;
        this.login = login;
        this.firstName = firstName;
        this.LastName = lastName;
    }

    public Collaborator(String login, String firstName, String lastName){
        this(0, login ,firstName, lastName);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    @Override
    public String toString() {
        return "Collaborator{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", LastName='" + LastName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Collaborator that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
