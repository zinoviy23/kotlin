// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
fun box() {
    val y : ArrayList<String> = arrayOf()
    y[0] = listOf()
}