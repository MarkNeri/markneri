package tables;

import com.google.common.collect.ImmutableList;

/**
 * Created by ubuntu on 3/19/17.
 */
public class ObjectArray extends Array {

    private final ImmutableList<Object> data;

    public ObjectArray(ImmutableList<Object> data) {
        this.data = data;
    }

    public double getD(int index)
    {
        return (Double) data.get(index);
    }

    @Override
    public int length() {
        return data.size();

    }

}
