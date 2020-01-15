/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
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
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation.*;

/**
 * Program for reproducing the experiments conducted for the EWC3 axiom.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EWC3
{
    /**
     * Main for executing the experiments for the EWC3 axiom.
     * @param args Execution arguments:
     *             <ol>
     *              <li><b>Train:</b> Route of a file containing the training graph.</li>
     *              <li><b>Test:</b> Route of a file containing the test edges.</li>
     *              <li><b>Directed:</b> True if the graph is directed, false otherwise.</li>
     *              <li><b>Output file:</b> File to store the results.</li>
     *             </ol>
     */
    public static void main(String[] args)
    {
        if(args.length < 3)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("Arguments:");
            System.err.println("\tTrain: file containing the training graph");
            System.err.println("\tTest: file containing the test graph");
            System.err.println("\tDirected: true if the graph is directed, false otherwise.");
            System.err.println("\tFile: the output file");
            return;
        }

        // Read the arguments.
        String trainFile = args[0];
        String testFile = args[1];
        boolean directed = args[2].equals("true");
        String file = args[3];

        // Read the graph
        GraphReader<Long> greader = new TextGraphReader<>(directed, false, false, "\t", Parsers.lp);
        Graph<Long> train = greader.read(trainFile);
        Graph<Long> test = greader.read(testFile);

        EdgeOrientation[] eos;
        if(directed)
        {
            eos = new EdgeOrientation[]{IN, OUT, UND};
        }
        else
        {
            eos = new EdgeOrientation[]{UND};
        }

        // Get users that appear in the training graph which create new outgoing edges in test:
        Set<Long> users = train.getAllNodes().filter(test::hasAdjacentEdges).collect(Collectors.toSet());

        // For each pair of orientations, get the reachable users from the target ones:
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            for(EdgeOrientation eo : eos)
            {
                for (EdgeOrientation eo2 : eos)
                {
                    System.out.println(eo + "\t" + eo2 + "started");
                    Stats posStats = new Stats();

                    // For each target user
                    for (Long u : users) {
                        // If the users have at least one neighbor
                        if (train.getNeighbourhoodSize(u, eo) > 1) {
                            Long2DoubleMap map = new Long2DoubleOpenHashMap();
                            // First, we obtain the MCN values between u and the users at distance 2.
                            train.getNeighbourhood(u, eo).forEach(v ->
                                    train.getNeighbourhood(v, eo2.invertSelection()).forEach(w ->
                                    {
                                        long widx = w;
                                        map.put(widx, map.getOrDefault(widx, 0.0) + 1.0);
                                    })
                            );

                            // Obtain the list for computing AUC.
                            List<Tuple2oo<Double, Boolean>> values = new ArrayList<>();
                            boolean include = false;

                            // Fill the list.
                            for (Entry<Long, Double> entry : map.long2DoubleEntrySet()) {
                                long v = entry.getKey();
                                double val = entry.getValue();

                                if (!train.containsEdge(u, v) && !train.containsEdge(v, u)) {
                                    if (test.containsEdge(u, v)) {
                                        values.add(new Tuple2oo<>(val, true));
                                        include = true;
                                    } else {
                                        values.add(new Tuple2oo<>(val, false));
                                    }
                                }
                            }

                            // If the user has, at least, one positive element...
                            if (include) {
                                AUC auc = new AUC();
                                double aucValue = auc.compute(values);
                                posStats.accept(aucValue);
                            }
                        }
                    }

                    bw.write(eo + "\t" + eo2 + "\t" + posStats.getMean());
                }
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the file");
        }
    }
}

