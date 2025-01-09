package de.ohnes.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

    private long id;
    private int[] processingTimes;
    private int startingTime;
    private int allotedMachines;

    public Job(long id, int[] processingTimes) {
        this.id = id;
        this.processingTimes = processingTimes;

    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Job && ((Job) obj).getId() == this.getId();
    }

    /**
     * 
     * @param i the amount of Machines [1..]
     * @return the execution time
     */
    public int getProcessingTime(int i) {
        return this.processingTimes[i - 1];
    }

    /**
     * find out the cononical number of machines for a job with max. execution
     * time @param h.
     * using binary search
     * O(log m)
     * 
     * @return -1 if the job cant be executed in time h
     */
    public int canonicalNumberMachines(double h) {
        int r = processingTimes.length - 1;
        if (this.processingTimes[r] > h) {
            return -1;
        }
        int l = 0;
        while (r >= l) {
            int mid = l + (r - l) / 2;
            if (processingTimes[mid] == h)
                break;
            if (processingTimes[mid] < h)
                r = mid - 1;
            if (processingTimes[mid] > h)
                l = mid + 1;
        }
        return l + (r - l) / 2 + 1;
    }

    public void reset() {
        this.startingTime = 0;
        this.allotedMachines = 0;
    }

}
