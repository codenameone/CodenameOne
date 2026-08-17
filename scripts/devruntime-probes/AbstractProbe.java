import com.codename1.ui.*;
import java.util.*;
public abstract class AbstractProbe {
    abstract String kind();
    String describe() { return "a " + kind(); }
    static class Dog extends AbstractProbe { String kind() { return "dog"; } }
    static class Cat extends AbstractProbe {
        String kind() { return "cat"; }
        String describe() { return "definitely " + super.describe(); }
    }
    interface Shape<T extends Number> { T area(); }
    static class Sq implements Shape<Integer> {
        public Integer area() { return 4; }
    }
    public static void main(String[] a) {
        java.util.List<AbstractProbe> l = new ArrayList<AbstractProbe>();
        l.add(new Dog()); l.add(new Cat());
        StringBuilder r = new StringBuilder();
        for (AbstractProbe p : l) r.append(p.describe()).append("; ");
        Shape<Integer> s = new Sq();
        r.append("area=").append(s.area());
        System.out.println("PROBE AbstractProbe: " + r);
        new Form("Abstract").show();
    }
}
