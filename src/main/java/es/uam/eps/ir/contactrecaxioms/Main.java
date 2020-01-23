/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0.
 *
 */
package es.uam.eps.ir.contactrecaxioms;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Main class for running experiments.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Main
{
    /**
     * Name for the EWC1 program.
     */
    private final static String EWC1 = "ewc1";
    /**
     * Name for the EWC2 program.
     */
    private final static String EWC2 = "ewc2";
    /**
     * Name for the EWC3 program.
     */
    private final static String EWC3 = "ewc3";
    /**
     * Name for the NDC program.
     */
    private final static String NDC = "ndc";
    /**
     * Name for the CLNCS program.
     */
    private final static String CLNCS = "clncs";
    /**
     * Name for the program used for validation
     */
    private final static String VALID = "validation";
    /**
     * Name for the program used for comparing accuracy vs. degree.
     */
    private final static String DEGREE = "degree";

    /**
     * Main method. Executes the main method in the class specified by the first
     * argument with the rest of run time arguments.
     *
     * @param args Arguments to select the class to run and arguments for its main method
     */
    public static void main(String[] args)
    {
        try
        {
            String main = args[0];
            String className;
            int from = 1;
            switch (main)
            {
                case EWC1:
                    className = "es.uam.eps.ir.contactrecaxioms.main.EWC1";
                    break;
                case EWC2:
                    className = "es.uam.eps.ir.contactrecaxioms.main.EWC2";
                    break;
                case EWC3:
                    className = "es.uam.eps.ir.contactrecaxioms.main.EWC3";
                    break;
                case NDC:
                    className = "es.uam.eps.ir.contactrecaxioms.main.NDC";
                    break;
                case CLNCS:
                    className = "es.uam.eps.ir.contactrecaxioms.main.CLNCS";
                    break;
                case DEGREE:
                    className = "es.uam.eps.ir.contactrecaxioms.main.AccuracyVSDegree";
                    break;
                case VALID:
                    className = "es.uam.eps.ir.contactrecaxioms.main.Validation";
                    break;
                default:
                    System.err.println("ERROR: Unknown program.");
                    return;
            }

            String[] executionArgs = Arrays.copyOfRange(args, from, args.length);
            @SuppressWarnings("rawtypes") Class[] argTypes = {executionArgs.getClass()};
            Object[] passedArgs = {executionArgs};
            Class.forName(className).getMethod("main", argTypes).invoke(null, passedArgs);
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
            System.err.println("The run time arguments were not correct");
            ex.printStackTrace();
        }
    }
}
