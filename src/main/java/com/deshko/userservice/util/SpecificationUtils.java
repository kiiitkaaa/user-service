package com.deshko.userservice.util;

public final class SpecificationUtils {
    public static final char LIKE_ESCAPE = '\\';

    private SpecificationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String toLikePattern(String value) {
        String escaped = value.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
