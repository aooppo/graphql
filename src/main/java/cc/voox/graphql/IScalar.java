package cc.voox.graphql;

import graphql.schema.Coercing;

public interface IScalar extends Coercing {
    String getName();
    String getDescription();
}
