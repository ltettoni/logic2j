package org.logic2j.engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PrologListsTest {

  @Test
  public void listsFundamentals() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{5, 6, 7});
    assertThat(pList).isNotNull();
    assertThat(pList.getArity()).isEqualTo(2);
    assertThat(pList.getName()).isEqualTo(PrologLists.FUNCTOR_LIST_NODE);
    assertThat(pList.getPredicateSignature()).isEqualTo("./2");
    assertThat(pList.getLHS()).isEqualTo(5);
    assertThat(PrologLists.listHead(pList)).isEqualTo(5);
    assertThat(PrologLists.listHead(pList)).isEqualTo(pList.getLHS());
    assertThat(pList.getRHS().toString()).isEqualTo("[6,7]");
    assertThat(PrologLists.listTail(pList).toString()).isEqualTo("[6,7]");
    assertThat(PrologLists.listTail(pList)).isEqualTo(pList.getRHS());
  }

  @Test
  public void listsFormattingEmpty() {
    final Struct<?> pList = PrologLists.createPList(new Object[0]);
    assertThat(pList).isNotNull();
    assertThat(pList.toString()).isEqualTo("[]");
  }

  @Test
  public void listsFormatting1() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{5});
    assertThat(pList).isNotNull();
    assertThat(pList.toString()).isEqualTo("[5]");
  }

  @Test
  public void listsFormatting2() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{5, 6});
    assertThat(pList).isNotNull();
    assertThat(pList.toString()).isEqualTo("[5,6]");
  }

  @Test
  public void listsFormatting5() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{1, 2, 3, 4, 5});
    assertThat(pList).isNotNull();
    assertThat(pList.toString()).isEqualTo("[1,2,3,4,5]");
  }


  @Test
  public void listsSize0() {
    final Struct<?> pList = PrologLists.createPList(new Object[0]);
    assertThat(pList).isNotNull();
    assertThat(PrologLists.listSize(pList)).isEqualTo(0);
  }

  @Test
  public void listsSize1() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{5});
    assertThat(pList).isNotNull();
    assertThat(PrologLists.listSize(pList)).isEqualTo(1);
  }

  @Test
  public void listsSize3() {
    final Struct<?> pList = PrologLists.createPList(new Object[]{5, 6, 7});
    assertThat(pList).isNotNull();
    assertThat(PrologLists.listSize(pList)).isEqualTo(3);
  }

}