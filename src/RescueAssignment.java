import java.time.LocalDateTime;
import java.util.UUID;

public class RescueAssignment {
    private String assignmentId;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private RescueIncident incident;
    private RescueTeam team;

    public RescueAssignment(RescueIncident incident, RescueTeam team) {
        this.assignmentId = "ASG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.incident = incident;
        this.team = team;
        this.assignedAt = LocalDateTime.now();
    }

    public void completeAssignment() {
        this.completedAt = LocalDateTime.now();
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public RescueIncident getIncident() {
        return incident;
    }

    public RescueTeam getTeam() {
        return team;
    }
}