package com.bench;
import java.util.HashMap;
public class HmOnly {
    public static void main(String[] args) {
        long total = 0;
        for (int rep = 0; rep < 60; rep++) {
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            long checksum = 0;
            int window = 50000;
            for (int i = 0; i < 3000000; i++) {
                Integer key = Integer.valueOf(i & 0x3FFFF);
                Integer prev = map.get(key);
                map.put(key, Integer.valueOf(prev == null ? i : prev.intValue() + i));
                if (prev != null) checksum += prev.intValue();
                if (map.size() > window) map.clear();
            }
            total += checksum + map.size();
        }
        System.out.println("DONE " + total);
    }
}
