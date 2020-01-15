package es.uam.eps.ir.contactrecaxioms.recommender.grid.ir;

import es.uam.eps.ir.contactrecaxioms.graph.edges.EdgeOrientation;
import es.uam.eps.ir.contactrecaxioms.graph.fast.FastGraph;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmGridSearch;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.Grid;
import es.uam.eps.ir.contactrecaxioms.recommender.grid.RecommendationAlgorithmFunction;
import es.uam.eps.ir.contactrecaxioms.recommender.ir.PivotedNormalizationVSMNoLengthNormalization;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.contactrecaxioms.recommender.grid.AlgorithmIdentifiers.PIVOTEDNOLEN;

public class PivotedNormalizationVSMNoLengthNormalizationGridSearch<U> implements AlgorithmGridSearch<U>
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
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.stream().forEach(uSel ->
        {
            vSels.stream().forEach(vSel ->
            {
                recs.put(PIVOTEDNOLEN + "_" + uSel + "_" + vSel, () ->
                {
                    return new PivotedNormalizationVSMNoLengthNormalization<>(graph, uSel, vSel);
                });
            });
        });

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);


        uSels.stream().forEach(uSel ->
        {
            vSels.stream().forEach(vSel ->
            {
                recs.put(PIVOTEDNOLEN + "_" + uSel + "_" + vSel, (graph, prefData) ->
                {
                    return new PivotedNormalizationVSMNoLengthNormalization<>(graph, uSel, vSel);
                });
            });
        });

        return recs;
    }
}
