/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.basic;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Destination degree recommender. Recommender based on the Preferential Attachment link prediction method.
 * When the selected neighbourhood is formed by the incoming nodes, then this method is equal to the Popularity
 * recommender method.
 * <p>
 * Newman, M.E.J. Clustering and Preferential Attachment in Growing Networks. Physical Review Letters E, 64(025102), April 2001.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public class Popularity<U> extends UserFastRankingRecommender<U>
{
    /**
     * Link orientation for selecting the neighbours of the candidate node.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor for recommendation mode.
     *
     * @param graph Graph.
     * @param vSel  Link orientation for selecting the neighbours of the candidate node.
     */
    public Popularity(FastGraph<U> graph, EdgeOrientation vSel)
    {
        super(graph);
        this.vSel = vSel;
    }

    /**
     * Constructor for recommendation mode.
     *
     * @param graph Graph.
     */
    public Popularity(FastGraph<U> graph)
    {
        super(graph);
        this.vSel = EdgeOrientation.IN;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        U u = this.uidx2user(uidx);

        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(-1.0);

        this.getAllUsers().forEach(v -> scoresMap.put(this.item2iidx(v), this.getGraph().getNeighbourhoodSize(v, vSel) + 0.0));
        return scoresMap;
    }


}
