package com.nevis.search.common;

import java.util.List;

/**
 * The single source of truth for normalisation test cases. Asserted against both
 * {@link SearchNormalizer} (Java) and Postgres' {@code search_normalize()} (SQL),
 * so any drift between the two implementations fails a test.
 */
public final class NormalizationFixtures {

    private NormalizationFixtures() {
    }

    /** {input, expectedNormalisedForm} pairs. */
    public static List<String[]> all() {
        return List.of(
                new String[]{"Jack O'Hara", "jackohara"},
                new String[]{"O’Hara", "ohara"},           // curly apostrophe
                new String[]{"José Álvarez", "josealvarez"},
                new String[]{"ACC-889 134", "acc889134"},
                new String[]{"john.doe@neviswealth.com", "johndoeneviswealthcom"},
                new String[]{"NevisWealth", "neviswealth"},
                new String[]{"  Multiple   Spaces  ", "multiplespaces"},
                new String[]{"---", ""},
                new String[]{"", ""});
    }
}
