package de.ohnes.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Machine {

    private int id;
    private double usedTime; // maybe not needed. ?
    private List<Job> jobs;

    public Machine(int id) {
        this.id = id;
        this.usedTime = 0;
        this.jobs = new ArrayList<>();
    }

    public void addJob(Job job) {
        this.jobs.add(job);
        this.jobs = this.jobs.stream().sorted(Comparator.comparing(Job::getStartingTime)).collect(Collectors.toList());
        this.usedTime += job.getProcessingTime(job.getAllotedMachines());
    }

    public void removeJob(Job job) {
        this.jobs.remove(job);
        this.usedTime -= job.getProcessingTime(job.getAllotedMachines());
    }

    public int getFirstFreeTime() {
        int t = 0;
        boolean used = true;
        while (used) {
            used = false;
            for (Job job : jobs) {
                if (job.getStartingTime() == t) {
                    t = job.getStartingTime() + job.getProcessingTime(job.getAllotedMachines());
                    used = true;
                }
            }
        }
        return t;
    }

}
