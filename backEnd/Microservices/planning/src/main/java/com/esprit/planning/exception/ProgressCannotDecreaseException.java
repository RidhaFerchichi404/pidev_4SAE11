package com.esprit.planning.exception;

/**
 * Thrown when a progress update's percentage is less than the current maximum
 * progress for the same project (progress cannot decrease).
 */
public class ProgressCannotDecreaseException extends RuntimeException {

    private final int minAllowed;
    private final int provided;

    public ProgressCannotDecreaseException(int minAllowed, int provided) {
        super(String.format("Progress cannot be less than the previous update. Minimum allowed: %d%%, provided: %d%%", minAllowed, provided));
        this.minAllowed = minAllowed;
        this.provided = provided;
    }

    public int getMinAllowed() {
        return minAllowed;
    }

    public int getProvided() {
        return provided;
    }
}
