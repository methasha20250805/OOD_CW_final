public class PriorityCalculator {

    public int calculatePriority(RescueIncident incident) {
        int score = 0;

        // a) animal condition
        switch (incident.getCondition()) {
            case INJURED:   score += 30; break;
            case TRAPPED:   score += 20; break;
            case ABANDONED: score += 15; break;
            case DISPLACED: score += 10; break;
        }

        // b) immediate danger
        if (incident.isImmediateDanger()) {
            score += 30;
        }

        // c) number of animals (4 points each, max 20)
        score += Math.min(incident.getNumberOfAnimals(), 5) * 4;

        // d) location / environmental risk
        score += getLocationRiskWeight(incident.getLocationRisk());

        return score;
    }

    public int getLocationRiskWeight(RiskLevel risk) {
        switch (risk) {
            case LOW:    return 5;
            case MEDIUM: return 10;
            case HIGH:   return 15;
            case SEVERE: return 20;
            default:     return 0;
        }
    }

    public PriorityLevel classifyPriority(int score) {
        if (score >= 65) {
            return PriorityLevel.CRITICAL;
        } else if (score >= 45) {
            return PriorityLevel.HIGH;
        } else if (score >= 30) {
            return PriorityLevel.MEDIUM;
        }
        return PriorityLevel.LOW;
    }
}