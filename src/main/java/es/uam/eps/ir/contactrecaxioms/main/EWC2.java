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
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Class for reproducing the experiments for the EWC2 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EWC2
{
    /**
     * Program that reproduces the experiments for the EWC1 axiom.
     * Generates a file comparing weigthed and unweighted algorithm variants.
     *
     * @param args Execution arguments:
     *             <ol>
     *               <li><b>Train:</b> Route to the file containing the training graph.</li>
     *               <li><b>Test:</b> Route to the file containing the test links.</li>
     *               <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations. Must include configurations for BM25.</li>
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
            System.err.println("\tAlgorithms: Route to an XML file containing the configurations for the BM25 algorithm.");
            System.err.println("\tOutput directory: Directory in which to store the recommendations and the output file.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
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
        int maxLength = Parsers.ip.parse(args[5]);
        boolean weighted = true;
        boolean printRecs = args[6].equalsIgnoreCase("true");

        long timea = System.currentTimeMillis();
        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainDataPath, weighted, false);
        if (graph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }
        // Read the test graph.

        TextGraphReader<Long> testGraphReader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> auxgraph = testGraphReader.read(testDataPath, false, false);
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

        int numUsers = testData.numUsersWithPreferences();
        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();
        // and obtain the parameters for BM25
        Grid grid = gridreader.getGrid(AlgorithmIdentifiers.BM25);
        if (grid == null)
        {
            System.err.println("ERROR: Configuration for the BM25 algorithm is not available");
            return;
        }

        // Obtain the configurations for BM25
        Configurations confs = grid.getConfigurations();
        AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>();

        // Initialize the maps to store the accuracy values in.
        Map<String, Double> bm25Values = new HashMap<>();
        Map<String, Double> ebm25Values = new HashMap<>();

        if(printRecs)
        {
            File file = new File(outputPath + "bm25" + File.separator);
            file.mkdir();
            file = new File(outputPath + "ebm25" + File.separator);
            file.mkdir();
        }

        // For each configuration, execute the BM25/EBM25 pair, and store its values.
        confs.getConfigurations().parallelStream().forEach(parameters ->
        {
            long timeaa = System.currentTimeMillis();
            // First, select the algorithms.
            Tuple2oo<String, RecommendationAlgorithmFunction<Long>> bm25Supp = algorithmSelector.getRecommender(AlgorithmIdentifiers.BM25, parameters);
            Tuple2oo<String, RecommendationAlgorithmFunction<Long>> ebm25Supp = algorithmSelector.getRecommender(AlgorithmIdentifiers.EBM25, parameters);
            String bm25name = bm25Supp.v1();
            String ebm25name = ebm25Supp.v1();

            // Configure the nDCG metric.
            NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

            // Configure the recommender runner.
            @SuppressWarnings("unchecked")
            Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));
            RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, testData.getUsersWithPreferences(), filter, maxLength);

            try
            {
                double bm25value;
                double ebm25value;
                Recommender<Long, Long> bm25 = bm25Supp.v2().apply(graph, trainData);
                Recommender<Long, Long> ebm25 = ebm25Supp.v2().apply(graph, trainData);

                // Execute and evaluate the recommenders
                if(printRecs)
                {
                    bm25value = AuxiliarMethods.computeAndEvaluate(outputPath + "bm25" + File.separator + bm25name + ".txt", bm25, runner, nDCG, numUsers);
                    ebm25value = AuxiliarMethods.computeAndEvaluate(outputPath + "ebm25" + File.separator + ebm25name + ".txt", ebm25, runner, nDCG, numUsers);
                }
                else
                {
                    bm25value = AuxiliarMethods.computeAndEvaluate(bm25, runner, nDCG, numUsers);
                    ebm25value = AuxiliarMethods.computeAndEvaluate(ebm25, runner, nDCG, numUsers);
                }

                // Store the accuracy values.
                bm25Values.put(bm25name, bm25value);
                ebm25Values.put(bm25name, ebm25value);
            }
            catch (IOException ioe)
            {
                System.err.println("ERROR: Something failed while executing " + bm25name);
            }

            long timebb = System.currentTimeMillis();
            System.out.println("Executed variant " + bm25name + " (" + (timebb-timeaa) + " ms.)");
        });

        // Print the output file.
        AuxiliarMethods.printFile(outputPath + "ewc2.txt", bm25Values, ebm25Values, "BM25", "EBM25", maxLength);
    }


}
