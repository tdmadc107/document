package com.exam.oop12;

import java.io.IOException;

public class Animal {

    public void fly() {
        System.out.println("I can fly!!");
    }

    public void eat() {
        System.out.println("I can eat " + vibe());
    }

    Object run() {
        System.out.println("I can run!!");
        return "";
    }

    public int element() throws Throwable {
        try {
            
        } catch (Exception e) {
            e = null;
            throw e;
        }
        System.out.println("I have element");
        return 0;
    }

    private String anynomus() {
        System.out.println("Retype method");
        return "";
    }

    public static String vibe() {
        return "parent";
    }

    public static void main(String[] args) {
        Animal animal = new Dog();
        animal.fly();
        System.out.println(Animal.vibe());
        System.out.println(Dog.vibe());
    }
}
