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

import com.illusivesoulworks.spectrelib.SpectreConstants;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public class SpectreConfigNetwork {

  public static List<FriendlyByteBuf> getConfigSync() {
    Map<String, byte[]> configData = SpectreConfigTracker.INSTANCE.getConfigSync();

    if (configData.isEmpty()) {
      return new ArrayList<>();
    }
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    buf.writeMap(configData, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeByteArray);
    return PacketSplitter.split(buf);
  }

  private static final List<FriendlyByteBuf> receivedBuffers = new ArrayList<>();

  public static void handleConfigSync(FriendlyByteBuf buffer) {

    if (!Minecraft.getInstance().isLocalServer()) {

      if (buffer.isReadable()) {
        buffer.retain();
        receivedBuffers.add(buffer);
        return;
      }
      FriendlyByteBuf full = new FriendlyByteBuf(
          Unpooled.wrappedBuffer(receivedBuffers.toArray(new FriendlyByteBuf[0])));
      Map<String, byte[]> configs =
          full.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readByteArray);
      configs.forEach(SpectreConfigNetwork::acceptSyncedConfigs);
      full.release();

      for (FriendlyByteBuf receivedBuffer : receivedBuffers) {
        receivedBuffer.release();
      }
      receivedBuffers.clear();
    }
  }

  public static void acceptSyncedConfigs(String fileName, byte[] data) {

    if (!Minecraft.getInstance().isLocalServer()) {
      SpectreConfigTracker.INSTANCE.acceptSyncedConfigs(fileName, data);
    }
  }

  private static class PacketSplitter {

    private static final int PART_SIZE = 1048576;

    public static List<FriendlyByteBuf> split(FriendlyByteBuf buf) {
      List<FriendlyByteBuf> result = new ArrayList<>();

      while (buf.isReadable(PART_SIZE)) {
        result.add(new FriendlyByteBuf(buf.readBytes(PART_SIZE)));
      }

      if (buf.isReadable()) {
        result.add(buf);
      }
      return result;
    }
  }
}
