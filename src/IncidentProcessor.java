import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncidentProcessor {
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private IncidentManager incidentManager;

    public IncidentProcessor(IncidentManager incidentManager) {
        this.incidentManager = incidentManager;
    }

    // a) process multiple incoming rescue reports at the same time
    public void processIncomingReports() {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (RescueIncident incident : incidentManager.getAllIncidents()) {
            if (incident.getStatus() == IncidentStatus.REPORTED) {
                tasks.add(() -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Processing report "
                            + incident.getIncidentId());
                    incidentManager.processIncident(incident);
                    System.out.println("[" + Thread.currentThread().getName() + "] " + incident.getIncidentId()
                            + " -> " + incident.getPriority() + ", status " + incident.getStatus());
                    return null;
                });
            }
        }
        runAll(tasks);
    }

    // b) calculate the priority of many incidents concurrently
    public void calculatePrioritiesConcurrently() {
        PriorityCalculator calculator = new PriorityCalculator();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (RescueIncident incident : incidentManager.getAllIncidents()) {
            tasks.add(() -> {
                int score = calculator.calculatePriority(incident);
                incident.setPriorityScore(score);
                incident.setPriority(calculator.classifyPriority(score));
                System.out.println("[" + Thread.currentThread().getName() + "] " + incident.getIncidentId()
                        + " score=" + score + " level=" + incident.getPriority());
                return null;
            });
        }
        runAll(tasks);
    }

    // c) search for suitable teams for many pending incidents concurrently
    public void matchPendingIncidentsConcurrently() {
        TeamMatcher matcher = new TeamMatcher();
        List<RescueTeam> teams = incidentManager.getAllTeams();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (RescueIncident incident : incidentManager.getPendingIncidents()) {
            tasks.add(() -> {
                List<RescueTeam> suitable = matcher.findSuitableTeams(incident, teams);
                StringBuilder ids = new StringBuilder();
                for (RescueTeam t : suitable) {
                    ids.append(t.getTeamId()).append(" ");
                }
                System.out.println("[" + Thread.currentThread().getName() + "] " + incident.getIncidentId()
                        + " -> suitable teams: " + (suitable.isEmpty() ? "none (still pending)" : ids.toString().trim()));
                return null;
            });
        }
        runAll(tasks);
    }

    private void runAll(List<Callable<Void>> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("Nothing to process.");
            return;
        }
        try {
            executor.invokeAll(tasks);   // waits until every task has finished
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Processing was interrupted.");
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}