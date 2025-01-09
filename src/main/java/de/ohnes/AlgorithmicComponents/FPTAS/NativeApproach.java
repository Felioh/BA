package de.ohnes.AlgorithmicComponents.FPTAS;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import lombok.NoArgsConstructor;

/**
 * A simple FPTAS.
 * Suitable for an Instance with a large number of machines.
 */
@NoArgsConstructor
public class NativeApproach implements Algorithm {

    private Instance I;

    /**
     * Schedule all Jobs in parallel
     * Time: O(n log(m)) -> cannonicalNumerMachines takes O(log(m))
     * 
     * @param I
     * @param d
     * @param epsilon
     * @return true if there exists a schedule of length d, false otherwise.
     */
    @Override
    public ApproximationRatio solve(double d, double epsilon) {
        int allotedMachines = 0;
        for (Job job : I.getJobs()) {
            int neededMachines = job.canonicalNumberMachines((1 + epsilon) * d);
            if (neededMachines == -1) {
                return ApproximationRatio.NONE; // there exists no schedule if a task cant be scheduled in (1 + epsilon)
                                                // * d
                // time
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
