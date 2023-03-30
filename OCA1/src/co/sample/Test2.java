package co.sample;

public class Test2 {

    static int i1 = 10;
    static int i2 = 20;
    private void change(int val) {
        i1=++val;
        i2=val--;
    }
    private static void change2(int val) {
        i1=++val;
        i2=val--;
    }
    static public void main(String[] args) {
        char c = 'a';
        int i = c;
        for (System.out.println(i++); i < 3; System.out.println(i++)) {
            System.out.println(i++);
        }
        
        System.out.println(i1+i2);
    }
}
