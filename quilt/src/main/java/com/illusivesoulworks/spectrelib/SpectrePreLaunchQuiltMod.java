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

package com.illusivesoulworks.spectrelib;

import com.illusivesoulworks.spectrelib.config.SpectreConfigEvents;
import com.illusivesoulworks.spectrelib.config.SpectreConfigInitializer;
import net.fabricmc.api.EnvType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;

public class SpectrePreLaunchQuiltMod implements PreLaunchEntrypoint {

  @Override
  public void onPreLaunch(ModContainer mod) {

    if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.SERVER) {
      EntrypointUtils.invoke("spectrelib", SpectreConfigInitializer.class,
          SpectreConfigInitializer::onInitializeConfig);
      SpectreConfigEvents.onLoadDefaultAndLocal();
    }
  }
}
