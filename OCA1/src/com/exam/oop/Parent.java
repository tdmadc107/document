package com.exam.oop;

public class Parent extends GrandParents {

    GrandMama grandParents = new GrandMama("N O No");
    
//    public void style() {
//        System.out.println("Bolero");
//        grandParents.style();
//    }

    
    public static void main(String[] args) {
        GrandParents grandParents = new Parent();
        Parent parent = new Child();
        grandParents.setGrandMama(grandParents.new GrandMama("Override V2.1"));
        grandParents.style();
        parent.style();
        
    }
}
