package com.deshko.userservice.specification;

import com.deshko.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import com.deshko.userservice.util.SpecificationUtils;

public final class UserSpecifications {
    private UserSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<User> nameContains(String name) {
        return fieldContains("name", name);
    }

    public static Specification<User> surnameContains(String surname) {
        return fieldContains("surname", surname);
    }

    public static Specification<User> byNameAndSurname(String name, String surname) {
        return Specification.allOf(nameContains(name), surnameContains(surname));
    }

    private static Specification<User> fieldContains(String field, String value) {
        if (SpecificationUtils.isBlank(value)) {
            return (_, _, cb) -> cb.conjunction();
        }
        String pattern = SpecificationUtils.toLikePattern(value);
        return (root, _, cb) ->
                cb.like(cb.lower(root.get(field)), pattern, SpecificationUtils.LIKE_ESCAPE);
    }
}
