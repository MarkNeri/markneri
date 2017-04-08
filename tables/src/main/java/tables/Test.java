package tables;

import com.google.common.base.Preconditions;


/**
 * Created by ubuntu on 3/19/17.
 */
public class Test {


  public static void main(String[] args) {
    Array.Builder cb = Column.builder();
    cb.add(7);
    cb.add(9);

    Column c = cb.build().toColumn("ts");

    TimeSeries ts = TimeSeries.from(c, c);

    System.out.println(ts.ts());

    System.out.println(ts.columns());

    System.out.println(ts.column("ts"));

    System.out.println(ts.maxTime());

    System.out.println(ts.numColumns());

    ts = ts.with(c);

    Preconditions.checkArgument(ts instanceof TimeSeries);

    System.out.println(ts.numColumns());

    System.out.println(ts.ts());

    System.out.println(ts.maxTime());

    System.out.println(ts.plusOne());

    // System.out.println(ts.columns());

  }


}




























