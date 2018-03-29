   logic2j - Bring Logic to your Java
   ==================================

Refactoring needed to extend from logic2j-engine:

TO REINTRODUCE: FunctionLibrary (+Test) - deleted because needed quite internal access to solver and unification.



Many diffs :-(
Use this branch: "over-engine"


Diff using WinMerge:
left:  C:\Git\logic2j-engine\src\main\java\org\logic2j\engine\
right: C:\Git\logic2j\src\main\java\org\logic2j\engine\

Hard part:

SingleVarExtractor:  TermAdapter removed !   We would need some reification function to be specifiable
  that would do:
  reifiedValue = this.termAdapter.fromTerm(reifiedValue, this.targetClass);



Struct: features lost: primitiveInfo, prolog lists, formatStruct for lists
TermApi: no lists, evaluate() removed, no LibraryContent, selectTerm() removed




GoalHolder: no more PrologReferenceImplementation, but has ref to Solver
SolutionHolder: remove support for arrays, FactoryExtractor
UnifyContext: lots of changes (but should not impact)


New exceptions.







A library to bring declarative & logic programming to your Java software.

Logic2j is designed for first-order predicate formal logic, it includes all necessary
components to manage Terms and their representations, an inference engine solver,
an extensible unification framework, an in-memory or database-backend knowledge base.

This work was triggered by using "tuProlog" from the University of Bologna, Italy, in production projects.
Logic2j is however of a different nature and based on other algorithms in particular for unification and inference.

The design guidelines were: closer integration with the JRE, minimal dependencies,
small footprint, high performance, and state-of-the-art coding patterns.

Although very close to Prolog, logic2j is NOT a complete Prolog programming environment, is not
intended to be compatible with Prolog specification, this is just the core engine...

Read further documentation at src/site/doc.

You must have received a LICENSE.txt file with this software package.
