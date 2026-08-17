import com.codename1.ui.*;
public class EnumProbe {
    enum Color { RED, GREEN, BLUE;
        String low() { return name().toLowerCase(); } }
    public static void main(String[] a) {
        StringBuilder r = new StringBuilder();
        for (Color c : Color.values()) r.append(c).append(":").append(c.ordinal()).append(" ");
        Color c = Color.GREEN;
        switch (c) { case GREEN: r.append("switch=green"); break; default: r.append("switch=?"); }
        r.append(" valueOf=").append(Color.valueOf("BLUE")).append(" low=").append(c.low());
        System.out.println("PROBE EnumProbe: " + r);
        new Form("Enum").show();
    }
}
