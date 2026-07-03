package com.bench;
public class SbOnly {
    public static void main(String[] args) {
        long checksum = 0;
        for (int rep = 0; rep < 80; rep++) {
            for (int i = 0; i < 400000; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("item-");
                sb.append(i);
                sb.append('/');
                sb.append(i & 1023);
                String s = sb.toString();
                checksum += s.length() + s.hashCode();
            }
        }
        System.out.println("DONE " + checksum);
    }
}
