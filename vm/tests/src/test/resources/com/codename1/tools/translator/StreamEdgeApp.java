import java.util.stream.Stream;

public class StreamEdgeApp {
    private static int calculate() {
        Object[] transformed = Stream.of(4, 2, 9, 2, 7, 4)
                .distinct()
                .sorted()
                .skip(1)
                .limit(3)
                .toArray();
        int transformedSum = ((Integer) transformed[0]).intValue()
                + ((Integer) transformed[1]).intValue()
                + ((Integer) transformed[2]).intValue();

        int reduce = Stream.of(1, 2, 3, 4).reduce(10, (a, b) -> a + b);

        final int[] forEachCode = new int[] { 0 };
        Stream.of(3, 1, 2).sorted().forEach(i -> forEachCode[0] = forEachCode[0] * 10 + i);

        Object[] arr = Stream.of(5, 6).skip(1).toArray();
        long emptyCount = Stream.<Integer>empty().count();

        int matchScore = 0;
        if (!Stream.<Integer>empty().anyMatch(v -> true)) {
            matchScore += 1;
        }
        if (Stream.<Integer>empty().allMatch(v -> false)) {
            matchScore += 10;
        }
        if (Stream.<Integer>empty().noneMatch(v -> true)) {
            matchScore += 100;
        }

        long clampCount = Stream.of(1, 2, 3).skip(5).limit(2).count();
        long distinctCount = Stream.of(1, 1, 2, 3, 3).distinct().count();

        int checksum = 0;
        checksum += transformedSum * 2;
        checksum += reduce;
        checksum += forEachCode[0];
        checksum += ((Integer) arr[0]).intValue() * 7;
        checksum += (int) emptyCount * 11;
        checksum += matchScore;
        checksum += (int) clampCount;
        checksum += (int) distinctCount * 13;
        return checksum;
    }

    public static void main(String[] args) {
        System.out.println("RESULT=" + calculate());
    }
}
