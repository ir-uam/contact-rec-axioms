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

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.novdiv.itemnovelty.ItemNovelty;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class UserLengthNovelty<U> extends ItemNovelty<U, U>
{
    private UserItemNoveltyModel<U, U> nov;

    public UserLengthNovelty(PreferenceData<U, U> recommenderData, EdgeOrientation vSel)
    {
        this.nov = new UserLengthNoveltyModel(recommenderData, vSel);
    }

    private final class UserLengthNoveltyModel implements UserItemNoveltyModel<U,U>
    {
        private final Object2DoubleMap<U> itemNovelty;

        public UserLengthNoveltyModel(PreferenceData<U, U> recommenderData, EdgeOrientation vSel)
        {
            this.itemNovelty = new Object2DoubleOpenHashMap<>();
            this.itemNovelty.defaultReturnValue(0.0);

            if (vSel == EdgeOrientation.IN)
            {
                recommenderData.getItemsWithPreferences().forEach(i -> itemNovelty.put(i, recommenderData.getItemPreferences(i).mapToDouble(IdPref::v2).sum()));
            }
            else if (vSel == EdgeOrientation.OUT)
            {
                recommenderData.getUsersWithPreferences().forEach(i -> itemNovelty.put(i, recommenderData.getUserPreferences(i).mapToDouble(IdPref::v2).sum()));
            }
            else
            {
                recommenderData.getItemsWithPreferences().forEach(i -> itemNovelty.put(i, recommenderData.getItemPreferences(i).mapToDouble(IdPref::v2).sum()));
                recommenderData.getUsersWithPreferences().forEach(i -> ((Object2DoubleOpenHashMap<U>) itemNovelty).addTo(i, recommenderData.getUserPreferences(i).mapToDouble(IdPref::v2).sum()));
            }
        }

        @Override
        public double novelty(U i)
        {
            return itemNovelty.getDouble(i);
        }
    }

    @Override
    protected UserItemNoveltyModel<U, U> get(U u)
    {
        return nov;
    }
}
