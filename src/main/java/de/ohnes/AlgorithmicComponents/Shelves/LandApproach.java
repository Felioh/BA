package de.ohnes.AlgorithmicComponents.Shelves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ohnes.AlgorithmicComponents.GeometricalRounding;
import de.ohnes.AlgorithmicComponents.Knapsack.ConvolutionKnapsack;
import de.ohnes.AlgorithmicComponents.Knapsack.KnapsackSolver;
import de.ohnes.util.*;

/**
 * An implementation of the algorithm by Felix Land.
 */
public class LandApproach extends MounieApproach {

    public LandApproach() {
        super();
    }

    /**
     * finds a two three shedule for the instance I with deadline d, if a schedule
     * of length d exists.
     * 
     * @param d       the deadline (makespan guess)
     * @param epsilon the "the small error"
     * @return true if a schedule of length d exists, false if none exists.
     */
    @Override
    public ApproximationRatio solve(double d, double epsilon) {

        // parameters
        final double delta = (1 / 5.0) * epsilon;
        final double roh = (1 / 4.0) * (Math.sqrt(1 + delta) - 1);
        final double b = 1 / (2 * roh - Math.pow(roh, 2));
        // final double d_quote = Math.pow((1 + roh), 2) * d;

        // "forget about small jobs"
        List<Job> shelf2 = new ArrayList<>(Arrays.asList(MyMath.findBigJobs(I, d / 2)));
        List<Job> smallJobs = new ArrayList<>(Arrays.asList(MyMath.findSmallJobs(I, d / 2)));

        // minimal work of small jobs
        double Ws = 0;
        double WShelf1 = 0;
        double WShelf2 = 0;
        for (Job job : smallJobs) {
            Ws += job.getProcessingTime(1);
        }

        // all the tasks are initially allotted to their canonical number of processors
        // to respect the d/2 threshold
        for (Job job : shelf2) {
            job.setAllotedMachines(job.canonicalNumberMachines(d / 2));
            if (job.getAllotedMachines() != -1) {
                WShelf2 += job.getAllotedMachines() * job.getProcessingTime(job.getAllotedMachines()); // update the
                                                                                                       // work of shelf2
            }
        }

        // transform to knapsack problem
        int[] profit = new int[shelf2.size()];
        int[] weight = new int[shelf2.size()];
        // int C = I.getM() - cap;
        for (int i = 0; i < shelf2.size(); i++) {
            Job job = shelf2.get(i);
            int dAllotment = job.canonicalNumberMachines(d); // Note: Can not be -1. Since the has to exost a schedule
                                                             // with makespan d.
            int dHalfAllotment = job.getAllotedMachines();

            if (dAllotment > b) { // rounding
                dAllotment = (int) GeometricalRounding.gFloor(dAllotment, b, I.getM(), 1 + roh);
            }
            if (dHalfAllotment > b) { // rounding
                dHalfAllotment = (int) GeometricalRounding.gFloor(dHalfAllotment, b, I.getM(), 1 + roh);
            }

            if (dAllotment == -1) { // there cant exists a schedule of legnth d if any job cant be scheduled in d
                                    // time.
                return ApproximationRatio.NONE;
            }

            // weight of an item-task will be its canonical number of processors needed to
            // respect the threshold d
            weight[i] = dAllotment;

            if (dHalfAllotment != -1) {
                // profit of an item-task will correspond to the work saving obtained by
                // executing the task just to respect the threshold d instead of d/2
                // w_{i, y{i, d/2} - w_{i, y{i, d}}
                profit[i] = (dHalfAllotment * job.getProcessingTime(dHalfAllotment))
                        - (dAllotment * job.getProcessingTime(dAllotment));
                if (dHalfAllotment < b) { // this means the job has been compressed.
                    if (profit[i] < (delta / 2) * d) {
                        profit[i] = 0;
                    } else {
                        profit[i] = (int) GeometricalRounding.gCeil(profit[i], (delta / 2) * d, (b / 2) * d,
                                1 + (delta / b));
                    }
                } else { // not compressed job
                    double dHalfTime = GeometricalRounding.gFloor(
                            job.getProcessingTime(job.canonicalNumberMachines(d / 2)), d / 4, d / 2, 1 + (delta / b));
                    double dTime = GeometricalRounding.gFloor(job.getProcessingTime(job.canonicalNumberMachines(d)),
                            d / 2, d, 1 + (delta / b));

                    profit[i] = (int) ((dHalfTime * dHalfAllotment) - (dTime * dAllotment));
                }

            } else { // job has to be scheduled on s1.
                profit[i] = (int) Math.round(I.getM() * d); // really big.
            }

        }

        KnapsackSolver kS = new ConvolutionKnapsack();
        List<Job> shelf1 = kS.solve(shelf2, weight, profit, shelf2.size(), I.getM());
        shelf2.removeAll(shelf1); // update shelf2
        int p1 = 0; // processors required by S1.
        for (Job selectedJob : shelf1) {
            // update WShelf2
            if (selectedJob.getAllotedMachines() != -1) {
                WShelf2 -= selectedJob.getAllotedMachines()
                        * selectedJob.getProcessingTime(selectedJob.getAllotedMachines());
            }
            // "move job to shelf1"
            int dAllotment = selectedJob.canonicalNumberMachines(d);
            if (dAllotment > b) { // rounding
                dAllotment = (int) GeometricalRounding.gFloor(dAllotment, b, I.getM(), 1 + roh);
            }
            selectedJob.setAllotedMachines(dAllotment);
            p1 += dAllotment; // keep track of the number of machines used by s1

            // update WShelf1
            WShelf1 += selectedJob.getAllotedMachines()
                    * selectedJob.getProcessingTime(selectedJob.getAllotedMachines());
        }

        if (WShelf1 + WShelf2 > I.getM() * d - Ws) { // there cant exists a schedule of with makespan d (s. Thesis Felix
                                                     // S. 76)
            return ApproximationRatio.NONE;
        }

        // ############################################## DEBUG
        // ##################################################################################################################
        // System.out.println();
        // // System.out.println(printSchedule.printTwoShelves(bigJobs, (int) d));
        // System.out.println(printSchedule.printTwoShelves(MyMath.findBigJobs(I, d),
        // (int) d));
        // ############################################## DEBUG
        // ##################################################################################################################

        List<Job> shelf0 = applyTransformationRules(d, shelf1, shelf2, p1);

        // ############################################## DEBUG
        // ##################################################################################################################
        // System.out.println();
        // // System.out.println(printSchedule.printTwoShelves(bigJobs, (int) d));
        // System.out.println(printSchedule.printThreeShelves(shelf0, shelf1, shelf2));
        // ############################################## DEBUG
        // ##################################################################################################################
        addSmallJobs(shelf1, shelf2, smallJobs, d, I.getM());

        List<Machine> machinesS0 = new ArrayList<>();
        double startTime = -1;
        for (Job job : shelf0) {
            if (job.getStartingTime() != startTime) {
                Machine m = new Machine(0);
                m.addJob(job);
                machinesS0.add(m);
                startTime = job.getProcessingTime(job.getAllotedMachines());
            } else {
                machinesS0.get(machinesS0.size() - 1).addJob(job);
                startTime += job.getProcessingTime(job.getAllotedMachines());
            }
        }
        I.addMachines(machinesS0);

        // ############################################## DEBUG
        // ##################################################################################################################
        // System.out.println(String.format("-".repeat(70) + "%04.2f" + "-".repeat(70),
        // d));
        // System.out.println(printSchedule.printMachines(I.getMachines()));
        // System.out.println(String.format("-".repeat(70) + "%04.2f" + "-".repeat(70),
        // d));
        // ############################################## DEBUG
        // ##################################################################################################################

        return ApproximationRatio.RATIO_3_2;
    }

}
