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

import es.uam.eps.ir.contactrecaxioms.graph.Graph;
import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.io.GraphReader;
import es.uam.eps.ir.contactrecaxioms.graph.io.TextGraphReader;
import es.uam.eps.ir.contactrecaxioms.metrics.AUC;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;
import es.uam.eps.ir.ranksys.core.util.Stats;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation.*;

/**
 * Class for reproducing the experiments for the EWC3 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EWC3
{
    /**
     * Main for executing the experiments for the EWC3 axiom.
     *
     * @param args Execution arguments:
     *             <ol>
     *              <li><b>Train:</b> Route of a file containing the training graph.</li>
     *              <li><b>Test:</b> Route of a file containing the test edges.</li>
     *              <li><b>Directed:</b> True if the graph is directed, false otherwise.</li>
     *              <li><b>Output directory:</b> Directory in which to store the results.</li>
     *              <li><b>Print recommendations:</b> True if, additionally to the results, you want to print the recommendations. False otherwise</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if (args.length < 5)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("Arguments:");
            System.err.println("\tTrain: Route of a file containing the training graph.");
            System.err.println("\tTest: Route of a file containing the test edges.");
            System.err.println("\tDirected: True if the graph is directed, false otherwise.");
            System.err.println("\tOutput directory: Directory in which to store the results.");
            System.err.println("\tPrint recommendations: True if, additionally to the results, you want to print the recommendations. False otherwise");
            return;
        }

        // Read the arguments.
        String trainFile = args[0];
        String testFile = args[1];
        boolean directed = args[2].equalsIgnoreCase("true");
        String output = args[3];
        boolean printRecs = args[4].equalsIgnoreCase("true");

        // Read the graph
        GraphReader<Long> greader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> train = greader.read(trainFile);
        Graph<Long> test = greader.read(testFile);

        // Obtain the possible EdgeOrientation values.
        EdgeOrientation[] eos;
        if (directed)
        {
            eos = new EdgeOrientation[]{IN, OUT, UND};
        }
        else
        {
            eos = new EdgeOrientation[]{UND};
        }

        // Get users that appear in the training graph which create new outgoing edges in test:
        Set<Long> users = train.getAllNodes().filter(test::hasAdjacentEdges).collect(Collectors.toSet());
        int numUsers = users.size();

        // If we choose to print the recommendations, create the folder to store them.
        if(printRecs)
        {
            File file = new File(output + "mcncurves" + File.separator);
            file.mkdir();
        }

        // For each pair of orientations, get the reachable users from the target ones:
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output + "ewc3.txt"))))
        {
            bw.write("Target user orientation\tCandidate user orientation\tAUC");
            for (EdgeOrientation eo : eos)
            {
                for (EdgeOrientation eo2 : eos)
                {
                    long timea = System.currentTimeMillis();
                    System.out.println(eo + " " + eo2 + " started");
                    Stats posStats = new Stats();

                    Map<Long, PriorityQueue<Tuple3<Long, Double, Boolean>>> recs = new HashMap<>();
                    // For each target user
                    for (Long u : users)
                    {
                        // If the users have at least one neighbor
                        if (train.getNeighbourhoodSize(u, eo) > 1)
                        {
                            Long2DoubleMap map = new Long2DoubleOpenHashMap();
                            // First, we obtain the MCN values between u and the users at distance 2.
                            train.getNeighbourhood(u, eo).forEach(v ->
                                train.getNeighbourhood(v, eo2.invertSelection()).forEach(w ->
                                {
                                    long widx = w;
                                    map.put(widx, map.getOrDefault(widx, 0.0) + 1.0);
                                }));

                            // Obtain the list for computing AUC.
                            List<Tuple2oo<Double, Boolean>> values = new ArrayList<>();
                            boolean include = false;
                            PriorityQueue<Tuple3<Long, Double, Boolean>> rankingHeap = new PriorityQueue<>(numUsers, (o1, o2) -> Double.compare(o2.v2(),o1.v2()));

                            // Fill the list.
                            for (Entry<Long, Double> entry : map.long2DoubleEntrySet())
                            {
                                long v = entry.getKey();
                                double val = entry.getValue();

                                if (!train.containsEdge(u, v) && !train.containsEdge(v, u))
                                {
                                    if (test.containsEdge(u, v))
                                    {
                                        values.add(new Tuple2oo<>(val, true));
                                        include = true;
                                        if(printRecs)
                                        {
                                            rankingHeap.add(new Tuple3<>(v,val, true));
                                        }
                                    }
                                    else
                                    {
                                        values.add(new Tuple2oo<>(val, false));
                                        if(printRecs)
                                        {
                                            rankingHeap.add(new Tuple3<>(v,val, false));
                                        }
                                    }
                                }
                            }

                            // If the user has, at least, one positive element...
                            if (include)
                            {
                                AUC auc = new AUC();
                                double aucValue = auc.compute(values);
                                posStats.accept(aucValue);
                                if(printRecs)
                                    recs.put(u, rankingHeap);
                            }
                        }
                    }

                    // If we choose to print the recommendations, then, do it.
                    if(printRecs)
                    {
                        EWC3.printRecs(output + "mcncurves" + File.separator + "mcn_" + eo.toString() + "_" + eo2.toString() + ".txt", recs);
                    }

                    long timeb = System.currentTimeMillis();
                    System.out.println(eo + " " + eo2 + " finished (" + (timeb-timea) + " ms.)");
                    bw.write("\n" + eo + "\t" + eo2 + "\t" + posStats.getMean());
                }
            }
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the file");
        }
    }


    /**
     * Prints the recommendation.
     * @param file File in which to store the recommendations.
     * @param recs The recommendation rankings.
     * @throws IOException if something fails while writing the file.
     */
    private static void printRecs(String file, Map<Long, PriorityQueue<Tuple3<Long, Double, Boolean>>> recs) throws IOException
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            bw.write("Target\tCandidate\tValue\tRelevant");
            for(long u : recs.keySet())
            {
                PriorityQueue<Tuple3<Long, Double, Boolean>> ranking = recs.get(u);
                while(!ranking.isEmpty())
                {
                    Tuple3<Long, Double, Boolean> tuple = ranking.poll();
                    bw.write("\n"+ u + "\t" + tuple.v1 + "\t" + tuple.v2 + "\t" + (tuple.v3 ? "1" : "0"));
                }
            }
        }
    }
}

