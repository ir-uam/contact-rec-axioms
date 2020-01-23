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
 * Class for reproducing the experiments for the CLNCs and EW-CLNC axioms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CLNCS
{
    /**
     * Program that reproduces the experiments for the CLNCs and EW-CLNC axioms.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations</li>
     *               <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *               <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *               <li><b>Weighted:</b> True if the network is weighted, false otherwise.</li>
     *               <li><b>Max. Length:</b> Maximum number of recommendations per user.</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Training data.");
            System.err.println("\tTest: Test data.");
            System.err.println("\tAlgorithms: XML file containing the configuration for the BM25 algorithm.");
            System.err.println("\tOutput directory: Directory for storing the recommendations and the output file.");
            System.err.println("\tDirected: true if the graph is directed, false if not.");
            System.err.println("\tMaxLength: maximum length of the recommendation ranking.");
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
        int numUsers = testData.numUsersWithPreferences();

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        Set<String> algorithms = gridreader.getAlgorithms();

        String lenNormDirectory = outputPath + "lennorm" + File.separator;
        String noLenNormDirectory = outputPath + "nolennorm" + File.separator;
        // If we choose to print the recommendations, create the folders to store them.
        if(printRecs)
        {
            File file = new File(lenNormDirectory);
            file.mkdir();
            file = new File(noLenNormDirectory);
            file.mkdir();
        }

        algorithms.forEach(lenNormIdentifier ->
        {
            long timeaa = System.currentTimeMillis();
            String noLenNormIdentifier = CLNCS.getNoLengthNormalizationVersion(lenNormIdentifier);

            Map<String, Double> lenNormValues = new HashMap<>();
            Map<String, Double> noLenNormValues = new HashMap<>();

            if (noLenNormIdentifier != null) // If it exists
            {
                System.out.println("-------- Starting algorithm " + lenNormIdentifier + " --------");
                Grid grid = gridreader.getGrid(lenNormIdentifier);
                Configurations confs = grid.getConfigurations();
                AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>();

                AtomicInteger counter = new AtomicInteger(0);
                List<Parameters> configurations = confs.getConfigurations();
                int totalCount = configurations.size();

                configurations.forEach(parameters ->
                {
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> lenNormSupp = algorithmSelector.getRecommender(lenNormIdentifier, parameters);
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> noLenNormSupp = algorithmSelector.getRecommender(noLenNormIdentifier, parameters);
                    String lenNormName = lenNormSupp.v1();
                    String noLenNormName = noLenNormSupp.v1();

                    // First, obtain the metric.
                    NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                    SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);

                    @SuppressWarnings("unchecked") Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(unweightedTrainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(unweightedGraph, index));
                    RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, testData.getUsersWithPreferences(), filter, maxLength);

                    try
                    {
                        Recommender<Long, Long> unweightedLenNorm = lenNormSupp.v2().apply(unweightedGraph, unweightedTrainData);
                        Recommender<Long, Long> unweightedNoLenNorm = noLenNormSupp.v2().apply(unweightedGraph, unweightedTrainData);
                        Recommender<Long, Long> weightedLenNorm = lenNormSupp.v2().apply(weightedGraph, weightedTrainData);
                        Recommender<Long, Long> weightedNoLenNorm = noLenNormSupp.v2().apply(weightedGraph, weightedTrainData);

                        double unweightedLenNormValue;
                        double unweightedNoLenNormValue;
                        double weightedLenNormValue = 0;
                        double weightedNoLenNormValue = 0;

                        if(printRecs)
                        {
                            if(weighted)
                            {
                                weightedLenNormValue = AuxiliarMethods.computeAndEvaluate(lenNormDirectory + "wei_" + lenNormName + ".txt", weightedLenNorm, runner, nDCG, numUsers);
                                weightedNoLenNormValue = AuxiliarMethods.computeAndEvaluate(noLenNormDirectory + "wei_" + noLenNormName + ".txt", weightedNoLenNorm, runner, nDCG, numUsers);
                            }
                            unweightedLenNormValue = AuxiliarMethods.computeAndEvaluate(lenNormDirectory + (weighted ? "unw_" : "") + lenNormName + ".txt", unweightedLenNorm, runner, nDCG, numUsers);
                            unweightedNoLenNormValue = AuxiliarMethods.computeAndEvaluate(noLenNormDirectory + noLenNormName + ".txt", unweightedNoLenNorm, runner, nDCG, numUsers);
                        }
                        else
                        {
                            if(weighted)
                            {
                                weightedLenNormValue = AuxiliarMethods.computeAndEvaluate(weightedLenNorm, runner, nDCG, numUsers);
                                weightedNoLenNormValue = AuxiliarMethods.computeAndEvaluate(weightedNoLenNorm, runner, nDCG, numUsers);
                            }
                            unweightedLenNormValue = AuxiliarMethods.computeAndEvaluate(unweightedLenNorm, runner, nDCG, numUsers);
                            unweightedNoLenNormValue = AuxiliarMethods.computeAndEvaluate(unweightedNoLenNorm, runner, nDCG, numUsers);
                        }

                        if(weighted)
                        {
                            lenNormValues.put("wei_" + lenNormName, weightedLenNormValue);
                            noLenNormValues.put("wei_" + lenNormName, weightedNoLenNormValue);
                            lenNormValues.put("unw_" + lenNormName, unweightedLenNormValue);
                            noLenNormValues.put("unw_" + lenNormName, unweightedNoLenNormValue);
                        }
                        else
                        {
                            lenNormValues.put(lenNormName, unweightedLenNormValue);
                            noLenNormValues.put(lenNormName, unweightedNoLenNormValue);
                        }

                        long timebb = System.currentTimeMillis();
                        System.out.println("Algorithm " + counter.incrementAndGet() + "/" + totalCount + ": " + lenNormName + " finished (" + (timebb-timeaa) + " ms.)");
                    }
                    catch (IOException ioe)
                    {
                        System.err.println("ERROR: Something failed while executing " + lenNormName);
                    }
                });

                // Print the file for this algorithm.
                AuxiliarMethods.printFile(outputPath + "clncs_" + lenNormIdentifier + ".txt", lenNormValues, noLenNormValues, "Len. Norm.", "No Len. Norm.", maxLength);
            }
            else
            {
                System.err.println("Algorithm " + lenNormIdentifier + " has no version without term discrimination");
            }

            long timecc = System.currentTimeMillis();
            System.out.println("-------- Finished algorithm " + lenNormIdentifier + " (" + (timecc-timeaa) + " ms.) --------");
        });


    }

    /**
     * If the algorithm has a version without length normalization, finds the identifier of the algorithm.
     *
     * @param algorithm the algorithm.
     *
     * @return the identifier of the algorithm if it exists, null otherwise.
     */
    private static String getNoLengthNormalizationVersion(String algorithm)
    {
        switch (algorithm)
        {
            case AlgorithmIdentifiers.BM25:
                return AlgorithmIdentifiers.BM25NOLEN;
            case AlgorithmIdentifiers.EBM25:
                return AlgorithmIdentifiers.EBM25NOLEN;
            case AlgorithmIdentifiers.PIVOTED:
                return AlgorithmIdentifiers.PIVOTEDNOLEN;
            case AlgorithmIdentifiers.QLD:
                return AlgorithmIdentifiers.QLDNOLEN;
            case AlgorithmIdentifiers.QLJM:
                return AlgorithmIdentifiers.QLJMNOLEN;
            default:
                return null;
        }
    }
}
