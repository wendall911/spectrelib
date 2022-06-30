/*
 * Copyright (C) 2022 Illusive Soulworks
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; you
 * may only use version 2.1 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <https://www.gnu.org/licenses/>.
 */

package com.illusivesoulworks.spectrelib.config;

import static com.illusivesoulworks.spectrelib.config.SpectreConfigLoader.CONFIG;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.illusivesoulworks.spectrelib.SpectreConstants;
import com.illusivesoulworks.spectrelib.platform.Services;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpectreConfigTracker {

  public static final SpectreConfigTracker INSTANCE = new SpectreConfigTracker();

  private final ConcurrentHashMap<String, SpectreConfig> files = new ConcurrentHashMap<>();
  private final EnumMap<SpectreConfig.Type, Set<SpectreConfig>> configsByType =
      new EnumMap<>(SpectreConfig.Type.class);
  private final ConcurrentHashMap<String, Map<SpectreConfig.Type, Set<SpectreConfig>>>
      configsByMod = new ConcurrentHashMap<>();

  private SpectreConfigTracker() {

    for (SpectreConfig.Type value : SpectreConfig.Type.values()) {
      this.configsByType.put(value, Collections.synchronizedSet(new LinkedHashSet<>()));
    }
  }

  void track(final SpectreConfig config) {

    if (this.files.containsKey(config.getFileName())) {
      SpectreConstants.LOG.error(CONFIG, "Detected config file conflict {} between {} and {}",
          config.getFileName(), this.files.get(config.getFileName()).getModId(), config.getModId());
      throw new RuntimeException("Conflicting config files");
    }
    this.files.put(config.getFileName(), config);
    this.configsByType.get(config.getType()).add(config);
    this.configsByMod.computeIfAbsent(config.getModId(),
        (k) -> new EnumMap<>(SpectreConfig.Type.class)).computeIfAbsent(config.getType(),
        (k) -> Collections.synchronizedSet(new LinkedHashSet<>())).add(config);
    SpectreConstants.LOG.debug(CONFIG, "Config file {} for {} added to tracking",
        config.getFileName(), config.getModId());
  }

  void loadDefaultConfigs() {
    SpectreConstants.LOG.debug(CONFIG, "Loading default configs");
    this.files.values().forEach(config -> {
      SpectreConstants.LOG.trace(CONFIG, "Loading config file type {} at {} for {}",
          config.getType(),
          config.getFileName(), config.getModId());
      boolean alreadyExists =
          Files.exists(Services.CONFIG.getDefaultConfigPath().resolve(config.getFileName()));
      final CommentedFileConfig configData =
          read(Services.CONFIG.getDefaultConfigPath()).apply(config);
      SpectreConfig.InstanceType type = SpectreConfig.InstanceType.DEFAULT;
      config.setConfigData(type, configData, !alreadyExists);
      config.fireLoad();
      config.save(type);
    });
  }

  void loadConfigs(SpectreConfig.InstanceType type, Path configDir) {
    SpectreConstants.LOG.debug(CONFIG, "Loading {} configs from {}", type.id(), configDir);
    this.files.values().forEach(config -> {
      Path configPath = configDir.resolve(config.getFileName());

      if (Files.exists(configPath)) {
        SpectreConstants.LOG.trace(CONFIG, "Loading config file type {} at {} from {} for {}",
            config.getType(), config.getFileName(), configDir, config.getModId());
        final CommentedFileConfig configData = read(configDir).apply(config);
        config.setConfigData(type, configData, false);
        config.fireLoad();
        config.save(type);
      }
    });
  }

  void unloadServerConfigs() {
    SpectreConstants.LOG.debug(CONFIG, "Unloading server configs");
    this.files.values().forEach(config -> {
      SpectreConstants.LOG.trace(CONFIG, "Unloading config file type {} at {}", config.getType(),
          config.getFileName());
      config.save(SpectreConfig.InstanceType.SERVER);
      config.clearServerConfigData();
    });
  }

  Function<SpectreConfig, CommentedFileConfig> read(Path basePath) {
    return (config) -> {
      final Path configPath = basePath.resolve(config.getFileName());
      final CommentedFileConfig configData = CommentedFileConfig.builder(configPath).sync().
          preserveInsertionOrder().
          autosave().
          onFileNotFound(this::setupConfigFile).
          writingMode(WritingMode.REPLACE).
          build();
      SpectreConstants.LOG.debug(CONFIG, "Built TOML config for {}", configPath);
      try {
        configData.load();
      } catch (ParsingException e) {
        throw new ConfigLoadingException(config, e);
      }
      SpectreConstants.LOG.debug(CONFIG, "Loaded TOML config file {}", configPath);
      return configData;
    };
  }

  private boolean setupConfigFile(final Path path, final ConfigFormat<?> conf) throws IOException {
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    conf.initEmptyFile(path);
    return true;
  }

  public void acceptSyncedConfigs(String fileName, byte[] data) {
    SpectreConfig configFile = this.files.get(fileName);

    if (configFile != null) {
      final CommentedConfig configData =
          TomlFormat.instance().createParser().parse(new ByteArrayInputStream(data));
      configFile.setConfigData(SpectreConfig.InstanceType.SERVER, configData, false);
      configFile.fireReload();
    }
  }

  public Map<String, byte[]> getConfigSync() {
    return this.configsByType.get(SpectreConfig.Type.SERVER).stream().collect(
        Collectors.toMap(SpectreConfig::getFileName, file -> {
          try {
            return Files.readAllBytes(file.getFullPath());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }));
  }

  private static class ConfigLoadingException extends RuntimeException {

    public ConfigLoadingException(SpectreConfig config, Exception cause) {
      super("Failed loading config file " + config.getFileName() + " of type " + config.getType() +
          " for " + config.getModId(), cause);
    }
  }
}
