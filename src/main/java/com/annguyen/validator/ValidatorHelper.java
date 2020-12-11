package com.annguyen.validator;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ValidatorHelper {

    private ValidatorHelper() {}

    public static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    public static <T> Predicate<T> eq(T value) {
        return x -> Objects.equals(x, value);
    }

    public static <T> Predicate<T> ne(T value) {
        return eq(value).negate();
    }

    public static <T> Predicate<T> in(List<T> values) {
        return values::contains;
    }

    public static Predicate<Integer> lt(int value) {
        return x -> x > value;
    }

    public static Predicate<Integer> range(int minValue, int maxValue) {
        return x -> x >= minValue && x <= maxValue;
    }

    public static Predicate<String> notBlank() {
        return x -> Objects.nonNull(x) && !x.trim().isEmpty();
    }

    public static Predicate<String> pattern(String pattern) {
        return x -> Pattern.compile(pattern).matcher(x).matches();
    }

}
