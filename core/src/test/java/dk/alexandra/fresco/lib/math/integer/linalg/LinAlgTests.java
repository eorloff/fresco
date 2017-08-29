/*******************************************************************************
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL, and Bouncy Castle.
 * Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.math.integer.linalg;

import dk.alexandra.fresco.framework.BuilderFactory;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.TestApplication;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.BuilderFactoryNumeric;
import dk.alexandra.fresco.framework.builder.numeric.NumericBuilder;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.ResourcePoolCreator;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;

public class LinAlgTests {


  public static class TestInnerProductClosed extends TestThreadFactory {

    @Override
    public TestThread next(TestThreadConfiguration conf) {

      return new TestThread() {
        private final List<Integer> data1 = Arrays.asList(200, 144, 99, 211);
        private final List<Integer> data2 = Arrays.asList(87, 14, 11, 21);
        private Computation<BigInteger> result;
        private final BigInteger expected = new BigInteger("24936");

        @Override
        public void test() throws Exception {
          TestApplication app = new TestApplication() {

            @Override
            public ProtocolProducer prepareApplication(BuilderFactory factoryProducer) {
              return ProtocolBuilderNumeric
                  .createApplicationRoot((BuilderFactoryNumeric) factoryProducer, (builder) -> {
                NumericBuilder sIntFactory = builder.numeric();

                List<Computation<SInt>> input1 = data1.stream().map(BigInteger::valueOf)
                    .map(sIntFactory::known).collect(Collectors.toList());
                // LinkedList<Computation<SInt>> bleh = new LinkedList(input1);
                System.out.println(input1);
                List<Computation<SInt>> input2 = data2.stream().map(BigInteger::valueOf)
                    .map(sIntFactory::known).collect(Collectors.toList());
                    Computation<SInt> min =
                        builder.seq(new InnerProduct(new LinkedList(input1), input2));

                result = builder.numeric().open(min);
              }).build();
            }
          };

          secureComputationEngine.runApplication(app,
              ResourcePoolCreator.createResourcePool(conf.sceConf));

          Assert.assertEquals(expected, result.out());
        }
      };
    }
  }

  public static class TestInnerProductOpen extends TestThreadFactory {

    @Override
    public TestThread next(TestThreadConfiguration conf) {

      return new TestThread() {
        private final List<Integer> data1 = Arrays.asList(200, 144, 99, 211);
        private final List<BigInteger> data2 = Arrays.asList(BigInteger.valueOf(87),
            BigInteger.valueOf(14), BigInteger.valueOf(11), BigInteger.valueOf(21));
        private Computation<BigInteger> result;
        private final BigInteger expected = new BigInteger("24936");

        @Override
        public void test() throws Exception {
          TestApplication app = new TestApplication() {

            @Override
            public ProtocolProducer prepareApplication(BuilderFactory factoryProducer) {
              return ProtocolBuilderNumeric
                  .createApplicationRoot((BuilderFactoryNumeric) factoryProducer, (builder) -> {
                NumericBuilder sIntFactory = builder.numeric();

                List<Computation<SInt>> input1 = data1.stream().map(BigInteger::valueOf)
                    .map(sIntFactory::known).collect(Collectors.toList());
                    Computation<SInt> min =
                        builder.seq(new InnerProductOpen(data2, input1));

                result = builder.numeric().open(min);
              }).build();
            }
          };

          secureComputationEngine.runApplication(app,
              ResourcePoolCreator.createResourcePool(conf.sceConf));

          Assert.assertEquals(expected, result.out());
        }
      };
    }
  }
}