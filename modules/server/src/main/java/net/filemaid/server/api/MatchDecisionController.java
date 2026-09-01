package net.filemaid.server.api;

import java.util.List;
import net.filemaid.application.service.MatchDecisionService;
import net.filemaid.core.model.MetadataSelection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/match-decisions")
public class MatchDecisionController {
    private final MatchDecisionService service;

    public MatchDecisionController(MatchDecisionService service) { this.service = service; }

    @PutMapping
    void save(@RequestBody MetadataSelection selection) {
        service.save(selection);
    }

    @GetMapping
    List<MetadataSelection> list(@RequestParam(required = false) String source) {
        if (source != null && !source.isBlank()) {
            return service.findBySource(source).map(List::of).orElseGet(List::of);
        }
        return service.findAll();
    }
}
