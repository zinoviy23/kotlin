// FILE: Base.java

public class Base<T extends CharSequence> {
    public T foo() {
        return null;
    }
}

// FILE: Derived.java
// FOREIGN_ANNOTATIONS
import javax.annotation.*;

public class Derived extends Base<@Nonnull String> {}
