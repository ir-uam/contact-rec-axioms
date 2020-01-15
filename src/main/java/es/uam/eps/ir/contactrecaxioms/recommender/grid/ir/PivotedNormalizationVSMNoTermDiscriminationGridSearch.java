package es.uam.eps.ir.contactrecaxioms.recommender.grid.ir;


import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.Grid;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.RecommendationAlgorithmFunction;
import es.uam.eps.ir.contactrecaxioms.recommender.ir.PivotedNormalizationVSMNoTermDiscrimination;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PivotedNormalizationVSMNoTermDiscriminationGridSearch<U> implements AlgorithmGridSearch<U>
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
    private String PIVOTEDNOTD;

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
                    recs.put(PIVOTEDNOTD + "_" + uSel + "_" + vSel + "_" + s, () ->
                    {
                        return new PivotedNormalizationVSMNoTermDiscrimination<>(graph, uSel, vSel, s);
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
                    recs.put(PIVOTEDNOTD + "_" + uSel + "_" + vSel + "_" + s, (graph, prefData) ->
                    {
                        return new PivotedNormalizationVSMNoTermDiscrimination<>(graph, uSel, vSel, s);
                    });
                });
            });
        });
        return recs;
    }
}
