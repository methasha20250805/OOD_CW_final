import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class IncidentManager {
    private CopyOnWriteArrayList<RescueIncident> incidents = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<RescueTeam> teams = new CopyOnWriteArrayList<>();
    private PriorityBlockingQueue<RescueIncident> pendingQueue = new PriorityBlockingQueue<>();

    private PriorityCalculator priorityCalculator = new PriorityCalculator();
    private TeamMatcher teamMatcher = new TeamMatcher();
    private FileManager fileManager = new FileManager();

    public IncidentManager() {
        // load existing data from the CSV files
        try {
            teams.addAll(fileManager.loadTeams());
            for (RescueIncident incident : fileManager.loadIncidents()) {
                incidents.add(incident);
                if (incident.getStatus() == IncidentStatus.PENDING_ASSIGNMENT) {
                    pendingQueue.add(incident);
                }
            }
            for (RescueAssignment a : fileManager.loadAssignments(incidents, teams)) {
                a.getIncident().addAssignment(a);
                a.getTeam().addAssignment(a);
            }
        } catch (FileOperationException | InvalidIncidentDataException e) {
            System.out.println("Warning while loading data: " + e.getMessage());
        }
    }

    public synchronized void reportIncident(RescueIncident incident)
            throws InvalidIncidentDataException, DuplicateIncidentException {
        validate(incident);
        for (RescueIncident existing : incidents) {
            if (existing.getIncidentId().equals(incident.getIncidentId())) {
                throw new DuplicateIncidentException("Duplicate incident ID: " + incident.getIncidentId());
            }
        }
        // assess priority before the first save, matching the Report Incident use case's step order
        int score = priorityCalculator.calculatePriority(incident);
        incident.setPriorityScore(score);
        incident.setPriority(priorityCalculator.classifyPriority(score));

        incidents.add(incident);
        try {
            fileManager.saveIncident(incident);
        } catch (FileOperationException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }

    private void validate(RescueIncident incident) throws InvalidIncidentDataException {
        if (incident == null) {
            throw new InvalidIncidentDataException("Incident is missing.");
        }
        if (incident.getAnimal() == null || incident.getAnimal().getCategory() == null) {
            throw new InvalidIncidentDataException("Invalid animal type.");
        }
        if (incident.getLocation() == null || incident.getLocation().trim().isEmpty()) {
            throw new InvalidIncidentDataException("Location is missing.");
        }
        if (incident.getNumberOfAnimals() <= 0) {
            throw new InvalidIncidentDataException("Number of animals must be at least 1.");
        }
        if (incident.getPriority() == null || incident.getPriorityScore() < 0 || incident.getPriorityScore() > 100) {
            throw new InvalidIncidentDataException("Invalid priority information.");
        }
    }

    public RescueIncident getIncident(String incidentId) {
        for (RescueIncident incident : incidents) {
            if (incident.getIncidentId().equalsIgnoreCase(incidentId)) {
                return incident;
            }
        }
        return null;
    }

    public List<RescueIncident> getAllIncidents() {
        return new ArrayList<>(incidents);
    }
    // Reporter's "View my incidents"
    public List<RescueIncident> getIncidentsByReporter(String contact) {
        List<RescueIncident> result = new ArrayList<>();
        for (RescueIncident incident : incidents) {
            if (incident.getReporterContact().equalsIgnoreCase(contact.trim())) {
                result.add(incident);
            }
        }
        return result;
    }

    // Rescue Team's "View assigned incidents"
    public List<RescueIncident> getIncidentsForTeam(String teamId) {
        List<RescueIncident> result = new ArrayList<>();
        for (RescueTeam team : teams) {
            if (team.getTeamId().equalsIgnoreCase(teamId)) {
                for (RescueAssignment a : team.getAssignments()) {
                    result.add(a.getIncident());
                }
            }
        }
        return result;
    }
    public List<RescueIncident> getPendingIncidents() {
        List<RescueIncident> pending = new ArrayList<>(pendingQueue);
        Collections.sort(pending);   // highest priority first
        return pending;
    }

    public RescueIncident getNextPendingIncident() {
        return pendingQueue.peek();
    }

    public synchronized void registerTeam(RescueTeam team) throws InvalidTeamDataException {
        if (team == null || team.getTeamId() == null || team.getTeamId().trim().isEmpty()) {
            throw new InvalidTeamDataException("Team ID is missing.");
        }
        if (team.getTeamMembers() == null || team.getTeamMembers().isEmpty()) {
            throw new InvalidTeamDataException("Team must have at least one member.");
        }
        if (team.getSupportedCategories() == null || team.getSupportedCategories().isEmpty()) {
            throw new InvalidTeamDataException("Team must support at least one animal category.");
        }
        if (team.getMaxSimultaneousIncidents() <= 0) {
            throw new InvalidTeamDataException("Maximum simultaneous incidents must be at least 1.");
        }
        for (RescueTeam existing : teams) {
            if (existing.getTeamId().equalsIgnoreCase(team.getTeamId())) {
                throw new InvalidTeamDataException("Team ID already exists: " + team.getTeamId());
            }
        }
        teams.add(team);
        try {
            fileManager.saveTeam(team);
        } catch (FileOperationException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }
    public synchronized void editTeam(RescueTeam updated) throws InvalidTeamDataException {
        if (updated == null || updated.getTeamId() == null || updated.getTeamId().trim().isEmpty()) {
            throw new InvalidTeamDataException("Team ID is missing.");
        }
        if (updated.getTeamMembers() == null || updated.getTeamMembers().isEmpty()) {
            throw new InvalidTeamDataException("Team must have at least one member.");
        }
        if (updated.getSupportedCategories() == null || updated.getSupportedCategories().isEmpty()) {
            throw new InvalidTeamDataException("Team must support at least one animal category.");
        }
        if (updated.getMaxSimultaneousIncidents() <= 0) {
            throw new InvalidTeamDataException("Maximum simultaneous incidents must be at least 1.");
        }
        RescueTeam existing = null;
        for (RescueTeam t : teams) {
            if (t.getTeamId().equalsIgnoreCase(updated.getTeamId())) {
                existing = t;
            }
        }
        if (existing == null) {
            throw new InvalidTeamDataException("Team not found: " + updated.getTeamId());
        }
        updated.setCurrentWorkload(existing.getCurrentWorkload());   // keep the live workload
        teams.remove(existing);
        teams.add(updated);
        try {
            fileManager.saveTeam(updated);
        } catch (FileOperationException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }

    public synchronized void removeTeam(String teamId) throws InvalidTeamDataException {
        RescueTeam existing = null;
        for (RescueTeam t : teams) {
            if (t.getTeamId().equalsIgnoreCase(teamId)) {
                existing = t;
            }
        }
        if (existing == null) {
            throw new InvalidTeamDataException("Team not found: " + teamId);
        }
        if (existing.getCurrentWorkload() > 0) {
            throw new InvalidTeamDataException("Cannot remove " + teamId + ": it still has active assignments.");
        }
        teams.remove(existing);
        try {
            fileManager.deleteTeam(teamId);
        } catch (FileOperationException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }
    public List<RescueTeam> getAllTeams() {
        return new ArrayList<>(teams);
    }

    public synchronized RescueAssignment createAssignment(RescueIncident incident, RescueTeam team)
            throws TeamAssignmentException {
        if (incident == null || team == null) {
            throw new TeamAssignmentException("Both an incident and a team must be selected.");
        }
        IncidentStatus s = incident.getStatus();
        if (s == IncidentStatus.ASSIGNED || s == IncidentStatus.IN_PROGRESS || s == IncidentStatus.COMPLETED) {
            throw new TeamAssignmentException("Incident " + incident.getIncidentId() + " is already " + s + ".");
        }
        if (!teamMatcher.checkAvailability(team)) {
            throw new TeamAssignmentException("Team " + team.getTeamId() + " is unavailable.");
        }
        if (!teamMatcher.checkAnimalCompatibility(incident, team)) {
            throw new TeamAssignmentException("Team " + team.getTeamId() + " cannot handle this animal type.");
        }
        if (!teamMatcher.checkRequiredSkills(incident, team)) {
            throw new TeamAssignmentException("Team " + team.getTeamId() + " does not have the required skills.");
        }
        teamMatcher.checkEquipment(incident, team);   // throws if equipment is missing

        RescueAssignment assignment = new RescueAssignment(incident, team);
        team.assignIncident(incident);
        team.addAssignment(assignment);
        incident.addAssignment(assignment);
        incident.updateStatus(IncidentStatus.ASSIGNED);
        pendingQueue.remove(incident);
        saveChanges(incident);
        return assignment;
    }

    // Assess priority -> find suitable teams -> mark Pending Assignment if none are available
    public synchronized void processIncident(RescueIncident incident) {
        IncidentStatus s = incident.getStatus();
        if (s != IncidentStatus.REPORTED && s != IncidentStatus.PENDING_ASSIGNMENT) {
            return;
        }
        int score = priorityCalculator.calculatePriority(incident);
        incident.setPriorityScore(score);
        incident.setPriority(priorityCalculator.classifyPriority(score));

        List<RescueTeam> suitable = teamMatcher.findSuitableTeams(incident, teams);
        if (suitable.isEmpty()) {
            incident.updateStatus(IncidentStatus.PENDING_ASSIGNMENT);
            if (!pendingQueue.contains(incident)) {
                pendingQueue.add(incident);
            }
        } else {
            incident.updateStatus(IncidentStatus.REPORTED);
            pendingQueue.remove(incident);
        }
        saveChanges(incident);
    }

    // saves the incident, and any team / assignment connected to it, to the CSV files
    public void saveChanges(RescueIncident incident) {
        try {
            fileManager.updateIncident(incident);
            for (RescueAssignment a : incident.getAssignments()) {
                fileManager.saveAssignment(a);
                fileManager.saveTeam(a.getTeam());
            }
        } catch (FileOperationException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }
}