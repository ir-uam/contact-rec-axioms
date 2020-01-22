package es.uam.eps.ir.contactrecaxioms.recommender.io;

import es.uam.eps.ir.ranksys.core.Recommendation;
import org.ranksys.formats.rec.RecommendationFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class EmptyWriter<U, I> implements RecommendationFormat.Writer<U, I>, RecommendationFormat.Reader<U, I>
{
    private final List<Recommendation<U, I>> recs;

    public EmptyWriter()
    {
        this.recs = new ArrayList<>();
    }

    @Override
    public void write(Recommendation<U, I> recommendation)
    {
        this.recs.add(recommendation);
    }

    @Override
    public void close()
    {

    }

    @Override
    public Stream<Recommendation<U, I>> readAll()
    {
        return recs.stream();
    }
}
