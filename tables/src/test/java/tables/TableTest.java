package tables;
import org.junit.Test;

/**
 * Created by ubuntu on 3/19/17.
 */
public class TableTest {

    public static interface AB extends Table<AB>
    {
        public static AB from(Array a, Array b) {return Table.from(AB.class, a, b);}
        public static AB from(Table t) {return Table.from(AB.class, t);};

        Column a();
        Column b();
    }

    @Test
    public void testConvertToTyped()
    {
        Table t = Table.from(
                Array.builder().build().toColumn("a"),
                Array.builder().build().toColumn("b"));

        AB ab = AB.from(t);

        assert(ab.a().name().equals("a"));
        assert(ab.b().name().equals("b"));
    }

    @Test(expected = Exception.class)
    public void testFailedCastToTyped()
    {
        Table t = Table.from(
                Array.builder().build().toColumn("a"),
                Array.builder().build().toColumn("c"));


        AB ab = AB.from(t);
    }

    @Test
    public void testBuildTyped()
    {
        AB ab = getTyped();

        assert(ab.a().name().equals("a"));
        assert(ab.b().name().equals("b"));
    }

    private AB getTyped() {
        return AB.from(
                    Array.builder().build(),
                    Array.builder().build());
    }

    @Test
    public void testAddToTyped()
    {
        AB ab = getTyped();

        ab = ab.with(Array.builder().build().toColumn("c"));

        assert(ab.a().name().equals("a"));
        assert(ab.b().name().equals("b"));

        assert(ab.column("a").name().equals("a"));
        assert(ab.column("b").name().equals("b"));
        assert(ab.column("c").name().equals("c"));


    }

    @Test
    public void testRemoveFromTyped()
    {
        AB ab = getTyped();
        Table t = ab.without("a");

        assert(t.column("b").name().equals("b"));
    }

    @Test(expected = Exception.class)
    public void testRemoveFromTyped2()
    {
        AB ab = getTyped();
        Table t = ab.without("a");

        assert(t.column("a").name().equals("a"));
    }

    public void testRemoveFromTyped3()
    {
        AB ab = getTyped();
        Table t = ab.without("a");
        t = t.with(ab.a());

        ab = AB.from(t);

        assert(ab.a().name().equals("a"));
        assert(ab.b().name().equals("b"));

        assert(ab.column("a").name().equals("a"));
        assert(ab.column("b").name().equals("b"));
    }

    @Test(expected = Exception.class)
    public void testDupName()
    {
        Table t = Table.from(
                Array.builder().build().toColumn("a"),
                Array.builder().build().toColumn("a"));
    }

    @Test(expected = Exception.class)
    public void testLengthMismatch()
    {
        Table t = Table.from(
                Array.builder().build().toColumn("a"),
                Array.builder().add(0).build().toColumn("b"));
    }




}
