// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KType
import kotlin.test.assertEquals

interface C

inline fun <reified T> get() = typeOf<T>()

inline fun <reified U> get1() = get<U?>()

inline fun <reified V> get2(): KType {
    val y = get1<Map<in V?, Array<V>>>()
    val x = get1<Map<in V?, Array<V>>>()
    val z = get1<Map<in V?, Array<V>>>()
    return x
}

fun box(): String {
    assertEquals("C?", get1<C>().toString())
    assertEquals("Map<in C?, Array<C>>?", get2<C>().toString())
    assertEquals("Map<in List<C>?, Array<List<C>>>?", get2<List<C>>().toString())
    return "OK"
}
