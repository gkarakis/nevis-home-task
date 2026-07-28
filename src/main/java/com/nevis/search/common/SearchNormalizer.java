package com.nevis.search.common;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The Java side of {@code search_normalize(text)} in V1__extensions_and_functions.sql.
 * Both must agree exactly: lowercase, strip accents, remove every non-alphanumeric
 * character. Drift between the two is silent and returns zero results, so the two
 * implementations are asserted against a shared fixture list in the tests.
 *
 * <pre>
 *   "Jack O'Hara"              -> jackohara
 *   "O’Hara" (U+2019)          -> ohara
 *   "José Álvarez"             -> josealvarez
 *   "ACC-889 134"              -> acc889134
 *   "john.doe@neviswealth.com" -> johndoeneviswealthcom
 * </pre>
 */
@Component
public class SearchNormalizer {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");

    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        // Decompose accented characters (NFD) then drop the combining marks, which
        // mirrors Postgres unaccent() for the Latin range we care about.
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        // Locale.ROOT so lowercasing never depends on the JVM default locale. Under a
        // Turkish locale, for example, 'I'.toLowerCase() is 'ı' (U+0131) which the
        // NON_ALNUM strip below would then drop — silently diverging from the SQL side.
        String lowered = decomposed.toLowerCase(Locale.ROOT);
        return NON_ALNUM.matcher(lowered).replaceAll("");
    }
}
