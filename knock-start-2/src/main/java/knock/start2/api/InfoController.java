package knock.start2.api;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    private final String version;
    private final boolean demo;

    public InfoController(ObjectProvider<BuildProperties> build, @Value("${app.demo:false}") boolean demo) {
        BuildProperties b = build.getIfAvailable();
        this.version = b != null ? b.getVersion() : "dev";
        this.demo = demo;
    }

    @GetMapping("/api/info")
    public Map<String, Object> info() {
        return Map.of("version", version, "demo", demo);
    }
}
