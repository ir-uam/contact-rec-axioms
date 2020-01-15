/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.contactrecaxioms.recommender.grid;

import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.baselines.PopularityGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.baselines.RandomGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.foaf.AdamicAdarGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.foaf.CosineGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.foaf.JaccardGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.foaf.MostCommonNeighborsGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.ir.*;
import es.uam.eps.ir.contactrecaxioms.utils.Tuple2oo;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmIdentifiers.*;

/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class AlgorithmGridSelector<U>
{
    /**
     * Given preference data, obtains recommenders.
     * @param algorithm the name of the algorithm.
     * @param grid the parameter grid for the algorithm.
     * @param graph the training graph.
     * @param prefData the training data
     * @return the suppliers for the different algorithm variants, indexed by name.
     */
    public Map<String, Supplier<Recommender<U,U>>> getRecommenders(String algorithm, Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if(gridsearch != null)
            return gridsearch.grid(grid, graph, prefData);
        return null;
    }
    
    /**
     * Given preference data, obtains a single algorithm.
     * @param algorithm the name of the algorithm.
     * @param params the parameters of the algorithm.
     * @param graph the training graph.
     * @param prefData the training data.
     * @return a tuple containing the name and a supplier of the algorithm.
     */
    public Tuple2oo<String, Supplier<Recommender<U,U>>> getRecommender(String algorithm, Parameters params, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<Recommender<U,U>>> map = getRecommenders(algorithm, grid, graph, prefData);
            if(map == null || map.isEmpty()) return null;
            
            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            Supplier<Recommender<U,U>> supplier = map.get(name);
            return new Tuple2oo<>(name, supplier);
        }
            
        return null;
    }
    
    /**
     * Given preference data and a graph, obtains a set of algorithms.
     * @param algorithm the name of the algorithm.
     * @param configs the different configurations for the algorithm.
     * @param graph the training graph.
     * @param prefData the training data.
     * @return a map containing the suppliers of the algorithms, ordered by name.
     */
    public Map<String, Supplier<Recommender<U,U>>> getRecommenders(String algorithm, Configurations configs, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        AlgorithmGridSearch<U> gridSearch = this.selectGridSearch(algorithm);
        if(gridSearch != null)
        {
            for(Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<Recommender<U,U>>> map = getRecommenders(algorithm, grid, graph, prefData);
                if(map == null || map.isEmpty()) return null;
            
                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);
                Supplier<Recommender<U,U>> supplier = map.get(name);
                recs.put(name, supplier);
            }
        }
        
        return recs;
    }
    
    /**
     * Given preference data, obtains recommenders.
     * @param algorithm the name of the algorithm.
     * @param grid the parameter grid for the algorithm.
     * @return functions for obtaining for the different algorithm variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationAlgorithmFunction<U>> getRecommenders(String algorithm, Grid grid)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return null;
    }
    
    /**
     * Given preference data, obtains recommenders.
     * @param algorithm the name of the algorithm.
     * @param configs configurations for the algorithm.
     * @return functions for obtaining for the different algorithm variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationAlgorithmFunction<U>> getRecommenders(String algorithm, Configurations configs)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        if(gridsearch != null)
        {
            for(Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, RecommendationAlgorithmFunction<U>> map = getRecommenders(algorithm, grid);
                if(map == null || map.isEmpty()) return null;
                
                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);
                
                recs.put(name, map.get(name));
            }
            
            return recs;
        }
            
        return null;
    }
    
    /**
     * Obtains a single algorithm.
     * @param algorithm the name of the algorithm.
     * @param params the parameters of the algorithm.
     * @return a tuple containing the name and a function for obtaining the algorithm.
     */
    public Tuple2oo<String, RecommendationAlgorithmFunction<U>> getRecommender(String algorithm, Parameters params)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, RecommendationAlgorithmFunction<U>> map = getRecommenders(algorithm, grid);
            if(map == null || map.isEmpty()) return null;
            
            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            return new Tuple2oo<>(name, map.get(name));
        }
        return null;
    }
    
    /**
     * Selects a grid search given the name of an algorithm.
     * @param algorithm the name of the algorithm.
     * @return if the algorithm exists, returns its grid search, null otherwise.
     */
    public AlgorithmGridSearch<U> selectGridSearch(String algorithm)
    {
        AlgorithmGridSearch<U> gridsearch;
        switch(algorithm)
        {
            // IR algorithms
            case BIR:
                gridsearch = new BIRGridSearch<>();
                break;
            case BM25:
                gridsearch = new BM25GridSearch<>();
                break;
            case EBM25:
                gridsearch = new EBM25GridSearch<>();
                break;
            case QLJM:
                gridsearch = new QLJMGridSearch<>();
                break;
            case QLD:
                gridsearch = new QLDGridSearch<>();
                break;
            case QLL:
                gridsearch = new QLLGridSearch<>();
                break;
            case PL2:
                gridsearch = new PL2GridSearch<>();
                break;
            case DLH:
                gridsearch = new DLHGridSearch<>();
                break;
            case DPH:
                gridsearch = new DPHGridSearch<>();
                break;
            case DFREE:
                gridsearch = new DFReeGridSearch<>();
                break;
            case DFREEKLIM:
                gridsearch = new DFReeKLIMGridSearch<>();
                break;
            case PIVOTED:
                gridsearch = new PivotedNormalizationVSMGridSearch<>();
                break;

            case BM25NOTD:
                gridsearch = new BM25NoTermDiscriminationGridSearch<>();
                break;
            case EBM25NOTD:
                gridsearch = new EBM25NoTermDiscriminationGridSearch<>();
                break;
            case QLDNOTD:
                gridsearch = new QLDNoTermDiscriminationGridSearch<>();
                break;
            case QLJMNOTD:
                gridsearch = new QLJMNoTermDiscriminationGridSearch<>();
                break;
            case PIVOTEDNOTD:
                gridsearch = new PivotedNormalizationVSMNoTermDiscriminationGridSearch<>();
                break;

            case PIVOTEDNOLEN:
                gridsearch = new PivotedNormalizationVSMNoLengthNormalizationGridSearch<>();
                break;
            case BM25NOLEN:
                gridsearch = new BM25NoLengthNormalizationGridSearch<>();
                break;
            case EBM25NOLEN:
                gridsearch = new EBM25NoLengthNormalizationGridSearch<>();
                break;
            case QLDNOLEN:
                gridsearch = new QLDNoLengthNormalizationGridSearch<>();
                break;
            case QLJMNOLEN:
                gridsearch = new QLJMNoLengthNormalizationGridSearch<>();
                break;

            // Link prediction - degree based algorithms
            case ADAMIC:
                gridsearch = new AdamicAdarGridSearch<>();
                break;
            case JACCARD:
                gridsearch = new JaccardGridSearch<>();
                break;
            case MCN:
                gridsearch = new MostCommonNeighborsGridSearch<>();
                break;
            case COSINE:
                gridsearch = new CosineGridSearch<>();
                break;
                
            // Baseline algorithms
            case RANDOM:
                gridsearch = new RandomGridSearch<>();
                break;
            case POP:
                gridsearch = new PopularityGridSearch<>();
                break;

            // Default behavior
            default:
                gridsearch = null;
        }
        
        return gridsearch;
    }
}
