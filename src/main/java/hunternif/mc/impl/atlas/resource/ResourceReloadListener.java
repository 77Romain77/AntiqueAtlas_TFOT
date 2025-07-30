package hunternif.mc.impl.atlas.resource;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public interface ResourceReloadListener<T> extends PreparableReloadListener {

    CompletableFuture<T> load(ResourceManager manager, ProfilerFiller profiler, Executor executor);

    CompletableFuture<Void> apply(T data,
                                  ResourceManager manager,
                                  ProfilerFiller profiler,
                                  Executor executor);


    default CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer,
                                           ResourceManager manager,
                                           ProfilerFiller prepareProfiler,
                                           ProfilerFiller applyProfiler,
                                           Executor prepareExecutor,
                                           Executor applyExecutor) {
        CompletableFuture<T> load = load(manager, prepareProfiler, prepareExecutor);

        return load.thenCompose(synchronizer::wait)
                .thenCompose(t -> apply(t, manager, applyProfiler, applyExecutor));
    }

    default String getName() {
        return id().toString();
    }

    ResourceLocation id();

    Collection<ResourceLocation> getDependencies();

}