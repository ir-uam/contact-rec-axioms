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

/**
 * Adaptation of an extreme version of the BM25 algorithm, where the k parameter tends to infinity, without term discrimination.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @see es.uam.eps.ir.contactrecaxioms.recommender.ir.BM25
 */
public class EBM25NoTermDiscrimination<U> extends BM25NoTermDiscrimination<U>
{
    /**
     * Constructor
     *
     * @param graph Graph
     * @param uSel  Selection of the neighbours of the target user
     * @param vSel  Selection of the neighbours of the candidate user
     * @param dlSel Selection of the neighbours for the document length
     * @param b     Tunes the effect of the neighborhood size. Between 0 and 1
     */
    public EBM25NoTermDiscrimination(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double b)
    {
        super(graph, uSel, vSel, dlSel, b, Double.POSITIVE_INFINITY);
    }
}
