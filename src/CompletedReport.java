import java.util.List;

public class CompletedReport extends AbstractReport {

    public CompletedReport(List<RescueIncident> incidents) {
        super(incidents);
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("COMPLETED RESCUE INCIDENTS \n");
        sb.append("Generated at: ").append(generatedAt).append("\n");
        sb.append("Total completed: ").append(incidents.size()).append("\n");
        for (RescueIncident incident : incidents) {
            sb.append(incident.getIncidentDetails()).append("\n");
        }
        return sb.toString();
    }
}
