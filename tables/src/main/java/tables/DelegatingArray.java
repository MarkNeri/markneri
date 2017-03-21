package tables;

/**
 * Created by ubuntu on 3/19/17.
 */
public class DelegatingArray extends Array {

    public DelegatingArray(Array delegate) {
        this.delegate = delegate;
    }

    @Override
    public double getD(int index) {
        return delegate.getD(index);
    }

    @Override
    public int length() {
        return delegate.length();
    }

    private final Array delegate;
}
