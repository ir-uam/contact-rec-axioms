/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
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

import java.io.*;
import java.util.*;
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
     * @param args Execution arguments:
     *        <ol>
     *          <li><b>Train:</b> Route to the file containing the training graph.</li>
     *          <li><b>Test:</b> Route to the file containing the test links.</li>
     *          <li><b>Algorithms:</b> Route to an XML file containing the recommender configurations. Must include configurations for BM25.</li>
     *          <li><b>Output directory:</b> Directory in which to store the recommendations and the output file.</li>
     *          <li><b>Directed:</b> True if the network is directed, false otherwise.</li>
     *          <li><b>Max. Length:</b> Maximum number of recommendations per user.</li>
     *        </ol>
     */
    public static void main(String args[])
    {
        if(args.length < 6)
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
        int maxLength = Parsers.ip.parse(args[5]);
        boolean weighted = true;

        long timea = System.currentTimeMillis();
        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        FastGraph<Long> graph = (FastGraph<Long>) greader.read(trainDataPath, weighted, false);
        if(graph == null)
        {
            System.err.println("ERROR: Could not read the training graph");
            return;
        }
        // Read the test graph.

        Graph<Long> auxgraph = greader.read(testDataPath, false, false);
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, graph);
        if(testGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" +(timeb-timea) + " ms.)");
        timea = System.currentTimeMillis();

        // Read the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();
        // and obtain the parameters for BM25
        Grid grid = gridreader.getGrid(AlgorithmIdentifiers.BM25);
        if(grid == null)
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

        // For each configuration, execute the BM25/EBM25 pair, and store its values.
        confs.getConfigurations().parallelStream().forEach(parameters ->
        {
            // First, select the algorithms.
            Tuple2oo<String, RecommendationAlgorithmFunction<Long>> bm25Supp = algorithmSelector.getRecommender(AlgorithmIdentifiers.BM25, parameters);
            Tuple2oo<String, RecommendationAlgorithmFunction<Long>> ebm25Supp = algorithmSelector.getRecommender(AlgorithmIdentifiers.EBM25, parameters);
            String bm25name = bm25Supp.v1();
            String ebm25name = ebm25Supp.v1();

            // Configure the nDCG metric.
            NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

            // Configure the recommender runner.
            Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph,index));
            RecommenderRunner<Long,Long> runner = new FastFilterRecommenderRunner<>(index, index, testData.getUsersWithPreferences(), filter, maxLength);

            try
            {
                // Execute and evaluate the recommenders
                Recommender<Long, Long> bm25 = bm25Supp.v2().apply(graph, trainData);
                double bm25value = AuxiliarMethods.computeAndEvaluate(outputPath + bm25name + ".txt", bm25, runner, nDCG);

                Recommender<Long, Long> ebm25 = ebm25Supp.v2().apply(graph, trainData);
                double ebm25value = AuxiliarMethods.computeAndEvaluate(outputPath + ebm25name + ".txt", ebm25, runner, nDCG);

                // Store the accuracy values.
                bm25Values.put(bm25name, bm25value);
                ebm25Values.put(bm25name, ebm25value);
            }
            catch(IOException ioe)
            {
                System.err.println("ERROR: Something failed while executing " + bm25name);
            }
        });

        // Print the output file.
        AuxiliarMethods.printFile(outputPath+"ewc2.txt", bm25Values, ebm25Values, "BM25", "EBM25", maxLength);
    }


}