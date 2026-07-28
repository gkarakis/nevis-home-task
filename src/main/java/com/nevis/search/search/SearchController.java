package com.nevis.search.search;

import com.nevis.search.search.dto.SearchHit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    @Operation(summary = "Search clients and documents as one ranked list",
            description = "200 with a top-level JSON array (empty when nothing clears the "
                    + "floors); 400 when q is missing, blank, or empty after normalisation. "
                    + "limit defaults to 20 and is clamped to 100.")
    public List<SearchHit> search(@RequestParam("q") String q,
                                  @RequestParam(value = "limit", required = false) Integer limit) {
        return searchService.search(q, limit);
    }
}
