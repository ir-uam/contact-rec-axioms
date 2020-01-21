/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;

import java.util.stream.Stream;

/**
 * Class that represents both user and item indexes for a graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig
 */
public interface GraphIndex<U> extends FastItemIndex<U>, FastUserIndex<U>
{
    @Override
    default int item2iidx(U i)
    {
        return this.user2uidx(i);
    }

    @Override
    default U iidx2item(int i)
    {
        return this.uidx2user(i);
    }

    @Override
    default boolean containsItem(U i)
    {
        return this.containsUser(i);
    }

    @Override
    default int numItems()
    {
        return this.numUsers();
    }

    @Override
    default Stream<U> getAllItems()
    {
        return this.getAllUsers();
    }


}
