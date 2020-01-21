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

import es.uam.eps.ir.contactrecaxioms.utils.Pair;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a list, finds the receiver operating characteristic (ROC) curve.
 */
public class ROCCurve
{
    /**
     * Computes the ROC curve.
     *
     * @param values a list containing pairs containing a) the estimated value by the method, b) if the element is a true
     *               positive (true) or a false positive (false).
     *
     * @return the list of points of the ROC curve.
     */
    public List<Pair<Double>> compute(List<Tuple2oo<Double, Boolean>> values)
    {
        List<Pair<Double>> curve = new ArrayList<>();
        values.sort((x, y) -> Double.compare(y.v1(), x.v1()));
        long size = values.size();
        long numPos = values.stream().filter(Tuple2oo::v2).count();
        long numNeg = size - numPos;

        double lastValue = Double.NaN;
        double numRels = 0;
        double numNotRels = 0;

        curve.add(new Pair<>(0.0, 0.0));
        for (Tuple2oo<Double, Boolean> value : values)
        {
            double val = value.v1();
            if (val != lastValue)
            {
                if (!Double.isNaN(lastValue))
                {
                    Pair<Double> pair = new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0) / (numNeg + 0.0));
                    curve.add(pair);
                }
                else
                {
                    lastValue = val;
                }
            }

            if (value.v2())
            {
                numRels++;
            }
            else
            {
                numNotRels++;
            }
        }

        curve.add(new Pair<>((numRels + 0.0) / (numPos + 0.0), (numNotRels + 0.0) / (numNeg + 0.0)));
        return curve;
    }
}
