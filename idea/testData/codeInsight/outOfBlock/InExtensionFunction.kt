// FALSE

class A {
    fun foo(): Int = 12
}

fun A.bar(): Int {
    return foo() + <caret>
}

// TYPE: 1
