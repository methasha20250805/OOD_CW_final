public class Equipment {
    private String equipmentId;
    private String equipmentName;
    private String description;

    public Equipment(String id, String name, String description) {
        this.equipmentId = id;
        this.equipmentName = name;
        this.description = description;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getEquipmentDetails() {
        return equipmentId + ": " + equipmentName + " - " + description;
    }
}