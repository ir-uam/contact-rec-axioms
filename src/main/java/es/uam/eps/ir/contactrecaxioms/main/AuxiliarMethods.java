package es.uam.eps.ir.contactrecaxioms.main;

import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Class containing auxiliar methods for the Main functions.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AuxiliarMethods
{
    /**
     * Computes a recommendation and evaluates it using nDCG metric.
     * @param output Route of the file in which to store the recommendation.
     * @param recommender The recommender to apply.
     * @param runner The recommender runner
     * @param metric The metric.
     * @return the value of the metric.
     * @throws IOException if something fails during the writing / reading of the recommendation file.
     */
    public static double computeAndEvaluate(String output, Recommender<Long,Long> recommender, RecommenderRunner<Long, Long> runner, SystemMetric<Long, Long> metric) throws IOException
    {
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(lp,lp);
        RecommendationFormat.Writer<Long, Long> writer;
        RecommendationFormat.Reader<Long, Long> reader;

        metric.reset();

        writer = format.getWriter(output);
        runner.run(recommender, writer);
        writer.close();

        reader = format.getReader(output);
        reader.readAll().forEach(r -> metric.add(r));
        return metric.evaluate();
    }

    /**
     * Given two maps with the same keys, generates a new file that prints the nDCG values for both.
     * @param output The output file.
     * @param first the first map.
     * @param second the second map.
     * @param firstId identifier for the first map.
     * @param secondId identifier for the second map.
     * @param maxLength maximum length of the recommendation.
     */
    public static void printFile(String output, Map<String, Double> first, Map<String, Double> second, String firstId, String secondId, int maxLength)
    {
        List<Tuple2<String, Double>> list = new ArrayList<>();
        int numVariants = 0;
        for(String variant : first.keySet())
        {
            double val = first.get(variant) - second.get(variant);
            list.add(new Tuple2<>(variant, val));
            ++numVariants;
        }

        list.sort((o1, o2) ->
        {
            int val = Double.compare(o1.v2, o2.v2);
            if (val == 0)
                return o1.v1.compareTo(o2.v1);
            return val;
        });

        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output))))
        {
            bw.write("Variant\t%\tnDCG@"+maxLength+"("+firstId+")\tnDCG@"+maxLength+"("+secondId+")\tDifference");
            int i = 0;
            for(Tuple2<String, Double> tuple : list)
            {
                ++i;
                bw.write("\n" + tuple.v1 + "\t" + i/(numVariants+0.0) + "\t" + first.get(tuple.v1) + "\t" + second.get(tuple.v1) + "\t" + tuple.v2);
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the output file");
        }
    }
}
