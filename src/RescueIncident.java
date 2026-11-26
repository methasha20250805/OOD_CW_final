import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RescueIncident implements Comparable<RescueIncident> {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Set<String> usedIds = ConcurrentHashMap.newKeySet();

    private String incidentId;
    private Animal animal;
    private AnimalCondition condition;
    private String location;
    private RiskLevel locationRisk;
    private int numberOfAnimals;
    private boolean immediateDanger;
    private Set<String> requiredSkills;
    private Set<String> requiredEquipment;
    private String reporterContact;
    private LocalDateTime reportedAt;
    private int priorityScore;
    private PriorityLevel priority;
    private volatile IncidentStatus status;
    private List<RescueAssignment> assignments = new CopyOnWriteArrayList<>();

    public RescueIncident(Animal animal, AnimalCondition condition, String location, RiskLevel locationRisk,
                          int count, boolean danger, Set<String> skills, Set<String> equipment,
                          String contact) throws InvalidIncidentDataException {
        if (animal == null || animal.getCategory() == null) {
            throw new InvalidIncidentDataException("Invalid animal type.");
        }
        if (condition == null) {
            throw new InvalidIncidentDataException("Invalid animal condition.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new InvalidIncidentDataException("Location is missing.");
        }
        if (locationRisk == null) {
            throw new InvalidIncidentDataException("Invalid location risk level.");
        }
        if (count <= 0) {
            throw new InvalidIncidentDataException("Number of animals must be at least 1.");
        }
        if (contact == null || contact.trim().isEmpty()) {
            throw new InvalidIncidentDataException("Reporter contact details are missing.");
        }
        try {
            this.incidentId = generateIncidentId();
        } catch (DuplicateIncidentException e) {
            throw new InvalidIncidentDataException(e.getMessage());
        }
        this.animal = animal;
        this.condition = condition;
        this.location = location.trim();
        this.locationRisk = locationRisk;
        this.numberOfAnimals = count;
        this.immediateDanger = danger;
        this.requiredSkills = normalise(skills);
        this.requiredEquipment = normalise(equipment);
        this.reporterContact = contact.trim();
        this.reportedAt = LocalDateTime.now();
        this.priorityScore = 0;
        this.priority = PriorityLevel.LOW;
        this.status = IncidentStatus.REPORTED;
    }

    private static Set<String> normalise(Set<String> input) {
        Set<String> result = new HashSet<>();
        if (input != null) {
            for (String s : input) {
                if (s != null && !s.trim().isEmpty()) {
                    result.add(s.trim().toLowerCase());
                }
            }
        }
        return result;
    }

    private String generateIncidentId() throws DuplicateIncidentException {
        String id = String.format("INC-%03d", counter.incrementAndGet());
        if (!usedIds.add(id)) {
            throw new DuplicateIncidentException("Duplicate incident ID generated: " + id);
        }
        return id;
    }

    // Used when loading incidents from CSV so the saved ID is kept
    public void setIncidentId(String id) throws DuplicateIncidentException {
        if (id.equals(this.incidentId)) {
            return;
        }
        if (usedIds.contains(id)) {
            throw new DuplicateIncidentException("Duplicate incident ID: " + id);
        }
        usedIds.remove(this.incidentId);
        usedIds.add(id);
        this.incidentId = id;
        if (id.startsWith("INC-")) {
            try {
                int number = Integer.parseInt(id.substring(4));
                counter.accumulateAndGet(number, Math::max);
            } catch (NumberFormatException e) {
                // non-numeric ID, nothing to update
            }
        }
    }

    public void updateStatus(IncidentStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public int compareTo(RescueIncident other) {
        int byLevel = this.priority.compareTo(other.priority);   // CRITICAL comes first
        if (byLevel != 0) {
            return byLevel;
        }
        int byScore = Integer.compare(other.priorityScore, this.priorityScore);
        if (byScore != 0) {
            return byScore;
        }
        return this.reportedAt.compareTo(other.reportedAt);
    }

    public String getIncidentDetails() {
        return incidentId + " | " + animal.getAnimalDetails() + " x" + numberOfAnimals
                + " | " + condition + " | " + location + " (risk " + locationRisk + ")"
                + " | danger=" + (immediateDanger ? "Yes" : "No")
                + " | skills=" + requiredSkills + " | equipment=" + requiredEquipment
                + " | contact=" + reporterContact
                + " | priority=" + priority + " (" + priorityScore + ")"
                + " | status=" + status;
    }

    public void addAssignment(RescueAssignment assignment) {
        assignments.add(assignment);
    }

    public String getIncidentId() { return incidentId; }
    public Animal getAnimal() { return animal; }
    public AnimalCondition getCondition() { return condition; }
    public String getLocation() { return location; }
    public RiskLevel getLocationRisk() { return locationRisk; }
    public int getNumberOfAnimals() { return numberOfAnimals; }
    public boolean isImmediateDanger() { return immediateDanger; }
    public Set<String> getRequiredSkills() { return requiredSkills; }
    public Set<String> getRequiredEquipment() { return requiredEquipment; }
    public String getReporterContact() { return reporterContact; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }
    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public IncidentStatus getStatus() { return status; }
    public List<RescueAssignment> getAssignments() { return assignments; }
}