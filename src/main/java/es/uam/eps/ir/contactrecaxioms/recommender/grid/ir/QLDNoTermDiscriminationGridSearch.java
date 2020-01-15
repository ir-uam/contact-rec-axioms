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
import es.uam.eps.ir.contactrecaxioms.recommender.ir.QLDNoTermDiscrimination;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmIdentifiers.QLDNOTD;


/**
 * Grid search generator for Query Likelihood algorithm with Dirichlet normalization (Term-based version).
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class QLDNoTermDiscriminationGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the trade-off between the regularization term and the original term in
     * the Query Likelihood Dirichlet formula.
     */
    private static final String MU  = "mu";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String VSEL = "vSel";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        List<Double> mus = grid.getDoubleValues(MU);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        mus.stream().forEach(mu -> 
        {
            uSels.stream().forEach(uSel -> 
            {
                vSels.stream().forEach(vSel -> 
                {
                    recs.put(QLDNOTD + "_" + uSel + "_" + vSel + "_" + mu, () ->
                    {
                       return new QLDNoTermDiscrimination<>(graph, uSel, vSel, mu);
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
        List<Double> mus = grid.getDoubleValues(MU);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        mus.stream().forEach(mu -> 
        {
            uSels.stream().forEach(uSel -> 
            {
                vSels.stream().forEach(vSel -> 
                {
                    recs.put(QLDNOTD + "_" + uSel + "_" + vSel + "_" + mu, (graph, prefData) ->
                    {
                       return new QLDNoTermDiscrimination<>(graph, uSel, vSel, mu);
                    });
                });
            });
        });
        return recs;    }
    
}
