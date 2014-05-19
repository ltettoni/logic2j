package org.logic2j.core.api.model.symbol;

import org.junit.Test;

import static org.junit.Assert.*;

public class StructTest {


    // ---------------------------------------------------------------------------
    // Struct
    // ---------------------------------------------------------------------------

    @Test
    public void struct0() throws Exception {
        Struct a1 = new Struct("f");
        assertEquals(0, a1.getArity());
        assertSame("f", a1.getName());
        Struct a2 = new Struct("f");
        assertNotSame(a1, a2);
        assertEquals(a1, a2);
    }

    @Test
    public void atomAsString() throws Exception {
        Object a1 = Struct.atom("f");
        assertTrue(a1 instanceof String);
        assertSame("f", a1);
        Object a2 = Struct.atom("f");
        assertSame(a1, a2);
    }


    @Test
    public void atomAsStruct() throws Exception {
        Object a1 = Struct.atom("true");
        assertTrue(a1 instanceof Struct);
        assertSame("true", ((Struct) a1).getName());
        Object a2 = Struct.atom("true");
        assertNotSame(a1, a2);
    }

    @Test
    public void struct2() throws Exception {
        Struct a1 = new Struct("f", "a", "b");
        assertEquals(2, a1.getArity());
        assertSame("f", a1.getName());
        assertSame("a", a1.getArg(0));
        assertSame("b", a1.getArg(1));
        Struct a2 = new Struct("f", "a", "b");
        assertNotSame(a1, a2);
        assertEquals(a1, a2);
        assertNotEquals(a1, new Struct("f", "b", "a"));
    }

    // ---------------------------------------------------------------------------
    // Lists
    // ---------------------------------------------------------------------------

    @Test
    public void listsFundamentals() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{5, 6, 7});
        assertNotNull(pList);
        assertEquals(2, pList.getArity());
        assertSame(Struct.FUNCTOR_LIST_NODE, pList.getName());
        assertEquals("./2", pList.getPredicateSignature());
        assertEquals(5, pList.getLHS());
        assertEquals(5, pList.listHead());
        assertSame(pList.getLHS(), pList.listHead());
        assertEquals("[6,7]", pList.getRHS().toString());
        assertEquals("[6,7]", pList.listTail().toString());
        assertSame(pList.getRHS(), pList.listTail());
    }

    @Test
    public void listsFormattingEmpty() throws Exception {
        final Struct pList = Struct.createPList(new Object[0]);
        assertNotNull(pList);
        assertEquals("[]", pList.toString());
    }

    @Test
    public void listsFormatting1() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{5});
        assertNotNull(pList);
        assertEquals("[5]", pList.toString());
    }

    @Test
    public void listsFormatting2() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{5, 6});
        assertNotNull(pList);
        assertEquals("[5,6]", pList.toString());
    }

    @Test
    public void listsFormatting5() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{1, 2, 3, 4, 5});
        assertNotNull(pList);
        assertEquals("[1,2,3,4,5]", pList.toString());
    }


    @Test
    public void listsSize0() throws Exception {
        final Struct pList = Struct.createPList(new Object[0]);
        assertNotNull(pList);
        assertEquals(0, pList.listSize());
    }

    @Test
    public void listsSize1() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{5});
        assertNotNull(pList);
        assertEquals(1, pList.listSize());
    }

    @Test
    public void listsSize3() throws Exception {
        final Struct pList = Struct.createPList(new Object[]{5, 6, 7});
        assertNotNull(pList);
        assertEquals(3, pList.listSize());
    }

}