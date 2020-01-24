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
 * Adaptation of the pivoted normalization vector space model (VSM), without length normalization.
 * <p>
 * Singhal, A., Choi, J., Hindle, D., Lewis, D.D., Pereira, F.C.N.: AT&T at TREC-7.In: Proceedings of the 7th Text REtrieval Conference (TREC 1998). pp. 186–198.NIST (1998)
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PivotedNormalizationVSMNoLengthNormalization<U> extends PivotedNormalizationVSM<U>
{
    /**
     * Constructor.
     *
     * @param graph the training network.
     * @param uSel  neighborhood orientation selected for the target user.
     * @param vSel  neighborhood orientation selected for the candidate user.
     */
    public PivotedNormalizationVSMNoLengthNormalization(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph, uSel, vSel, 0.0);
    }
}
