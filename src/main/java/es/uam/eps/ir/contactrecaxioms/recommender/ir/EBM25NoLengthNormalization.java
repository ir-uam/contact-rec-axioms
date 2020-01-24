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
 * Adaptation of the Extreme BM-25 Information Retrieval Algorithm for user recommendation. Uses a term-based implementation.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EBM25NoLengthNormalization<U> extends BM25<U>
{

    /**
     * Constructor.
     *
     * @param graph Graph
     * @param uSel  Selection of the neighbours of the target user
     * @param vSel  Selection of the neighbours of the candidate user
     * @param dlSel Selection of the neighbours for the document length
     */
    public EBM25NoLengthNormalization(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel)
    {
        super(graph, uSel, vSel, dlSel, 0.0, Double.POSITIVE_INFINITY);
    }
}
