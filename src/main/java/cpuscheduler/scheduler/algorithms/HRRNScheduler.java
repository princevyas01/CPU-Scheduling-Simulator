package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

import java.util.Locale;

/**
 * Highest Response Ratio Next (HRRN) Scheduling:
 * Non-preemptive. For each arrived waiting process, computes:
 *   Response Ratio (RR) = (Waiting Time + Burst Time) / Burst Time
 * Selects the process with the highest RR. Ties are broken by arrival time, then input order.
 */
public class HRRNScheduler extends BaseScheduler {

    public HRRNScheduler() {
        super(SchedulingAlgorithmType.HRRN);
    }

    public static double computeResponseRatio(ProcessModel p, int currentTime) {
        int waitTime = Math.max(0, currentTime - p.getArrivalTime());
        return (waitTime + (double) p.getBurstTime()) / p.getBurstTime();
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues execution non-preemptively until completion.",
                    "Remaining Burst = " + running.getRemainingBurst()
            );
        }

        if (readyQueue.isEmpty()) {
            return SchedulingDecision.idle("Ready queue empty; CPU is idle.");
        }

        readyQueue.sort((a, b) -> {
            double rrA = computeResponseRatio(a, currentTime);
            double rrB = computeResponseRatio(b, currentTime);
            if (Math.abs(rrA - rrB) > 1e-9) {
                return Double.compare(rrB, rrA); // Highest RR first
            }
            return tieBreak(a, b);
        });

        ProcessModel next = readyQueue.remove(0);
        int w = Math.max(0, currentTime - next.getArrivalTime());
        double rr = computeResponseRatio(next, currentTime);

        return SchedulingDecision.dispatch(
                next,
                String.format(Locale.US, "Process %s selected with highest response ratio RR = (%d + %d) / %d = %.2f.",
                        next.getId(), w, next.getBurstTime(), next.getBurstTime(), rr),
                String.format(Locale.US, "RR = %.2f (Wait=%d, Burst=%d)", rr, w, next.getBurstTime())
        );
    }
}
