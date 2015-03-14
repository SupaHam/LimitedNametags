package com.supaham.limitednametags;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketOutputAdapter;

import java.util.Arrays;

public class LimitedNametagAdapter extends PacketAdapter {

  public LimitedNametagAdapter(LimitedNametagsPlugin plugin) {
    super(plugin, Server.SCOREBOARD_TEAM);
    this.plugin = plugin;
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    PacketType type = event.getPacketType();
    event.getNetworkMarker().addOutputHandler(new PacketOutputAdapter(plugin, ListenerPriority.NORMAL) {
      @Override
      public byte[] handle(PacketEvent event, byte[] buffer) {
        System.out.println("Packet sent to " + event.getPlayer().getName() + ": " + Arrays.toString(buffer));
        return buffer;
      }
    });
  }
}
