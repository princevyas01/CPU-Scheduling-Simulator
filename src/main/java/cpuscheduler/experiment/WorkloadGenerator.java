package cpuscheduler.experiment;

import cpuscheduler.model.ProcessModel;

import java.util.*;

/**
 * Deterministic generator for single and batch workloads.
 */
public class WorkloadGenerator {

    public static List<ProcessModel> generate(WorkloadPreset preset, int count, Long seed) {
        Random rnd = seed != null ? new Random(seed) : new Random();
        List<ProcessModel> list = new ArrayList<>();

        switch (preset) {
            case SHORT_JOBS:
                for (int i = 0; i < count; i++) {
                    int at = rnd.nextInt(6);
                    int bt = 1 + rnd.nextInt(4);
                    int pr = 1 + rnd.nextInt(4);
                    list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
                }
                break;

            case LONG_JOBS:
                for (int i = 0; i < count; i++) {
                    int at = rnd.nextInt(8);
                    int bt = 10 + rnd.nextInt(15);
                    int pr = 1 + rnd.nextInt(5);
                    list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
                }
                break;

            case MIXED_WORKLOAD:
                for (int i = 0; i < count; i++) {
                    int at = rnd.nextInt(10);
                    int bt = (i % 2 == 0) ? (1 + rnd.nextInt(3)) : (10 + rnd.nextInt(12));
                    int pr = 1 + rnd.nextInt(5);
                    list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
                }
                break;

            case STAGGERED_ARRIVALS:
                int currentAt = 0;
                for (int i = 0; i < count; i++) {
                    int bt = 3 + rnd.nextInt(6);
                    int pr = 1 + rnd.nextInt(4);
                    list.add(new ProcessModel("P" + (i + 1), currentAt, bt, pr, i));
                    currentAt += (i % 2 == 0) ? (bt - 1) : (bt + 2); // Occasional idle gaps
                }
                break;

            case PRIORITY_CONFLICT:
                for (int i = 0; i < count; i++) {
                    int at = i * 2;
                    int bt = 4 + rnd.nextInt(6);
                    int pr = (i == 0) ? 5 : (i == count - 1) ? 1 : (2 + rnd.nextInt(3));
                    list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
                }
                break;

            case HEAVY_CONTENTION:
                for (int i = 0; i < count; i++) {
                    int at = rnd.nextInt(4); // All arrive tightly clustered
                    int bt = 2 + rnd.nextInt(8);
                    int pr = 1 + rnd.nextInt(5);
                    list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
                }
                break;
        }

        return list;
    }

    public static List<ProcessModel> generateCustom(int count, int minAt, int maxAt, int minBt, int maxBt, int minPr, int maxPr, Long seed) {
        Random rnd = seed != null ? new Random(seed) : new Random();
        List<ProcessModel> list = new ArrayList<>();
        int atRange = Math.max(1, maxAt - minAt + 1);
        int btRange = Math.max(1, maxBt - minBt + 1);
        int prRange = Math.max(1, maxPr - minPr + 1);

        for (int i = 0; i < count; i++) {
            int at = minAt + rnd.nextInt(atRange);
            int bt = minBt + rnd.nextInt(btRange);
            int pr = minPr + rnd.nextInt(prRange);
            list.add(new ProcessModel("P" + (i + 1), at, bt, pr, i));
        }
        return list;
    }
}
