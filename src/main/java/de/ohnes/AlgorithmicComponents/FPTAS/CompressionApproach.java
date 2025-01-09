package de.ohnes.AlgorithmicComponents.FPTAS;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import lombok.NoArgsConstructor;

/**
 * An implementation as suggested by Felix Land.
 * Suitable for an Instance with a large number of machines.
 */
@NoArgsConstructor
public class CompressionApproach implements Algorithm {

    private Instance I;

    /**
     * Schedule Jobs to respect d threshold and then compress big jobs.
     * 
     * @param I       the instance {@link Instance}.
     * @param d       the deadline (estimated Optimum.)
     * @param epsilon
     * @return true if there exists a schedule of length d, false otherwise.
     */
    @Override
    public ApproximationRatio solve(double d, double epsilon) {
        int allotedMachines = 0;
        for (Job job : I.getJobs()) {
            int neededMachines = job.canonicalNumberMachines(d);
            if (neededMachines == -1) {
                return ApproximationRatio.NONE; // there exists no schedule if a task cant be scheduled in (1 + epsilon)
                                                // * d
                // time
            }
            if (neededMachines >= (4 / epsilon)) { // compress big jobs
                // free (epsilon / 4) * neededMachines (compression)
                neededMachines = (int) Math.ceil((epsilon / 4) * neededMachines); // because of monotony the jobs should
                                                                                  // not take longer than (1 +
                                                                                  // epsilon)*d
            }
            job.setAllotedMachines(neededMachines);
            allotedMachines += neededMachines;
        }

        if (allotedMachines > I.getM()) {
            return ApproximationRatio.NONE; // reject d
        }

        return ApproximationRatio.RATIO_FPTAS;
    }

    @Override
    public void setInstance(Instance I) {
        this.I = I;

    }

}
