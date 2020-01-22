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
import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.graph.io.TextGraphReader;
import es.uam.eps.ir.contactrecaxioms.metrics.UserLength;
import es.uam.eps.ir.contactrecaxioms.metrics.UserLengthNovelty;
import es.uam.eps.ir.contactrecaxioms.recommender.FastGraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.GraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.SocialFastFilters;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridReader;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSelector;
import es.uam.eps.ir.contactrecaxioms.recommender.io.EmptyWriter;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.metrics.rank.NoDiscountModel;
import es.uam.eps.ir.ranksys.metrics.rank.RankingDiscountModel;
import es.uam.eps.ir.ranksys.metrics.rel.NoRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.RelevanceModel;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Class for reproducing the experiments that compare accuracy with the average degree of the recommended users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AccuracyVSDegree
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
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\tTrain: Training data");
            System.err.println("\tTest: Test data");
            System.err.println("\tAlgorithms: Location of the XML grid file");
            System.err.println("\tOutput directory: Directory for storing the recommendations");
            System.err.println("\tDirected: true if the graph is directed, false if not");
            System.err.println("\tMaxLength: maximum length of the recommendation ranking");
            return;
        }

        String trainDataPath = args[0];
        String testDataPath = args[1];
        String algorithmsPath = args[2];
        String outputPath = args[3];
        boolean directed = args[4].equalsIgnoreCase("true");
        boolean weighted = args[5].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[6]);
        boolean printRecs = args[7].equalsIgnoreCase("true");

        Map<String, Double> nDCGvalues = new HashMap<>();
        Map<String, Double> inDegreeValues = new HashMap<>();
        Map<String, Double> outDegreeValues = new HashMap<>();
        Map<String, Double> undDegreeValues = new HashMap<>();

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

        Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>();
        // Get the different recommenders to execute
        gridreader.getAlgorithms().forEach(algorithm ->
        {
            AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>();
            recMap.putAll(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm), graph, trainData));
        });

        timeb = System.currentTimeMillis();
        System.out.println("Algorithms selected (" + (timeb - timea) + " ms.)");

        // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
        Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));

        System.out.println("Num. target users: " + targetUsers.size());
        RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp, lp);

        @SuppressWarnings("unchecked")
        Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph, index));

        RecommenderRunner<Long, Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);

        // Execute the recommendations
        recMap.forEach((name, recomm) ->
        {
            long a = System.currentTimeMillis();

            // First, obtain the accuracy metric.
            NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
            SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

            // Then, the average degree metrics.
            RelevanceModel<Long, Long> noRel = new NoRelevanceModel<>();
            RankingDiscountModel noDisc = new NoDiscountModel();
            UserLengthNovelty<Long> inNovelty = new UserLengthNovelty<>(trainData, EdgeOrientation.IN);
            SystemMetric<Long, Long> inDegree = new AverageRecommendationMetric<>(new UserLength<>(maxLength, inNovelty, noRel, noDisc), true);
            UserLengthNovelty<Long> outNovelty = new UserLengthNovelty<>(trainData, EdgeOrientation.OUT);
            SystemMetric<Long, Long> outDegree = new AverageRecommendationMetric<>(new UserLength<>(maxLength, outNovelty, noRel, noDisc), true);
            UserLengthNovelty<Long> undNovelty = new UserLengthNovelty<>(trainData, EdgeOrientation.UND);
            SystemMetric<Long, Long> degree = new AverageRecommendationMetric<>(new UserLength<>(maxLength, undNovelty, noRel, noDisc), true);

            // Prepare the recommender
            Recommender<Long, Long> rec = recomm.get();

            String directory = outputPath + "degreerecs" + File.separator;

            // If we have to print the recommendations, create a directory to store them.
            if (printRecs)
            {
                File file = new File(directory);
                file.mkdir();
            }

            // Write the recommendations
            RecommendationFormat.Writer<Long, Long> writer;
            RecommendationFormat.Reader<Long, Long> reader;
            try
            {
                if (printRecs)
                {
                    // Execute the recommendations
                    writer = format.getWriter(directory + name + ".txt");
                }
                else
                {
                    writer = new EmptyWriter<>();
                }

                runner.run(rec, writer);
                long b = System.currentTimeMillis();
                System.out.println("Done " + name + " (" + (b - a) + " ms.)");

                writer.close();

                // Evaluate the recommendations
                if (printRecs)
                {
                    reader = format.getReader(directory + name + ".txt");
                }
                else
                {
                    reader = (EmptyWriter<Long, Long>) writer;
                }

                reader.readAll().forEach(r ->
                {
                    nDCG.add(r);
                    if (directed)
                    {
                        inDegree.add(r);
                        outDegree.add(r);
                    }
                    degree.add(r);

                });
                nDCGvalues.put(name, nDCG.evaluate());
                if (directed)
                {
                    inDegreeValues.put(name, inDegree.evaluate());
                    outDegreeValues.put(name, outDegree.evaluate());
                }
                undDegreeValues.put(name, degree.evaluate());
            }
            catch (IOException ioe)
            {
                System.err.println("Algorithm " + name + " failed");
            }
        });

        // Write everything in the file
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "degrees.txt"))))
        {
            if(directed)
            {
                bw.write("Algorithm\tnDCG@" + maxLength + "\tIn-degree@" + maxLength + "\tOut-degree@" + maxLength + "\tDegree@" + maxLength);
            }
            else
            {
                bw.write("Algorithm\tnDCG@" + maxLength + "\tDegree@" + maxLength);
            }

            Set<String> algs = nDCGvalues.keySet();
            for (String alg : algs)
            {
                if(directed)
                {
                    bw.write("\n" + alg + "\t" + nDCGvalues.get(alg) + "\t" + inDegreeValues.get(alg) + "\t" + outDegreeValues.get(alg) + "\t" + undDegreeValues.get(alg));
                }
                else
                {
                    bw.write("\n" + alg + "\t" + nDCGvalues.get(alg) + "\t" + undDegreeValues.get(alg));
                }

            }
        }
        catch (IOException ioe)
        {
            System.err.println("Something failed while writing the results file");
        }
    }
}
