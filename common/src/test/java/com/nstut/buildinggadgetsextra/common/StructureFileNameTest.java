package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureFileNameTest {
    @Test
    void normalizesWhitespaceAndCase() {
        assertEquals("castle/west-wing", StructureFileName.normalize("  Castle/West-Wing  "));
        assertEquals("", StructureFileName.normalize(null));
    }

    @Test
    void acceptsSafeVanillaStyleNames() {
        assertTrue(StructureFileName.isValid("house"));
        assertTrue(StructureFileName.isValid("folder/house_2-final.nbt"));
        assertTrue(StructureFileName.isValid("UPPERCASE_IS_NORMALIZED"));
        assertTrue(StructureFileName.isValid(repeat('a', StructureFileName.MAX_LENGTH)));
    }

    @Test
    void rejectsTraversalSeparatorsAndInvalidCharacters() {
        String[] invalid = {null, "", " ", "/house", "house/", "house//roof", ".", "..",
                "house/../secret", "house/./roof", "house\\roof", "mod:house", "house roof",
                repeat('a', StructureFileName.MAX_LENGTH + 1)};
        for (String name : invalid) assertFalse(StructureFileName.isValid(name), String.valueOf(name));
    }

    private static String repeat(char character, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int index = 0; index < count; index++) value.append(character);
        return value.toString();
    }
}
