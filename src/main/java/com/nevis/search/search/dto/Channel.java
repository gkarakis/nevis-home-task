package com.nevis.search.search.dto;

/** How a hit was matched. Documents carry SEMANTIC and/or LEXICAL; clients carry
 *  DIRECT (substring on name or email) or FUZZY (trigram similarity). */
public enum Channel {
    SEMANTIC,
    LEXICAL,
    DIRECT,
    FUZZY
}
