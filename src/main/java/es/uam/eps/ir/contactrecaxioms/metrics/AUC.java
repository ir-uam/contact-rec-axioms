/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.metrics;

import es.uam.eps.ir.contactrecaxioms.utils.Pair;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;

import java.util.List;

/**
 * Class for computing the area under the ROC curve (AUC) metric.
 */
public class AUC
{
    /**
     * Computes the area under the ROC curve.
     * @param values a list containing pairs containing a) the estimated value by the method, b) if the element is a true
     *               positive (true) or a false positive (false).
     * @return the list of points of the ROC curve.
     */
    public double compute(List<Tuple2oo<Double,Boolean>> values)
    {
        ROCCurve curveGen = new ROCCurve();
        List<Pair<Double>> roc = curveGen.compute(values);
        int size = roc.size();
        double auc = 0.0;
        for(int i = 1; i < size; ++i)
        {
            double currentX = roc.get(i).v2();
            double previousX = roc.get(i-1).v2();

            double currentY = roc.get(i).v1();
            double previousY = roc.get(i-1).v1();

            auc += (currentX - previousX)*(currentY + previousY)/2.0;
        }
        return auc;
    }
}
