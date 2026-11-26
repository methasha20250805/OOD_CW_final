import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TeamMatcher {

    public List<RescueTeam> findSuitableTeams(RescueIncident incident, List<RescueTeam> teams) {
        List<RescueTeam> suitable = new ArrayList<>();
        for (RescueTeam team : teams) {
            try {
                if (checkAnimalCompatibility(incident, team)
                        && checkRequiredSkills(incident, team)
                        && checkEquipment(incident, team)
                        && checkAvailability(team)) {
                    suitable.add(team);
                }
            } catch (TeamAssignmentException e) {
                // team does not have the required equipment, so it is not suitable
            }
        }
        return rankTeams(incident, suitable);
    }

    public List<RescueTeam> rankTeams(RescueIncident incident, List<RescueTeam> teams) {
        List<RescueTeam> ranked = new ArrayList<>(teams);
        Collections.sort(ranked, new Comparator<RescueTeam>() {
            @Override
            public int compare(RescueTeam a, RescueTeam b) {
                return Integer.compare(calculateMatchScore(incident, b), calculateMatchScore(incident, a));
            }
        });
        return ranked;
    }

    public int calculateMatchScore(RescueIncident incident, RescueTeam team) {
        int score = 0;
        if (checkAnimalCompatibility(incident, team)) score += 40;
        if (checkRequiredSkills(incident, team)) score += 30;
        if (hasEquipment(incident, team)) score += 20;
        if (checkAvailability(team)) score += 10;
        score -= team.getCurrentWorkload() * 5;   // busier teams rank lower
        return score;
    }

    public boolean checkAnimalCompatibility(RescueIncident incident, RescueTeam team) {
        return team.getSupportedCategories().contains(incident.getAnimal().getCategory());
    }

    public boolean checkRequiredSkills(RescueIncident incident, RescueTeam team) {
        return team.getRescueSkills().containsAll(incident.getRequiredSkills());
    }

    public boolean checkEquipment(RescueIncident incident, RescueTeam team) throws TeamAssignmentException {
        if (!hasEquipment(incident, team)) {
            throw new TeamAssignmentException("Team " + team.getTeamId()
                    + " does not have the required equipment: " + incident.getRequiredEquipment());
        }
        return true;
    }

    public boolean checkAvailability(RescueTeam team) {
        return team.getStatus() != TeamStatus.UNAVAILABLE
                && team.getCurrentWorkload() < team.getMaxSimultaneousIncidents();
    }

    private boolean hasEquipment(RescueIncident incident, RescueTeam team) {
        return team.getEquipmentNames().containsAll(incident.getRequiredEquipment());
    }
}