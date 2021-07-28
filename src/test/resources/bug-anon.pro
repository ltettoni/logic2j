/*
  2021-07-26 Found a serious bug with querying with the anonymous variable.
  Querying with c(_Z) will find 2 solutions.
  Querying with c(_) will find 2 x 2 solutions :-(

  The cause of this bug is serious.
  When we invoke from the root level with variable Z, the innermost unification with a(1) follows this unification scheme:
    a(1) unifies with a(Local), unifies with c(Local), unifies with c(Z), and this sets Z=1 before issuing the first solution.
    Then we try the RHS of the "And", with a(Local=1) and this won't match a(2).
    Then we backtrack to the LHS of the "And" and a(2) unifies with a(Local), unifies with c(Local), unifies with c(Z), and this sets Z=2
    In the RHS of the "And", a(Local) cannot unify with a(1), only unifies with a(2).
    This gives 2 solutions.

  When we invoke from the root level with anonymous variable _ :
    The innermost unification with a(1) succeeds in unifying with _, but does not set
    The first solution to Local=1. The RHS side of the AND can unify 2 with _.
    This gives 4 solutions.

    This has the same effect as if the inside of the rule c/1 referenced "_" instead of "Local".

  This bug is likely to only happen with the AND predicate because this is the only way to continue further
  execution after partial unification.

  Solutions:

  1. Rule hard to enforce: any rule with an "and" should never be invoked with the anonymous variable, from the root-level or from another rule.
  2. When unifying a free var that is linked to an anonymous var, bind the free var to the value, not delegate the unification to the latest (anonymous) of the chain.
  3. When unifying a free var to the anonymous variable, we should not link the free var to the anon.

Solution 3 is the right one, a 2 liner in logic2j-engine, class UnifyContext
*/

a(1).
a(2).

b(1).
b(2).

% The "Local" variable is local to the rule, whichever it binds with a real or anonymous variable
% in the invoking context, there will be 2 solutions. Not 4.
c(Local) :- a(Local), a(Local).

% Just a one-to-one synonym rule
c2(Z) :- c(Z).

% Invoking c/1 with the anonymous variable
d(Z) :- c(_).
