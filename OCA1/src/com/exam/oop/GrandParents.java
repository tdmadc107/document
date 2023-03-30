package com.exam.oop;

public class GrandParents {

    private String name;
    private int age;
    private GrandMama grandMama = new GrandMama("Override Mama");

    public GrandMama getGrandMama() {
        return grandMama;
    }

    public void setGrandMama(GrandMama grandMama) {
        this.grandMama = grandMama;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void style() {
        System.out.println("Oldschool");
        System.out.println(this.grandMama.nameOther);
    }

    public class GrandMama {

        private String nameOther = "Just other";

        public GrandMama(String s) {
            this.nameOther = s;
        }

        public void style() {
            System.out.println("Also OldSchool, but say mama");
        }
    }
}
