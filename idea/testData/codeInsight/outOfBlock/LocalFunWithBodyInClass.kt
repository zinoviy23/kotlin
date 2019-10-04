// FALSE
class LocalFunWithBodyInClass {
  fun test() {
    fun hello() {
      <caret>
    }
  }
}