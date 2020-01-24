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
 * Identifiers for the different contact recommendation algorithms available in
 * the library
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmIdentifiers
{
    // IR algorithms
    public final static String BIR = "BIR";
    public final static String BM25 = "BM25";
    public final static String EBM25 = "EBM25";

    public final static String QLJM = "QLJM";
    public final static String QLD = "QLD";
    public final static String QLL = "QLL";

    public final static String PIVOTED = "Pivoted VSM";

    public final static String PL2 = "PL2";
    public final static String DLH = "DLH";
    public final static String DPH = "DPH";
    public final static String DFREE = "DFRee";
    public final static String DFREEKLIM = "DFReeKLIM";

    // Variants of IR algorithms
    public final static String BM25NOTD = "BM25 No Term Discrimination";
    public final static String EBM25NOTD = "EBM25 No Term Discrimination";
    public final static String QLJMNOTD = "QLJM No Term Discrimination";
    public final static String QLDNOTD = "QLD No Term Discrimination";
    public final static String PIVOTEDNOTD = "Pivoted VSM No Term Discrimination";

    public final static String BM25NOLEN = "BM25 No Length Normalization";
    public final static String EBM25NOLEN = "ExtremeBM25 No Length Normalization";
    public final static String QLJMNOLEN = "QLJM No Length Normalization";
    public final static String QLDNOLEN = "QLD No Length Normalization";
    public final static String PIVOTEDNOLEN = "Pivoted VSM No Length Normalization";

    // Friends of friends
    public final static String ADAMIC = "Adamic";
    public final static String JACCARD = "Jaccard";
    public final static String MCN = "MCN";
    public final static String COSINE = "Cosine";

    public final static String POP = "Popularity";
    public final static String RANDOM = "Random";
}
