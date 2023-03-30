package com.exam.oop;

public class Child extends Parent {

    public void style() {
        System.out.println("NewSchool - Trap");
        grandParents.style();
    }
}
