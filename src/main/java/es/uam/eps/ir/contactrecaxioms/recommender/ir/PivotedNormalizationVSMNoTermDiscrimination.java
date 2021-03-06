/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.ir;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.OptionalDouble;

/**
 * Adaptation of the pivoted normalization vector space model (VSM), without term discrimination.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PivotedNormalizationVSMNoTermDiscrimination<U> extends UserFastRankingRecommender<U>
{
    /**
     * Value that balances the importance of the document length.
     */
    private final double s;

    /**
     * Neighborhood orientation selected for the target user.
     */
    private final EdgeOrientation uSel;

    /**
     * Neighborhood orientation selected for the candidate user.
     */
    private final EdgeOrientation vSel;

    /**
     * Average length of the candidate users.
     */
    private final double avgSize;

    /**
     * User lengths.
     */
    private final Int2DoubleMap lengths;

    /**
     * Constructor.
     *
     * @param graph the training network.
     * @param uSel  neighborhood orientation selected for the target user.
     * @param vSel  neighborhood orientation selected for the candidate user.
     * @param s     parameter for balancing the importance of the document length.
     */
    public PivotedNormalizationVSMNoTermDiscrimination(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, double s)
    {
        super(graph);

        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.s = s;

        this.lengths = new Int2DoubleOpenHashMap();

        OptionalDouble opt = this.getAllUidx().mapToDouble(vidx ->
        {
            // User length.
            double len = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(widx -> widx.v2).sum();
            this.lengths.put(vidx, len);

            return len;
        }).average();

        this.avgSize = opt.isPresent() ? opt.getAsDouble() : 0.0;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhoodWeights(uidx, uSel).forEach(w ->
        {
            int widx = w.v1;
            double uW = w.v2;
            //double idf = this.idfs.get(w.v1);

            graph.getNeighborhoodWeights(widx, vSel).forEach(v ->
            {
                double val = (1 + Math.log(1 + Math.log(w.v2))) * uW;//*Math.log(idf);
                scoresMap.addTo(v.v1, val);
            });
        });

        scoresMap.replaceAll((vidx, val) -> val / (1 - s + s * lengths.get(vidx.intValue()) / avgSize));

        return scoresMap;
    }
}
