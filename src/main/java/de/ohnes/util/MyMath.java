package de.ohnes.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MyMath {

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static double getRandomNumber(double min, double max) {
        return ((Math.random() * (max - min)) + min);
    }

    /**
     * 
     * @param I the instance object including all jobs
     * @param t the threshold
     * @return all Jobs that have a sequential processing time > d/2
     */
    public static Job[] findBigJobs(Instance I, double t) {
        Stream<Job> jobs = Arrays.stream(I.getJobs());
        return jobs.filter(j -> j.getProcessingTimes()[0] > t).toArray(Job[]::new);
    }

    /**
     * 
     * @param I the instance object including all jobs
     * @param d the threshold
     * @return all Jobs that have a sequential processing time <= d/2
     */
    public static Job[] findSmallJobs(Instance I, double t) {
        Stream<Job> jobs = Arrays.stream(I.getJobs());
        return jobs.filter(j -> j.getProcessingTimes()[0] <= t).toArray(Job[]::new);
    }

    /**
     * 
     * @param I the instance object including all jobs
     * @param d the deadline
     * @return all Jobs that have a processing Time > d/2
     */
    public static Job[] findShelf1(Instance I, double d) {
        Stream<Job> jobs = Arrays.stream(I.getJobs());
        return jobs.filter(j -> j.getAllotedMachines() != 0 && j.getProcessingTime(j.getAllotedMachines()) > d / 2)
                .toArray(Job[]::new);
    }

    /**
     * 
     * @param I the instance object including all jobs
     * @param d the deadline
     * @return all Jobs that have a processing Time > d/2
     */
    public static Job[] findShelf2(Instance I, double d) {
        Stream<Job> jobs = Arrays.stream(I.getJobs());
        return jobs.filter(j -> j.getAllotedMachines() != 0 && j.getProcessingTime(j.getAllotedMachines()) <= d / 2)
                .toArray(Job[]::new);
    }

    /**
     * addition of two convolution Elements used in the knapsackSolver.
     * 
     * @param a first Convolution Element
     * @param b second convolution Element
     * @return a new Convolution Element
     */
    public static ConvolutionElement addConvolutionElements(ConvolutionElement a, ConvolutionElement b) {
        List<Job> jobs = new ArrayList<>();
        jobs.addAll(a.getJobs());
        jobs.addAll(b.getJobs());
        return new ConvolutionElement(a.getProfit() + b.getProfit(), jobs);
    }

}
