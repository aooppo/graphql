package cc.voox.graphql;

import org.dataloader.DataLoader;
import org.dataloader.Try;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface IDataLoader<K, V> {
    default DataLoader<K, V> get() {
        return DataLoader.newDataLoader(keys -> CompletableFuture.supplyAsync(() -> handle(keys)));
    }

    default DataLoader<K, V> getTry() {
        return DataLoader.newDataLoaderWithTry(keys -> CompletableFuture.supplyAsync(() -> handleTry(keys)));
    }

    default List<Try<V>> handleTry(List<K> keys) {
        return keys.stream().map(k -> handleTry(k)).collect(Collectors.toList());
    }

    default  Try<V> handleTry(K k) {
        return Try.tryCall(() -> handle(k));
    }

    List<V> handle(List<K> keys);
    V handle(K key);

    default boolean useTryMode() {
        return false;
    }

}
