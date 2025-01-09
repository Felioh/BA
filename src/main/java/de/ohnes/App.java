package de.ohnes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ohnes.AlgorithmicComponents.Algorithm;
import de.ohnes.AlgorithmicComponents.Approximation.Approximation;
import de.ohnes.AlgorithmicComponents.Approximation.TwoApproximation;
import de.ohnes.AlgorithmicComponents.FPTAS.CompressionApproach;
import de.ohnes.AlgorithmicComponents.FPTAS.DoubleCompressionApproach;
import de.ohnes.AlgorithmicComponents.Shelves.GrageApproach;
import de.ohnes.AlgorithmicComponents.Shelves.LandApproach;
import de.ohnes.AlgorithmicComponents.Shelves.OhnesorgeApproach;
import de.ohnes.logger.MyElasticsearchClient;
import de.ohnes.logger.printSchedule;
import de.ohnes.util.Instance;
import de.ohnes.util.MyMath;
import de.ohnes.util.TestResult;

public class App {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private static String rand;
    private static String epsilon;
    private static String minJobs;
    private static String maxJobs;
    private static String minMachines;
    private static String maxMachines;
    private static String maxSeqTime;
    private static String ESHost;
    private static String loop;
    private static String ESIndexPrefix;
    private static String ExecutionsBeforePush;
    private static String InstancePolicy;
    private static String printResult;

    private static String algo;

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.ALL);

        rand = System.getenv("INSTANCE_RANDOM");
        epsilon = System.getenv("EPSILON");
        minJobs = System.getenv("INSTANCE_MINJOBS");
        maxJobs = System.getenv("INSTNACE_MAXJOBS");
        minMachines = System.getenv("INSTANCE_MINMACHINES");
        maxMachines = System.getenv("INSTANCE_MAXMACHINES");
        maxSeqTime = System.getenv("INSTANCE_MAX_SEQUENTIAL_TIME");
        ESHost = System.getenv("ELASTICSEARCH_HOST");
        loop = System.getenv("DETATCHED");
        algo = System.getenv("ALGO");
        ESIndexPrefix = System.getenv("ES_INDEX");
        ExecutionsBeforePush = System.getenv("EXECS_BEFORE_PUSH");
        InstancePolicy = System.getenv("INSTANCE_POLICY");
        printResult = System.getenv("PRINT_RESULT");

        LOGGER.info("Starting Algorithm!");
        if (loop != null) {
            MyElasticsearchClient.makeConnection(ESHost);
            while (true) {
                for (int i = 0; i < Integer.parseInt(ExecutionsBeforePush); i++) {
                    MyElasticsearchClient.addData(runTest());
                }
                MyElasticsearchClient.pushData(ESIndexPrefix + java.time.LocalDate.now().toString());

            }
        } else {
            runTest();

        }

    }

    /**
     * @return TestResult
     */
    private static TestResult runTest() {
        if (algo == null) {
            LOGGER.error("No Algorithm specified. Exiting.");
            return null;
        }
        Instance I = new Instance();
        if (rand == null) {
            try {
                I = new ObjectMapper().readValue(Paths.get("TestInstances/TestInstance.json").toFile(),
                        Instance.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            I = getInstance();
        }

        DualApproximationFramework dF;
        if (algo.equals("Grage")) {
            Algorithm shelves = new GrageApproach();
            Algorithm fptas = new DoubleCompressionApproach();
            Approximation approx = new TwoApproximation();
            dF = new DualApproximationFramework(fptas, shelves, approx, I);
        } else if (algo.equals("Land")) {
            Algorithm shelves = new LandApproach();
            Algorithm fptas = new CompressionApproach();
            Approximation approx = new TwoApproximation();
            dF = new DualApproximationFramework(fptas, shelves, approx, I);
        } else if (algo.equals("Ohnesorge")) {
            Algorithm shelves = new OhnesorgeApproach();
            Algorithm fptas = new CompressionApproach();
            Approximation approx = new TwoApproximation();
            dF = new DualApproximationFramework(fptas, shelves, approx, I);
        } else {
            return null;
        }

        long startTime = System.currentTimeMillis();
        double d = dF.start(Double.parseDouble(epsilon));
        long endTime = System.currentTimeMillis();
        LOGGER.info("Ran instance with {} machines and {} jobs in {} milliseconds.", I.getM(), I.getN(),
                (endTime - startTime));

        // ############################################## DEBUG
        // ##################################################################################################################
        if (printResult.equals("true")) {
            System.out.println(String.format("-".repeat(70) + "%04.2f" + "-".repeat(70), d));
            System.out.println(printSchedule.printMachines(I.getMachines()));
            System.out.println(String.format("-".repeat(70) + "%04.2f" + "-".repeat(70), d));
        }
        // DrawSchedule.drawSchedule(I); //TODO: comment this line in to create .png
        // fiels of the result schedule.
        // ############################################## DEBUG
        // ##################################################################################################################

        TestResult tr = new TestResult();
        tr.setApproximation(dF.getApproximationName());
        tr.setFptas(dF.getFPTASName());
        tr.setShelvesAlgo(dF.getShelvesAlgoName());
        tr.setAchivedMakespan(I.getMakespan());
        tr.setEstimatedOptimum(d);
        tr.setJobs(I.getN());
        tr.setMachines(I.getM());
        tr.setMilliseconds((endTime - startTime));
        tr.setBigJobs(MyMath.findBigJobs(I, d / 2).length);
        tr.setSmallJobs(MyMath.findSmallJobs(I, d / 2).length);
        tr.setInstanceID(I.getId());
        // tr.setProcessingTimes(I.getJobs());

        return tr;
    }

    /**
     * @return Instance
     * @throws InterruptedException
     */
    private static Instance getInstance() {
        Instance I = new Instance();
        if (InstancePolicy == null) {
            I.generateRandomInstance(Integer.parseInt(minJobs), Integer.parseInt(maxJobs),
                    Integer.parseInt(minMachines), Integer.parseInt(maxMachines), Integer.parseInt(maxSeqTime));

        } else if (InstancePolicy.equals("push")) {
            I.generateRandomInstance(Integer.parseInt(minJobs), Integer.parseInt(maxJobs),
                    Integer.parseInt(minMachines), Integer.parseInt(maxMachines), Integer.parseInt(maxSeqTime));
            try {
                new ObjectMapper().writeValue(Paths.get("/home/instances/instance_" + I.getId() + ".json").toFile(), I);
                LOGGER.info("Saved file {}.", "/home/instances/instance_" + I.getId());
                ;
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("The Instance couldn't be saved to a File.");
            }

        } else if (InstancePolicy.equals("pull")) {

            File dir = new File("/home/instances");
            File[] files = dir.listFiles();
            while (files.length == 0) {
                LOGGER.debug("Could not find an Instance to execute. Waiting 60 seconds.");
                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (InterruptedException e) {
                }
                dir = new File("/home/instances");
                files = dir.listFiles();
            }
            try {
                I = new ObjectMapper().readValue(files[0], Instance.class);
                if (files[0].delete()) {
                    LOGGER.debug("Successfully loaded Instance {} and deleted the file.", files[0].getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Could not read File {}", files[0].getName());
                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (InterruptedException e2) {
                }
                return getInstance();
            }

        }
        return I;

    }

}
