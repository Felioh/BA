package de.ohnes;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ohnes.AlgorithmicComponents.Approximation.TwoApproximation;
import de.ohnes.AlgorithmicComponents.FPTAS.DoubleCompressionApproach;
import de.ohnes.AlgorithmicComponents.Shelves.GrageApproach;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import de.ohnes.util.Machine;

/**
 * Tests for the complete program.
 * 
 * tests all predefined instances from the TestInstances Folder as well as some
 * randomly generated ones.
 */
@RunWith(Parameterized.class)
public class GrageTests {

    private Instance I;
    private double d;

    /**
     * generate a Random Instance an solve it.
     */
    public GrageTests(Instance I) {
        this.I = I;
        DualApproximationFramework dualApproxFramework = new DualApproximationFramework(new DoubleCompressionApproach(),
                new GrageApproach(), new TwoApproximation(), I);
        this.d = dualApproxFramework.start(0.1);
    }

    /**
     * Test all test-Instances from the folder TestInstances
     * and some randomly generated Isntances.
     * 
     * @return
     */
    @Parameterized.Parameters
    public static List<Object[]> input() {
        List<Instance[]> instances = new ArrayList<>();

        File dir = new File("TestInstances");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File testFile : files) {
                Instance[] args = new Instance[1];
                try {
                    args[0] = new ObjectMapper().readValue(testFile, Instance.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                instances.add(args);
            }
        }

        for (int i = 0; i < 20; i++) {
            Instance[] args = new Instance[1];
            args[0] = new Instance(0, 0, null);
            args[0].generateRandomInstance(100, 1000, 10, 50, 100);
            instances.add(args);
        }

        return Arrays.asList(instances.toArray(Object[][]::new));
    }

    /**
     * Tests if the returned Schedule is valid
     */
    @Test
    public void scheduleIsValid() {
        for (Job job : I.getJobs()) {
            assertTrue("Every Job should be alloted to at least one Machine", job.getAllotedMachines() > 0);
            int allotedMachines = job.getAllotedMachines();
            for (Machine m : I.getMachines()) {
                if (m.getJobs().contains(job)) {
                    allotedMachines--;
                }
            }
            assertTrue("The number of machines referencing this job differs from the specified amount in the job",
                    allotedMachines == 0);
        }
    }

    /**
     * Test if the Makespan is smaller than the promised one. (3/2 * d) Note:
     * epsilon is not accounted for.
     */
    @Test
    public void testMakespan() {
        // System.out.println(printSchedule.printThreeShelves(MyMath.findBigJobs(I, d),
        // (int) d));
        assertTrue("The total makespan needs to be smaller than the promised one.", I.getMakespan() <= d * (3 / 2.0));
    }

    /**
     * there should not be more than m machines used at time 0.
     */
    @Test
    public void machineUsageTime0() {
        int usedMachines = 0;
        for (Job job : I.getJobs()) {
            if (job.getStartingTime() == 0) {
                usedMachines += job.getAllotedMachines();
            }
        }
        assertTrue("there should not be more than m machines used at time 0.", I.getM() >= usedMachines);
    }

    /**
     * there should not be more than m machines used at time d + 1.
     */
    @Test
    public void machineUsageTimed1() {
        int usedMachines = 0;
        for (Job job : I.getJobs()) {
            if (job.getStartingTime() < d + 1
                    && job.getStartingTime() + job.getProcessingTime(job.getAllotedMachines()) > d + 1) {
                usedMachines += job.getAllotedMachines();
            }
        }
        assertTrue("there should not be more than m machines used at time 0.", I.getM() >= usedMachines);
    }
}
