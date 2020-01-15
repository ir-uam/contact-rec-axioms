/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.ir;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;

/**
 * Adaptation of the BM-25 Information Retrieval Algorithm for user recommendation, without length normalization.
 * 
 * Sparck Jones, K., Walker, S., Roberton S.E. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments. 
 * Information Processing and Management 36. February 2000, pp. 779-808 (part 1), pp. 809-840 (part 2).
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class BM25NoLengthNormalization<U> extends BM25<U>
{
    /**
     * Constructor.
     * @param graph Graph
     * @param uSel Selection of the neighbours of the target user
     * @param vSel Selection of the neighbours of the candidate user
     * @param dlSel Selection of the neighbours for the document length
     * @param k parameter of the algorithm.
     */
    public BM25NoLengthNormalization(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double k)
    {
        super(graph, uSel, vSel, dlSel, 0.0, k);
    }
}
