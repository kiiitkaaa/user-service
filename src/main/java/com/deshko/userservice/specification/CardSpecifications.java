package com.deshko.userservice.specification;

import com.deshko.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;
import com.deshko.userservice.util.SpecificationUtils;

public final class CardSpecifications {
    private CardSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<PaymentCard> ownerNameContains(String name) {
        return ownerFieldContains("name", name);
    }

    public static Specification<PaymentCard> ownerSurnameContains(String surname) {
        return ownerFieldContains("surname", surname);
    }

    public static Specification<PaymentCard> byOwnerNameAndSurname(String name, String surname) {
        return Specification.allOf(ownerNameContains(name), ownerSurnameContains(surname));
    }

    private static Specification<PaymentCard> ownerFieldContains(String field, String value) {
        if (SpecificationUtils.isBlank(value)) {
            return (_, _, cb) -> cb.conjunction();
        }
        String pattern = SpecificationUtils.toLikePattern(value);
        return (root, _, cb) ->
                cb.like(cb.lower(root.join("user").get(field)), pattern);
    }
}
