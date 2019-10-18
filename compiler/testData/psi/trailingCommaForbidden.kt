fun main() {
    val (
        y,
        z,
    ) = Pair(1, 2)
    val (
        y,
        ,z
    ) = Pair(1, 2)
}

class A<
        T1: Number,
        T2: Iterable<Iterable<Iterable<Number>>>,
        T3: Comparable<Comparable<Comparable<Number>>>,
        > { }

fun <
        T1: Comparable<Comparable<Number>>,
        T2: Iterable<Iterable<Number>>
        ,
        > foo() {}

fun main() {
    foo<Comparable<Comparable<Number>>, Iterable<Iterable<Number>>,>()
}

fun main() {
    val x: (
        y: Comparable<Comparable<Number>>,
        z: Iterable<Iterable<Number>>,
    ) -> Int = { 10 }
}

class A: A1, A2, { }

class A<T1, T2> where
    T1: Comparable<Comparable<Number>>,
    T2: Iterable<Iterable<Number>>, { }

class A(
    val x: String,
    val y: String,,
    ) {
    constructor(,) {}
}

fun foo(
    x: Int,
    y: Number
    ,
    ,
    ) {}

fun foo() = listOf(
    foo.bar.something(),
    "foo bar something",
    ,
    )

fun foo() = listOf(
    foo.bar.something(),
    ,"foo bar something")

fun foo() = listOf(
    foo.bar.something(),
    ,"foo bar something",
    )

fun foo() {
    val x = x[
            1,
            3,
            ,]
    val y = x[,]
    val z1 = x[,1]
    val z2 = x[,,1]
    val z3 = x[,1,]
}

fun main() {
    val x = { x: Comparable<Comparable<Number>>,
              y: Iterable<Iterable<Number>>,
              , ->
        println("1")
    }
    val y = { , ->
        println("1")
    }
}

fun foo(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class,
    ,
    -> println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    ,Comparable::class,
    -> println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    ,Comparable::class -> println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    , -> println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    true -> println(1)
    else, -> println(3)
}

fun foo(x: Any) = when (x) {
    true, ,-> println(1)
    else -> println(3)
}
