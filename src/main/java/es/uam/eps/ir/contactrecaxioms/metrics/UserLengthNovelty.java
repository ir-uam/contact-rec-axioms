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

/**
 * For each user, computes its degree.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public class UserLengthNovelty<U> extends ItemNovelty<U, U>
{
    /**
     * The novelty model.
     */
    private UserItemNoveltyModel<U, U> nov;

    /**
     * Constructor.
     * @param recommenderData the preference data.
     * @param vSel  neighborhood selection.
     */
    public UserLengthNovelty(PreferenceData<U, U> recommenderData, EdgeOrientation vSel)
    {
        this.nov = new UserLengthNoveltyModel(recommenderData, vSel);
    }

    /**
     * Novelty model for the average degree.
     */
    private final class UserLengthNoveltyModel implements UserItemNoveltyModel<U,U>
    {
        /**
         * Map to store the degree of the different users.
         */
        private final Object2DoubleMap<U> itemNovelty;

        /**
         * Constructor.
         * @param recommenderData preference data.
         * @param vSel neighborhood selection.
         */
        public UserLengthNoveltyModel(PreferenceData<U, U> recommenderData, EdgeOrientation vSel)
        {
            this.itemNovelty = new Object2DoubleOpenHashMap<>();
            this.itemNovelty.defaultReturnValue(0.0);

            if (vSel == EdgeOrientation.IN) // If we select the incoming neighborhood
            {
                recommenderData.getItemsWithPreferences().forEach(i -> itemNovelty.put(i, recommenderData.getItemPreferences(i).mapToDouble(IdPref::v2).sum()));
            }
            else if (vSel == EdgeOrientation.OUT) // If we select the outgoing neighborhood
            {
                recommenderData.getUsersWithPreferences().forEach(i -> itemNovelty.put(i, recommenderData.getUserPreferences(i).mapToDouble(IdPref::v2).sum()));
            }
            else // If we select the undirected neighborhood (vSel == EdgeOrientation.UND)
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
