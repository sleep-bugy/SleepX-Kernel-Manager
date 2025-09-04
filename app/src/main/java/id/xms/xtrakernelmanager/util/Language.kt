package id.xms.xtrakernelmanager.util

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    INDONESIAN("id", "Indonesia");

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
