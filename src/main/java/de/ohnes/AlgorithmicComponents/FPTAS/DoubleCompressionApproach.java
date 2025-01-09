package de.ohnes.AlgorithmicComponents.FPTAS;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.Exceptions.NoExistingSchedule;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import lombok.NoArgsConstructor;

/**
 * An implementation as suggested by Kilian Grage.
 * Suitable for an Instance with a large number of machines.
 */
@NoArgsConstructor
public class DoubleCompressionApproach implements Algorithm {

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
        double roh = epsilon / 4;
        double b = 1 / roh;
        int allotedMachines = 0;

        for (Job job : I.getJobs()) {
            int gamma_prime = 0;
            try {
                gamma_prime = allotMachines(job, d, roh, b);
            } catch (NoExistingSchedule e) {
                return ApproximationRatio.NONE; // there exists no schedule if a task cant be scheduled in (1 + epsilon)
                                                // * d time
            }
            if (gamma_prime <= b) {
                job.setAllotedMachines(gamma_prime);
                allotedMachines += gamma_prime;

            } else {
                gamma_prime = (int) Math.floor((1 - roh) * gamma_prime);
                job.setAllotedMachines(gamma_prime);
                allotedMachines += gamma_prime;
            }
        }

        if (allotedMachines > I.getM()) {
            return ApproximationRatio.NONE; // reject d
        }

        return ApproximationRatio.RATIO_FPTAS;
    }

    private int allotMachines(Job job, double d, double roh, double b) throws NoExistingSchedule {
        int gamma = job.canonicalNumberMachines(d);
        if (gamma == -1) {
            throw new NoExistingSchedule("Job cant be scheduled in deadline d.");
        }
        int gamma_prime = gamma;
        while (Math.ceil((1 - roh) * (gamma_prime + 1)) <= gamma) {
            gamma_prime++;
        }
        return (int) Math.ceil((1 - roh) * gamma_prime);
    }

    @Override
    public void setInstance(Instance I) {
        this.I = I;

    }

}
