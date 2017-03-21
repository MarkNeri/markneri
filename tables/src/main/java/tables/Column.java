package tables;

/**
 * Created by ubuntu on 3/19/17.
 */
public class Column extends DelegatingArray {

    public Column(String name, Array array) {
        super(array);
        this.name = name;
    }

    final private String name;

    public static Builder builder() {
        return new Builder();
    }

    public static Column from(String name, Array array)
    {
        return new Column(name, array);
    }

    public String name(){
        return name;
    }



}
