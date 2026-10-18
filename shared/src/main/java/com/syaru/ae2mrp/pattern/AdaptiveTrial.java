package com.syaru.ae2mrp.pattern;

import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.inv.ChildCraftingSimulationState;

/** An isolated successful native attempt, applied to its parent at most once. */
public record AdaptiveTrial(
    CraftingTreeProcess process,
    ChildCraftingSimulationState inventory,
    long target,
    long produced) {}
