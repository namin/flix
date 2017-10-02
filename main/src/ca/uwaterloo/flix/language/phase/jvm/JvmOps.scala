/*
 * Copyright 2017 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.language.phase.jvm

import java.nio.file.{Files, Path}

import ca.uwaterloo.flix.language.ast.ExecutableAst.Root
import ca.uwaterloo.flix.language.ast.{Symbol, Type}
import ca.uwaterloo.flix.util.InternalCompilerException

object JvmOps {

  /**
    * The root package name.
    */
  val RootPackage: List[String] = Nil

  /**
    * Returns the given Flix type `tpe` as JVM type.
    *
    * For example, if the type is:
    *
    * Bool                  =>      Boolean
    * Char                  =>      Char
    * Option[Int]           =>      Option$Int
    * Result[Bool, Int]     =>      Result$Bool$Int
    * Int -> Bool           =>      Fn1$Int$Bool
    * (Int, Int) -> Bool    =>      Fn2$Int$Int$Bool
    */
  def getJvmType(tpe: Type, root: Root): JvmType = {


    val base = tpe.typeConstructor
    val args = tpe.typeArguments

    base match {
      case Type.Arrow(arity) =>
        // Compute a name of the form:
        // Fn1$Int$Bool
        // Fn2$Int$Int$Bool
        // Fn3$Char$Int$Int$Bool
        val name = "Fn" + arity + "$" + args.map(getSimpleTypeAsString).mkString("$")
        JvmType.Reference(JvmName(Nil, name))
      case Type.Enum(sym, _) => ???
      case _ => ???
    }
  }

  /**
    * Returns the continuation type `Cont$X` for the given type `tpe`.
    *
    * NB: The given type `tpe` must be an arrow type.
    */
  def getContinuationType(tpe: Type): JvmType = {
    // Check that the given type is an arrow type.
    if (!tpe.typeConstructor.isArrow)
      throw InternalCompilerException(s"Unexpected type: '$tpe'.")

    // Check that the given type has at least one type argument.
    if (tpe.typeArguments.isEmpty)
      throw InternalCompilerException(s"Unexpected type: '$tpe'.")

    // The return type is the last type argument.
    val returnType = tpe.typeArguments.last

    // The name of the continuation class is Cont$SimpleType
    val name = "Cont$" + getSimpleTypeAsString(returnType)
    JvmType.Reference(JvmName(RootPackage, name))
  }

  /**
    * Returns the JVM type of the given enum symbol `sym` with `tag` and inner type `tpe`.
    *
    * For example, if the symbol is `Option`, the tag `Some` and the inner type is `Int` then the result is None$Int.
    */
  def getJvmTypeFromEnumAndTag(sym: Symbol.EnumSym, tag: String, tpe: Type): JvmType = ???

  /**
    * Returns the information about the tags of the given type `tpe`.
    */
  def getTagsOf(tpe: Type): Set[TagInfo] = ???

  /**
    * Returns the JVM type of the given tag info `i`.
    */
  def getJvmType(i: TagInfo, root: Root): JvmType = ???

  /**
    * Returns the string name of the given type constructor `tpe`.
    *
    * Returns the name of the primitive types and `Obj` for reference types.
    *
    * NB: The type must be a type constructor.
    */
  // TODO: Rename to ErasedType and introduce a ADT or something?
  private def getSimpleTypeAsString(tpe: Type): String = tpe match {
    case Type.Bool => "Bool"
    case Type.Char => "Char"
    case Type.Float32 => "Float32"
    case Type.Float64 => "Float64"
    case Type.Int8 => "Int8"
    case Type.Int16 => "Int16"
    case Type.Int32 => "Int32"
    case Type.Int64 => "Int64"
    case _ => "Obj"
  }

  /**
    * Returns the set of all instantiated types in the given AST `root`.
    *
    * This include type components. For example, if the program contains
    * the type (Bool, (Char, Int)) this includes the type (Char, Int).
    */
  def typesOf(root: Root): Set[Type] = {
    // TODO: Temporary implementation which just returns some types to get us started.

    root.defs.map(_._2.tpe).toSet
  }

  /**
    * Returns all the type components of the given type `tpe`.
    *
    * For example, if the given type is `Option[(Bool, Char, Int)]`
    * this returns the set `Bool`, `Char`, `Int`, `(Bool, Char, Int)`, and `Option[(Bool, Char, Int)]`.
    */
  def typesOf(tpe: Type): Set[Type] = ??? // TODO


  /**
    * Writes the given JVM class `clazz` to a sub path under the given `prefixPath`.
    *
    * For example, if the prefix path is `/tmp/` and the class name is Foo.Bar.Baz
    * then the bytecode is written to the path `/tmp/Foo/Bar/Baz.class` provided
    * that this path either does not exist or is already a JVM class file.
    */
  def writeClass(prefixPath: Path, clazz: JvmClass): Unit = {
    // Compute the absolute path of the class file to write.
    val path = prefixPath.resolve(clazz.name.toPath).toAbsolutePath

    // TODO: For safety, let us not write anything yet.
    println(path)
    return

    // Create all parent directories (in case they don't exist).
    Files.createDirectories(path.getParent)

    // Check if the file already exists.
    if (Files.exists(path)) {
      if (!isClassFile(path)) {
        throw InternalCompilerException(s"Refusing to overwrite non-class file: '$path'.")
      }
    }

    // Write the bytecode.
    Files.write(path, clazz.bytecode)
  }

  /**
    * Returns `true` if the given `path` exists and is a Java Virtual Machine class file.
    */
  def isClassFile(path: Path): Boolean = {
    if (Files.exists(path) && Files.isReadable(path) && Files.isRegularFile(path)) {
      // Read the first four bytes of the file.
      val is = Files.newInputStream(path)
      val b1 = is.read()
      val b2 = is.read()
      val b3 = is.read()
      val b4 = is.read()
      is.close()

      // Check if the four first bytes match CAFE BABE.
      return b1 == 0xCA && b2 == 0xFE && b3 == 0xBA && b4 == 0xBE
    }
    false
  }

}