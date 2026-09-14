package example

fun main() {
    // ソースには存在しないが、FIRで宣言されるため通常の関数として型検査・補完できる。
    val id = UserId.generate()
    // Companionを持っていても生成されること
    val tenantId = TenantId.generate()
    println(id.value)
    println(tenantId.value)
}
