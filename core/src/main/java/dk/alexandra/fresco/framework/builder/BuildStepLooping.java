package dk.alexandra.fresco.framework.builder;

import dk.alexandra.fresco.framework.BuilderFactory;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.ProtocolCollection;
import dk.alexandra.fresco.framework.ProtocolProducer;
import java.util.function.Predicate;

class BuildStepLooping<BuilderT extends ProtocolBuilder<BuilderT>,
    BuilderParallelT extends ProtocolBuilder<BuilderT>,
    InputT> extends BuildStep<BuilderT, BuilderT, BuilderParallelT, InputT, InputT> {

  private final Predicate<InputT> predicate;
  private final FrescoLambda<InputT, BuilderT, InputT> function;

  BuildStepLooping(Predicate<InputT> predicate, FrescoLambda<InputT, BuilderT, InputT> function) {
    super(function);
    this.predicate = predicate;
    this.function = function;
  }

  @Override
  protected ProtocolProducer createProducer(InputT input,
      BuilderFactory<BuilderT, BuilderParallelT> factory) {
    LoopProtocolProducer<BuilderT, BuilderParallelT, InputT> loopProtocolProducer =
        new LoopProtocolProducer<>(factory, input, predicate, function, next);
    output = loopProtocolProducer;
    return loopProtocolProducer;
  }

  @Override
  protected BuilderT createBuilder(BuilderFactory<BuilderT, BuilderParallelT> factory) {
    throw new IllegalStateException("Should not be called");
  }

  private static class LoopProtocolProducer<
      BuilderT extends ProtocolBuilder<BuilderT>,
      BuilderParallelT extends ProtocolBuilder<BuilderT>,
      InputT
      > implements ProtocolProducer, Computation<InputT> {

    private final BuilderFactory<BuilderT, BuilderParallelT> factory;
    private boolean isDone;
    private boolean doneWithOwn;
    private Computation<InputT> currentResult;
    private ProtocolProducer currentProducer;
    private Predicate<InputT> predicate;
    private FrescoLambda<InputT, BuilderT, InputT> function;
    private BuildStep<?, BuilderT, BuilderParallelT, ?, InputT> next;

    LoopProtocolProducer(
        BuilderFactory<BuilderT, BuilderParallelT> factory,
        InputT input,
        Predicate<InputT> predicate,
        FrescoLambda<InputT, BuilderT, InputT> function,
        BuildStep<?, BuilderT, BuilderParallelT, ?, InputT> next) {
      this.factory = factory;
      this.predicate = predicate;
      this.function = function;
      this.next = next;
      isDone = false;
      doneWithOwn = false;
      currentProducer = null;
      currentResult = () -> input;
      updateToNextProducer(input);
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
          BuilderT builder = factory.createSequential();
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
