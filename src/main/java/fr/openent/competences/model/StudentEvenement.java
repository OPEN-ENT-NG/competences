package fr.openent.competences.model;

public class StudentEvenement {
    private String name;
    private long count;
    private String unit = "";

    public StudentEvenement() {
    }

    public StudentEvenement(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public StudentEvenement(String name, long count, String unit) {
        this.name = name;
        this.count = count;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }


    public void setCount(int count) {
        this.count = count;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String toString(){
        return name + " : [" + count + "]" + unit;
    }
}
