# iterable-play-utils

Collection of utilites used by Iterable in Scala Play! projects. Built/tested with sbt 0.13.7, Scala 2.10, and Play! 2.2.2

## Automatic Case Class Mappings

See [com.iterable.play.utils.CaseClassMapping](https://github.com/Iterable/iterable-play-utils/blob/master/src/main/scala/com/iterable/play/utils/CaseClassMapping.scala). Uses ***runtime reflection*** to generate form mappings for case classes without all the manual typing. See [the tests](https://github.com/Iterable/iterable-play-utils/blob/master/src/test/scala/com/iterable/play/utils/CaseClassMappingSpec.scala) for sample usage.  

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
