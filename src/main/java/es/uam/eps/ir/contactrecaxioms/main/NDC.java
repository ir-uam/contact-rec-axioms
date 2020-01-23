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
import es.uam.eps.ir.contactrecaxioms.recommender.basic.Random;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.*;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Class for reproducing the experiments for the NDC axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NDC
{
    /**
     * Program that reproduces the experiments for the NDC axiom.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations. Only algorithms with a version without term discrimination will be executed</li>
     *               <li><b>Output directory:</b> Directory in which to store the recommendations and the output files.</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Weighted:</b> True if the network is weighted, false otherwise.</li>
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
            System.err.println("\tAlgorithms: Route to an XML file containing the recommender configuration. Only algorithms with a version without term discrimination will be executed.");
            System.err.println("\tOutput directory: Directory in which to store the recommendations and the output files.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
            System.err.println("\tWeighted: True if the network is weighted, false otherwise.");
            System.err.println("\tRec. Length: Maximum number of recommendations per user.");
            System.err.println("\tPrint recommendations: True if, additionally to the results, you want to print the recommendations. False otherwise");
            return;
        }

        // Read the program arguments.
        String trainDataPath = args[0];
        String testDataPath = args[1];
        String algorithmsPath = args[2];
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        boolean weighted = args[5].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[6]);
        boolean printRecs = args[7].equalsIgnoreCase("true");

        long timea = System.currentTimeMillis();
        // Read the training graph.
        TextGraphReader<Long> weightedReader = new TextGraphReader<>(directed, true, false,"\t", Parsers.lp);
        FastGraph<Long> weightedGraph = (FastGraph<Long>) weightedReader.read(trainDataPath, true, false);
        if (weightedGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        TextGraphReader<Long> unweightedReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        FastGraph<Long> unweightedGraph = (FastGraph<Long>) unweightedReader.read(trainDataPath, false, false);
        if (unweightedGraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        // Read the test graph.
        Graph<Long> auxgraph = unweightedReader.read(testDataPath, false, false);
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, unweightedGraph);
        if (testGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Read the training and test data
        FastPreferenceData<Long, Long> unweightedTrainData = GraphSimpleFastPreferenceData.load(unweightedGraph);
        FastPreferenceData<Long, Long> weightedTrainData = GraphSimpleFastPreferenceData.load(weightedGraph);

        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(unweightedGraph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        Set<String> algorithms = gridreader.getAlgorithms();

        String tdDirectory = outputPath + "td" + File.separator;
        String noTdDirectory = outputPath + "notd" + File.separator;
        // If we choose to print the recommendations, create the folders to store them.
        if(printRecs)
        {
            File file = new File(tdDirectory);
            file.mkdir();
            file = new File(noTdDirectory);
            file.mkdir();
        }

        int numUsers = testData.numUsersWithPreferences();

        // For each algorithm.
        algorithms.forEach(tdIdentifier ->
        {
            // First, create the maps for the values.
            Map<String, Double> tdValues = new HashMap<>();
            Map<String, Double> noTDValues = new HashMap<>();

            // Get the identifier of the version of the algorithm without term discrimination.
            String noTdIdentifier = NDC.getNoTermDiscriminationVersion(tdIdentifier);

            if (noTdIdentifier != null) // If it exists (otherwise, ignore the algorithm)
            {
                System.out.println("-------- Starting algorithm " + tdIdentifier + " --------");
                long timeaa = System.currentTimeMillis();
                Grid grid = gridreader.getGrid(tdIdentifier);
                Configurations confs = grid.getConfigurations();
                AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>();

                // Configure the recommender runner
                @SuppressWarnings("unchecked")
                Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(unweightedTrainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(unweightedGraph, index));
                RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, testData.getUsersWithPreferences(), filter, maxLength);

                AtomicInteger counter = new AtomicInteger(0);
                List<Parameters> configurations = confs.getConfigurations();
                int totalCount = configurations.size();

                // Now, execute each possible variant.
                configurations.parallelStream().forEach(parameters ->
                {
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> tdSupp = algorithmSelector.getRecommender(tdIdentifier, parameters);
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> noTdSupp = algorithmSelector.getRecommender(noTdIdentifier, parameters);
                    String tdName = tdSupp.v1();
                    String noTdName = noTdSupp.v1();

                    // First, obtain the metric.
                    NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                    SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);

                    try
                    {
                        Recommender<Long, Long> weightedTd = new Random<>(unweightedGraph);
                        Recommender<Long, Long> weightedNoTd = new Random<>(unweightedGraph);
                        Recommender<Long, Long> unweightedTd = tdSupp.v2().apply(unweightedGraph, unweightedTrainData);
                        Recommender<Long, Long> unweightedNoTd = noTdSupp.v2().apply(unweightedGraph, unweightedTrainData);

                        if(weighted)
                        {
                            weightedTd = tdSupp.v2().apply(weightedGraph, weightedTrainData);
                            weightedNoTd = noTdSupp.v2().apply(weightedGraph, weightedTrainData);
                        }

                        double weightedTdValue = 0;
                        double weightedNoTdValue = 0;
                        double unweightedTdValue;
                        double unweightedNoTdValue;

                        if(printRecs) // If we want to print the recommendations
                        {
                            if(weighted)
                            {
                                weightedTdValue = AuxiliarMethods.computeAndEvaluate(tdDirectory + "wei_" + tdName + ".txt", weightedTd, runner, nDCG, numUsers);
                                weightedNoTdValue = AuxiliarMethods.computeAndEvaluate(noTdDirectory + "wei_" + noTdName + ".txt", weightedNoTd, runner, nDCG, numUsers);
                            }

                            unweightedTdValue = AuxiliarMethods.computeAndEvaluate(tdDirectory + (weighted ? "unw_" : "") + tdName + ".txt", unweightedTd, runner, nDCG, numUsers);
                            unweightedNoTdValue = AuxiliarMethods.computeAndEvaluate(noTdDirectory + (weighted ? "unw_" : "") + noTdName + ".txt", unweightedNoTd, runner, nDCG, numUsers);
                        }
                        else // Otherwise
                        {
                            if(weighted)
                            {
                                weightedTdValue = AuxiliarMethods.computeAndEvaluate(weightedTd, runner, nDCG, numUsers);
                                weightedNoTdValue = AuxiliarMethods.computeAndEvaluate(weightedNoTd, runner, nDCG, numUsers);
                            }
                            unweightedTdValue = AuxiliarMethods.computeAndEvaluate(unweightedTd, runner, nDCG, numUsers);
                            unweightedNoTdValue = AuxiliarMethods.computeAndEvaluate(unweightedNoTd, runner, nDCG, numUsers);
                        }

                        long timebb = System.currentTimeMillis();
                        System.out.println("Algorithm " + counter.incrementAndGet() + "/" + totalCount + ": " + tdName + " finished (" + (timebb-timeaa) + " ms.)");

                        // Store the nDCG values.
                        if(weighted)
                        {
                            tdValues.put("wei_" + tdName, weightedTdValue);
                            noTDValues.put("wei_" + tdName, weightedNoTdValue);
                            tdValues.put("unw_" + tdName, unweightedTdValue);
                            noTDValues.put("unw_" + tdName, unweightedNoTdValue);
                        }
                        else
                        {
                            tdValues.put(tdName, unweightedTdValue);
                            noTDValues.put(tdName, unweightedNoTdValue);
                        }

                    }
                    catch (IOException ioe)
                    {
                        System.err.println("ERROR: Something failed while executing " + tdName);
                    }
                });

                // Print the file for this algorithm.
                AuxiliarMethods.printFile(outputPath + "ndc_" + tdIdentifier + ".txt", tdValues, noTDValues, "TD", "No TD", maxLength);
                long timecc = System.currentTimeMillis();
                System.out.println("-------- Finished algorithm " + tdIdentifier + " (" + (timecc-timeaa) + " ms.) --------");
            }
            else
            {
                System.err.println("Algorithm " + tdIdentifier + " has no version without term discrimination");
            }
        });
    }

    /**
     * If the algorithm has a version without termm discrimination, finds the identifier of the algorithm.
     *
     * @param algorithm the algorithm.
     *
     * @return the identifier of the algorithm if it exists, null otherwise.
     */
    private static String getNoTermDiscriminationVersion(String algorithm)
    {
        switch (algorithm)
        {
            case AlgorithmIdentifiers.BM25:
                return AlgorithmIdentifiers.BM25NOTD;
            case AlgorithmIdentifiers.EBM25:
                return AlgorithmIdentifiers.EBM25NOTD;
            case AlgorithmIdentifiers.PIVOTED:
                return AlgorithmIdentifiers.PIVOTEDNOTD;
            case AlgorithmIdentifiers.QLD:
                return AlgorithmIdentifiers.QLDNOTD;
            case AlgorithmIdentifiers.QLJM:
                return AlgorithmIdentifiers.QLJMNOTD;
            default:
                return null;
        }
    }
}
