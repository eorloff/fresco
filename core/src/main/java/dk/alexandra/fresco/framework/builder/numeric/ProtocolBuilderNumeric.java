package dk.alexandra.fresco.framework.builder.numeric;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderImpl;
import dk.alexandra.fresco.lib.compare.MiscOIntGenerators;
import dk.alexandra.fresco.lib.field.integer.BasicNumeric;
import java.util.function.Consumer;

/**
 * Central class for building protocols that are based on numeric protocols.
 */
public class ProtocolBuilderNumeric extends
    ProtocolBuilderImpl<ProtocolBuilderNumeric> {

  private final BuilderFactoryNumeric factory;
  private BasicNumeric basicNumeric;
  private NumericBuilder numericBuilder;
  private ComparisonBuilder comparison;
  private AdvancedNumericBuilder advancedNumeric;
  private DebugBuilder utilityBuilder;

  ProtocolBuilderNumeric(BuilderFactoryNumeric factory, boolean parallel) {
    super(factory, parallel);
    this.factory = factory;
    this.basicNumeric = factory.getBasicNumericFactory();
  }

  public BasicNumeric getBasicNumeric() {
    return basicNumeric;
  }

  /**
   * Creates a root for this builder - should be applied when construcing protocol producers from an
   * {@link Application}.
   *
   * @param factory the protocol factory to get native protocols and composite builders from
   * @param consumer the root of the protocol producer
   * @return a sequential protocol builder that can create the protocol producer
   * @deprecated - protocol suite can do this themselves
   */
  @Deprecated
  public static ProtocolBuilderNumeric createApplicationRoot(BuilderFactoryNumeric factory,
      Consumer<ProtocolBuilderNumeric> consumer) {
    ProtocolBuilderNumeric builder = new ProtocolBuilderNumeric(factory, false);
    builder.addConsumer(consumer, () -> new ProtocolBuilderNumeric(factory, false));
    return builder;
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
    if (comparison == null) {
      comparison = factory.createComparison(this);
    }
    return comparison;
  }

  public AdvancedNumericBuilder advancedNumeric() {
    if (advancedNumeric == null) {
      advancedNumeric = factory.createAdvancedNumeric(this);
    }
    return advancedNumeric;
  }

  public DebugBuilder debug() {
    if (utilityBuilder == null) {
      utilityBuilder = factory.createDebugBuilder(this);
    }
    return utilityBuilder;
  }

  public MiscOIntGenerators getBigIntegerHelper() {
    return factory.getBigIntegerHelper();
  }
}