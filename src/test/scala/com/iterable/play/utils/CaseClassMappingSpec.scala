package com.iterable.play.utils

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.validation._
import play.api.data.{Form, FormError, Mapping}

case class Baz(pls: String, work: Option[Long])
object Baz {
  implicit val mapping = CaseClassMapping.mapping[Baz]
}

case class Foo(a: String, omg: Option[Seq[Baz]])
object Foo {
  implicit def mapping = CaseClassMapping.mapping[Foo]
}

case class Bar(firstOne: Option[List[Long]], secondOne: String, third: Option[Foo], fourth: Option[Int]) extends UnbindableToWsRequest[Bar]
object Bar {
  implicit val mapping = CaseClassMapping.mapping[Bar]
}

class CaseClassMappingSpec extends WordSpec with MustMatchers with MockitoSugar {
  // TODO - it breaks on this, because
  //   scala.ScalaReflectionException: class Bar2 is an inner class, use reflectClass on an InstanceMirror to obtain its ClassMirror
  case class Bar2(firstOne: Int)
  object Bar2 {
    implicit lazy val mapping = CaseClassMapping.mapping[Bar2]
  }

  "CaseClassMapping" should {
    "not work for inner classes" in {
      val e = intercept[ScalaReflectionException] {
        Bar2.mapping
      }
      e.msg mustBe "class Bar2 is an inner class, use reflectClass on an InstanceMirror to obtain its ClassMirror"
    }

    "work for ints, longs, options of those, and seqs of those" in {
      val request = Map(
        "firstOne[]" -> List(17.toString, 19.toString),
        "secondOne" -> List("lulz"),
        "third.a" -> List("it's an b!"),
        "third.omg[0].pls" -> List("and even the omg"),
        "third.omg[0].work" -> List(666.toString),
        "third.omg[1].pls" -> List("what happens"),
        "third.omg[1].work" -> List(777.toString),
        "fourth" -> List(1231.toString)
      )
      val expectedUnbound = Map(
        "firstOne[0]" -> 17.toString,
        "firstOne[1]" -> 19.toString,
        "secondOne" -> "lulz",
        "third.a" -> "it's an b!",
        "third.omg[0].pls" -> "and even the omg",
        "third.omg[0].work" -> 666.toString,
        "third.omg[1].pls" -> "what happens",
        "third.omg[1].work" -> 777.toString,
        "fourth" -> 1231.toString
      )
      val res = Form(implicitly[Mapping[Bar]]).bindFromRequest(request)
      val expectedBar = Bar(Some(List(17, 19)), "lulz", Some(Foo("it's an b!", Some(Seq(Baz("and even the omg", Some(666)), Baz("what happens", Some(777)))))), Some(1231))
      res.get mustEqual expectedBar
      val (unbound, unboundErrors) = implicitly[Mapping[Bar]].unbindAndValidate(expectedBar)
      unboundErrors mustBe empty
      unbound mustEqual expectedUnbound
    }

    "fail if constraints aren't met" in {
      val request = Map(
        "pls" -> List("foo"),
        "work" -> List(7.toString)
      )
      val mapping = implicitly[Mapping[Baz]]
      val mappingWithConstraint = mapping.verifying {
        Constraint[Baz]("Baz.pls") { baz =>
          Constraints.minLength(5)(baz.pls)
        }
      }
      val res = Form(mappingWithConstraint).bindFromRequest(request)
      val expectedError = FormError("Baz.pls", "error.minLength", Seq(5))
      res.errors.head mustEqual expectedError
      val invalidBaz = Baz("foo", Some(7))
      val (_, unboundErrors) = mappingWithConstraint.unbindAndValidate(invalidBaz)
      unboundErrors.head mustEqual expectedError
      // TODO - should unbound be empty when there are errors? or should it have the unbound data anyways?
    }
  }
}
