package com.nevis.search.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SearchNormalizerTest {

    private final SearchNormalizer normalizer = new SearchNormalizer();

    @Test
    void everyFixtureNormalisesAsExpected() {
        for (String[] fixture : NormalizationFixtures.all()) {
            assertThat(normalizer.normalize(fixture[0]))
                    .as("input: %s", fixture[0])
                    .isEqualTo(fixture[1]);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Jack O'Hara      | jackohara",
            "NevisWealth      | neviswealth",
            "NEVISWEALTH      | neviswealth",
            "ACC-889 134      | acc889134",
            "o hara           | ohara"
    })
    void normalizes(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    void nullBecomesEmptyString() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }

    @Test
    void normalisationIsLocaleStable() {
        // Under the Turkish locale, 'I'.toLowerCase() is the dotless 'ı' (U+0131), which
        // the non-alphanumeric strip would drop — diverging from the SQL side. Locale.ROOT
        // in the normaliser must keep this identical regardless of the JVM default locale.
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertThat(normalizer.normalize("INVESTMENT")).isEqualTo("investment");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void stripsAccentsAndPunctuation() {
        assertThat(normalizer.normalize("José Álvarez")).isEqualTo("josealvarez");
        assertThat(normalizer.normalize("O’Hara")).isEqualTo("ohara");
    }
}
