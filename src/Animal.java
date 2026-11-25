public class Animal {
    private String animalId;
    private AnimalCategory category;
    private String species;

    public Animal(String id, AnimalCategory category, String species) {
        this.animalId = id;
        this.category = category;
        this.species = species;
    }

    public AnimalCategory getCategory() {
        return category;
    }

    public String getSpecies() {
        return species;
    }

    public String getAnimalId() {
        return animalId;
    }

    public String getAnimalDetails() {
        return category + " (" + species + ")";
    }
}