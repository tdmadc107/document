package com.exam.oop1;

import java.io.IOException;

import com.exam.oop.GrandParents;
import com.exam.oop.GrandParents.GrandMama;
import com.exam.oop.Parent;

public class Parent2 {

    private String s;
    private int number;
    protected String s2;
    
    public Parent2() {
        
    }

    public Parent2(String s, int number) {
        this.s = s;
        this.number = number;
        System.out.println(this.s);
        System.out.println(this.number);
    }

    public String toString() {
        return this.s + this.number;
    }

    protected int countNumber(int number) throws IOException {
        return ++number;
    }

    public static void main(String[] args) {
        GrandParents grandParents = new GrandParents();
        GrandMama grandMama = grandParents.new GrandMama("Override Mama in Parent");
        Parent parent = new Parent();
        parent.setAge(5);
        System.out.println(parent.getAge());
        System.out.println();
        parent.style();
        grandMama.style();
    }
}
