package ai.grakn.graql.internal.gremlin.fragment;

import ai.grakn.graql.Graql;
import ai.grakn.graql.Var;
import ai.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import static ai.grakn.util.Schema.VertexProperty.INSTANCE_TYPE_ID;
import static ai.grakn.util.Schema.EdgeLabel.PLAYS;
import static ai.grakn.util.Schema.EdgeLabel.SUB;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InPlaysFragmentTest {

    private final Var start = Graql.var();
    private final Var end = Graql.var();
    private final InPlaysFragment fragment = new InPlaysFragment(null, start, end, false);

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyTraversalFollowsSubsDownwards() {
        GraphTraversal<Vertex, Vertex> traversal = __.V();
        fragment.applyTraversal(traversal, null);

        // Make sure we traverse plays and downwards subs once
        assertThat(traversal, is(__.V()
                .in(PLAYS.getLabel())
                .union(__.<Vertex>not(__.has(INSTANCE_TYPE_ID.name())).not(__.hasLabel(Schema.BaseType.SHARD.name())), __.repeat(__.in(SUB.getLabel())).emit()).unfold()
        ));
    }
}
