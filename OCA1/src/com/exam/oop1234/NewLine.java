package com.exam.oop1234;

import java.io.IOException;
import java.sql.SQLWarning;

public class NewLine implements Lion, Human {

//    @Override
//    public double getAge() {
//        // TODO Auto-generated method stub
//        return 50;
//    }

    public static void main(String[] args) throws SQLWarning {
        String s = "Rebus";
        System.out.println(new NewLine().getHeigth());
        System.out.println(new NewLine().getAge());

        Animal[] animals = new Animal[2];

        for (Animal a : animals) {
            System.out.println(a.heigth);
            System.out.println(a.getAge());
        }

        NewLine[] nls = new NewLine[2];
        for (NewLine nl : nls) {
            System.out.println(nl.age);
            System.out.println(nl.getAge());
        }

    }

    @Override
    public double getHeigth() throws SQLWarning {
        // TODO Auto-generated method stub
        return 50;
    }

}
