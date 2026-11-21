package knock.start2.api;

import java.util.List;
import knock.start2.api.Dtos.*;
import knock.start2.collector.ClaudeCodeCollector;
import knock.start2.collector.CollectResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService service;
    private final ClaudeCodeCollector collector;

    public DashboardController(DashboardService service, ClaudeCodeCollector collector) {
        this.service = service;
        this.collector = collector;
    }

    @GetMapping("/sessions")
    public List<SessionSummary> sessions(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String project,
                                         @RequestParam(required = false) Integer days,
                                         @RequestParam(defaultValue = "false") boolean starred) {
        return service.list(q, project, days, starred);
    }

    @GetMapping("/sessions/{id}")
    public SessionDetail session(@PathVariable String id) {
        return service.detail(id);
    }

    @PatchMapping("/sessions/{id}")
    public SessionSummary update(@PathVariable String id, @RequestBody SessionUpdate body) {
        return service.update(id, body);
    }

    @GetMapping("/stats")
    public Stats stats(@RequestParam(required = false) String project, @RequestParam(required = false) Integer days) {
        return service.stats(project, days);
    }

    @GetMapping("/projects")
    public List<String> projects() {
        return service.projects();
    }

    @PostMapping("/collect")
    public CollectResult collect() {
        return collector.collect();
    }

    @GetMapping("/collect/last")
    public CollectResult lastCollect() {
        return collector.lastResult();
    }
}
