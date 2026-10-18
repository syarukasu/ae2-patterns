package com.syaru.ae2mrp.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Optional, shape-checked adapter for the installed native VM; no bundled VM classes. */
public final class AcoVmAccess {
  private AcoVmAccess() {}

  private static Class<?> type(String name) throws ClassNotFoundException {
    return Class.forName(name, false, AcoVmAccess.class.getClassLoader());
  }

  private static Method method(Class<?> owner, String name, Class<?>... args)
      throws ReflectiveOperationException {
    var method = owner.getDeclaredMethod(name, args);
    method.setAccessible(true);
    return method;
  }

  private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
    var field = owner.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private static Constructor<?> constructor(Class<?> owner, Class<?>... args)
      throws ReflectiveOperationException {
    var constructor = owner.getDeclaredConstructor(args);
    constructor.setAccessible(true);
    return constructor;
  }

  private static final class Layout {
    final Class<?> engine, node, process, state, missing;
    final Method execute, run, extract, commit, checkpoint, conflicts, read, times;
    final Field key, processes, parent, ancestor, simulation, contextAccess, root, inputs;
    final Constructor<?> childState, newProcess;

    Layout() throws ReflectiveOperationException {
      engine = type("com.ae2vm.addon.vm.VmInstructionExecution");
      node = type(engine.getName() + "$Node");
      process = type(engine.getName() + "$Process");
      state = type(engine.getName() + "$State");
      missing = type(engine.getName() + "$Missing");
      execute =
          method(
              type("com.ae2vm.addon.vm.CraftingVM"),
              "execute",
              type("com.ae2vm.addon.vm.CraftingBytecode"),
              type("com.ae2vm.addon.vm.VmSimulationState"),
              boolean.class);
      run = method(engine, "run", process, BigInteger.class, state);
      extract = method(state, "extract", AEKey.class, BigInteger.class);
      commit = method(state, "commitTo", state);
      checkpoint = method(engine, "checkpoint");
      conflicts = method(node, "conflicts", IPatternDetails.class);
      read = method(type("com.ae2vm.addon.nativeengine.NativeVmAccess"), "read", Supplier.class);
      times = method(type("com.ae2vm.addon.vm.VmCraftingPlan"), "patternTimes");
      key = field(node, "key");
      processes = field(node, "processes");
      root = field(engine, "root");
      inputs = field(process, "inputs");
      parent = field(process, "parent");
      ancestor = field(node, "ancestor");
      simulation = field(engine, "simulate");
      contextAccess = field(type("com.ae2vm.addon.nativeengine.NativeVmPlanningContext"), "access");
      childState = constructor(state, engine, state);
      newProcess = constructor(process, engine, IPatternDetails.class, node);
    }
  }

  private static final class Holder {
    static final Layout VALUE;
    static final RuntimeException FAILURE;

    static {
      Layout value = null;
      RuntimeException failure = null;
      try {
        value = new Layout();
      } catch (ReflectiveOperationException incompatible) {
        failure =
            new IllegalStateException("COMPOSITE_ACO_VM_UNAVAILABLE_OR_INCOMPATIBLE", incompatible);
      }
      VALUE = value;
      FAILURE = failure;
    }
  }

  private static Layout layout() {
    if (Holder.FAILURE != null) throw Holder.FAILURE;
    return Holder.VALUE;
  }

  public static void requireAvailable() {
    layout();
  }

  private static RuntimeException propagate(ReflectiveOperationException failure) {
    if (failure instanceof InvocationTargetException invoked) {
      if (invoked.getCause() instanceof Error fatal) throw fatal;
      if (invoked.getCause() instanceof RuntimeException runtime) return runtime;
    }
    return new IllegalStateException("COMPOSITE_ACO_VM_ADAPTER_FAILURE", failure);
  }

  private static Object invoke(Method method, Object owner, Object... args) {
    try {
      return method.invoke(owner, args);
    } catch (ReflectiveOperationException failure) {
      throw propagate(failure);
    }
  }

  private static Object get(Field field, Object owner) {
    try {
      return field.get(owner);
    } catch (IllegalAccessException failure) {
      throw propagate(failure);
    }
  }

  private static void set(Field field, Object owner, Object value) {
    try {
      field.set(owner, value);
    } catch (IllegalAccessException failure) {
      throw propagate(failure);
    }
  }

  private static Object create(Constructor<?> constructor, Object... args) {
    try {
      return constructor.newInstance(args);
    } catch (ReflectiveOperationException failure) {
      throw propagate(failure);
    }
  }

  public static Object execute(Object vm, Object code, Object inventory, boolean less) {
    return invoke(layout().execute, vm, code, inventory, less);
  }

  public static void validate(Object access, Runnable action) {
    invoke(
        layout().read,
        get(layout().contextAccess, access),
        (Supplier<Object>)
            () -> {
              action.run();
              return null;
            });
  }

  @SuppressWarnings("unchecked")
  public static Map<IPatternDetails, BigInteger> times(Object plan) {
    return (Map<IPatternDetails, BigInteger>) invoke(layout().times, plan);
  }

  public static AEKey key(Object node) {
    return (AEKey) get(layout().key, node);
  }

  /** Control expansion replaces the candidate list, so its pre-expansion flag is stale. */
  public static boolean multiplePaths(Object engine) {
    var pending = new ArrayDeque<Object>();
    pending.add(get(layout().root, engine));
    while (!pending.isEmpty()) {
      checkpoint(engine);
      var node = pending.removeLast();
      var processes = (List<?>) get(layout().processes, node);
      if (processes == null) continue;
      if (processes.size() > 1) return true;
      for (var process : processes) {
        var inputs = (List<?>) get(layout().inputs, process);
        if (inputs != null) pending.addAll(inputs);
      }
    }
    return false;
  }

  public static Object parent(Object process) {
    return get(layout().parent, process);
  }

  public static void processes(Object node, List<Object> processes) {
    set(layout().processes, node, processes);
  }

  public static void checkpoint(Object engine) {
    invoke(layout().checkpoint, engine);
  }

  public static boolean simulation(Object engine) {
    return (Boolean) get(layout().simulation, engine);
  }

  public static void simulation(Object engine, boolean value) {
    set(layout().simulation, engine, value);
  }

  public static Object process(Object engine, IPatternDetails pattern, Object node) {
    var ancestor = get(layout().ancestor, node);
    if (ancestor != null && (Boolean) invoke(layout().conflicts, ancestor, pattern))
      throw new IllegalStateException("COMPOSITE_RECURSION: " + key(node));
    return create(layout().newProcess, engine, pattern, node);
  }

  public static Object child(Object engine, Object state) {
    return create(layout().childState, engine, state);
  }

  public static void commit(Object child, Object state) {
    invoke(layout().commit, child, state);
  }

  public static BigInteger extract(Object state, AEKey key, BigInteger amount) {
    return (BigInteger) invoke(layout().extract, state, key, amount);
  }

  public static boolean run(
      Object engine, Object process, BigInteger times, Object state, boolean probe) {
    boolean previous = simulation(engine);
    if (probe) simulation(engine, false);
    try {
      invoke(layout().run, engine, process, times, state);
      return true;
    } catch (RuntimeException failure) {
      if (probe && layout().missing.isInstance(failure)) return false;
      throw failure;
    } finally {
      simulation(engine, previous);
    }
  }
}
