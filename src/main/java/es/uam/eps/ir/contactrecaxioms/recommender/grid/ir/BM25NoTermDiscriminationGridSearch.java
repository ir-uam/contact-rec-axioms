/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.grid.ir;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.Grid;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.RecommendationAlgorithmFunction;
import es.uam.eps.ir.contactrecaxioms.recommender.ir.BM25NoTermDiscrimination;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmIdentifiers.BM25NOTD;

/**
 * Grid search generator for the BM25 algorithm (Term-based version).
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class BM25NoTermDiscriminationGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for parameter b
     */
    private static final String B = "b";
    /**
     * Identifier for parameter k
     */
    private static final String K = "k";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for the orientation for the document length
     */
    private static final String DLSEL = "dlSel";
    
    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);
        
        bs.stream().forEach(b -> 
        {
            ks.stream().forEach(k -> 
            {
                uSels.stream().forEach(uSel -> 
                {
                    vSels.stream().forEach(vSel -> 
                    {
                        dlSels.stream().forEach(dlSel -> 
                        {
                            recs.put(BM25NOTD + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, () ->
                            { 
                                return new BM25NoTermDiscrimination<>(graph, uSel, vSel, dlSel, b, k);
                            });
                        });
                    });
                });
            });
        });

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);
        
        bs.stream().forEach(b -> 
        {
            ks.stream().forEach(k -> 
            {
                uSels.stream().forEach(uSel -> 
                {
                    vSels.stream().forEach(vSel -> 
                    {
                        dlSels.stream().forEach(dlSel -> 
                        {
                            recs.put(BM25NOTD + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, (graph, prefData) ->
                            { 
                                return new BM25NoTermDiscrimination<>(graph, uSel, vSel, dlSel, b, k);
                            });
                        });
                    });
                });
            });
        });

        return recs;
    }
    
}
