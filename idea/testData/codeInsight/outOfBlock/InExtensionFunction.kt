// FALSE

class A {
    fun foo(): Int = 12
}

A.bar(): Int = {
    return bar() + <caret>
}

// TYPE: 1
