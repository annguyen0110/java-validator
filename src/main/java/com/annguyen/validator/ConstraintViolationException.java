package com.annguyen.validator;

import java.util.Set;

public class ConstraintViolationException extends RuntimeException {
    private final Set<ConstraintViolation> violations;

    public ConstraintViolationException(String message, Set<ConstraintViolation> violations) {
        super(message);
        this.violations = violations;
    }

    public Set<ConstraintViolation> getViolations() {
        return violations;
    }
}
