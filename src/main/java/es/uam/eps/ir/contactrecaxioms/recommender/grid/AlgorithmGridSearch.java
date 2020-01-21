/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.grid;

import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given algorithm.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado Puig
 */
public interface AlgorithmGridSearch<U>
{
    /**
     * Obtains the different recommendation algorithms to execute in a grid.
     *
     * @param grid The grid for the algorithm.
     *
     * @return a map containing the different recommendations.
     */
    Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid);

    /**
     * Obtains the different recommendation algorithms to execute in a grid.
     *
     * @param grid     The grid for the algorithm.
     * @param graph    The training graph.
     * @param prefData The preference training data.
     *
     * @return a map containing the different recommendations.
     */
    Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData);
}
