package com.nstut.buildinggadgetsextra.common;

import java.util.Locale;

public final class StructureFileName {
    public static final int MAX_LENGTH = 128;

    private StructureFileName() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        String name = normalize(value);
        if (name.isEmpty() || name.length() > MAX_LENGTH || name.startsWith("/") || name.endsWith("/")) {
            return false;
        }
        String[] parts = name.split("/", -1);
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                char character = part.charAt(i);
                if (!((character >= 'a' && character <= 'z')
                        || (character >= '0' && character <= '9')
                        || character == '_' || character == '-' || character == '.')) {
                    return false;
                }
            }
        }
        return true;
    }
}
