/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.metrics;

import es.uam.eps.ir.ranksys.metrics.rank.RankingDiscountModel;
import es.uam.eps.ir.ranksys.metrics.rel.RelevanceModel;
import es.uam.eps.ir.ranksys.novdiv.itemnovelty.metrics.ItemNoveltyMetric;

public class UserLength<U> extends ItemNoveltyMetric<U, U>
{
    /**
     * Constructor.
     *
     * @param cutoff   maximum size of the recommendation list that is evaluated
     * @param novelty  novelty model
     * @param relModel relevance model
     * @param disc     ranking discount model
     */
    public UserLength(int cutoff, UserLengthNovelty<U> novelty, RelevanceModel<U, U> relModel, RankingDiscountModel disc)
    {
        super(cutoff, novelty, relModel, disc);
    }
}
