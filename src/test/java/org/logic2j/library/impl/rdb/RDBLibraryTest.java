package org.logic2j.library.impl.rdb;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;
import org.logic2j.library.impl.rdb.RDBLibrary;
import org.logic2j.model.symbol.Term;

/**
 */
// @Ignore
public class RDBLibraryTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibraryTest.class);

  @Override
  @Before
  public void setUp() {
    super.setUp();
    final EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName("C:/Soft/Java/db-derby-10.7.1.1-bin/bin/gd30");
    ds.setUser("APP");
    ds.setPassword("APP");
    bind("gd30", ds);
  }

  @Test
  public void test_solver_one() throws Exception {
    addTheory("test/input/rdb.pl");
    assertEquals(term("['WG', 'JWG', 'SWG']"), assertOneSolution("solve(wgtypes, X)").binding("X"));
  }

  @Test
  public void test_solver() throws Exception {
    addTheory("test/input/rdb.pl");
    assertEquals(term(69), assertOneSolution("solve(isoiec, X)").binding("X"));
    assertEquals(term("['WG', 'JWG', 'SWG']"), assertOneSolution("solve(wgtypes, X)").binding("X"));
    assertEquals(null, assertOneSolution("solve(Var, X)").binding("X"));
    //
    Map<String, Term> bindings;
    String goal;
    //
    goal = "solve(comm_orig(Com, 68), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
    //
    goal = "solve(comm_orig(Com, isoiec), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
    //
    goal = "solve(comm_category(Com,  'WG'), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
    //
    goal = "solve(comm_category(Com,  wgtypes), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
    //
    goal = "solve((comm_orig(Com, isoiec), comm_category(Com, 'WG')), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
    //
    goal = "solve((comm_orig(Com, isoiec), comm_category(Com, 'WG'), comm_numbers(Com, _, 27, _)), X)";
    bindings = assertOneSolution(goal).bindings();
    logger.info("Bindings of {}: {}", goal, bindings);
  }

  @Test
  public void test_newdb_one() throws Exception {
    loadLibrary(new IOLibrary(getProlog()));
    loadLibrary(new RDBLibrary(getProlog()));
    addTheory("test/input/rdb.pl");

    List<Term> bindings = getProlog().solve("preds(a,X), clause(X, Z), Z\\=true").all().binding("Z");
    logger.info("binding: " + bindings);

    //    assertNSolutions(6, "TCn=22, gd3((comm_numbers(Com, TCn, 4, _))), write(Com), nl");
  }

  //  @Ignore
  @Test
  public void test_select() throws Exception {
    loadLibrary(new IOLibrary(getProlog()));
    loadLibrary(new RDBLibrary(getProlog()));
    addTheory("test/input/rdb.pl");

    // Joining against the same table, with matching args
    assertNSolutions(
        1,
        "gd3((comm_parent(46780, Par1), comm_parent(46780, Par1), comm_parent(46780, Par1), comm_parent(46780, Par1), comm_parent(46780, Par1))), write(Par1)");
    assertNSolutions(1, "gd3((comm_parent(46780, Par1), comm_parent(46780, Par1), comm_parent(46780, Par1))), write(Par1)");
    assertNSolutions(1, "gd3((comm_parent(46780, Par1), comm_parent(46780, Par1))), write(Par1)");
    assertNSolutions(1, "gd3((comm_parent(46780, Par1))), write(Par1)");
    // Same parent and same var
    assertOneSolution("gd3((comm_parent(46780, Par1), comm_parent(46782, Par1))), write(Par1)");
    // Same parent on different vars (one solution!)
    assertOneSolution("gd3((comm_parent(46780, Par1), comm_parent(46782, Par2))), write(Par1), write(' '), write(Par2)");
    // Inexistent WG
    assertNoSolution("gd3((comm_parent(46780, Par1), comm_parent(46781, Par1))), write(Par1)");
    // Has a different parent
    assertNoSolution("gd3((comm_parent(46780, Par1), comm_parent(46778, Par1))), write(Par1)");
    // All free vars
    assertNSolutions(6461, "gd3((comm_parent(Com, Par1), comm_parent(Par1, Par2)))");

    assertNoSolution("gd3((comm_category(123, _)))");
    assertOneSolution("gd3((comm_category(47228, _)))");

    assertOneSolution("gd3((comm_category(47228, Cat)))");
    assertNSolutions(259, "gd3((comm_category(_, 'TC')))");
    assertNSolutions(259, "gd3((comm_category(Com, 'TC')))");
    assertNSolutions(218, "gd3((comm_orig(Com, isoiec), comm_category(Com, 'WG')))");
    assertNSolutions(231, "gd3((comm_orig(Com, isoiec), comm_category(Com, wgtypes)))");
    assertNoSolution("gd3((comm_category(Com, 'TC'),comm_category(Com, 'SC'), comm_category(Com, 'WG')))");

    // gd3 meta-predicate
    assertNSolutions(231, "gd3((comm_orig(Com, isoiec), comm_category(Com, wgtypes)))");
    assertNSolutions(231, "gd3((comm_orig(Com, isoiec), comm_category(Com, wgtypes)))");

    // coco_names
    assertNSolutions(6, "gd3(comm_ref(Com, coco_names))");
    assertNSolutions(6, "gd3(coco(Com))");

    // Meta-predicates
    assertNSolutions(277, "gd3(isoiec_comm(Com))");
    assertNSolutions(6, "gd3((isoiec_comm(Com), comm_numbers(Com, _, 27, _)))"); // All WGs of JTC 1/SC 27
    assertNSolutions(1, "gd3((isoiec_comm(Com), comm_numbers(Com, 1, 27, 1)))");
    assertNSolutions(1, "gd3((isoiec_comm(Com), comm_numbers(Com, _, 27, _), notwg(Com)))"); // All WGs of JTC 1/SC 27
    assertNSolutions(47, "gd3((isoiec_comm(Com), notwg(Com)))"); // All non-WGs of JTC 1

    // Multiple projections
    assertNSolutions(6, "gd3((comm_numbers(Com, 1, 27, _), comm_ref(Com, Reference)))");

    // Potential multiple joins
    assertNSolutions(0, "gd3((isoiec_comm(Com), comm_numbers(Com, _, 27, _), comm_numbers(Com, _, 28, _)))");
    assertNSolutions(1, "gd3((isoiec_comm(Com), comm_numbers(Com, 1, 27, 1), comm_numbers(Com, 1, 27, 1)))");
    assertNSolutions(1, "gd3((isoiec_comm(Com), comm_numbers(Com, 1, _, _), comm_numbers(Com, _, 27, 1)))");
    assertNSolutions(1, "gd3((isoiec_comm(Com), comm_numbers(Com, 1, 27, _), comm_numbers(Com, _, 27, 1)))");
    assertNSolutions(0, "gd3((isoiec_comm(Com), comm_numbers(Com, 1, 27, _), comm_numbers(Com, _, 28, 1)))");

    // distinct
    assertNSolutions(6762, "gd3((comm_category(_, Cat))), write(Cat), nl");
    assertNSolutions(55, "gd3distinct((comm_category(_, Cat))), write(Cat), nl");

    // Number of children (aggregated views)
    assertNSolutions(8, "gd3((comm_nbchildren(Com, 20))), write(Com), nl");
    assertNSolutions(3, "gd3((comm_nbchildren(Com, Nbr))), Nbr>50, write(Com), nl");

    // Operators in predicates (here, not equals or not in) 
    assertNSolutions(2884, "gd3((wg(Com), comm_status(Com, '!=', 'ACTIVE')))");
    assertNSolutions(550, "gd3((notwgtypes(Com), comm_status(Com, '!=', 'ACTIVE')))");

    // Multiple tables, some joined some not
    assertNSolutions(50, "gd3((comm_numbers(Com1, _, 15, _)))");
    assertNSolutions(2500, "gd3((comm_numbers(Com1, _, 15, _), comm_numbers(Com2, _, 15, _)))");
    assertNSolutions(2500, "gd3((comm_numbers(Com1, _, 15, _), comm_numbers(Com2, _, 15, _), comm_ref(Com1, Ref))), write(Ref), nl");
  }

  //  @Ignore
  @Test
  public void test_real_use_cases() throws Exception {
    loadLibrary(new IOLibrary(getProlog()));
    loadLibrary(new RDBLibrary(getProlog()));
    addTheory("test/input/rdb.pl");

    assertNSolutions(277, "gd3((isoiec_comm(Com)))");
    assertNSolutions(5348, "gd3((wg(Com)))");
    assertNSolutions(2464, "gd3((wg(Com), comm_status(Com, 'ACTIVE')))");
    assertNSolutions(2464, "gd3((wg(Com), comm_active(Com)))");
    assertNSolutions(2464, "gd3((wg_active(Com)))");
    assertNSolutions(110, "gd3((isoiec_wg_active(Com)))");
  }

}
