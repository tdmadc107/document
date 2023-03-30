package new1;

public class Sample {

    static String a;
    static int b;

    public Sample(String a, int b) {
        this.a = a;
        this.b = b;
    }

    public Sample(String a) {
        new Sample(a, 1);
    }

    public static void fly(long i) {
        System.out.println("Int");
    };

    public static void fly(Long... l) {
        System.out.println("Long");
    };

    public static void main(String[] args) {
        fly(1L);
        fly(1);

    }
}
