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

package com.illusivesoulworks.spectrelib.network;

import com.illusivesoulworks.spectrelib.config.SpectreConfigNetwork;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ConfigSyncPacket(FriendlyByteBuf data) {

  public void encoder(FriendlyByteBuf buffer) {
    buffer.writeBytes(data);
  }

  public static ConfigSyncPacket decoder(FriendlyByteBuf buffer) {
    return new ConfigSyncPacket(buffer);
  }

  public static void messageConsumer(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
    packet.data.retain();
    ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
        () -> () -> SpectreConfigNetwork.handleConfigSync(packet.data)));
    ctx.get().setPacketHandled(true);
  }
}
