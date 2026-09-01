package net.filemaid.server.api;

import java.util.List;
import net.filemaid.application.service.OperationHistoryService;
import net.filemaid.application.service.UndoService;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {
    private final OperationHistoryService historyService;
    private final UndoService undoService;

    public OperationController(OperationHistoryService historyService, UndoService undoService) {
        this.historyService = historyService;
        this.undoService = undoService;
    }

    @GetMapping
    List<OperationRecord> list() {
        return historyService.findAll();
    }

    @PostMapping("/{id}/undo")
    OperationResult undo(@PathVariable long id, @RequestParam String rootId) {
        return undoService.undo(rootId, id);
    }
}
