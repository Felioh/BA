package de.ohnes.AlgorithmicComponents.Shelves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.AlgorithmicComponents.Knapsack.DynamicKnapsack;
import de.ohnes.AlgorithmicComponents.Knapsack.KnapsackSolver;
import de.ohnes.logger.printSchedule;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import de.ohnes.util.Machine;
import de.ohnes.util.MyMath;
import lombok.NoArgsConstructor;

/**
 * An implementation of the Algorithm by Mounie, Rapine, Trystram
 */
@NoArgsConstructor
public class MounieApproach implements Algorithm {

    protected Instance I;

    /**
     * finds a two two shedule for the instance I with deadline d, if a schedule of
     * length d exists.
     * 
     * @param d       the deadline (makespan guess)
     * @param epsilon the "the small error"
     * @return true if a schedule of length d exists, false if none exists.
     */
    public ApproximationRatio solve(double d, double epsilon) {
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
            WShelf2 += job.getAllotedMachines() * job.getProcessingTime(job.getAllotedMachines()); // update the work of
                                                                                                   // shelf2
        }

        // System.out.println(printSchedule.printTwoShelves(bigJobs, (int) d));

        // transform to knapsack problem
        int[] profit = new int[shelf2.size()];
        int[] weight = new int[shelf2.size()];
        // int C = I.getM() - cap;
        for (int i = 0; i < shelf2.size(); i++) {
            Job job = shelf2.get(i);
            int dAllotment = job.canonicalNumberMachines(d); // Note: Can not be -1. Since the has to exost a schedule
                                                             // with makespan d.
            int dHalfAllotment = job.getAllotedMachines();

            if (dAllotment == -1) { // there cant exists a schedule of legnth d if any job cant be scheduled in d
                                    // time.
                return ApproximationRatio.NONE;
            }

            weight[i] = dAllotment;

            if (dHalfAllotment != -1) {
                // profit of an item-task will correspond to the work saving obtained by
                // executing the task just to respect the threshold d instead of d/2
                // w_{i, y{i, d/2} - w_{i, y{i, d}}
                profit[i] = (dHalfAllotment * job.getProcessingTime(dHalfAllotment))
                        - (dAllotment * job.getProcessingTime(dAllotment));
                // weight of an item-task will be its canonical number of processors needed to
                // respect the threshold d
            } else {
                profit[i] = (int) Math.round(I.getM() * d); // really big.
            }

        }

        // bigJobs = MyMath.dynamicKnapsack(bigJobs, weight, profit, bigJobs.length,
        // I.getM(), I, d);
        KnapsackSolver kS = new DynamicKnapsack();
        // int p1 = 0;
        List<Job> shelf1 = kS.solve(shelf2, weight, profit, shelf2.size(), I.getM());
        shelf2.removeAll(shelf1); // update shelf2
        for (Job selectedJob : shelf1) {
            // update WShelf2
            WShelf2 -= selectedJob.getAllotedMachines()
                    * selectedJob.getProcessingTime(selectedJob.getAllotedMachines());

            // "move job to shelf1"
            selectedJob.setAllotedMachines(selectedJob.canonicalNumberMachines(d));
            // p1 += selectedJob.canonicalNumberMachines(d); //keep track of p1

            // update WShelf1
            WShelf1 += selectedJob.getAllotedMachines()
                    * selectedJob.getProcessingTime(selectedJob.getAllotedMachines());
        }

        if (WShelf1 + WShelf2 > I.getM() * d - Ws) { // there cant exists a schedule of with makespan (s. Thesis Felix
                                                     // S. 76)
            return ApproximationRatio.NONE;
        }

        System.out.println();
        // System.out.println(printSchedule.printTwoShelves(bigJobs, (int) d));
        System.out.println(printSchedule.printTwoShelves(MyMath.findBigJobs(I, d), (int) d));

        // List<Job> shelf0 = applyTransformationRules(d, shelf1, shelf2, p1);

        addSmallJobs(shelf1, shelf2, smallJobs, d, I.getM());

        return ApproximationRatio.RATIO_3_2;
    };

    protected void addSmallJobs(List<Job> shelf1, List<Job> shelf2, List<Job> smallJobs, double d,
            int availableMachines) {

        Machine[] machines = new Machine[availableMachines]; // keep track of the free time of each processor.
        int s1_i = 0;
        int s2_i = 0;
        int m1 = 0;
        int m2 = 0;
        for (int m = 0; m < availableMachines; m++) {
            machines[m] = new Machine(m);

            if (s1_i < shelf1.size()) {
                machines[m].addJob(shelf1.get(s1_i));
                if (m1 == 0)
                    m1 = shelf1.get(s1_i).getAllotedMachines();
            }
            if (--m1 == 0) {
                s1_i++;
            }

            if (s2_i < shelf2.size()) {
                // set starting time of the job from shelf 2.
                int startTime = (int) (3 / 2.0 * d)
                        - shelf2.get(s2_i).getProcessingTime(shelf2.get(s2_i).getAllotedMachines());
                shelf2.get(s2_i).setStartingTime(startTime);
                machines[m].addJob(shelf2.get(s2_i));
                if (m2 == 0)
                    m2 = shelf2.get(s2_i).getAllotedMachines();
            }
            if (--m2 == 0) {
                s2_i++;
            }

        }

        // allot small jobs.
        int i = 0;
        for (Job job : smallJobs) {
            while ((3 / 2.0 * d) - machines[i].getUsedTime() < job.getProcessingTime(1)) { // should not be an infinite
                                                                                           // loop if "WShelf1 + WShelf2
                                                                                           // <= I.getM() * d - Ws" ->
                                                                                           // (s. Thesis Felix p.78)
                if (i == machines.length - 1) {
                    i = 0;
                } else {
                    i++;
                }
            }
            job.setAllotedMachines(1);
            job.setStartingTime((int) machines[i].getFirstFreeTime());
            machines[i].addJob(job);

        }

        this.I.setMachines(machines);
    }

    /**
     * converts a two shelf schedule to a feasible three schelves schedule. (s.
     * 5.3.1.3 Thesis Felix)
     * 
     * @param I an Instance. A two Shelves schedule has to aleady been build.
     * @param d the deadline (also of the two shelves Schedule)
     * @return true if there exists a feasible schedule, false if not
     */
    protected List<Job> applyTransformationRules(double d, List<Job> shelf1, List<Job> shelf2, int p1) {

        List<Job> shelf0 = new ArrayList<>();
        int p0 = 0; // processors required by S0.

        List<Job> jobsToDelete = new ArrayList<>();
        for (Job job : shelf2) {
            int q = I.getM() - (p1 + p0);
            if (q > 0 && job.getProcessingTime(q) <= (3 / 2.0) * d) {
                int p = job.canonicalNumberMachines((3 / 2.0) * d);
                job.setAllotedMachines(p); // either S0 or S1.
                jobsToDelete.add(job);
                if (job.getProcessingTime(p) > d) {
                    p0 += p;
                    shelf0.add(job);
                } else {
                    p1 += p;
                    shelf1.add(job);
                }
            }
        }
        shelf2.removeAll(jobsToDelete);

        Job singleSmallJob = null;
        int i = 0;
        while (i < shelf1.size()) {
            Job job = shelf1.get(i);
            int allotedMachines = job.getAllotedMachines();
            int pTime = job.getProcessingTime(allotedMachines);

            if (pTime <= (3 / 4.0) * d && allotedMachines > 1) {
                p1 -= job.getAllotedMachines();
                job.setAllotedMachines(allotedMachines - 1); // assign to shelf 0.
                shelf0.add(job);
                shelf1.remove(job);
                p0 += job.getAllotedMachines();
                continue;
            } else if (pTime <= (3 / 4.0) * d && allotedMachines == 1) {
                if (singleSmallJob == null) {
                    singleSmallJob = job;
                } else {
                    p1 -= job.getAllotedMachines(); // == 1
                    p1 -= singleSmallJob.getAllotedMachines();
                    p0 += job.getAllotedMachines();
                    singleSmallJob.setStartingTime(pTime); // assign both to shelf 0.
                    shelf0.add(job);
                    shelf0.add(singleSmallJob);
                    shelf1.remove(job);
                    shelf1.remove(singleSmallJob);
                    singleSmallJob = null;
                    continue;
                }
            }
            i++;
        }

        return shelf0;

    }

    @Override
    public void setInstance(Instance I) {
        this.I = I;

    }

}
