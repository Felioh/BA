package de.ohnes.AlgorithmicComponents.Shelves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.AlgorithmicComponents.Knapsack.MCKnapsack;
import de.ohnes.util.ApproximationRatio;
import de.ohnes.util.Instance;
import de.ohnes.util.Job;
import de.ohnes.util.Machine;
import de.ohnes.util.MyMath;

public class OhnesorgeApproach implements Algorithm {

    private Instance I;

    /**
     * finds a shedule for the instance I with deadline d, if a schedule of
     * length d exists.
     * 
     * @param d       the deadline (makespan guess)
     * @param epsilon the "the small error"
     * @return true if a schedule of length d exists, false if none exists.
     */
    @Override
    public ApproximationRatio solve(double d, double epsilon) {
        double t = 3 * d / 7;
        // forget about small jobs
        List<Job> bigJobs = new ArrayList<>(Arrays.asList(MyMath.findBigJobs(I, t)));
        List<Job> smallJobs = new ArrayList<>(Arrays.asList(MyMath.findSmallJobs(I, t)));

        // minimal work of small jobs
        int Ws = smallJobs.stream().mapToInt(j -> j.getProcessingTime(1)).sum();

        // solve the knapsack problem
        List<Job> shelf1 = new ArrayList<>();
        List<Job> shelf0 = new ArrayList<>();
        List<Job> shelf2 = new ArrayList<>();
        MCKnapsack knapsack = new MCKnapsack();
        knapsack.solve(bigJobs, I.getM(), shelf1, shelf0, shelf2, d);

        assert (shelf1.stream().noneMatch(j -> shelf2.contains(j)));

        // check work constraint
        int WShelf1 = shelf1.stream()
                .mapToInt(j -> j.getAllotedMachines() * j.getProcessingTime(j.getAllotedMachines()))
                .sum();
        int WShelf2 = shelf2.stream()
                .mapToInt(j -> j.getAllotedMachines() * j.getProcessingTime(j.getAllotedMachines()))
                .sum();
        int WShelf0 = shelf0.stream()
                .mapToInt(j -> j.getAllotedMachines() * j.getProcessingTime(j.getAllotedMachines()))
                .sum();

        if (WShelf1 + WShelf2 + WShelf0 > I.getM() * d - Ws) {
            return ApproximationRatio.NONE;
        }

        // adjust jobs chosen in c2, i.e. shelf0
        List<Job> jobsToRemove = new ArrayList<>();
        Job j1 = null;
        Job j3 = null;
        for (Job job : shelf0) {
            int allotedMachines = job.getAllotedMachines();
            switch (allotedMachines) {
                case 1:
                    if (j1 == null) {
                        j1 = job;
                    } else {
                        // schedule these jobs after each other
                        j1.setStartingTime(job.getProcessingTime(1));
                        j1 = null;
                    }
                    break;
                case 3:
                    if (j3 == null) {
                        j3 = job;
                    } else {
                        // schedule these jobs after each other
                        j3.setStartingTime(job.getProcessingTime(3));
                        j3 = null;
                    }
                    break;
                default:
                    // schedule job in \gamma(, 10/7d)
                    job.setAllotedMachines(job.canonicalNumberMachines(10 * d / 7));
                    if (job.getProcessingTime(job.getAllotedMachines()) <= d) {
                        shelf1.add(job);
                        jobsToRemove.add(job);
                    }
                    break;
            }
        }
        shelf0.removeAll(jobsToRemove);
        if (j3 != null && j1 != null) {
            // this job can be artificially split, since it will not be altered during the
            // algorithm in any way.
            // TODO: do we need to exchange this job in the instance?
            int pTime = j3.getProcessingTime(2);

            shelf0.remove(j3);
            Job virtualJob1 = new Job(j3.getId(), new int[] { pTime, pTime });
            Job virtualJob2 = new Job(j3.getId(), new int[] { pTime, pTime });
            virtualJob1.setAllotedMachines(1);
            virtualJob2.setAllotedMachines(1);
            shelf0.add(virtualJob1);
            shelf1.add(virtualJob2);
            j1.setStartingTime(pTime);

            // exchange job in instance
            Job[] jobs = I.getJobs();
            List<Job> jobList = new ArrayList<>(Arrays.asList(jobs));
            jobList.remove(j3);
            jobList.add(virtualJob1);
            jobList.add(virtualJob2);
            I.setJobs(jobList.toArray(Job[]::new));

        } else {
            if (j3 != null) {
                // schedule job in \gamma(, 10/7d)
                int allottedMachines = j3.canonicalNumberMachines(10 * d / 7);
                j3.setAllotedMachines(allottedMachines);
                if (j3.getProcessingTime(allottedMachines) <= d) {
                    shelf0.remove(j3);
                    shelf1.add(j3);
                }
            }
            if (j1 != null) {
                shelf0.remove(j1);
                shelf1.add(j1);
            }
        }

        int m2 = shelf2.stream().mapToInt(Job::getAllotedMachines).sum();
        int m1 = shelf1.stream().mapToInt(Job::getAllotedMachines).sum();
        int m0 = shelf0.stream().filter(j -> j.getStartingTime() == 0).mapToInt(Job::getAllotedMachines).sum();

        // shelf 0 and shelf 1 should not use more than m machines
        assert (m0 + m1 <= I.getM());

        double lambdad = 10 * d / 7;
        if (m2 <= I.getM() - m0) {
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_10_7; // done. the schedule should be feasible.
        }

        // apply transformation rules with increasing \lambda
        applyTransformationRules(lambdad, d, shelf0, shelf1, shelf2, I.getM() - m0 - m1);

        m2 = shelf2.stream().mapToInt(Job::getAllotedMachines).sum();
        m1 = shelf1.stream().mapToInt(Job::getAllotedMachines).sum();
        m0 = shelf0.stream().filter(j -> j.getStartingTime() == 0).mapToInt(Job::getAllotedMachines).sum();

        if (m2 <= I.getM() - m0) {
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_10_7; // done. the schedule should be feasible.
        }

        // if q = 0, we can find a feasible schedule
        if (I.getM() - m0 - m1 == 0) {
            algorithm3(shelf1, shelf2, m2, m1);
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_10_7;
        }

        lambdad = 13 * d / 9;
        applyTransformationRules(lambdad, d, shelf0, shelf1, shelf2, I.getM() - m0 - m1);

        m2 = shelf2.stream().mapToInt(Job::getAllotedMachines).sum();
        m1 = shelf1.stream().mapToInt(Job::getAllotedMachines).sum();
        m0 = shelf0.stream().filter(j -> j.getStartingTime() == 0).mapToInt(Job::getAllotedMachines).sum();

        if (m2 <= I.getM() - m0) {
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_13_9; // done. the schedule should be feasible.
        }

        // for \lambda \leq m_1/6, we can find a feasible schedule
        if (I.getM() - m0 - m1 <= m1 / 6) {
            algorithm3(shelf1, shelf2, m2, m1);
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_13_9;
        }

        lambdad = 73 * d / 50;
        applyTransformationRules(lambdad, d, shelf0, shelf1, shelf2, I.getM() - m0 - m1);

        m2 = shelf2.stream().mapToInt(Job::getAllotedMachines).sum();
        m1 = shelf1.stream().mapToInt(Job::getAllotedMachines).sum();
        m0 = shelf0.stream().filter(j -> j.getStartingTime() == 0).mapToInt(Job::getAllotedMachines).sum();

        if (m2 <= I.getM() - m0) {
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_73_50; // done. the schedule should be feasible.
        }

        // we can find a feasible schedule with either algorithm 3 or 4
        if (I.getM() - m0 - m1 <= m1 / 6) {
            algorithm3(shelf1, shelf2, m2, m1);
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_73_50;
        } else {
            assert (shelf2.size() == 1); // Observation 3.5
            algorithm4(lambdad, shelf1, shelf2.get(0), m1);
            placeJobs(shelf0, shelf1, shelf2, smallJobs, I, lambdad);
            return ApproximationRatio.RATIO_73_50;
        }

    }

    protected void placeJobs(List<Job> shelf0, List<Job> shelf1, List<Job> shelf2, List<Job> smallJobs, Instance I,
            double lambdad) {

        // init machines
        Machine[] machinesArray = new Machine[I.getM()];
        for (int i = 0; i < I.getM(); i++) {
            machinesArray[i] = new Machine(i);
        }
        I.setMachines(machinesArray);

        List<Machine> machines = new ArrayList<>(Arrays.asList(machinesArray));

        int i = 0;
        // S0
        for (Job job : shelf0.stream().filter(j -> j.getStartingTime() == 0).toArray(Job[]::new)) {
            int allottedMachines = job.getAllotedMachines();
            for (; allottedMachines > 0; allottedMachines--, i++) {
                machines.get(i).addJob(job);
            }
        }
        for (Job job : shelf0.stream().filter(j -> j.getStartingTime() != 0).toArray(Job[]::new)) {
            int allottedMachines = job.getAllotedMachines();
            for (Machine machine : machines.stream()
                    .filter(m -> m.getFirstFreeTime() == job.getStartingTime()).toArray(Machine[]::new)) {
                // TODO: check if job fits on top
                machine.addJob(job);
                allottedMachines--;
                if (allottedMachines == 0) {
                    break;
                }
            }
            assert (allottedMachines == 0); // all jobs should be placed.
        }

        // there should be enough machines left.
        assert (shelf1.stream().filter(j -> j.getStartingTime() == 0).mapToInt(Job::getAllotedMachines)
                .sum() <= I.getM() - i);
        assert (shelf2.stream().mapToInt(Job::getAllotedMachines).sum() <= I.getM() - i);

        int m0 = i;

        // TODO: these do not always need to be sorted.
        shelf1.sort((j1, j2) -> Integer.compare(j1.getProcessingTime(j1.getAllotedMachines()),
                j2.getProcessingTime(j2.getAllotedMachines())));
        shelf2.sort((j1, j2) -> Integer.compare(j1.getProcessingTime(j1.getAllotedMachines()),
                j2.getProcessingTime(j2.getAllotedMachines())));

        // place shelf1 in ascending order.
        for (Job job : shelf1.stream().filter(j -> j.getStartingTime() == 0).toArray(Job[]::new)) {
            int allottedMachines = job.getAllotedMachines();
            for (; allottedMachines > 0; allottedMachines--, i++) {
                machines.get(i).addJob(job); // TODO: IndexOutOfBoundsException
            }
        }
        for (Job job : shelf1.stream().filter(j -> j.getStartingTime() != 0).toArray(Job[]::new)) {
            int allottedMachines = job.getAllotedMachines();
            for (Machine machine : machines.stream()
                    .filter(m -> m.getFirstFreeTime() == job.getStartingTime()).toArray(Machine[]::new)) {
                // TODO: check if job fits on top
                machine.addJob(job);
                allottedMachines--;
                if (allottedMachines == 0) {
                    break;
                }
            }
            assert (allottedMachines == 0); // all jobs should be placed.
        }

        assert (i <= I.getM()); // we do not use more machines than available

        // place shelf2 in descending order
        i = I.getM() - 1;
        for (Job job : shelf2) {
            int allottedMachines = job.getAllotedMachines();
            // TODO: this introduces a rounding error of up to 1
            job.setStartingTime((int) Math.ceil(lambdad - job.getProcessingTime(allottedMachines)));
            for (; allottedMachines > 0; allottedMachines--, i--) {
                machines.get(i).addJob(job);
            }

        }

        assert (i + 1 >= m0); // we do not place any jobs into shelf 0.

        // place small jobs.
        for (Job job : smallJobs) {
            // place on any machine with enough idle time.
            // this list should never be empty
            Machine machine = machines.stream().filter(m -> lambdad - m.getUsedTime() >= job.getProcessingTime(1))
                    .findAny().get();
            job.setAllotedMachines(1);
            job.setStartingTime(machine.getFirstFreeTime());
            machine.addJob(job);

        }

    }

    protected void algorithm3(List<Job> shelf1, List<Job> shelf2, int m2, int m1) {
        // sort jobs in shelf2 by their processing time
        shelf2.sort((j1, j2) -> Integer.compare(j1.getProcessingTime(j1.getAllotedMachines()),
                j2.getProcessingTime(j2.getAllotedMachines())));
        while (m2 > m1) {
            // get the smallest job in shelf2
            Job job = shelf2.get(0);
            job.setAllotedMachines(job.getAllotedMachines() - 1);
            // resort the list
            // TODO: this can be done smarter
            shelf2.sort((j1, j2) -> Integer.compare(j1.getProcessingTime(j1.getAllotedMachines()),
                    j2.getProcessingTime(j2.getAllotedMachines())));
        }
    }

    protected void algorithm4(double lambdad, List<Job> shelf1, Job j0, int m1) {
        // sort jobs in shelf1 by their processing time (ascending!)
        shelf1.sort((j1, j2) -> Integer.compare(j2.getProcessingTime(j1.getAllotedMachines()),
                j1.getProcessingTime(j2.getAllotedMachines())));

        int i = shelf1.size() - 1;
        int w = shelf1.get(i).getAllotedMachines();
        for (int x = m1; x > 0; x--) {
            // check, if j0 fits
            if (j0.getProcessingTime(x)
                    + shelf1.get(i).getProcessingTime(shelf1.get(i).getAllotedMachines()) <= lambdad) {
                // schedule j0 like this
                j0.setAllotedMachines(x);
                return;
            }
            w--;
            if (w == 0) {
                i--;
                w = shelf1.get(i).getAllotedMachines();
            }
        }
        assert (false); // this should never happen

    }

    // TODO: combine sequential small jobs that remain on shelf1 to single bigger
    // job for simplicity
    protected void applyTransformationRules(double lambdad, double d, List<Job> shelf0, List<Job> shelf1,
            List<Job> shelf2, int q) {
        Job j1 = null;
        List<Job> jobsToRemove = new ArrayList<>();
        for (Job job : shelf1) {
            if (job.getAllotedMachines() == 1) {
                // T2
                if (job.getProcessingTime(1) > lambdad / 2) {
                    continue; // ignore jobs that are big enough
                }
                if (j1 == null) {
                    j1 = job;
                } else {
                    // schedule these jobs after each other
                    shelf0.add(j1);
                    shelf0.add(job);
                    jobsToRemove.add(j1);
                    jobsToRemove.add(job);
                    j1.setStartingTime(job.getProcessingTime(1));
                    j1 = null;
                    q -= 1;
                }
                continue;
            }

            // T1
            if (job.canonicalNumberMachines(lambdad) < job.getAllotedMachines()) {
                q += job.getAllotedMachines() - job.canonicalNumberMachines(lambdad);
                job.setAllotedMachines(job.canonicalNumberMachines(lambdad));
                shelf0.add(job);
                jobsToRemove.add(job);
            }
        }
        shelf1.removeAll(jobsToRemove);

        // T3
        jobsToRemove.clear();
        for (Job job : shelf2) {
            if (job.canonicalNumberMachines(lambdad) <= q) {
                job.setAllotedMachines(job.canonicalNumberMachines(lambdad));
                if (job.getProcessingTime(job.getAllotedMachines()) <= d) {
                    shelf1.add(job);
                } else {
                    shelf0.add(job);
                }
                jobsToRemove.add(job);
                q -= job.getAllotedMachines();
            }
        }
        shelf2.removeAll(jobsToRemove);
    }

    @Override
    public void setInstance(Instance I) {
        this.I = I;
    }

}
