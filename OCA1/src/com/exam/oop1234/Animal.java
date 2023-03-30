package com.exam.oop1234;

import java.sql.SQLException;

public interface Animal {

    int heigth = 10;
    public static final int age = 0;

    double getHeigth() throws SQLException;

    default double getAge() {
        return 90;
    }

    static String setAge() {
        return " age";
    }

}
