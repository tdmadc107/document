package co.sample;

public class ChildOne extends ParentOne implements CheckLog {

    static int b;
    Object o = "Hello";
    protected String s = "ChildOne";

    public String getS() throws IllegalArgumentException, RuntimeException, Exception {
        return s;
    }
    
    

    public static void main(String[] args) {
        b= 100;
        ParentOne p = new ChildOne();
        try {
            System.out.println(p.s + p.getS());
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        ChildOne childOne = new ChildOne();
        

        if(childOne instanceof ParentOne) {
            System.out.println("TRUE");
        }
        
        System.out.println(CheckLog.i);
        System.out.println(CheckLog2.i);
        System.out.println(CheckLog3.i);
        
        if(b< 200)
            b = 200;
        else if (b>200)
            b = 300;
        else if (b== 100)
            b = 100;
        System.out.println(b);
    }

    @Override
    public void check() {
        System.out.println("Checked");

    }
}
