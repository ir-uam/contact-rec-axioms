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
import java.util.Map;
import java.util.Set;
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

        long timea = System.currentTimeMillis();
        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> auxgraph = greader.read(trainDataPath, weighted, false);
        if (auxgraph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }

        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxgraph);
        if (graph == null)
        {
            System.err.println("ERROR: Could not remove autoloops from the training graph");
            return;
        }

        // Read the test graph.
        auxgraph = greader.read(testDataPath, false, false);
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, graph);
        if (testGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Read the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        Set<String> algorithms = gridreader.getAlgorithms();

        Map<String, Double> lenNormValues = new HashMap<>();
        Map<String, Double> noLenNormValues = new HashMap<>();

        algorithms.forEach(lenNormIdentifier ->
        {
            String noLenNormIdentifier = CLNCS.getNoTermDiscriminationVersion(lenNormIdentifier);

            if (noLenNormIdentifier != null) // If it exists
            {
                Grid grid = gridreader.getGrid(lenNormIdentifier);
                Configurations confs = grid.getConfigurations();
                AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>();

                confs.getConfigurations().parallelStream().forEach(parameters ->
                {
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> lenNormSupp = algorithmSelector.getRecommender(lenNormIdentifier, parameters);
                    Tuple2oo<String, RecommendationAlgorithmFunction<Long>> noLenNormSupp = algorithmSelector.getRecommender(noLenNormIdentifier, parameters);
                    String tdName = lenNormSupp.v1();
                    String noTdName = noLenNormSupp.v1();

                    // First, obtain the metric.
                    NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                    SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

                    @SuppressWarnings("unchecked") Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));
                    RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, testData.getUsersWithPreferences(), filter, maxLength);

                    try
                    {
                        Recommender<Long, Long> lenNorm = lenNormSupp.v2().apply(graph, trainData);
                        double tdValue = AuxiliarMethods.computeAndEvaluate(outputPath + File.pathSeparator + tdName + ".txt", lenNorm, runner, nDCG);

                        Recommender<Long, Long> noLenNorm = noLenNormSupp.v2().apply(graph, trainData);
                        double noTdValue = AuxiliarMethods.computeAndEvaluate(outputPath + File.pathSeparator + noTdName + ".txt", noLenNorm, runner, nDCG);

                        lenNormValues.put(tdName, tdValue);
                        noLenNormValues.put(tdName, noTdValue);
                    }
                    catch (IOException ioe)
                    {
                        System.err.println("ERROR: Something failed while executing " + tdName);
                    }
                });
            }
            else
            {
                System.err.println("Algorithm " + lenNormIdentifier + " has no version without term discrimination");
            }
        });

        AuxiliarMethods.printFile(outputPath + "lncs.txt", lenNormValues, noLenNormValues, "Len Norm.", "No Len Norm.", maxLength);
    }

    /**
     * If the algorithm has a version without length normalization, finds the identifier of the algorithm.
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
