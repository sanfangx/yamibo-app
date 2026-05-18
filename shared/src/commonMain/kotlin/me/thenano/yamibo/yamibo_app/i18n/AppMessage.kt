package me.thenano.yamibo.yamibo_app.i18n

object AppMessage {
    private const val Prefix = "ymb-i18n:"
    private const val Separator = "\u001F"

    fun of(key: String, vararg args: Any?): String {
        if (args.isEmpty()) return Prefix + key
        return buildString {
            append(Prefix)
            append(key)
            args.forEach { arg ->
                append(Separator)
                append(arg?.toString().orEmpty())
            }
        }
    }

    fun parse(value: String): Parsed? {
        if (!value.startsWith(Prefix)) return null
        val payload = value.removePrefix(Prefix)
        val parts = payload.split(Separator)
        val key = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return Parsed(key = key, args = parts.drop(1))
    }

    data class Parsed(
        val key: String,
        val args: List<String>,
    )
}
