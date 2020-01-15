/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.foaf;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Most Common Neighbours (MCN) recommender. Recommends the users that share the maximum number of neighbours with the target user.
 *
 * Liben-Nowell, D., Kleinberg, J. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7), May 2007.
 * Newman, M.E.J. Clustering and Preferential Attachment in Growing Networks. Physical Review Letters E, 64(025102), April 2001.
 * *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 * @param <U> type of the users.
 */
public class MostCommonNeighbors<U> extends UserFastRankingRecommender<U>
{
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     * @param graph User graph.
     * @param uSel Link orientation for the target users.
     * @param vSel Link orientation for the candidate users.
     */
    public MostCommonNeighbors(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);
        
        graph.getNeighborhood(uidx, uSel).forEach(widx -> 
        {
            graph.getNeighborhood(widx, vSel).forEach(vidx -> 
            {
                scoresMap.addTo(vidx, 1.0);
            });
        });
       
        return scoresMap;
    }
}
