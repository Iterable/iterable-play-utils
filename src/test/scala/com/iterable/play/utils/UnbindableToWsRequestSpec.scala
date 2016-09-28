package com.iterable.play.utils

import org.scalatest.SpecLike
import org.specs2.mock.Mockito

case class User(name: String, age: Int, email: String, favoriteBands: Seq[String]) extends UnbindableToWsRequest[User]
object User {
    implicit val mapping = CaseClassMapping.mapping[User]
}

class UnbindableToWsRequestSpec extends SpecLike with Mockito {

  object `UnbindableToWsRequest ` {
    def `should be able to unbind a case class which has a Mapping` {
      val thing = Bar(
        firstOne = Some(List(17, 19)),
        secondOne = "lulz",
        third = Some(Foo(
          a = "it's an b!",
          omg = Some(List(
            Baz(pls = "and even the omg", work = Some(666)),
            Baz(pls = "what happens", work = Some(777))
          ))
        )),
        fourth = Some(1231)
      )
      val expectedUnbound = Map(
        "firstOne[0]" -> List(17.toString),
        "firstOne[1]" -> List(19.toString),
        "secondOne" -> List("lulz"),
        "third.a" -> List("it's an b!"),
        "third.omg[0].pls" -> List("and even the omg"),
        "third.omg[0].work" -> List(666.toString),
        "third.omg[1].pls" -> List("what happens"),
        "third.omg[1].work" -> List(777.toString),
        "fourth" -> List(1231.toString)
      )
      val res = thing.unbindToWsRequest
      assert(res == expectedUnbound)
    }

    def `example from README works`: Unit = {
      val user = User(name = "ilya", age = 9001, email = "ilya at iterable dot com", favoriteBands = Seq("Judas Priest", "Accept"))
      val expectedUnbound = Map(
        "name" -> Seq("ilya"),
        "age" -> Seq(9001.toString),
        "email" -> Seq("ilya at iterable dot com"),
        "favoriteBands[0]" -> Seq("Judas Priest"),
        "favoriteBands[1]" -> Seq("Accept")
      )
      val res = user.unbindToWsRequest
      assert(res == expectedUnbound)
    }
  }
}