package models;

public enum TaskState {
    UNOPENED(1),
    PROGRESSING(2),
    CLOSED(3),
    PAUSED(4),
    LATE(5),
    BLOCKED(6),
    CANCELED(7);

    private final int value;

    TaskState(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TaskState fromValue(int value){
        TaskState state;
        switch (value){
            case 1 -> state = UNOPENED;
            case 2 -> state = PROGRESSING;
            case 3 -> state = CLOSED;
            case 4 -> state = PAUSED;
            case 5 -> state = LATE;
            case 6 -> state = BLOCKED;
            case 7 -> state = CANCELED;
            default -> throw new IllegalArgumentException("Unknow value " + value);

        }
        return state;
    }
}
