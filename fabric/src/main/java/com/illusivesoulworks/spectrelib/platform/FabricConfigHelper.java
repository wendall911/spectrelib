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

package com.illusivesoulworks.spectrelib.platform;

import com.illusivesoulworks.spectrelib.SpectreConstants;
import com.illusivesoulworks.spectrelib.mixin.SpectreLibMixinLevelResource;
import com.illusivesoulworks.spectrelib.platform.services.IConfigHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class FabricConfigHelper implements IConfigHelper {

  private static LevelResource SERVERCONFIG = null;

  @Override
  public Path getDefaultConfigPath() {
    return FabricLoader.getInstance().getGameDir().resolve("defaultconfigs");
  }

  @Override
  public Path getLocalConfigPath() {
    return FabricLoader.getInstance().getConfigDir();
  }

  @Override
  public Path getServerConfigPath(MinecraftServer server) {

    if (SERVERCONFIG == null) {
      SERVERCONFIG = SpectreLibMixinLevelResource.spectrelib$create("serverconfig");
    }
    final Path serverConfig = server.getWorldPath(SERVERCONFIG);

    if (!Files.isDirectory(serverConfig)) {
      try {
        Files.createDirectory(serverConfig);
      } catch (IOException e) {
        SpectreConstants.LOG.error("Could not create serverconfig directory!");
        e.printStackTrace();
      }
    }
    return serverConfig;
  }
}
