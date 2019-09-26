// FALSE
class InFunInFunWithBodyInClass {
  fun test() {
    fun hello() {
      <caret>
    }
  }
}