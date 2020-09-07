package cc.voox.graphql;

import graphql.schema.idl.SchemaDirectiveWiring;

public interface IDirective extends SchemaDirectiveWiring {
    String getName();
}