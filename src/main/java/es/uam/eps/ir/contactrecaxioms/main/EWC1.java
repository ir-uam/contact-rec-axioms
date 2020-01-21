/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.main;

import es.uam.eps.ir.contactrecaxioms.data.GraphSimpleFastPreferenceData;
import es.uam.eps.ir.contactrecaxioms.graph.Adapters;
import es.uam.eps.ir.contactrecaxioms.graph.Graph;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.graph.io.TextGraphReader;
import es.uam.eps.ir.contactrecaxioms.recommender.FastGraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.GraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.SocialFastFilters;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridReader;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSelector;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Class for reproducing the experiments for the EWC1 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EWC1
{
    /**
     * Program that reproduces the experiments for the EWC1 axiom.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations.</li>
     *               <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Rec. Length:</b> Maximum number of recommendations per user.</li>
     *               <li><b>Print recommendations:</b> True if, additionally to the results, you want to print the recommendations. False otherwise</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tTest: Route to the file containing the test links.");
            System.err.println("\tAlgorithms: Route to an XML file containing the recommender configurations.");
            System.err.println("\tOutput directory: Directory in which to store the recommendations and the output file.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
            System.err.println("\tRec. Length: Maximum number of recommendations per user.");
            System.err.println("\tPrint recommendations: True if, additionally to the results, you want to print the recommendations. False otherwise");
            return;
        }

        // Read the execution arguments:
        String trainDataPath = args[0];
        String testDataPath = args[1];
        String algorithmsPath = args[2];
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[5]);
        boolean printRecommenders = args[6].equalsIgnoreCase("true");

        // Initialize the maps to store the accuracy values.
        Map<String, Double> weightedValues = new ConcurrentHashMap<>();
        Map<String, Double> unweightedValues = new ConcurrentHashMap<>();

        // Store the different values of weighted / unweighted
        List<Boolean> weightedVals = new ArrayList<>();
        weightedVals.add(true);
        weightedVals.add(false);

        // First, we do create the directories.
        if (printRecommenders)
        {
            File weightedDirectory = new File(outputPath + "weighted" + File.separator);
            weightedDirectory.mkdirs();
            File unweightedDirectory = new File(outputPath + "unweighted" + File.separator);
            unweightedDirectory.mkdirs();
        }

        // Read the test graph
        TextGraphReader<Long> testGraphReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxTestGraph = testGraphReader.read(testDataPath, false, false);

        if (auxTestGraph == null)
        {
            System.err.println("ERROR: Could not read the test graph");
            return;
        }
        // Execute the loop for weighted and unweighted.
        for (boolean weighted : weightedVals)
        {
            System.out.println("-------- Started " + (weighted ? "weighted" : "unweighted") + " variants --------");
            long timea = System.currentTimeMillis();

            // Read the training graph.
            TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
            FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainDataPath);
            if (graph == null)
            {
                System.err.println("ERROR: Could not read the training graph");
                return;
            }

            // Read the test graph.


            long timeb = System.currentTimeMillis();
            System.out.println("Data read (" + (timeb - timea) + " ms.)");
            timea = System.currentTimeMillis();

            // Prepare the training and test data
            FastPreferenceData<Long, Long> trainData;
            trainData = GraphSimpleFastPreferenceData.load(graph);

            // Clean the test graph.
            FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxTestGraph, graph);
            FastPreferenceData<Long, Long> testData;
            testData = GraphSimpleFastPreferenceData.load(testGraph);
            GraphIndex<Long> index = new FastGraphIndex<>(graph);

            // Read the XML containing the parameter grid for each algorithm
            AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
            gridreader.readDocument();

            Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>();
            // Get the different recommenders to execute
            gridreader.getAlgorithms().forEach(algorithm ->
            {
                AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>();
                Map<String, Supplier<Recommender<Long, Long>>> suppliers = ags.getRecommenders(algorithm, gridreader.getGrid(algorithm), graph, trainData);
                if (suppliers == null)
                {
                    System.err.println("ERROR: Algorithm " + algorithm + " could not be read");
                }
                else
                {
                    recMap.putAll(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm), graph, trainData));
                }
            });

            timeb = System.currentTimeMillis();
            System.out.println("Algorithms selected (" + (timeb - timea) + " ms.)");
            // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
            Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));
            System.out.println("Num. target users: " + targetUsers.size());

            // Prepare the elements for the recommendation:
            RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp, lp);
            @SuppressWarnings("unchecked") Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));
            RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);
            // Execute the recommendations
            recMap.entrySet().parallelStream().forEach(entry ->
            {
                String name = entry.getKey();

                String path = outputPath + File.separator + (weighted ? "weighted" : "unweighted") + File.separator + name + ".txt";

                // First, create the nDCG metric (for measuring accuracy)
                NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

                // Prepare the recommender
                System.out.println("Preparing " + name);
                Supplier<Recommender<Long, Long>> recomm = entry.getValue();
                long a = System.currentTimeMillis();
                Recommender<Long, Long> rec = recomm.get();
                long b = System.currentTimeMillis();
                System.out.println("Prepared " + name + " (" + (b - a) + " ms.)");

                // Obtain the nDCG value
                double value;
                try
                {
                    if (printRecommenders)
                    {
                        value = AuxiliarMethods.computeAndEvaluate(path, rec, runner, nDCG);
                    }
                    else
                    {
                        value = AuxiliarMethods.computeAndEvaluate(rec, runner, nDCG);
                    }

                    if (weighted)
                    {
                        weightedValues.put(name, value);
                    }
                    else
                    {
                        unweightedValues.put(name, value);
                    }
                }
                catch (IOException ioe)
                {
                    System.err.println("Algorithm " + name + " failed");
                }
            });
        }

        // Print the file.
        AuxiliarMethods.printFile(outputPath + "ewc1.txt", weightedValues, unweightedValues, "Weighted", "Unweighted", maxLength);
    }
}
