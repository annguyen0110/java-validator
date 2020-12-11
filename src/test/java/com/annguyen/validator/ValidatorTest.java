package com.annguyen.validator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.annguyen.validator.Validator.rule;
import static com.annguyen.validator.ValidatorHelper.in;
import static com.annguyen.validator.ValidatorHelper.lt;
import static com.annguyen.validator.ValidatorHelper.ne;
import static com.annguyen.validator.ValidatorHelper.notBlank;
import static com.annguyen.validator.ValidatorHelper.notNull;
import static com.annguyen.validator.ValidatorHelper.pattern;
import static com.annguyen.validator.ValidatorHelper.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void testValidator() {
        TestClass testClass = new TestClass();
        testClass.setAge(19);
        testClass.setName(" ");
        testClass.setGenre(Genre.MALE);

        Validator<TestClass> validator = Validator.<TestClass>of()
                .constraint(TestClass::getEmail,
                        "email",
                        rule(notNull(), "field.not.empty")
                )
                .constraint(TestClass::getName,
                        "name",
                        rule(notBlank(), "field.not.blank")
                )
                .constraint(TestClass::getAge,
                        "age",
                        Validator.RuleBuilder.<Integer>builder()
                        .rule(notNull(), "field.not.empty")
                        .rule(range(20, 30), "field.not-in.range-20-30")
                )
                .constraint(TestClass::getGenre,
                        "genre",
                        Validator.RuleBuilder.<Genre>builder()
                        .rule(notNull(), "field.not.empty")
                        .rule(in(Arrays.asList(Genre.MALE, Genre.FEMALE)), "field.not.valid")
                )
                ;

        validator.validate(testClass);
        assertErrors(
                new HashSet<String>() {{
                    add("email: field.not.empty");
                    add("name: field.not.blank");
                    add("age: field.not-in.range-20-30");
                }},
                validator.getViolations()
        );

        testClass.setName("Test name");
        testClass.setEmail("Test email");
        Validator<TestClass> validatorWithBreakFirst = Validator.<TestClass>of()
                .constraint(TestClass::getEmail,
                        "email",
                        Validator.RuleBuilder.<String>builder()
                        .rule(notNull(), "field.not.empty")
                        .rule(pattern("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$"), "field.not.valid")
                )
                .constraint(TestClass::getAge,
                        "email",
                        Validator.RuleBuilder.<Integer>builder()
                        .rule(notNull(), "field.not.empty")
                        .rule(ne(20), "field.not-equal.20")
                        .rule(lt(30), "field.not-lt.30")
                )
                .breakFirst(true)
                ;

        validatorWithBreakFirst.validate(testClass);
        assertErrors(
                new HashSet<String>() {{
                    add("email: field.not.valid");
                }},
                validatorWithBreakFirst.getViolations()
        );

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> validatorWithBreakFirst.reject(testClass));
        assertErrors(
                new HashSet<String>() {{
                    add("email: field.not.valid");
                }},
                exception.getViolations()
        );
    }

    private static void assertErrors(Set<String> expected, Set<ConstraintViolation> violations) {
        Set<String> actual = violations.stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getMessage()))
                .collect(Collectors.toSet());

        assertEquals(expected.size(), actual.size());
        expected.removeAll(actual);
        assertTrue(expected.isEmpty());
    }

    private static final class TestClass {
        private String name;
        private String email;
        private Integer age;
        private Genre genre;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Genre getGenre() {
            return genre;
        }

        public void setGenre(Genre genre) {
            this.genre = genre;
        }
    }

    enum Genre {
        MALE, FEMALE
    }
}
