package com.minimo.launcher.utils

import kotlin.math.min

/**
 * Typo-tolerant app search: "телеграмм" → Telegram, "whatsap" → WhatsApp, "ютуб" → YouTube.
 * Names and queries are compared in lowercase Latin (Cyrillic is transliterated), letters and
 * digits only, against the start of every word of the name.
 */
object FuzzySearch {
    private const val MIN_QUERY_LENGTH = 3

    private val translit = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "",
        'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya"
    )

    /** Latin, lowercase, only letters/digits, words separated by single spaces. */
    private fun normalize(text: String): String {
        val out = StringBuilder()
        for (char in text.lowercase()) {
            when {
                char in translit -> out.append(translit.getValue(char))
                char.isLetterOrDigit() -> out.append(char)
                else -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
            }
        }
        return out.toString().trim()
    }

    /**
     * How far [appName] is from [query] (0 = same letters, e.g. only a different alphabet),
     * or null when it is not a plausible typo. Short queries allow 1 mistake, longer ones 2.
     */
    fun distance(appName: String, query: String): Int? {
        val q = normalize(query).replace(" ", "")
        if (q.length < MIN_QUERY_LENGTH) return null
        val allowed = if (q.length <= 5) 1 else 2

        val name = normalize(appName)
        val joined = name.replace(" ", "")
        // Word starts of the name, and the whole name without spaces ("what sapp" → "whatsapp")
        val starts = buildList {
            add(joined to 0)
            var index = 0
            name.split(' ').forEach { word ->
                if (index > 0) add(name.replace(" ", "") to index)
                index += word.length
            }
        }

        var best = Int.MAX_VALUE
        for ((text, start) in starts) {
            for (length in (q.length - allowed)..(q.length + allowed)) {
                if (length <= 0 || start + length > text.length) continue
                best = min(best, editDistance(q, text.substring(start, start + length)))
                if (best == 0) return 0
            }
        }
        return best.takeIf { it <= allowed }
    }

    /** Damerau–Levenshtein (with neighbouring letters swapped counting as one mistake). */
    private fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
