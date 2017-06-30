package dk.alexandra.fresco.framework.builder;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.NativeProtocol;
import dk.alexandra.fresco.framework.ProtocolCollection;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.lib.compare.MiscOIntGenerators;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.ParallelProtocolProducer;
import dk.alexandra.fresco.lib.helper.ProtocolProducerCollection;
import dk.alexandra.fresco.lib.helper.SequentialProtocolProducer;
import dk.alexandra.fresco.lib.helper.SingleProtocolProducer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Central class for building protocols that are based on numeric protocols.
 */
public abstract class ProtocolBuilderNumeric implements ProtocolBuilder {

  private BasicNumericFactory basicNumericFactory;
  private List<ProtocolBuilderNumeric.ProtocolEntity> protocols;
  public BuilderFactoryNumeric factory;
  private NumericBuilder numericBuilder;

  private ProtocolBuilderNumeric(BuilderFactoryNumeric factory) {
    this.factory = factory;
    this.basicNumericFactory = factory.getBasicNumericFactory();
    this.protocols = new LinkedList<>();
  }

  public BasicNumericFactory getBasicNumericFactory() {
    return basicNumericFactory;
  }

  /**
   * Creates a root for this builder - should be applied when construcing
   * protocol producers from an {@link Application}.
   *
   * @param factory the protocol factory to get native protocols and composite builders from
   * @param consumer the root of the protocol producer
   * @return a sequential protocol builder that can create the protocol producer
   */
  public static SequentialNumericBuilder createApplicationRoot(
      BuilderFactoryNumeric factory,
      Consumer<SequentialNumericBuilder> consumer) {
    SequentialNumericBuilder builder = new SequentialNumericBuilder(
        factory);
    builder
        .addConsumer(consumer, () -> new SequentialNumericBuilder(factory));
    return builder;
  }

  public static SequentialNumericBuilder createApplicationRoot(
      BuilderFactoryNumeric builderFactoryNumeric) {
    return new SequentialNumericBuilder(builderFactoryNumeric);
  }

  /**
   * Re-creates this builder based on this basicNumericFactory but with a nested parallel protocol
   * producer inserted into the original protocol producer.
   *
   * @param function of the protocol producer - will be lazy evaluated
   */
  public <R> Computation<R> createParallelSub(ParallelComputationBuilder<R> function) {
    DelayedComputation<R> result = new DelayedComputation<>();
    addConsumer((builder) -> result.setComputation(function.build(builder)),
        () -> new ParallelNumericBuilder(factory));
    return result;
  }

  /**
   * Re-creates this builder based on this basicNumericFactory but with a nested sequential protocol
   * producer inserted into the original protocol producer.
   *
   * @param function creation of the protocol producer - will be lazy evaluated
   */
  public <R> Computation<R> createSequentialSub(ComputationBuilder<R> function) {
    DelayedComputation<R> result = new DelayedComputation<>();
    addConsumer((builder) -> result.setComputation(function.build(builder)),
        () -> new SequentialNumericBuilder(factory));
    return result;
  }

  /**
   * Creates another protocol builder based on the supplied consumer.
   * This method re-creates the builder based on a sequential protocol producer inserted into this
   * original protocol producer as a child.
   *
   * @param consumer lazy creation of the protocol producer
   */
  public <T extends Consumer<SequentialNumericBuilder>> void createIteration(
      T consumer) {
    addConsumer(consumer, () -> new SequentialNumericBuilder(factory));
  }

  <T extends ProtocolBuilderNumeric> void addConsumer(Consumer<T> consumer,
      Supplier<T> supplier) {
    ProtocolBuilderNumeric.ProtocolEntity protocolEntity = createAndAppend();
    protocolEntity.child = new LazyProtocolProducer(() -> {
      T builder = supplier.get();
      consumer.accept(builder);
      return builder.build();
    });
  }

  ProtocolBuilderNumeric.ProtocolEntity createAndAppend() {
    ProtocolBuilderNumeric.ProtocolEntity protocolEntity = new ProtocolBuilderNumeric.ProtocolEntity();
    if (protocols == null) {
      throw new IllegalStateException("Cannot build this twice, it has all ready been constructed");
    }
    protocols.add(protocolEntity);
    return protocolEntity;
  }


  /**
   * Appends a concrete, native protocol to the list of producers - useful for the native protocol
   * factories that needs to be builders.
   *
   * @param nativeProtocol the native protocol to add
   * @param <T> the type of the native protocol - pass-through buildable object
   * @return the original native protocol.
   */
  public <T extends NativeProtocol> T append(T nativeProtocol) {
    ProtocolBuilderNumeric.ProtocolEntity protocolEntity = createAndAppend();
    protocolEntity.protocolProducer = SingleProtocolProducer.wrap(nativeProtocol);
    return nativeProtocol;
  }

  // This will go away and should not be used - users should recode their applications to
  // use closures
  @Deprecated
  public <T extends ProtocolProducer> T append(T protocolProducer) {
    ProtocolBuilderNumeric.ProtocolEntity protocolEntity = createAndAppend();
    protocolEntity.protocolProducer = protocolProducer;
    return protocolProducer;
  }

  /**
   * Building the actual protocol producer. Implementors decide which producer to create.
   *
   * @return the protocol producer that has been build
   */
  public abstract ProtocolProducer build();

  void addEntities(ProtocolProducerCollection producerCollection) {
    for (ProtocolBuilderNumeric.ProtocolEntity protocolEntity : protocols) {
      if (protocolEntity.computation != null) {
        producerCollection.append(protocolEntity.computation);
      } else if (protocolEntity.protocolProducer != null) {
        producerCollection.append(protocolEntity.protocolProducer);
      } else {
        producerCollection.append(protocolEntity.child);
      }
    }
    protocols = null;
  }

  /**
   * Creates a numeric builder for this instance - i.e. this intended producer.
   *
   * @return the numeric builder.
   */
  public NumericBuilder numeric() {
    if (numericBuilder == null) {
      numericBuilder = factory.createNumericBuilder(this);
    }
    return numericBuilder;
  }

  /**
   * Creates a comparison builder for this instance - i.e. this intended producer.
   *
   * @return the comparison builder.
   */
  public ComparisonBuilder comparison() {
    return factory.createComparisonBuilder(this);
  }

  public AdvancedNumericBuilder createAdvancedNumericBuilder() {
    return factory.createAdvancedNumericBuilder(this);
  }

  public MiscOIntGenerators getBigIntegerHelper() {
    return factory.getBigIntegerHelper();
  }

  private static class ProtocolEntity {

    Computation<?> computation;
    ProtocolProducer protocolProducer;
    LazyProtocolProducer child;
  }

  /**
   * A specific instance of the protocol builder that produces a sequential producer.
   */
  public static class SequentialNumericBuilder extends ProtocolBuilderNumeric {

    private SequentialNumericBuilder(BuilderFactoryNumeric factory) {
      super(factory);
    }

    @Override
    public ProtocolProducer build() {
      SequentialProtocolProducer sequentialProtocolProducer = new SequentialProtocolProducer();
      addEntities(sequentialProtocolProducer);
      return sequentialProtocolProducer;
    }


    public <R> BuildStep<SequentialNumericBuilder, R, Void> seq(ComputationBuilder<R> function) {
      BuildStep<SequentialNumericBuilder, R, Void> builder =
          new ProtocolBuilderNumeric.BuildStepSequential<>(
              (ignored, inner) -> function.build(inner));
      ProtocolBuilderNumeric.ProtocolEntity protocolEntity = createAndAppend();
      protocolEntity.child = new LazyProtocolProducer(
          () -> builder.createProducer(null, factory)
      );
      return builder;
    }

    public <R> BuildStep<ParallelNumericBuilder, R, Void> par(ParallelComputationBuilder<R> f) {
      BuildStep<ParallelNumericBuilder, R, Void> builder =
          new ProtocolBuilderNumeric.BuildStepParallel<>((ignored, inner) -> f.build(inner));
      ProtocolBuilderNumeric.ProtocolEntity protocolEntity = createAndAppend();
      protocolEntity.child = new LazyProtocolProducer(
          () -> builder.createProducer(null, factory)
      );
      return builder;
    }
  }

  /**
   * A specific instance of the protocol builder that produces a parallel producer.
   */
  public static class ParallelNumericBuilder extends ProtocolBuilderNumeric {

    private ParallelNumericBuilder(BuilderFactoryNumeric factory) {
      super(factory);
    }

    @Override
    public ProtocolProducer build() {
      ParallelProtocolProducer parallelProtocolProducer = new ParallelProtocolProducer();
      addEntities(parallelProtocolProducer);
      return parallelProtocolProducer;
    }
  }

  public static abstract class BuildStep<BuilderT extends ProtocolBuilderNumeric, OutputT, InputT>
      implements Computation<OutputT> {

    protected final BiFunction<InputT, BuilderT, Computation<OutputT>> function;

    protected ProtocolBuilderNumeric.BuildStep<?, ?, OutputT> next;
    protected Computation<OutputT> output;

    private BuildStep(
        BiFunction<InputT, BuilderT, Computation<OutputT>> function) {
      this.function = function;
    }

    public <NextOutputT> ProtocolBuilderNumeric.BuildStep<SequentialNumericBuilder, NextOutputT, OutputT> seq(
        FrescoLambda<OutputT, NextOutputT> function) {
      ProtocolBuilderNumeric.BuildStep<SequentialNumericBuilder, NextOutputT, OutputT> localChild =
          new ProtocolBuilderNumeric.BuildStepSequential<>(function);
      this.next = localChild;
      return localChild;
    }

    public <NextOutputT> ProtocolBuilderNumeric.BuildStep<ParallelNumericBuilder, NextOutputT, OutputT> par(
        FrescoLambdaParallel<OutputT, NextOutputT> function) {
      ProtocolBuilderNumeric.BuildStep<ParallelNumericBuilder, NextOutputT, OutputT> localChild =
          new ProtocolBuilderNumeric.BuildStepParallel<>(function);
      this.next = localChild;
      return localChild;
    }

    public ProtocolBuilderNumeric.BuildStep<SequentialNumericBuilder, OutputT, OutputT> whileLoop(
        Predicate<OutputT> test,
        FrescoLambda<OutputT, OutputT> function) {
      ProtocolBuilderNumeric.BuildStepLooping<OutputT> localChild = new ProtocolBuilderNumeric.BuildStepLooping<>(
          test, function);
      this.next = localChild;
      return localChild;
    }

    public <FirstOutputT, SecondOutputT>
    ProtocolBuilderNumeric.BuildStep<ParallelNumericBuilder, Pair<FirstOutputT, SecondOutputT>, OutputT> par(
        FrescoLambda<OutputT, FirstOutputT> firstFunction,
        FrescoLambda<OutputT, SecondOutputT> secondFunction) {
      ProtocolBuilderNumeric.BuildStep<ParallelNumericBuilder, Pair<FirstOutputT, SecondOutputT>, OutputT> localChild =
          new ProtocolBuilderNumeric.BuildStepParallel<>(
              (OutputT output1, ParallelNumericBuilder builder) -> {
                Computation<FirstOutputT> firstOutput =
                    builder.createSequentialSub(
                        seq -> firstFunction.apply(output1, seq));
                Computation<SecondOutputT> secondOutput =
                    builder.createSequentialSub(
                        seq -> secondFunction.apply(output1, seq));
                return () -> new Pair<>(firstOutput.out(), secondOutput.out());
              }
          );
      this.next = localChild;
      return localChild;
    }

    public OutputT out() {
      if (output != null) {
        return output.out();
      }
      return null;
    }

    protected ProtocolProducer createProducer(
        InputT input,
        BuilderFactoryNumeric factory) {

      BuilderT builder = createBuilder(factory);
      Computation<OutputT> output = function.apply(input, builder);
      if (next != null) {
        return
            new SequentialProtocolProducer(
                builder.build(),
                new LazyProtocolProducer(() ->
                    next.createProducer(output.out(), factory)
                )
            );
      } else {
        this.output = output;
        return builder.build();
      }
    }

    protected abstract BuilderT createBuilder(BuilderFactoryNumeric factory);
  }

  private static class BuildStepParallel<OutputT, InputT>
      extends
      ProtocolBuilderNumeric.BuildStep<ParallelNumericBuilder, OutputT, InputT> {

    private BuildStepParallel(FrescoLambdaParallel<InputT, OutputT> function) {
      super(function);
    }

    @Override
    protected ParallelNumericBuilder createBuilder(
        BuilderFactoryNumeric factory) {
      return new ParallelNumericBuilder(factory);
    }
  }


  private static class BuildStepLooping<InputT> extends
      ProtocolBuilderNumeric.BuildStep<SequentialNumericBuilder, InputT, InputT> {

    private final Predicate<InputT> predicate;
    private final FrescoLambda<InputT, InputT> function;

    private BuildStepLooping(
        Predicate<InputT> predicate,
        FrescoLambda<InputT, InputT> function) {
      super(function);
      this.predicate = predicate;
      this.function = function;
    }

    @Override
    protected ProtocolProducer createProducer(InputT input, BuilderFactoryNumeric factory) {
      LoopProtocolProducer loopProtocolProducer = new LoopProtocolProducer<>(factory, input,
          predicate, function, next);
      output = loopProtocolProducer;
      return loopProtocolProducer;
    }

    @Override
    protected SequentialNumericBuilder createBuilder(
        BuilderFactoryNumeric factory) {
      throw new IllegalStateException("Should not be called");
    }

    private static class LoopProtocolProducer<InputT> implements ProtocolProducer,
        Computation<InputT> {

      private final BuilderFactoryNumeric factory;
      private boolean isDone;
      private boolean doneWithOwn;
      private Computation<InputT> currentResult;
      private ProtocolProducer currentProducer;
      private Predicate<InputT> predicate;
      private FrescoLambda<InputT, InputT> function;
      private BuildStep<?, ?, InputT> next;

      LoopProtocolProducer(BuilderFactoryNumeric factory,
          InputT input,
          Predicate<InputT> predicate,
          FrescoLambda<InputT, InputT> function,
          BuildStep<?, ?, InputT> next) {
        this.factory = factory;
        this.predicate = predicate;
        this.function = function;
        this.next = next;
        isDone = false;
        doneWithOwn = false;
        currentProducer = null;
        updateToNextProducer(input);
        currentResult = () -> input;
      }

      @Override
      public void getNextProtocols(ProtocolCollection protocolCollection) {
        currentProducer.getNextProtocols(protocolCollection);
      }

      private void next() {
        while (!isDone && !currentProducer.hasNextProtocols()) {
          updateToNextProducer(currentResult.out());
        }
      }

      private void updateToNextProducer(InputT input) {
        if (doneWithOwn) {
          isDone = true;
        } else {
          if (predicate.test(input)) {
            SequentialNumericBuilder builder = new SequentialNumericBuilder(
                factory);
            currentResult = function.apply(input, builder);
            currentProducer = builder.build();
          } else {
            doneWithOwn = true;
            if (next != null) {
              currentProducer = next.createProducer(input, factory);
              next = null;
            }
          }
        }
      }

      @Override
      public boolean hasNextProtocols() {
        next();
        return !isDone;
      }

      @Override
      public InputT out() {
        return currentResult.out();
      }
    }
  }

  private static class BuildStepSequential<OutputT, InputT>
      extends
      ProtocolBuilderNumeric.BuildStep<SequentialNumericBuilder, OutputT, InputT> {

    private BuildStepSequential(FrescoLambda<InputT, OutputT> function) {
      super(function);
    }

    @Override
    protected SequentialNumericBuilder createBuilder(
        BuilderFactoryNumeric factory) {
      return new SequentialNumericBuilder(factory);
    }

  }
}
