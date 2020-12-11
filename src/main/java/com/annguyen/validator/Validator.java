package com.annguyen.validator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class Validator<T> {
    private final List<Constraint> constrains = new ArrayList<>();
    private Set<ConstraintViolation> violations = new LinkedHashSet<>();
    private boolean breakFirst;

    private Validator() {}

    public static <T> Validator<T> of() {
        return new Validator<>();
    }

    public <R> Validator<T> constraint(Function<T, R> getter, String fieldName, RuleBuilder<R> ruleBuilder) {
        ruleBuilder.getRules().forEach(rule -> constrains.add(new Constraint<>(getter, fieldName, rule)));
        return this;
    }

    public Validator<T> constraintOnTarget(Predicate<T> predicate, String fieldName, String message) {
        constrains.add(new Constraint<>(Function.identity(), fieldName, new Rule<>(predicate, message)));
        return this;
    }

    public <R> Validator<T> constraintWhen(boolean when, Function<T, R> getter, String fieldName,
                                           RuleBuilder<R> ruleBuilder) {
        if (when) {
            ruleBuilder.getRules().forEach(rule -> constrains.add(new Constraint<>(getter, fieldName, rule)));
        }
        return this;
    }

    public <R, U> Validator<T> nest(Function<T, R> getter, String fieldName, Validator<R> validator) {
        for (Constraint constraint : validator.constrains) {
            constrains.add(new NestedConstraint<T, R, U>(getter, fieldName, constraint));
        }
        return this;
    }

    public Validator<T> breakFirst(boolean breakFirst) {
        this.breakFirst = breakFirst;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <R, U> void validate(T object) {
        Objects.requireNonNull(object);
        Set<String> fieldNames = new LinkedHashSet<>();
        final boolean[] checkBreaking = {true};

        this.violations = new LinkedHashSet<>();

        constrains.stream()
                .filter(constraint -> checkBreaking[0])
                .forEach(constraint -> {
                    Object value;
                    String fieldName;
                    if (constraint instanceof NestedConstraint) {
                        NestedConstraint<T, R, U> nestedConstraint = (NestedConstraint<T, R, U>) constraint;
                        value = constraint.getGetter().apply(nestedConstraint.valueExtractor.apply(object));
                        fieldName = nestedConstraint.rootFieldName + "." + nestedConstraint.getFieldName();
                    } else {
                        value = constraint.getGetter().apply(object);
                        fieldName = constraint.getFieldName();
                    }

                    Rule<R> rule = constraint.getRule();
                    boolean valid = rule.getPredicate().test((R) value);
                    if (!valid && !fieldNames.contains(fieldName)) {
                        fieldNames.add(fieldName);
                        violations.add(new ConstraintViolation(fieldName, rule.getMessage()));
                    }
                    if (!valid && breakFirst) {
                        checkBreaking[0] = false;
                    }
                });
    }

    public void reject(T object) {
        validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(
                    "Validation failed for object " + object.getClass().getSimpleName(), violations);
        }
    }

    public Set<ConstraintViolation> getViolations() {
        return violations;
    }

    public static <R> RuleBuilder<R> rule(Predicate<R> predicate, String message) {
        return new RuleBuilder<R>().rule(predicate, message);
    }

    public static final class RuleBuilder<R> {
        private final List<Rule<R>> rules = new ArrayList<>();

        private RuleBuilder() {}

        public static <R> RuleBuilder<R> builder() {
            return new RuleBuilder<>();
        }

        public RuleBuilder<R> rule(Predicate<R> predicate, String message) {
            this.rules.add(new Rule<>(predicate, message));
            return this;
        }

        public List<Rule<R>> getRules() {
            return this.rules;
        }
    }

    private static final class Rule<R> {
        private final Predicate<R> predicate;
        private final String message;

        public Rule(Predicate<R> predicate, String message) {
            this.predicate = predicate;
            this.message = message;
        }

        public Predicate<R> getPredicate() {
            return this.predicate;
        }

        public String getMessage() {
            return this.message;
        }
    }

    private static class Constraint<T, R> {
        private final Function<T, R> getter;
        private final String fieldName;
        private final Rule<R> rule;

        private Constraint(Function<T, R> getter, String fieldName, Rule<R> rule) {
            this.getter = getter;
            this.fieldName = fieldName;
            this.rule = rule;
        }

        public Function<T, R> getGetter() {
            return getter;
        }

        public String getFieldName() {
            return fieldName;
        }

        public Rule<R> getRule() {
            return rule;
        }
    }

    private static final class NestedConstraint<T, R, U> extends Constraint<R, U> {
        private final Function<T, R> valueExtractor;
        private final String rootFieldName;

        private NestedConstraint(Function<T, R> valueExtractor, String rootFieldName, Constraint<R, U> constraint) {
            super(constraint.getGetter(), constraint.getFieldName(), constraint.getRule());
            this.valueExtractor = valueExtractor;
            this.rootFieldName = rootFieldName;
        }
    }
}
