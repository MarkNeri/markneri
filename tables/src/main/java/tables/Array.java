package tables;

import com.google.common.collect.ImmutableList;

/**
 * Created by ubuntu on 3/19/17.
 */
public abstract class Array {
    public static Builder builder(){
        return new Builder();
    }

    static class Builder
    {
        final ImmutableList.Builder<Object> data = ImmutableList.builder();

        public Builder add(Object o)
        {
            data.add(o);
            return this;
        }

        public Array build()
        {
            return new ObjectArray(data.build());
        }
    }

    public Column toColumn(String name)
    {
        return Column.from(name, this);
    }

    public abstract double getD(int index);

    public abstract int length();
}
