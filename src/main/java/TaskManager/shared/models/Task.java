package TaskManager.shared.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Task {
    private int id;
    private String description;
    private LocalDateTime echeance;
    private TaskState state;
    private Map<Collaborator,Long> collaboratorElapsedTime;
    private Collaborator creator;
    private LocalDateTime startTime;
    private Collaborator currentWorker;


    public Task(String desc, LocalDateTime echeance, Collaborator creator){
        id = 0;
        description = desc;
        this.echeance = echeance;
        state = TaskState.UNOPENED;
        collaboratorElapsedTime = new HashMap<>();
        this.creator = creator;
        startTime = null;
        currentWorker = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEcheance() {
        return echeance;
    }

    public void setEcheance(LocalDateTime echeance) {
        this.echeance = echeance;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Map<Collaborator, Long> getCollaboratorElapsedTime() {
        return collaboratorElapsedTime;
    }

    public void setCollaboratorElapsedTime(Map<Collaborator, Long> collaboratorElapsedTime) {
        this.collaboratorElapsedTime = collaboratorElapsedTime;
    }

    public Collaborator getCreator() {
        return creator;
    }

    public void setCreator(Collaborator creator) {
        this.creator = creator;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Collaborator getCurrentWorker() {
        return currentWorker;
    }

    public void setCurrentWorker(Collaborator currentWorker) {
        this.currentWorker = currentWorker;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", echeance=" + echeance.toString() +
                ", state=" + state.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Task task)) return false;
        return id == task.id && state == task.state && Objects.equals(collaboratorElapsedTime, task.collaboratorElapsedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state, collaboratorElapsedTime);
    }
}
