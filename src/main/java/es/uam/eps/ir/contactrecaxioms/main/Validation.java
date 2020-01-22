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
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Class that executes the validation process for the different contact recommendation algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Validation
{
    /**
     * Program that reproduces the experiments for the NDC axiom.
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
     *               <li><b>Print recommendations:</b> True if, additionally to the results, you want to print the recommendations. False otherwise</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 8)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tTest: Route to the file containing the test links.");
            System.err.println("\tAlgorithms: Route to an XML file containing the recommender configurations.");
            System.err.println("\tOutput directory: Directory in which to store the outcome of the program.");
            System.err.println("\tDirected: True if the network is directed, false otherwise.");
            System.err.println("\tWeighted: True if the network is weighted, false otherwise.");
            System.err.println("\tMax. Length:  Maximum number of recommendations per user.");
            System.err.println("\tPrint recommendations: True if, additionally to the validation results, you want to store the recommendations. False otherwise.");
        }

        // Read the program arguments.
        String trainDataPath = args[0];
        String testDataPath = args[1];
        String algorithmsPath = args[2];
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        boolean weighted = args[5].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[6]);
        boolean printRecommendations = args[7].equalsIgnoreCase("true");

        // Read the graph.
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
        Graph<Long> auxgraph = greader.read(testDataPath, false, false);
        FastGraph<Long> testGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, graph);
        if (testGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Create the training and test data
        FastPreferenceData<Long, Long> trainData;
        trainData = GraphSimpleFastPreferenceData.load(graph);

        FastPreferenceData<Long, Long> testData;
        testData = GraphSimpleFastPreferenceData.load(testGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(graph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        // Obtain the set of algorithms, and prepare the basic elements for the recommendation.
        Set<String> algorithms = gridreader.getAlgorithms();
        AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>();
        @SuppressWarnings("unchecked") Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));
        Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));
        int numUsers = testData.numUsersWithPreferences();

        System.out.println("Num. target users: " + targetUsers.size());

        // For each algorithm, obtain the ranking of variants.
        algorithms.forEach(algorithm ->
        {
            System.out.println("Start algorithm " + algorithm);
            long timeaa = System.currentTimeMillis();

            // Obtain the different variants.
            Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm), graph, trainData));

            long timebb = System.currentTimeMillis();
            System.out.println("Identified " + recMap.size() + " variants of " + algorithm + "(" + (timebb - timeaa) + " ms.)");

            // If we have to store the recommendations in disk, obtain the route and create the directory.
            String route = outputPath + algorithm + File.pathSeparator;
            if (printRecommendations)
            {
                File dir = new File(route);
                dir.mkdir();
            }

            // Initialize the algorithm ranking
            PriorityBlockingQueue<Tuple2od<String>> ranking = new PriorityBlockingQueue<>(recMap.size(), (x, y) -> Double.compare(y.v2, x.v2));

            // Execute each variant and obtain the evaluation value.
            recMap.entrySet().parallelStream().forEach(entry ->
            {
                long timec;
                // Obtain the name of the variant
                String name = entry.getKey();

                RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);
                // First, obtain the accuracy metric (nDCG).
                NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

                // Initialize the recommender
                Recommender<Long, Long> rec = entry.getValue().get();

                timec = System.currentTimeMillis();
                System.out.println("Initialized " + name + " (" + (timec - timeaa) + " ms.)");

                // Execute it and obtain the nDCG value
                double value;
                try
                {
                    if (printRecommendations)
                    {
                        value = AuxiliarMethods.computeAndEvaluate(route + name + ".txt", rec, runner, nDCG, numUsers);
                    }
                    else
                    {
                        value = AuxiliarMethods.computeAndEvaluate(rec, runner, nDCG, numUsers);
                    }

                    // Add the variant to the ranking
                    ranking.add(new Tuple2od<>(name, value));
                    System.out.println("Finished " + name + " (" + (timec - timeaa) + " ms.)");
                }
                catch (IOException e)
                {
                    System.err.println("ERROR: Something failed while executing the " + name + " recommender");
                }
            });

            timebb = System.currentTimeMillis();
            System.err.println("Finished executing algorithm " + algorithm + "(" + (timebb - timeaa) + " ms.)");

            // Write the algorithm ranking
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + algorithm + ".txt"))))
            {
                bw.write("Algorithm\tnDCG@" + maxLength);
                while (!ranking.isEmpty())
                {
                    Tuple2od<String> elem = ranking.poll();
                    bw.write("\n" + elem.v1 + "\t" + elem.v2);
                }
            }
            catch (IOException ioe)
            {
                System.err.println("ERROR: Something failed while writing the output file for algorithm" + algorithm);
            }

            timebb = System.currentTimeMillis();
            System.err.println("Finished writing the outcome for algorithm " + algorithm + "(" + (timebb - timeaa) + " ms.)");
        });
    }
}
