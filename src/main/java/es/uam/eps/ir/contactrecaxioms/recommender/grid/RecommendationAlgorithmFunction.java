/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.grid;

import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

/**
 * Functions for retrieving trained recommendation algorithms.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
@FunctionalInterface
public interface RecommendationAlgorithmFunction<U>
{
    /**
     * Given a graph, and the preference data, obtains a trained algorithm.
     * @param graph the graph.
     * @param prefData the preference data.
     * @return the trained recommender.
     */
    public Recommender<U,U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData);
}
