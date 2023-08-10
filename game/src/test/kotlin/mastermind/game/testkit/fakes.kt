package mastermind.game.testkit

import java.lang.reflect.Proxy

/**
 * Taken from https://github.com/dmcg/gilded-rose-tdd/blob/407769aae411cd207f7768af4a349a4d100715c7/src/test/java/com/gildedrose/testing/faking.kt
 */
inline fun <reified T> fake(): T =
    Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, _, _ ->
        TODO("not implemented")
    } as T
