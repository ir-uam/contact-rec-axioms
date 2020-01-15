package es.uam.eps.ir.contactrecaxioms.main;

import es.uam.eps.ir.contactrecaxioms.graph.Adapters;
import es.uam.eps.ir.contactrecaxioms.graph.Graph;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.graph.io.TextGraphReader;
import es.uam.eps.ir.contactrecaxioms.recommender.FastGraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.GraphIndex;
import es.uam.eps.ir.contactrecaxioms.recommender.SocialFastFilters;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridReader;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSelector;
import es.uam.eps.ir.contactrecaxioms.data.GraphSimpleFastPreferenceData;
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

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


import static org.ranksys.formats.parsing.Parsers.lp;

public class EWC1
{
    public static void main(String args[])
    {
        if(args.length < 11)
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
        int maxLength = Parsers.ip.parse(args[8]);

        Map<String, Double> weightedValues = new HashMap<>();
        Map<String, Double> unweightedValues = new HashMap<>();

        List<Boolean> weightedVals = new ArrayList<>();
        weightedVals.add(true);
        weightedVals.add(false);

        // First, we do create the directories.
        File weightedDirectory = new File(outputPath + File.pathSeparator + "weighted");
        weightedDirectory.mkdir();
        File unweightedDirectory = new File(outputPath + File.pathSeparator + "unweighted");
        unweightedDirectory.mkdir();

        weightedVals.forEach(weighted ->
        {
            long timea = System.currentTimeMillis();
            // Read the training graph.
            TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
            Graph<Long> auxgraph = greader.read(trainDataPath, weighted, false);
            if(auxgraph == null)
            {
                System.err.println("ERROR: Could not read the training graph");
                return;
            }

            FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxgraph);
            if(graph == null)
            {
                System.err.println("ERROR: Could not remove autoloops from the training graph");
                return;
            }

            // Read the test graph.
            auxgraph = greader.read(testDataPath, false, false);
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

            Map<String, Supplier<Recommender<Long,Long>>> recMap = new HashMap<>();
            // Get the different recommenders to execute
            gridreader.getAlgorithms().forEach(algorithm ->
            {
                AlgorithmGridSelector<Long> ags = new AlgorithmGridSelector<>();
                recMap.putAll(ags.getRecommenders(algorithm, gridreader.getGrid(algorithm), graph, trainData));
            });

            timeb = System.currentTimeMillis();
            System.out.println("Algorithms selected (" +(timeb-timea) + " ms.)");

            // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
            Set<Long> targetUsers = testData.getUsersWithPreferences().collect(Collectors.toCollection(HashSet::new));

            System.out.println("Num. target users: " + targetUsers.size());
            RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp,lp);

            Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(trainData), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(graph,index));

            RecommenderRunner<Long,Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);

            // Execute the recommendations
            recMap.entrySet().parallelStream().forEach(entry ->
            {
                String name = entry.getKey();

                String path = outputPath + File.pathSeparator + (weighted ? "weighted" : "unweighted") + File.pathSeparator + name + ".txt";
                File f = new File(path);

                // First, obtain the metric.
                NDCG.NDCGRelevanceModel<Long, Long> ndcgModel = new NDCG.NDCGRelevanceModel<>(false, testData, 0.5);
                SystemMetric<Long, Long> nDCG = new AverageRecommendationMetric<>(new NDCG<>(maxLength, ndcgModel), true);

                // Prepare the recommender
                System.out.println("Preparing " + name);
                Supplier<Recommender<Long,Long>> recomm = entry.getValue();
                long a = System.currentTimeMillis();
                Recommender<Long, Long> rec = recomm.get();
                long b = System.currentTimeMillis();
                System.out.println("Prepared " + name + " (" + (b-a) + " ms.)");

                // Write the recommendations
                RecommendationFormat.Writer<Long, Long> writer;
                RecommendationFormat.Reader<Long, Long> reader;
                try
                {
                    // Execute the recommendations
                    writer = format.getWriter(outputPath + name + ".txt");

                    System.out.println("Running " + name);
                    a = System.currentTimeMillis();
                    runner.run(rec, writer);
                    b = System.currentTimeMillis();
                    System.out.println("Done " + name + " (" + (b - a) + " ms.)");

                    writer.close();

                    // Evaluate the recommendations
                    reader = format.getReader(path);
                    reader.readAll().forEach(r -> nDCG.add(r));
                    if(weighted)
                    {
                        weightedValues.put(name, nDCG.evaluate());
                    }
                    else
                    {
                        unweightedValues.put(name, nDCG.evaluate());
                    }
                }
                catch(IOException ioe)
                {
                    System.err.println("Algorithm " + name + " failed");
                }
            });
        });

        // Write everything in the file
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath+"res.txt"))))
        {
            bw.write("Algorithm\tWeighted\tUnweighted\n");
            Set<String> algs = weightedValues.keySet();
            for(String alg : algs)
            {
                bw.write(alg + "\t" + weightedValues.get(alg) + "\t" + unweightedValues.get(alg) + "\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("Something failed while writing the results file");
        }
    }
}
