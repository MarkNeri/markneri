package tables;

/**
 * Created by ubuntu on 3/19/17.
 */
public interface TimeSeries extends Table<TimeSeries> {

    public static TimeSeries from(Array ts, Array value) {
        return Table.from(TimeSeries.class, ts, value);
    }
/*
    public static TimeSeries from(Table table) {
        return Table.from(TimeSeries.class, table);
    }
    */


    public abstract Column ts();

    public abstract Column value();

    public default long maxTime() {
        return 6;
    }

    public default int plusOne() {
        return numColumns() + 1;
    }
}
