# iterable-play-utils

Collection of utilites used by Iterable in Scala Play! projects. Built/tested with SBT 0.13.7, Scala 2.10.4, and Play! 2.2.2

## Automatic Case Class Mappings (via runtime reflection)

See [com.iterable.play.utils.CaseClassMapping](https://github.com/Iterable/iterable-play-utils/blob/master/src/main/scala/com/iterable/play/utils/CaseClassMapping.scala). Uses ***runtime reflection*** to generate form mappings for case classes without all the manual typing. 
 
***Once again, this uses runtime reflection, not compile-time macros.***

***Please note that this suffers from thread safety issues in Scala 2.10, which is why the build includes `parallelExecution in Test := false`
See [the relevant document from Scala](http://docs.scala-lang.org/overviews/reflection/thread-safety.html)***

Suppose you have the following case class:
```scala
case class Foo(
  bar: String,
  baz: Option[Long]
)
```

In order to use forms/mappings, you would normally do:
```scala
val fooForm = Form(
  mapping(
    "bar" -> nonEmptyText,
    "baz" -> optional(longNumber)
  )(Foo.apply)(Foo.unapply)
)
```

This works fine, but it can get very cumbersome if your case classes take many parameters. It's also difficult to keep track of things if you rename the various arguments. `CaseClassMapping` seeks to take care of this by automatically generating a `Mapping[T]` for your case class `T`. In order to use it, any non-standard types that your case class uses must expose an `implicit Mapping[T]` of that type in their companion object; additionally, that mapping must be either a `nullary def` or a `val`. 

For example, to create a form for our example `case class Foo`, you can do:
```scala
val fooForm = Form(CaseClassMapping.mapping[Foo])
```

...and that's it!

More often we use it like this:
```scala
object Foo {
  implicit val mapping = CaseClassMapping.mapping[Foo]
}

// somewhere later where we need a Form[Foo]
val fooForm = Form(implicitly[Mapping[Foo]])
```

If you want to add constraints to your mapping, you can still do so; for example, if we want to make sure that `Foo.bar` is at least 5 characters long:
```scala
object Foo {
  implicit val mappingWithConstraint = CaseClassMapping.mapping[Foo].verifying {
    Constraint[Foo]("Foo.bar") { foo =>
      Constraints.minLength(5)(foo.bar)
    }
  }
}
```

Another use case for this is for posting form data via Play's `WS`. Normally, you would do something like this:
```scala
WS.url("https://some.api.expecting.form.encoded.data").post(
    Map(
        "name" -> Seq("ilya"),
        "age" -> Seq(9001.toString),
        "email" -> Seq("ilya at iterable dot com"),
        "favoriteBands" -> Seq("Judas Priest", "Accept")
    )
)
```

That doesn't look particularly nice... so you can use `UnbindableToWsRequest`:
```scala
case class User(name: String, age: Int, email: String, favoriteBands: Seq[String]) extends UnbindableToWsRequest[User]
object User {
    implicit val mapping = CaseClassMapping.mapping[User]
}

val user = User(name = "ilya", age = 9001, email = "ilya at iterable dot com", favoriteBands = Seq("Judas Priest", "Accept"))
WS.url("some url").post(user.unbindToWsRequest)
```

Note that there is one caveat with this; `Seq` types. Using the previous example, the unbound data will look like this:
```scala
Map(
    "name" -> Seq("ilya"),
    "age" -> Seq(9001.toString),
    "email" -> Seq("ilya at iterable dot com"),
    "favoriteBands[0]" -> Seq("Judas Priest"),
    "favoriteBands[1]" -> Seq("Accept")
)
```

Specifically, note that `favoriteBands` is unbound as
```scala
    "favoriteBands[0]" -> Seq("Judas Priest"),
    "favoriteBands[1]" -> Seq("Accept")
```

instead of 
```scala
    "favoriteBands" -> Seq("Judas Priest", "Accept")
```

See [the tests](https://github.com/Iterable/iterable-play-utils/blob/master/src/test/scala/com/iterable/play/utils/CaseClassMappingSpec.scala) for more sample usage. 
