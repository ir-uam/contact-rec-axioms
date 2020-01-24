/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.main.grid;

/**
 * Basic type identifiers for grids.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BasicTypeIdentifiers
{
    /**
     * Type identifier for integers
     */
    public final static String INTEGER_TYPE = "Integer";
    /**
     * Type identifier for doubles
     */
    public final static String DOUBLE_TYPE = "Double";
    /**
     * Type identifier for booleans
     */
    public final static String BOOLEAN_TYPE = "Boolean";
    /**
     * Type identifier for strings
     */
    public final static String STRING_TYPE = "String";
    /**
     * Type identifier for longs
     */
    public final static String LONG_TYPE = "Long";
    /**
     * Type identifier for edge orientations
     */
    public final static String ORIENTATION_TYPE = "LinkOrientation";
    /**
     * Type identifier for grids
     */
    public final static String GRID_TYPE = "Grid";
    /**
     * Type for recursive definitions
     */
    public final static String RECURSIVE_TYPE = "Recursive";
}
