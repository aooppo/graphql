package cc.voox.graphql;

import org.dataloader.DataLoader;

public interface IDataLoader {
    DataLoader<?, ?> get();
    String key();
}
