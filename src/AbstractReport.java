import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReport implements Report {
    protected LocalDateTime generatedAt;
    protected List<RescueIncident> incidents;

    public AbstractReport(List<RescueIncident> incidents) {
        this.incidents = new ArrayList<>(incidents);
        this.generatedAt = LocalDateTime.now();
    }

    @Override
    public abstract String generateReport();
}