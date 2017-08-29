/*
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
package dk.alexandra.fresco.lib.compare;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.network.ResourcePoolCreator;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.ByteArithmetic;
import dk.alexandra.fresco.framework.value.SBool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;

public class CompareTests {

  public static class CompareAndSwapTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderBinary> {

    public CompareAndSwapTest() {}

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderBinary> next(
        TestThreadConfiguration<ResourcePoolT, ProtocolBuilderBinary> conf) {
      return new TestThread<ResourcePoolT, ProtocolBuilderBinary>() {
        @Override
        public void test() throws Exception {
          List<Boolean> rawLeft = Arrays.asList(ByteArithmetic.toBoolean("ee"));
          List<Boolean> rawRight = Arrays.asList(ByteArithmetic.toBoolean("00"));


          Application<List<List<Boolean>>, ProtocolBuilderBinary> app =
              new Application<List<List<Boolean>>, ProtocolBuilderBinary>() {

            @Override
            public Computation<List<List<Boolean>>> prepareApplication(
                ProtocolBuilderBinary producer) {
              return producer.seq(seq -> {
                List<Computation<SBool>> left =
                    rawLeft.stream().map(seq.binary()::known).collect(Collectors.toList());
                List<Computation<SBool>> right =
                    rawRight.stream().map(seq.binary()::known).collect(Collectors.toList());

                Computation<List<List<Computation<SBool>>>> compared =
                    new CompareAndSwap(left, right).buildComputation(seq);
                return compared;
              }).seq((seq, opened) -> {
                List<List<Computation<Boolean>>> result =
                    new ArrayList<List<Computation<Boolean>>>();
                for (List<Computation<SBool>> entry : opened) {
                  result.add(entry.stream().map(Computation::out).map(seq.binary()::open)
                      .collect(Collectors.toList()));
                }

                return () -> result;
              }).seq((seq, opened) -> {
                List<List<Boolean>> result = new ArrayList<List<Boolean>>();
                for (List<Computation<Boolean>> entry : opened) {
                  result.add(entry.stream().map(Computation::out).collect(Collectors.toList()));
                }

                return () -> result;
              });
            }
          };

          List<List<Boolean>> res = secureComputationEngine.runApplication(app,
              ResourcePoolCreator.createResourcePool(conf.sceConf));

          Assert.assertEquals("00", ByteArithmetic.toHex(res.get(0)));
          Assert.assertEquals("ee", ByteArithmetic.toHex(res.get(1)));
        }
      };
    }
  }
}