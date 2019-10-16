// FULL_JDK
// FILE: Base.java

public class Base<T extends CharSequence> {
    public T foo() {
        return null;
    }
}

// FILE: test.kt
fun test() {
    val base = Base<String>()
    val s = base.foo()
    val length = s.length
}

