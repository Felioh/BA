package AlgorithmicComponents;

import java.util.ArrayList;
import java.util.List;

import logger.printSchedule;
import util.ConvolutionElement;
import util.Job;

public class MaxConvolution {


    /**
     * native Approach for a max convolution on 2 seqences with equal length.
     * @param seqA
     * @param seqB
     * @param len
     * @return
     */
    public static ConvolutionElement[] nativeApproach(ConvolutionElement[] seqA, ConvolutionElement[] seqB, int len) {

        ConvolutionElement[] result = new ConvolutionElement[len];
        for(int j = 0; j < len; j++) {
            ConvolutionElement bestElement = new ConvolutionElement(0, new ArrayList<>());
            int x = 0;
            int y = j;
            while(y >= 0) {
                if(seqA[y].getProfit() + seqB[x].getProfit() > bestElement.getProfit()) {
                    ArrayList<Job> selected_jobs = new ArrayList<>();
                    selected_jobs.addAll(seqA[y].getJobs());
                    selected_jobs.addAll(seqB[x].getJobs());
                    bestElement = new ConvolutionElement(seqB[x].getProfit() + seqA[y].getProfit(), selected_jobs);
                }
                x++;
                y--;
            }
            result[j] = bestElement;
        }

        return result;
    }


    public static ConvolutionElement[] linearApproach(ConvolutionElement[] seqA, ConvolutionElement[] seqB) {

        ImaginaryMatrix A = new ImaginaryMatrix(seqA, seqB);

        int[] max_ind = linearApproach_maxCompute(A);   //TODO column index is wrong
        ConvolutionElement[] seqC = new ConvolutionElement[max_ind.length];
        for(int i = 0; i < seqC.length; i++) {
            seqC[i] = A.getConvolutionElement(i, max_ind[i]);
        }
        return seqC;
    }

    /**
     * recursive Funktion.
     * 
     * @param A the imaginary Matrix for one arbitrary and one concave sequence.
     * @return an array containing the index for the max Element of each row. considering the Matrix A with  A_ij = a_j + b_{i-j}
     */
    private static int[] linearApproach_maxCompute(ImaginaryMatrix A) {
        // System.out.println(A);
        //delete rows until the matrix has shape nxn
        List<Integer> deletedCols = linearApproach_reduce(A);
        if(A.getColumns() == 1) {       //end of recursion
            int[] res = {A.getRealColumn(0)};
            return res;
        }
        //delete every second row.
        List<Integer> deletedRows = A.deleteRows();

        //recursive call
        int[] prev_res = linearApproach_maxCompute(A);

        A.addColumns(deletedCols);
        A.addRows(deletedRows);
        int[] new_res = new int[deletedRows.size() + prev_res.length];
        for(int i = 0; i < new_res.length; i++) {
            if(i % 2 == 0) {
                new_res[i] = prev_res[i / 2]; //values are already known due to recursion.
            } else {
                int maxIndex = (i / 2) + 1 > prev_res.length - 1 ? A.getColumns() : prev_res[(i / 2) + 1];
                for(int col = prev_res[i / 2]; col <= maxIndex; col ++) { //search max between prev_res[i] and prev_res[i + 1]
                    if (A.getElement(i, col) > A.getElement(i, new_res[i])) {
                        new_res[i] = col;
                    }

                }
                //TODO search max between prev_res[i] and prev_res[i + 1]
            }
        }

        return new_res;
    }
    
    /**
     * Delete columns of the matrix until it has shape nxn
     * @param A the imaginary Matrix
     * @return a List of the columns, that have been deleted.
     */
    private static List<Integer> linearApproach_reduce(ImaginaryMatrix A) {        //TODO: nb. Rows as constant (n)
        List<Integer> delCols = new ArrayList<>();
        int k = 0;
        while(A.getColumns() > A.getRows()) {
            if(A.getElement(k, k) > A.getElement(k, k + 1)) {
                if(k < A.getRows()) {
                    k++;
                }
                if(k == A.getRows()) {
                    delCols.add(A.deleteColumn(k + 1));      //delete the next column

                }
            }
            if(A.getElement(k, k) < A.getElement(k, k + 1)) {
                delCols.add(A.deleteColumn(k));
                k--;
            }
        }

        return delCols;
    }
    
}

class ImaginaryMatrix {

    ConvolutionElement[] seqA;
    ConvolutionElement[] seqB;

    ArrayList<Integer> killedCols = new ArrayList<>(); //TODO array. values are monotone
    ArrayList<Integer> killedRows = new ArrayList<>(); //TODO maybe linked list (bisection search)

    ImaginaryMatrix(ConvolutionElement[] seqA, ConvolutionElement[] seqB) {
        this.seqA = seqA;
        this.seqB = seqB;
    }

    int getRealRow(int i) {
        int x = -1;
        while (i >= 0) {
            // x++;
            if(!killedRows.contains(++x)) {
                i--;
            }
        }
        return x;
    }
    int getRealColumn(int j) {
        int y = -1;
        while (j >= 0) {
            // y++;
            if(!killedCols.contains(++y)) {
                j--;
            }
        }
        return y;
    }

    /**
     * A_ij = a_j + b_{i-j}
     * @param i
     * @param j
     * @return
     */
    int getElement(int i, int j) {
        try {
            return seqA[getRealColumn(j)].getProfit() + seqB[getRealRow(i) - getRealColumn(j)].getProfit();

        } catch(IndexOutOfBoundsException e) {
            return Integer.MIN_VALUE;
        }
    }

    ConvolutionElement getConvolutionElement(int i, int j) {
        List<Job> jobs = new ArrayList<>();
        jobs.addAll(seqA[getRealColumn(j)].getJobs());
        jobs.addAll(seqB[getRealRow(i) - getRealColumn(j)].getJobs());
        return new ConvolutionElement(getElement(i, j), jobs);
    }

    int getColumns() {
        return this.seqA.length - killedCols.size();
    }

    int getRows() {
        return this.seqB.length - killedRows.size();
    }
    
    /**
     * Delete every 2nd row. 
     * C <- b[2, 4, ..., 2(n/2)]
     */
    List<Integer> deleteRows() {
        List<Integer> delRows = new ArrayList<>();
        boolean kill = false;
        for(int i = 0; i < seqB.length; i++) {
            if(this.killedRows.contains(i)) {
                continue;
            }
            if(kill) {
                this.killedRows.add(i);
                delRows.add(i);
            }
            kill = !kill;
        }

        return delRows;
    }
    int deleteColumn(int j) {
        int rCol = getRealColumn(j);
        this.killedCols.add(rCol);
        return rCol;
    }

    void addRows(List<Integer> rows) {
        this.killedRows.removeAll(rows);
    }

    void addColumns(List<Integer> cols) {
        this.killedCols.removeAll(cols);
    }

    @Override
    public String toString() {
        String res = "";
        for(int i = 0; i < this.getRows(); i++) {
            for(int j = 0; j < this.getColumns(); j++) {
                res += this.getElement(i, j) + "|";    
            }
            res += "\n" + "-".repeat(this.getColumns() * 2) + "\n";
        }
        return res;
    }
}
