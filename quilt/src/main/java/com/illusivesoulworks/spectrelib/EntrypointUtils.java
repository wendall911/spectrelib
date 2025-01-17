package com.illusivesoulworks.spectrelib;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.entrypoint.EntrypointContainer;

public class EntrypointUtils {

  public static <T> void invoke(String name, Class<T> type, BiConsumer<T, ModContainer> invoker) {
    invokeContainer(name, type,
        container -> invoker.accept(container.getEntrypoint(), container.getProvider()));
  }

  public static <T> void invokeContainer(String name, Class<T> type,
                                         Consumer<EntrypointContainer<T>> invoker) {
    RuntimeException exception = null;
    Collection<EntrypointContainer<T>> entrypoints =
        QuiltLoader.getEntrypointContainers(name, type);

    SpectreConstants.LOG.debug("Iterating over entrypoint '{}'", name);

    for (EntrypointContainer<T> container : entrypoints) {
      try {
        invoker.accept(container);
      } catch (Throwable t) {
        exception = gatherExceptions(t,
            exception,
            exc -> new RuntimeException(String.format(
                "Could not execute entrypoint stage '%s' due to errors, provided by '%s'!",
                name, container.getProvider().metadata().id()),
                exc));
      }
    }

    if (exception != null) {
      throw exception;
    }
  }

  public static <T extends Throwable> T gatherExceptions(Throwable exc, T prev,
                                                         Function<Throwable, T> mainExcFactory) {
    exc = unwrap(exc);

    if (prev == null) {
      return mainExcFactory.apply(exc);
    } else if (exc != prev) {

      for (Throwable t : prev.getSuppressed()) {

        if (exc.equals(t)) {
          return prev;
        }
      }
      prev.addSuppressed(exc);
    }
    return prev;
  }

  private static Throwable unwrap(Throwable exc) {

    if (exc instanceof UncheckedIOException || exc instanceof ExecutionException ||
        exc instanceof CompletionException) {
      Throwable ret = exc.getCause();

      if (ret != null) {
        return unwrap(ret);
      }
    }
    return exc;
  }
}
