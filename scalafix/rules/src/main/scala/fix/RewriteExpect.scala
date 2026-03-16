package fix

import scalafix.v1._
import scala.meta._

/**
 * Rewrites `expect` and `expect.all` calls into `expect.eql` and `expect.same`.
 *
 * As of weaver `0.9.0`, `expect` and `expect.all` do not capture any
 * information on failure. `expect.eql` and `expect.same` have better error
 * messages.
 *
 * This rule can be applied for weaver versions `0.9.x` and above.
 */
class RewriteExpect
    extends SemanticRule("RewriteExpect") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val expectMethod =
      SymbolMatcher.normalized("weaver/Expectations.Helpers#expect.")
    val expectAllMethod = SymbolMatcher.normalized("weaver/ExpectMacro#all().")
    doc.tree.collect {
      case expectTree @ Term.Apply.After_4_6_0(expectMethod(_),
                                               Term.ArgClause(List(tree), _)) =>
        // Matched `expect(tree)`
        rewrite(tree) match {
          case Some(next) => Patch.replaceTree(expectTree, next.toString)
          case None       => Patch.empty
        }
      case expectAll @ Term.Apply.After_4_6_0(expectAllMethod(_),
                                              Term.ArgClause(trees, _)) =>
        // Matched `expect.all(trees)`
        val (equalityAssertions, otherAssertions) = partition(trees)(tree =>
          rewrite(tree) match {
            case Some(equality) => Left(equality)
            case None           => Right(tree)
          })
        equalityAssertions match {
          case firstEqAssertion :: remainingEqAssertions =>
            val combinedEqAssertion =
              remainingEqAssertions.foldLeft(firstEqAssertion: Term) {
                (acc, cur) =>
                  q"$acc.and($cur)"
              }
            otherAssertions match {
              case Nil =>
                // All assertions were == or ===. Remove the `expect.all` statement.
                Patch.replaceTree(expectAll, combinedEqAssertion.toString)
              case singleAssertion :: Nil =>
                // A single assertion is not == or ===. Wrap this in `expect`.
                val combinedAssertion =
                  q"$combinedEqAssertion.and(expect($singleAssertion))"
                Patch.replaceTree(expectAll, combinedAssertion.toString)
              case _ :: _ :: _ =>
                // Several assertions are not == or ===. Wrap these in `expect.all`.
                val combinedAssertion =
                  q"$combinedEqAssertion.and(expect.all(..$otherAssertions))"
                Patch.replaceTree(expectAll, combinedAssertion.toString)
            }
          case Nil =>
            // `expect.all` didn't contain any == or === assertions.
            Patch.empty
        }
    }.asPatch
  }

  /**
   * Rewrites boolean assertions into `expect.same` and `expect` calls.
   *
   * For example:
   *   - `a == b` is rewritten to `expect.same(a, b)`
   *   - `a && b` is rewritten to `expect(a).and(expect(b))`
   *   - `if (cond) a else b` is rewritten to
   *     `if (cond) expect(a) else expect(b)` .
   */
  def rewrite(tree: Tree)(implicit doc: SemanticDocument): Option[Term] = {
    val catsEqMethod = SymbolMatcher.normalized(
      "cats/syntax/EqOps#`===`().") + SymbolMatcher.normalized(
      "cats/syntax/EqOps#eqv().")

    tree match {
      case q"$lhs == $rhs" if !containsClues(tree) =>
        val (expected, found) = inferExpectedAndFound(lhs, rhs)
        Some(q"expect.same($expected, $found)")
      case Term.ApplyInfix.After_4_6_0(lhs,
                                       catsEqMethod(_),
                                       _,
                                       Term.ArgClause(List(rhs), _))
          if !containsClues(tree) =>
        val (expected, found) = inferExpectedAndFound(lhs, rhs)
        Some(q"expect.eql($expected, $found)")
      case Term.Apply.After_4_6_0(Term.Select(lhs, catsEqMethod(_)),
                                  Term.ArgClause(List(rhs), _))
          if !containsClues(tree) =>
        val (expected, found) = inferExpectedAndFound(lhs, rhs)
        Some(q"expect.eql($expected, $found)")
      case q"$lhs && $rhs" =>
        val nextLhs = rewrite(lhs).getOrElse(q"expect($lhs)")
        val nextRhs = rewrite(rhs).getOrElse(q"expect($rhs)")
        Some(q"$nextLhs.and($nextRhs)")
      case q"$lhs || $rhs" =>
        val nextLhs = rewrite(lhs).getOrElse(q"expect($lhs)")
        val nextRhs = rewrite(rhs).getOrElse(q"expect($rhs)")
        Some(q"$nextLhs.or($nextRhs)")
      case q"true"  => Some(q"success")
      case q"false" => Some(q"""failure("Assertion failed")""")
      case q"if ($cond) $lhs else $rhs" if !containsClues(cond) =>
        val nextLhs = rewrite(lhs).getOrElse(q"expect($lhs)")
        val nextRhs = rewrite(rhs).getOrElse(q"expect($rhs)")
        Some(q"if ($cond) $nextLhs else $nextRhs")
      case q"$expr match { ..case $casesnel }" if !containsClues(expr) =>
        // Rewrite assertions with `case _ => false` to use `matches`
        val wildcardFalse = casesnel.find {
          case p"case _ => false" => true
          case _                  => false
        }
        wildcardFalse match {
          case Some(wildcardCase) if casesnel.size > 1 =>
            val nextCases = rewriteCases(casesnel.filterNot(_ == wildcardCase))
            Some(q"matches($expr) {..case $nextCases}")
          case _ =>
            val nextCases = rewriteCases(casesnel)
            Some(q"$expr match {..case $nextCases }")
        }
      case _ =>
        None
    }
  }

  def rewriteCases(casesnel: List[Case])(implicit
      doc: SemanticDocument): List[Case] = {
    casesnel.map { caseTree =>
      val nextExpr =
        rewrite(caseTree.body).getOrElse(q"expect(${caseTree.body})")
      p"case ${caseTree.pat} if ${caseTree.cond} => $nextExpr"
    }
  }

  /**
   * Checks is an assertion contains `clue(...)`. If so, it should not be
   * rewritten.
   */
  def containsClues(tree: Tree)(implicit doc: SemanticDocument): Boolean = {
    val clueSymbol =
      SymbolMatcher.normalized("weaver/internals/ClueHelpers#clue().")
    tree.collect {
      case clueSymbol(_) => ()
    }.nonEmpty

  }

  /**
   * Infers the order of `expected` and `found` parameters in `expect.same`.
   *
   * When converting from `expect(a == b)` to `expect.same(a, b)`, we do not
   * know which of `a` or `b` is the expected and found value.
   *
   * The expected value is likely to be:
   *   - A literal e.g. `"hello-world"`
   *   - An ADT e.g. `Pet.Cat`
   *   - An term containing "expected" in its name e.g. `expectedValue`
   *   - An expression containing literals e.g. `makeId(1)`
   */
  def inferExpectedAndFound(left: Term, right: Term)(implicit
      doc: SemanticDocument): (Term, Term) = {
    def isAlgebraicDataType(tree: Tree): Boolean = tree.symbol.info.exists {
      info =>
        info.isObject && info.isFinal
    }
    def containsLiterals(term: Term): Boolean = term.collect {
      case Lit(_) => ()
    }.nonEmpty

    def startsWithCapital(term: Term): Boolean = {
      val firstLetter = term.syntax.head.toString
      firstLetter.capitalize == firstLetter
    }
    def hasFoundInName(term: Term): Boolean = {
      val foundKeywords = List("obtained", "actual", "result", "found")
      foundKeywords.exists(term.syntax.contains)
    }
    def hasExpectedInName(term: Term): Boolean = {
      term.syntax.contains("expected")
    }

    (left, right) match {
      case (Lit(_), _) => (left, right)
      case (_, Lit(_)) => (right, left)
      case _ if isAlgebraicDataType(right) && !isAlgebraicDataType(left) =>
        (right, left)
      case _ if isAlgebraicDataType(left) && !isAlgebraicDataType(right) =>
        (left, right)
      // Test for expected and found values using naming conventions instead
      case _ if hasExpectedInName(right) && !hasExpectedInName(left) =>
        (right, left)
      case _ if hasExpectedInName(left) && !hasExpectedInName(right) =>
        (left, right)
      case _ if hasFoundInName(right) && !hasFoundInName(left) =>
        (left, right)
      case _ if hasFoundInName(left) && !hasFoundInName(right) =>
        (right, left)
      // Assume the symbol is an expected ADT if it starts with a capital
      // Symbol information is not present in Scala 3 - see https://github.com/scalacenter/scalafix/issues/2054
      // If the symbol-based ADT test cannot be performed, we perform an additional test based on naming conventions.
      case _ if startsWithCapital(right) && !startsWithCapital(left) =>
        (right, left)
      case _ if startsWithCapital(left) && !startsWithCapital(right) =>
        (left, right)
      case _ if containsLiterals(right) && !containsLiterals(left) =>
        (right, left)
      case _ if containsLiterals(left) && !containsLiterals(right) =>
        (left, right)
      case _ =>
        (left, right)
    }
  }

  def partition[A, B, C](
      values: List[A])(f: A => Either[B, C]): (List[B], List[C]) = {
    val eithers = values.map(f)
    val lefts   = eithers.collect { case Left(v) => v }
    val rights  = eithers.collect { case Right(v) => v }
    (lefts, rights)
  }
}
