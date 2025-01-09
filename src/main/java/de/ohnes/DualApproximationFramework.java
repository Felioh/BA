package de.ohnes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.AlgorithmicComponents.Approximation.Approximation;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;

public class DualApproximationFramework {

    private static final Logger LOGGER = LogManager.getLogger(DualApproximationFramework.class);

    // an fptas that is to be used for a large number of machines (>= 8*(n/epsilon))
    private Algorithm fptas;
    private Algorithm knapsack;
    private Instance I;
    private Approximation approx;

    public DualApproximationFramework(Algorithm fptas, Algorithm knapsack, Approximation approx, Instance I) {
        this.fptas = fptas;
        this.knapsack = knapsack;
        this.approx = approx;
        this.I = I;
    }

    public double start(double epsilon) {

        Algorithm usedAlgo;
        if (I.getM() >= 8 * (I.getN() / epsilon)) {
            LOGGER.info("Starting dual approximation Framework with fptas: {}", this.getFPTASName());
            usedAlgo = this.fptas;
            usedAlgo.setInstance(I);
        } else {
            LOGGER.info("Starting dual approximation Framework with shelvesAlgo: {}", this.getShelvesAlgoName());
            usedAlgo = this.knapsack;
            usedAlgo.setInstance(I);
        }
        double lowerBound = this.approx.approximate(I) / 2; // TODO this bound could be thighter.
        double upperBound = lowerBound * 8; // TODO add list scheduling. -> schedule twiari greedy and divide by 2.

        return binarySearch(usedAlgo, epsilon, lowerBound, upperBound);
    }

    private double binarySearch(Algorithm algo, double epsilon, double l, double r) {

        double mid = l + (r - l) / 2;
        I.resetInstance(); // reset the instance because it was altered in previous attempt.
        ApproximationRatio result = algo.solve(mid, epsilon);
        if (!result.equals(ApproximationRatio.NONE)) { // a schedule of length "mid" exists

            if (r - mid < epsilon) {
                LOGGER.info("Found schedule with makespan: {}", mid);
                LOGGER.info("Guaranteed approximation ratio: {}", result);
                return mid;
            }

            return binarySearch(algo, epsilon, l, mid); // try to find a better schedule
        } else { // no schedule for length "mid" exists
            return binarySearch(algo, epsilon, mid, r); // find a schedule for worse makespan
        }
    }

    public String getFPTASName() {
        return this.fptas.getClass().getSimpleName();
    }

    public String getApproximationName() {
        return this.approx.getClass().getSimpleName();
    }

    public String getShelvesAlgoName() {
        return this.knapsack.getClass().getSimpleName();
    }

}
