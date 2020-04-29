package cc.voox.graphql;

import org.springframework.context.annotation.Import;

public class GraphqlProperties {
    private String schema = "schema.graphql";
    private String scanPath;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getScanPath() {
        return scanPath;
    }

    public void setScanPath(String scanPath) {
        this.scanPath = scanPath;
    }
}
