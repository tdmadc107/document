package test2;

import java.io.IOException;
import java.sql.SQLException;

public class TestOca1 {

    private static void name1() throws SQLException {
        try {
            throw new SQLException();
        } catch (SQLException e) {
            e = null;
            throw e;
        }
    }

    public static void main(String[] args) {
        TestSample.main(args);
        try {
            name1();
        } catch (SQLException e) {
            System.out.println("A");
        }
    }
}
