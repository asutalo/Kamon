/* ===================================================
 * Copyright © 2013 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================== */
package kamon.scala.instrumentation

import kamon.Kamon
import kamon.Kamon.buildSpan
import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class FutureInstrumentationSpec extends WordSpec with ScalaFutures with Matchers
    with PatienceConfiguration with OptionValues {

  "a Future created when instrumentation is active" should {
    "capture the active span available when created" which {
      "must be available when executing the future's body" in {
        val testSpan = buildSpan("future-body").start().addBaggage("propagate", "in-future-body")
        val baggageInBody = Kamon.withActiveSpan(testSpan) {
          Future(Kamon.activeSpan().getBaggage("propagate"))
        }

        whenReady(baggageInBody)(baggageValue ⇒ baggageValue should be(Some("in-future-body")))
      }

      "must be available when executing callbacks on the future" in {
        val testSpan = buildSpan("future-transformations").start().addBaggage("propagate", "in-future-transformations")
        val baggageAfterTransformations = Kamon.withActiveSpan(testSpan) {
            Future("Hello Kamon!")
              // The active span is expected to be available during all intermediate processing.
              .map(_.length)
              .flatMap(len ⇒ Future(len.toString))
              .map(_ ⇒ Kamon.activeSpan().getBaggage("propagate"))
          }

        whenReady(baggageAfterTransformations)(baggageValue ⇒ baggageValue should equal(Some("in-future-transformations")))
      }
    }
  }
}

