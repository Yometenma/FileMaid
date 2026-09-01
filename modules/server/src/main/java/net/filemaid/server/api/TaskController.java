package net.filemaid.server.api;

import java.util.List;
import net.filemaid.core.model.Task;
import net.filemaid.server.BackgroundTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final BackgroundTaskService tasks;

    public TaskController(BackgroundTaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    List<Task> list() {
        return tasks.list();
    }

    @GetMapping("/{id}")
    Task get(@PathVariable String id) {
        return tasks.get(id);
    }

    @PostMapping("/{id}/cancel")
    Task cancel(@PathVariable String id) {
        tasks.cancel(id);
        return tasks.get(id);
    }
}
