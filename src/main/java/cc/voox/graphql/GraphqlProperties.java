package cc.voox.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;

@ConfigurationProperties(prefix = "graphql")
public class GraphqlProperties {
    private String schema = "schema.graphql";
    private String scanPath = ClassUtils.getPackageName(getClass());
    private String pageTitle = "GraphQL UI";
    private String mapping = "/app/altair";
    private boolean enabled = true;
    private Endpoint endpoint = new Endpoint();
    private Static STATIC = new Static();
    private Cdn cdn = new Cdn();
    private boolean log = false;
    private boolean openStatistics = false;
    private int maxQueryDepth = 100;

    public int getMaxQueryDepth() {
        return maxQueryDepth;
    }

    public void setMaxQueryDepth(int maxQueryDepth) {
        this.maxQueryDepth = maxQueryDepth;
    }

    public boolean isOpenStatistics() {
        return openStatistics;
    }

    public void setOpenStatistics(boolean openStatistics) {
        this.openStatistics = openStatistics;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Static getSTATIC() {
        return STATIC;
    }

    public void setSTATIC(Static STATIC) {
        this.STATIC = STATIC;
    }

    public Cdn getCdn() {
        return cdn;
    }

    public void setCdn(Cdn cdn) {
        this.cdn = cdn;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public static class Endpoint {
        private String graphql = "/app/graphql";
        private String subscriptions = "/app/graphql/subscriptions";

        public String getGraphql() {
            return graphql;
        }

        public void setGraphql(String graphql) {
            this.graphql = graphql;
        }

        public String getSubscriptions() {
            return subscriptions;
        }

        public void setSubscriptions(String subscriptions) {
            this.subscriptions = subscriptions;
        }
    }


    public static class Static {
        private String basePath = "/webjars/graphql";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }

    public static class Cdn {
        private boolean enabled = false;
        private String version = "2.4.6";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

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
