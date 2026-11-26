import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class RescueTeam {
    private String teamId;
    private List<String> teamMembers;
    private Set<AnimalCategory> supportedCategories;
    private Set<String> rescueSkills;
    private Set<Equipment> equipment;
    private TeamStatus status;
    private int maxSimultaneousIncidents;
    private int currentWorkload;
    private List<RescueAssignment> assignments = new CopyOnWriteArrayList<>();

    public RescueTeam(String id, List<String> members, Set<AnimalCategory> cats, Set<String> skills,
                      Set<Equipment> equipment, TeamStatus status, int maxIncidents) {
        this.teamId = id;
        this.teamMembers = members;
        this.supportedCategories = cats;
        this.rescueSkills = new HashSet<>();
        if (skills != null) {
            for (String s : skills) {
                this.rescueSkills.add(s.trim().toLowerCase());
            }
        }
        this.equipment = equipment;
        this.status = status;
        this.maxSimultaneousIncidents = maxIncidents;
        this.currentWorkload = 0;
    }

    public synchronized void assignIncident(RescueIncident incident) {
        currentWorkload++;
        if (currentWorkload >= maxSimultaneousIncidents) {
            status = TeamStatus.ASSIGNED;
        }
    }

    public synchronized void releaseIncident() {
        if (currentWorkload > 0) {
            currentWorkload--;
        }
        if (status == TeamStatus.ASSIGNED) {
            status = TeamStatus.AVAILABLE;
        }
    }

    public void addAssignment(RescueAssignment assignment) {
        assignments.add(assignment);
    }

    public Set<String> getEquipmentNames() {
        Set<String> names = new HashSet<>();
        for (Equipment e : equipment) {
            names.add(e.getEquipmentName().trim().toLowerCase());
        }
        return names;
    }

    public String getTeamId() { return teamId; }
    public List<String> getTeamMembers() { return teamMembers; }
    public Set<AnimalCategory> getSupportedCategories() { return supportedCategories; }
    public Set<String> getRescueSkills() { return rescueSkills; }
    public Set<Equipment> getEquipment() { return equipment; }
    public TeamStatus getStatus() { return status; }
    public void setStatus(TeamStatus status) { this.status = status; }
    public int getMaxSimultaneousIncidents() { return maxSimultaneousIncidents; }
    public int getCurrentWorkload() { return currentWorkload; }
    public void setCurrentWorkload(int workload) { this.currentWorkload = workload; }
    public List<RescueAssignment> getAssignments() { return assignments; }

    @Override
    public String toString() {
        return teamId + " | members=" + teamMembers + " | animals=" + supportedCategories
                + " | skills=" + rescueSkills + " | equipment=" + getEquipmentNames()
                + " | status=" + status + " | workload=" + currentWorkload + "/" + maxSimultaneousIncidents;
    }
}