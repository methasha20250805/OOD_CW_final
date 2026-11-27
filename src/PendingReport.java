import java.util.List;

public class PendingReport extends AbstractReport {

    public PendingReport(List<RescueIncident> incidents) {
        super(incidents);
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("PENDING RESCUE INCIDENTS\n");
        sb.append("Generated at: ").append(generatedAt).append("\n");
        sb.append("Total pending: ").append(incidents.size()).append("\n");
        for (RescueIncident incident : incidents) {
            sb.append(incident.getIncidentDetails()).append("\n");
        }
        return sb.toString();
    }
}