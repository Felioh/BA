package de.ohnes.AlgorithmicComponents;

import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;

public interface Algorithm {
    public ApproximationRatio solve(double d, double epsilon);

    public void setInstance(Instance I);
}
