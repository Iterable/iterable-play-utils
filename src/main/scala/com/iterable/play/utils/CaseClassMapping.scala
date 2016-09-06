package com.iterable.play.utils

import play.api.Logger
import play.api.data._
import play.api.data.format.{Formats, Formatter}
import play.api.data.validation.{Constraint, Invalid}

import scala.reflect.runtime.universe._

trait CaseClassMapping[T] extends Mapping[T] {
  def unbindToWsRequest(value: T): Map[String, Seq[String]] = unbind(value)._1.mapValues(Seq(_))
}

object CaseClassMapping {
  private val mirror = runtimeMirror(getClass.getClassLoader)

  private def getTypeOfFormatter[T: TypeTag](t: Formatter[T]) = typeOf[T]
  // TODO - build this by grabbing implicit val's and nullary implicit def's in format/Format
  private val formatters = Seq(
    Formats.stringFormat -> getTypeOfFormatter(Formats.stringFormat),
    Formats.longFormat -> getTypeOfFormatter(Formats.longFormat),
    Formats.intFormat -> getTypeOfFormatter(Formats.intFormat),
    Formats.floatFormat -> getTypeOfFormatter(Formats.floatFormat),
    Formats.doubleFormat -> getTypeOfFormatter(Formats.doubleFormat),
    Formats.bigDecimalFormat -> getTypeOfFormatter(Formats.bigDecimalFormat),
    Formats.booleanFormat -> getTypeOfFormatter(Formats.booleanFormat),
    Formats.dateFormat -> getTypeOfFormatter(Formats.dateFormat),
    Formats.sqlDateFormat -> getTypeOfFormatter(Formats.sqlDateFormat),
    Formats.jodaDateTimeFormat -> getTypeOfFormatter(Formats.jodaDateTimeFormat),
    Formats.jodaLocalDateFormat -> getTypeOfFormatter(Formats.jodaLocalDateFormat)
  )

  // http://stackoverflow.com/questions/12842729/finding-type-parameters-via-reflection-in-scala-2-10
  private def getParameterType(tpe: Type) = {
    // TODO - instead, try using tpe.asClass.typeParams
    tpe.asInstanceOf[TypeRefApi].args match {
      case x :: Nil =>
        Some(x)

      case x :: remaining =>
        throw new RuntimeException(s"Sorry, I can't work on stuff that requires two types")

      case _ =>
        None
    }
  }

  private def unwrappedType(tpe: Type): Type = {
    if (typeIsSeq(tpe) || typeIsOption(tpe)) {
      unwrappedType(getParameterType(tpe).get)
    } else {
      tpe
    }
  }

  private def isCorrectFormatterOrMapping(tpe: Type, formatterType: Type): Boolean = {
//    Logger.trace(s"Checking if formatter type $formatterType is the right one for $tpe")
    if (typeIsFormatter(formatterType) || typeIsMapping(formatterType)) {
      // get the parameter type of the formatter/mapping
//      Logger.trace(s"Unwrapping formatter/mapping type $formatterType")
      isCorrectFormatterOrMapping(tpe, getParameterType(formatterType).get)
    } else {
      formatterType =:= unwrappedType(tpe)
    }
  }

  private def generateWrappedMappingForFormatter(fieldName: String, tpe: Type, formatter: Formatter[_]): Mapping[_] = {
    val mapping = FieldMapping("")(formatter.asInstanceOf[Formatter[Any]])
    generateWrappedMappingForMapping(fieldName, tpe, mapping)
  }

  private def generateWrappedMappingForMapping(fieldName: String, tpe: Type, mapping: Mapping[_]): Mapping[_] = {
    if (typeIsOption(tpe)) {
      if (typeIsSeq(getParameterType(tpe).get)) {
        // support optional(seq(text))
        OptionalMapping(RepeatedMapping(mapping, key = fieldName))
      } else {
        OptionalMapping(mapping.withPrefix(fieldName))
      }
    } else if (typeIsSeq(tpe)) {
      RepeatedMapping(mapping, key = fieldName)
    } else {
      mapping.withPrefix(fieldName)
    }
  }

  private def typeIsOption(tpe: Type) = tpe <:< typeOf[Option[Any]]
  private def typeIsSeq(tpe: Type) = tpe <:< typeOf[Seq[Any]]
  // TODO - is checking erasure the right way to do this?
  private def typeIsFormatter(tpe: Type) = tpe.erasure <:< typeOf[Formatter[Any]].erasure
  private def typeIsMapping(tpe: Type) = tpe.erasure <:< typeOf[Mapping[Any]].erasure

  private def getInstanceOfCompanion(companion: ModuleSymbol) = mirror.reflectModule(companion).instance

  private def getNullaryFieldFromCompanionObject(companion: ModuleSymbol, member: TermSymbol) = {
    val m = mirror.reflect(getInstanceOfCompanion(companion))
    // both the getter accessor and a nullary def are methods, so might as well just use reflectMethod
    m.reflectMethod(member.asMethod).apply()
  }

  // both getter accessors and nullary defs are of type NullaryMethodType
  private def isFieldForMapping(memberType: Type, forType: Type) = memberType match {
    case nullaryMethod: NullaryMethodType =>
      val fieldType = nullaryMethod.resultType
      typeIsMapping(fieldType) && isCorrectFormatterOrMapping(forType, fieldType)
    case _ => false
  }

  // given a Type, search its companion object for an implicit val or nullary def of a mapping of that Type
  private def getMappingFromCompanionOfType(tpe: Type): Option[Mapping[_]] = {
//    Logger.trace(s"Looking for mapping in companion of $tpe")
    tpe.typeSymbol.companionSymbol match {
      case NoSymbol =>
//        Logger.trace(s"No companion symbol for type $tpe")
        None

      case companion =>
//        Logger.trace(s"Found companion symbol $companion for type $tpe")
        // use .members, not .declarations; we want to include things that its super class might declare
        val companionTypeSignature = companion.typeSignature
        companionTypeSignature.members.collectFirst {
          // The generated getter is marked as implicit; but the actual underlying val is not implicit! So we check if the getter is implicit
          // In the case of an implicit val, the generated getter is a method; in the case of a nullary def... well it's already a method
          case member: TermSymbol with MethodSymbol if member.isImplicit && isFieldForMapping(member.typeSignatureIn(companionTypeSignature), tpe) =>
            Logger.trace(s"Found a mapping in the companion object of $tpe, it's $member")
            getNullaryFieldFromCompanionObject(companion.asModule, member).asInstanceOf[Mapping[_]]
        }
    }
  }

  // given a symbol, find a Mapping for its Type
  private def getMappingForSymbol(symbol: Symbol, tpeOfEnclosing: Type): Mapping[_] = {
    val paramName = symbol.name.toString // the name of the parameter
    val realTpe = symbol.typeSignature // the parameter type
    val tpe = unwrappedType(realTpe) // if the parameter is of type Seq/List/Option, unwrap the underlying type
    Logger.trace(s"Looking for default formatter for type $realTpe (unwrapped $tpe)")
    formatters.collectFirst { case (formatter, formatterType) if isCorrectFormatterOrMapping(tpe, formatterType) =>
      generateWrappedMappingForFormatter(paramName, realTpe, formatter)
    }.orElse {
      Logger.trace(s"Unable to find default formatter for type $tpe; searching for companion object formatters")
      getMappingFromCompanionOfType(tpe).map { mapping =>
        generateWrappedMappingForMapping(paramName, realTpe, mapping)
      }
    } match {
      case Some(mapping) => mapping
      case None => throw new RuntimeException(s"Can't find an existing mapping for argument ${symbol.name.decoded} of type $tpe in class $tpeOfEnclosing! If this is a non-primitive type, make sure you have declared an implicit Mapping in its companion object as a val or nullary def!")
    }
  }

  // http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html
  // http://stackoverflow.com/questions/13814288/how-to-get-constructor-argument-names-using-scala-macros
  // http://stackoverflow.com/a/24100624
  def mapping[T <: Product : TypeTag]: CaseClassMapping[T] = {
    Logger.trace(s"Generating CaseClassMapping for ${typeOf[T]}...")
    typeOf[T].declarations.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    } match {
      case Some(primaryConstructor) =>
//        Logger.trace(s"Found primary constructor! It's $primaryConstructor, and the params are ${primaryConstructor.paramss}")
        // paramss returns a list of lists; if it's a nullary it's empty list, otherwise it's a 2d list (where first list just has one element)
        // we don't care about the nullary case though, never going to have a case class with no arguments
        val mappings = primaryConstructor.paramss.flatten.map(getMappingForSymbol(_, typeOf[T]))
        val result = CaseClassMappingImpl[T](primaryConstructor, mappings)
        Logger.trace(s"Done generating CaseClassMapping for ${typeOf[T]}")
        result

      case None =>
        throw new RuntimeException(s"Unable to find primary constructor for ${typeOf[T]}! generate mapping is only available for case classes")
    }
  }

  // representation of a Mapping for a case class. It stores the constructor, and an (in-order) seq of mappings for the constructor params
  private case class CaseClassMappingImpl[T <: Product : TypeTag](constructor: MethodSymbol, mappings: Seq[Mapping[_]], key: String = "", constraints: Seq[Constraint[T]] = Nil) extends CaseClassMapping[T] {
    private val constructorMirror = {
      val classSymbol = typeOf[T].typeSymbol.asClass
      val classMirror = mirror.reflectClass(classSymbol)
      classMirror.reflectConstructor(constructor)
    }

    private def createInstance(constructor: MethodSymbol, args: Seq[Any]): T = {
      Logger.trace(s"Creating a ${typeOf[T]} with arguments: $args")
      constructorMirror(args:_*).asInstanceOf[T]
    }

    def bind(data: Map[String, String]): Either[Seq[FormError], T] = {
      Logger.trace(s"Binding to type ${typeOf[T]}: $data")
      val args = mappings.map(_.withPrefix(key).bind(data))
      if (args.forall(_.isRight)) {
        applyConstraints(createInstance(constructor, args.map(_.right.get)))
      } else {
        Left(args.collect { case Left(errors) => errors }.flatten)
      }
    }

    def unbind(value: T): (Map[String, String], Seq[FormError]) = {
      val args = mappings.zip(value.productIterator.toIterable).map { case (mapping, field) =>
        mapping.asInstanceOf[Mapping[Any]].withPrefix(key).unbind(field)
      }
      val errors = args.flatMap(_._2) ++ collectErrors(value)
      args.flatMap(_._1.toSeq).toMap -> errors
    }

    def verifying(addConstraints: Constraint[T]*): Mapping[T] = copy(constraints = constraints ++ addConstraints.toSeq)
    def withPrefix(prefix: String): Mapping[T] = addPrefix(prefix).map(newKey => copy(key = newKey)).getOrElse(this)

    // override this to use the Constraint name if there's no key (which happens because we can't get to the individual mapping for the fields)
    override protected def collectErrors(t: T): Seq[FormError] = {
      constraints.map { c => c -> c(t)}.collect {
        case (c, Invalid(errors)) => c -> errors
      }.flatMap { case (c, ves) => ves.map(ve => FormError(if (key.nonEmpty) key else c.name.getOrElse(""), ve.message, ve.args)) }
    }
  }
}
