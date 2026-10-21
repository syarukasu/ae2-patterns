package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.encoder.EncoderState;
import com.syaru.ae2mrp.encoder.TerminalLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface EncoderHost
    extends appeng.api.util.IConfigurableObject,
        appeng.helpers.IPatternTerminalMenuHost,
        appeng.api.networking.security.IActionHost {
  EncoderState editor();

  TerminalLogic terminal();

  Level hostLevel();

  BlockPos position();

  boolean present();

  default int partSide() {
    return -1;
  }
}
