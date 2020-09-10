package cc.voox.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import java.util.Map;

public final class GraphQLContextUtil {
    private static ThreadLocal threadLocal = new ThreadLocal<>();

    protected final static void add(Object value) {
        threadLocal.set(value);
    }

    public final static DataFetchingEnvironment get() {
        return (DataFetchingEnvironment) threadLocal.get();
    }

    public final static DataLoader getDataLoader(String key) {
        return isFromGraphQL()? get().getDataLoader(key) : null;
    }

    public final static <X extends IDataLoader> DataLoader getDataLoader(Class<X> dataLoaderClass) {
        return isFromGraphQL()? get().getDataLoader(dataLoaderClass.getSimpleName()) : null;
    }

    public final static <T> T getSource(Class<T> src) {
        return (T) get().getSource();
    }

    public final static Map<String, Object> getArguments() {
        return get().getArguments();
    }

    protected final static void clear() {
        threadLocal.remove();
    }

    public final static boolean isFromGraphQL() {
        return get() != null;
    }
}
