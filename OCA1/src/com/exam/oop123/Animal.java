package com.exam.oop123;

public abstract class Animal {

    protected int age;

    public void eat(String s) {
        System.out.println("Animal is eating ");
    }

    abstract String getName(String name) throws RuntimeException;
//    
//    protected abstract int getAge();
}
