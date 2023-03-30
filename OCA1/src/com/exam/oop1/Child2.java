package com.exam.oop1;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.sql.SQLException;

public class Child2 extends Parent2 {

    private static String childName = "Say Hi";
    private int childAge;
    Parent2 parent2;

    public Child2(String s, int number, int age) {
        super(childName, number);
        super.s2 = s;
        this.childAge = number + age;
        // TODO Auto-generated constructor stub
    }

    public Child2() {
        super("Name", 99);
    }

    public String toString() {
        return "Child2 is: " + this.childName + " " + this.childAge + " " + super.toString();
    }

    public int countNumber(int number) throws CharacterCodingException {
        return number += number + number;
    }

    public static void main(String[] args) {
        Child2 c = new Child2("Hello", 5, 10);
        System.out.println(c.toString());
        new Parent2("Override", 100795).main(args);
        Parent2 c2 = new Child2();
        try {
            System.out.println(c2.countNumber(999));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Parent2 p = new Child2();
        Child2 child = (Child2) p;
        System.out.println(child.toString());
    }

}
