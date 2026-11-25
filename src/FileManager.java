// FILE: FileManager.java
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class FileManager {
    private String incidentsFile = "incidents.csv";
    private String teamsFile = "teams.csv";
    private String assignmentsFile = "assignments.csv";

    private static final String INCIDENT_HEADER =
            "incidentId,animalCategory,species,condition,location,locationRisk,numberOfAnimals,"
                    + "immediateDanger,requiredSkills,requiredEquipment,reporterContact,reportedAt,"
                    + "priorityScore,priority,status";
    private static final String TEAM_HEADER =
            "teamId,members,categories,skills,equipment,status,maxIncidents,currentWorkload";
    private static final String ASSIGNMENT_HEADER =
            "assignmentId,incidentId,teamId,assignedAt,completedAt";

//Loading the data
    public synchronized List<RescueIncident> loadIncidents()
            throws FileOperationException, InvalidIncidentDataException {
        List<RescueIncident> list = new ArrayList<>();
        File file = new File(incidentsFile);
        if (!file.exists()) {
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || line.startsWith("incidentId")) {
                    continue;
                }
                try {
                    list.add(parseIncident(line));
                } catch (InvalidIncidentDataException | DuplicateIncidentException e) {
                    System.out.println("Skipping " + incidentsFile + " line " + lineNo + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("Could not read " + incidentsFile + ": " + e.getMessage());
        }
        return list;
    }

    public synchronized List<RescueTeam> loadTeams() throws FileOperationException {
        List<RescueTeam> list = new ArrayList<>();
        File file = new File(teamsFile);
        if (!file.exists()) {
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || line.startsWith("teamId")) {
                    continue;
                }
                try {
                    list.add(parseTeam(line));
                } catch (InvalidTeamDataException e) {
                    System.out.println("Skipping " + teamsFile + " line " + lineNo + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("Could not read " + teamsFile + ": " + e.getMessage());
        }
        return list;
    }

    //Saving the data
    public synchronized void saveIncident(RescueIncident incident) throws FileOperationException {
        appendLine(incidentsFile, INCIDENT_HEADER, incidentToCsv(incident));
    }

    public synchronized void updateIncident(RescueIncident incident) throws FileOperationException {
        upsertLine(incidentsFile, INCIDENT_HEADER, incident.getIncidentId(), incidentToCsv(incident));
    }

    public synchronized void saveTeam(RescueTeam team) throws FileOperationException {
        upsertLine(teamsFile, TEAM_HEADER, team.getTeamId(), teamToCsv(team));
    }

    public synchronized void saveAssignment(RescueAssignment assignment) throws FileOperationException {
        String line = String.join(",",
                assignment.getAssignmentId(),
                assignment.getIncident().getIncidentId(),
                assignment.getTeam().getTeamId(),
                assignment.getAssignedAt().toString(),
                assignment.getCompletedAt() == null ? "" : assignment.getCompletedAt().toString());
        upsertLine(assignmentsFile, ASSIGNMENT_HEADER, assignment.getAssignmentId(), line);
    }

//Convert the lists to CSV files
    private String incidentToCsv(RescueIncident i) {
        return String.join(",",
                i.getIncidentId(),
                i.getAnimal().getCategory().name(),
                clean(i.getAnimal().getSpecies()),
                i.getCondition().name(),
                clean(i.getLocation()),
                i.getLocationRisk().name(),
                String.valueOf(i.getNumberOfAnimals()),
                String.valueOf(i.isImmediateDanger()),
                String.join(";", i.getRequiredSkills()),
                String.join(";", i.getRequiredEquipment()),
                clean(i.getReporterContact()),
                i.getReportedAt().toString(),
                String.valueOf(i.getPriorityScore()),
                i.getPriority().name(),
                i.getStatus().name());
    }

    private String teamToCsv(RescueTeam t) {
        List<String> cats = new ArrayList<>();
        for (AnimalCategory c : t.getSupportedCategories()) {
            cats.add(c.name());
        }
        return String.join(",",
                t.getTeamId(),
                clean(String.join(";", t.getTeamMembers())),
                String.join(";", cats),
                String.join(";", t.getRescueSkills()),
                String.join(";", t.getEquipmentNames()),
                t.getStatus().name(),
                String.valueOf(t.getMaxSimultaneousIncidents()),
                String.valueOf(t.getCurrentWorkload()));
    }

    private RescueIncident parseIncident(String line)
            throws InvalidIncidentDataException, DuplicateIncidentException {
        String[] p = line.split(",", -1);
        if (p.length != 15) {
            throw new InvalidIncidentDataException("Invalid CSV data: expected 15 columns but found " + p.length);
        }
        try {
            AnimalCategory category = parseEnum(AnimalCategory.class, p[1], "animal type");
            AnimalCondition condition = parseEnum(AnimalCondition.class, p[3], "animal condition");
            RiskLevel risk = parseEnum(RiskLevel.class, p[5], "location risk");
            int count = Integer.parseInt(p[6].trim());
            boolean danger = parseBoolean(p[7]);
            PriorityLevel priority = parseEnum(PriorityLevel.class, p[13], "priority information");
            IncidentStatus status = parseEnum(IncidentStatus.class, p[14], "incident status");
            int score = Integer.parseInt(p[12].trim());
            if (score < 0 || score > 100) {
                throw new InvalidIncidentDataException("Invalid priority information: score " + score);
            }

            Animal animal = new Animal("AN-" + p[0], category, p[2]);
            RescueIncident incident = new RescueIncident(animal, condition, p[4], risk, count, danger,
                    splitSet(p[8]), splitSet(p[9]), p[10]);
            incident.setIncidentId(p[0].trim());
            incident.setReportedAt(LocalDateTime.parse(p[11].trim()));
            incident.setPriorityScore(score);
            incident.setPriority(priority);
            incident.updateStatus(status);
            return incident;
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new InvalidIncidentDataException("Invalid CSV data: " + e.getMessage());
        }
    }

    private RescueTeam parseTeam(String line) throws InvalidTeamDataException {
        String[] p = line.split(",", -1);
        if (p.length != 8) {
            throw new InvalidTeamDataException("Invalid CSV data: expected 8 columns but found " + p.length);
        }
        try {
            if (p[0].trim().isEmpty()) {
                throw new InvalidTeamDataException("Team ID is missing");
            }
            List<String> members = new ArrayList<>(splitSet(p[1]));
            Set<AnimalCategory> cats = new HashSet<>();
            for (String c : splitSet(p[2])) {
                cats.add(AnimalCategory.valueOf(c.toUpperCase()));
            }
            Set<Equipment> equipment = new HashSet<>();
            int n = 1;
            for (String name : splitSet(p[4])) {
                equipment.add(new Equipment("EQ-" + p[0].trim() + "-" + n++, name, name));
            }
            TeamStatus status = TeamStatus.valueOf(p[5].trim().toUpperCase());
            int max = Integer.parseInt(p[6].trim());
            int workload = Integer.parseInt(p[7].trim());
            if (members.isEmpty() || cats.isEmpty() || max <= 0 || workload < 0) {
                throw new InvalidTeamDataException("Invalid team information for " + p[0]);
            }
            RescueTeam team = new RescueTeam(p[0].trim(), members, cats, splitSet(p[3]), equipment, status, max);
            team.setCurrentWorkload(workload);
            return team;
        } catch (IllegalArgumentException e) {
            throw new InvalidTeamDataException("Invalid CSV data: " + e.getMessage());
        }
    }


    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String what)
            throws InvalidIncidentDataException {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidIncidentDataException("Invalid " + what + ": '" + value + "'");
        }
    }

    private boolean parseBoolean(String value) {
        String v = value.trim().toLowerCase();
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        throw new IllegalArgumentException("Invalid yes/no value: '" + value + "'");
    }

    private Set<String> splitSet(String text) {
        Set<String> set = new LinkedHashSet<>();
        if (text != null && !text.trim().isEmpty()) {
            for (String s : text.split(";")) {
                if (!s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        return set;
    }

    private String clean(String text) {
        return text.replace(",", " ").trim();
    }

    private void appendLine(String fileName, String header, String line) throws FileOperationException {
        File file = new File(fileName);
        boolean writeHeader = !file.exists() || file.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            if (writeHeader) {
                pw.println(header);
            }
            pw.println(line);
        } catch (IOException e) {
            throw new FileOperationException("Could not write to " + fileName + ": " + e.getMessage());
        }
    }


    private void upsertLine(String fileName, String header, String key, String newLine)
            throws FileOperationException {
        List<String> lines = new ArrayList<>();
        File file = new File(fileName);
        boolean found = false;
        try {
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        if (line.startsWith(key + ",")) {
                            lines.add(newLine);
                            found = true;
                        } else {
                            lines.add(line);
                        }
                    }
                }
            }
            if (lines.isEmpty()) {
                lines.add(header);
            }
            if (!found) {
                lines.add(newLine);
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                for (String l : lines) {
                    pw.println(l);
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("Could not update " + fileName + ": " + e.getMessage());
        }
    }
}