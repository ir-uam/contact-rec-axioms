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
import es.uam.eps.ir.contactrecaxioms.data.FastGraphIndex;
import es.uam.eps.ir.contactrecaxioms.data.GraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.SocialFastFilters;
import es.uam.eps.ir.contactrecaxioms.recommender.basic.Random;
import es.uam.eps.ir.contactrecaxioms.main.grid.*;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;
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
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;

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
     *               <li><b>Validation:</b> Route to the file containing the validation links.</li>
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
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Route to the file containing the training graph.");
            System.err.println("\tValidation: Route to the file containing the validation links.");
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
        String validationDataPath = args[1];
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
        Graph<Long> auxgraph = unweightedReader.read(validationDataPath, false, false);
        FastGraph<Long> validationGraph = (FastGraph<Long>) Adapters.onlyTrainUsers(auxgraph, unweightedGraph);
        if (validationGraph == null)
        {
            System.err.println("ERROR: Could not remove users from the test graph");
            return;
        }

        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" + (timeb - timea) + " ms.)");

        // Read the training and test data
        FastPreferenceData<Long, Long> unweightedTrainData = GraphSimpleFastPreferenceData.load(unweightedGraph);
        FastPreferenceData<Long, Long> weightedTrainData = GraphSimpleFastPreferenceData.load(weightedGraph);

        FastPreferenceData<Long, Long> validationData;
        validationData = GraphSimpleFastPreferenceData.load(validationGraph);
        GraphIndex<Long> index = new FastGraphIndex<>(unweightedGraph);

        // Read the XML containing the parameter grid for each algorithm
        AlgorithmGridReader gridreader = new AlgorithmGridReader(algorithmsPath);
        gridreader.readDocument();

        Set<String> algorithms = gridreader.getAlgorithms();

        int numUsers = validationData.numUsersWithPreferences();

        // For each algorithm.
        algorithms.forEach(algorithm ->
        {
            String directory = outputPath + algorithm + File.separator;
            if(printRecs)
            {
                File file = new File(directory);
                file.mkdir();
            }

            System.out.println("-------- Starting algorithm " + algorithm + " --------");
            long timeaa = System.currentTimeMillis();
            Grid grid = gridreader.getGrid(algorithm);
            Configurations confs = grid.getConfigurations();
            AlgorithmGridSelector<Long> algorithmSelector = new AlgorithmGridSelector<>();

            // Configure the recommender runner
            @SuppressWarnings("unchecked")
            Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(unweightedTrainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(unweightedGraph, index));
            RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, validationData.getUsersWithPreferences(), filter, maxLength);

            AtomicInteger counter = new AtomicInteger(0);
            List<Parameters> configurations = confs.getConfigurations();
            int totalCount = configurations.size();

            PriorityBlockingQueue<Tuple2od<String>> ranking = new PriorityBlockingQueue<>(totalCount, (x,y) -> Double.compare(y.v2, x.v2));

            // Now, execute each possible variant.
            configurations.parallelStream().forEach(parameters ->
            {
                Tuple2oo<String, RecommendationAlgorithmFunction<Long>> algSupp = algorithmSelector.getRecommender(algorithm, parameters);
                String algorithmName = algSupp.v1();

                // First, obtain the metric.
                NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, validationData, 0.5);
                SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), numUsers);

                try
                {
                    Recommender<Long, Long> weightedAlg = new Random<>(unweightedGraph);
                    Recommender<Long, Long> unweightedAlg = algSupp.v2().apply(unweightedGraph, unweightedTrainData);

                    if(weighted)
                    {
                        weightedAlg = algSupp.v2().apply(weightedGraph, weightedTrainData);
                    }

                    double weightedValue = 0;
                    double unweightedValue;

                    if(printRecs) // If we want to print the recommendations
                    {
                        if(weighted)
                        {
                            weightedValue = AuxiliarMethods.computeAndEvaluate(directory + "wei_" + algorithmName + ".txt", weightedAlg, runner, nDCG, numUsers);
                        }
                        unweightedValue = AuxiliarMethods.computeAndEvaluate(directory + (weighted ? "unw_" : "") + algorithmName + ".txt", unweightedAlg, runner, nDCG, numUsers);
                    }
                    else // Otherwise
                    {
                        if(weighted)
                        {
                            weightedValue = AuxiliarMethods.computeAndEvaluate(weightedAlg, runner, nDCG, numUsers);
                        }
                        unweightedValue = AuxiliarMethods.computeAndEvaluate(unweightedAlg, runner, nDCG, numUsers);
                    }



                    // Store the nDCG values.
                    if(weighted)
                    {
                        ranking.add(new Tuple2od<>("wei_" + algorithmName, weightedValue));
                        ranking.add(new Tuple2od<>("unw_" + algorithmName, unweightedValue));
                    }
                    else
                    {
                        ranking.add(new Tuple2od<>(algorithmName, unweightedValue));
                    }

                }
                catch (IOException ioe)
                {
                    System.err.println("ERROR: Something failed while executing " + algorithmName);
                }

                long timebb = System.currentTimeMillis();
                System.out.println("Algorithm " + counter.incrementAndGet() + "/" + totalCount + ": " + algorithmName + " finished (" + (timebb-timeaa) + " ms.)");
            });

            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "validation_" + algorithm + ".txt"))))
            {
                bw.write("Ranking\tVariant\tnDCG@"+maxLength);
                int i = 1;
                while(!ranking.isEmpty())
                {
                    Tuple2od<String> tuple = ranking.poll();
                    bw.write("\n" + i + "\t" + tuple.v1 + "\t" + tuple.v2);
                    ++i;
                }
            }
            catch(IOException ioe)
            {
                System.err.println("ERROR: Something failed while writing the output file for algorithm " + algorithm);
            }

            long timecc = System.currentTimeMillis();
            System.out.println("-------- Finished algorithm " + algorithm + " (" + (timecc-timeaa) + " ms.) --------");
        });
    }
}
