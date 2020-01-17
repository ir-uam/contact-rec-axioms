package es.uam.eps.ir.contactrecaxioms.recommender.grid.ir;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.Grid;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.RecommendationAlgorithmFunction;
import es.uam.eps.ir.contactrecaxioms.recommender.ir.PivotedNormalizationVSM;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmIdentifiers.PIVOTED;

public class PivotedNormalizationVSMGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the term that graduates the importance of the document lenght.
     */
    private static final String S  = "s";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String VSEL = "vSel";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        List<Double> ss = grid.getDoubleValues(S);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        ss.stream().forEach(s ->
        {
            uSels.stream().forEach(uSel ->
            {
                vSels.stream().forEach(vSel ->
                {
                    recs.put(PIVOTED + "_" + uSel + "_" + vSel + "_" + s, () ->
                    {
                        return new PivotedNormalizationVSM<>(graph, uSel, vSel, s);
                    });
                });
            });
        });
        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<Double> ss = grid.getDoubleValues(S);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        ss.stream().forEach(s ->
        {
            uSels.stream().forEach(uSel ->
            {
                vSels.stream().forEach(vSel ->
                {
                    recs.put(PIVOTED + "_" + uSel + "_" + vSel + "_" + s, (graph, prefData) ->
                    {
                        return new PivotedNormalizationVSM<>(graph, uSel, vSel, s);
                    });
                });
            });
        });
        return recs;
    }
}