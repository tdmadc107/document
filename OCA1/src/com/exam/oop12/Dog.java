package com.exam.oop12;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Dog extends Animal {

    public void fly() { // is override
        System.out.println("I can't fly :((");
    }

    public void fly(int n) { // is overload
        System.out.println("I can fly " + n + "m");
    }

    public void eat() { // is override
        System.out.println("I can eat " + " and run " + "m" + vibe());
    }

    protected StringBuilder run() { // is override
        System.out.println("I can run is very fast!!");
        return new StringBuilder("Hello");
    }

    protected int run(int m) { // is overload
        System.out.println("I can run " + m + "m");
        return 0;
    }

    public int element() throws Error {
        return 10;
    }

    public String anynomus() {
        System.out.println("Retype method at child class.");
        return "";
    }

    public static String vibe() {
        return "child";
    }
}
