package de.ohnes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.ohnes.AlgorithmicComponents.Knapsack.MCKnapsack;
import de.ohnes.util.Job;

public class MCKnapsackTests {

    @Test
    public void testSolve() {
        // Test case 1
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job(0, new int[] { 5, 3, 1 }));
        jobs.add(new Job(1, new int[] { 4, 2, 1 }));
        jobs.add(new Job(2, new int[] { 3, 1, 1 }));
        MCKnapsack knapsack = new MCKnapsack();
        List<Job> c1 = new ArrayList<>();
        List<Job> c2 = new ArrayList<>();
        List<Job> c3 = new ArrayList<>();

        knapsack.solve(jobs, 3, c1, c2, c3, 2.5);
        assertEquals(0, c1.size());
        assertEquals(2, c2.size());
        assertEquals(1, c3.size());

    }

}
