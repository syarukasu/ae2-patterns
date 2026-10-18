# Adaptive allocation

ADAPTIVE is an explicit encoding mode, not a fallback for a failed STRICT plan.
Only mounted, enabled, positive-weight source patterns participate. Their order,
identity and percentage configuration do not change. An absent/changed pattern,
overflow, cancellation or internal failure is not treated as material shortage.

## Native accounting

1. Apply the ordinary largest-remainder output shares first.
2. For each share, try the original native CraftingTreeProcess in a fresh
   ChildCraftingSimulationState. On CraftBranchFailure, binary-search a smaller
   whole number of recipe batches. Each probe has its own process tree.
3. Apply only the successful child's diff, exactly once, to the common parent
   inventory. Retain only committed processes for native tree-byte accounting.
4. Redistribute outstanding output units across surviving routes, using their
   relative configured weights and stable registration-order ties.
5. A route that cannot meet its new share is removed from that redistribution
   round. Every incomplete round removes a route; no quantity-linear outer loop.
6. Real rounding excess can cover an outstanding shortfall. Unused excess remains
   in native accounting and is returned by the ordinary CPU.
7. If the order still cannot be filled, fail the real attempt. During AE2's final
   Missing simulation only, plan the remainder with the preferred shares and let
   AE2 report the actual missing ingredients for that rounded plan.

Probe state belongs to one CraftingCalculation and its existing worker. A
try/finally restores it, including cancellation. The node's native isSimulation
decision is false inside probes, so discarded attempts cannot add hypothetical
Missing ingredients to the job. The job's own simulation flag is not changed.
No probe touches real ME storage, submits a job or creates output items.

## Meaning And Limits

This is deterministic, preferred-share-first water filling, not a global integer
optimizer. Already feasible preferred portions are not undone to search every
possible alternative use of a scarce shared ingredient. A Missing result
describes this policy's native rounded plan, not proof that no other allocation
could work. Upstream AE2 dependency/candidate exploration still has its own cost.

The shared allocator uses BigInteger and bounded route rounds. The current
native adapter remains checked-long only; wide native plans explicitly fail.
STRICT arithmetic and execution are unchanged.

## Saved Mode And UI

The existing schema-1 distributionMode field now accepts STRICT and ADAPTIVE.
Unknown names fail explicitly. Existing STRICT items retain their interpretation.
Block state saves DistributionMode alongside its existing draft settings.
Server menu commands 121/122 select STRICT/ADAPTIVE; DataSlot 18 synchronizes it.
Mouse and keyboard selection update the server-owned draft before encoding.
The header displays the primary output item with its tooltip, and encoding uses
the native AE2 ARROW_RIGHT icon button rather than a text command.

## Relation To ME Requester

The installed ME Requester 1.1.5 delegates a completed ICraftingPlan to
ICraftingService.submitJob. The normal AE2 request API identifies an output and
amount, not a source-pattern percentage. Accordingly this addon resolves routes
inside ONE native plan and ONE common simulation inventory; it does not start
independent requester jobs for each route. Calculation scheduling, CPU
submission, real extraction/insertion, progress and execution remain AE2-owned.

## Evidence

Six pure-core tests cover unchanged preferred shares, weighted shortfall transfer,
shared capacity, disabled routes, 10^64 targets, bounded callback count,
cancellation and invalid-result propagation.

Five additional native fixtures cover feasible preferred shares, real four-item
completion after redistribution, rollback after consuming the first of two input
types, exact rounded Missing, and CRAFT_LESS. Encoding/re-edit and block persistence
fixtures also exercise the mode. These supplement the twelve existing fixtures.
