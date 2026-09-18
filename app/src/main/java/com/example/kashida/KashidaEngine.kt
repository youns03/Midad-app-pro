package com.example.kashida

import com.example.model.KashidaLevel

/**
 * Intelligent Arabic Kashida (Tatweel) Engine.
 * Follows classical Arabic typographic rules and distributes extensions
 * harmoniously across words and lines.
 */
object KashidaEngine {

    const val TATWEEL = '\u0640' // ـ

    // Arabic letters that DO NOT connect to the subsequent letter (حروف الانفصال)
    private val NON_CONNECTING_FORWARD: Set<Char> = setOf(
        'ا', 'أ', 'إ', 'آ', 'ء', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ة', 'ى',
        '\u0671', // Wasla Alef
        '\u0672', '\u0673', '\u0675', '\u0688', '\u068c', '\u068d', '\u068e', '\u0698', '\u06c6', '\u06c7', '\u06c8', '\u06cb', '\u06cf'
    )

    // Arabic diacritics / Tashkeel
    private val DIACRITICS: Set<Char> = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', '\u0670'
    )

    /**
     * Checks if a character is an Arabic letter.
     */
    fun isArabicLetter(c: Char): Boolean {
        return (c in '\u0621'..'\u064A') || (c in '\u0671'..'\u06D3')
    }

    /**
     * Removes all tatweel characters from the given text.
     */
    fun stripKashida(text: String): String {
        return text.replace(TATWEEL.toString(), "")
    }

    /**
     * Checks whether a tatweel can be placed between char at index [pos] and the next letter.
     */
    fun canConnectForward(firstChar: Char, secondChar: Char): Boolean {
        if (!isArabicLetter(firstChar) || !isArabicLetter(secondChar)) return false
        if (firstChar in NON_CONNECTING_FORWARD) return false
        // Avoid breaking Lam-Alef ligature (لا, لأ, لإ, لآ)
        if (firstChar == 'ل' && (secondChar == 'ا' || secondChar == 'أ' || secondChar == 'إ' || secondChar == 'آ')) {
            return false
        }
        return true
    }

    /**
     * A connection point in an Arabic word where kashida can be added or adjusted.
     */
    data class ConnectionPoint(
        val indexInWord: Int,        // Index in clean word after which tatweel goes
        val firstChar: Char,
        val secondChar: Char,
        val currentTatweels: Int = 0,
        val aestheticScore: Int = 1  // Higher means preferred in traditional calligraphy
    )

    /**
     * Finds all valid connection points in a single Arabic word.
     */
    fun findConnectionPointsInWord(word: String): List<ConnectionPoint> {
        val cleanWord = stripKashida(word)
        val points = mutableListOf<ConnectionPoint>()
        if (cleanWord.length < 2) return points

        for (i in 0 until cleanWord.length - 1) {
            val c1 = cleanWord[i]
            val c2 = cleanWord[i + 1]

            if (canConnectForward(c1, c2)) {
                // Calculate aesthetic score based on calligraphy conventions
                var score = 1
                // Preferred before final letter
                if (i == cleanWord.length - 2) score += 3
                // Preferred after Seen/Sheen
                if (c1 == 'س' || c1 == 'ش') score += 2
                // Preferred after Saad/Daad/Taa/Zhaa
                if (c1 in setOf('ص', 'ض', 'ط', 'ظ')) score += 2
                // Preferred between Ba/Ta/Tha/Noon/Ya
                if (c1 in setOf('ب', 'ت', 'ث', 'ن', 'ي', 'ئ')) score += 1
                // Preferred after Fa/Qaf
                if (c1 == 'ف' || c1 == 'ق') score += 1

                points.add(
                    ConnectionPoint(
                        indexInWord = i,
                        firstChar = c1,
                        secondChar = c2,
                        aestheticScore = score
                    )
                )
            }
        }
        return points
    }

    /**
     * Applies automatic smart Kashida distribution across text.
     * When level is OFF or text is empty, returns original stripped text.
     * Distributes extensions gracefully across lines and words without overloading any single point.
     */
    fun applySmartKashida(text: String, level: KashidaLevel): String {
        if (level == KashidaLevel.OFF || text.isBlank()) {
            return stripKashida(text)
        }

        val lines = text.split("\n")
        val processedLines = lines.map { line ->
            processLineKashida(line, level)
        }
        return processedLines.joinToString("\n")
    }

    private fun processLineKashida(line: String, level: KashidaLevel): String {
        if (line.isBlank()) return line

        val cleanLine = stripKashida(line)
        val tokens = cleanLine.split(" ")
        if (tokens.isEmpty()) return line

        val wordsWithPoints = tokens.mapIndexed { wordIdx, word ->
            val points = findConnectionPointsInWord(word)
            Triple(wordIdx, word, points)
        }

        // Collect all available points with global scoring
        val candidatePoints = mutableListOf<Triple<Int, Int, ConnectionPoint>>() // (wordIdx, pointIdxInWord, point)
        for ((wordIdx, _, points) in wordsWithPoints) {
            for ((pointIdx, point) in points.withIndex()) {
                candidatePoints.add(Triple(wordIdx, pointIdx, point))
            }
        }

        if (candidatePoints.isEmpty()) return cleanLine

        // Sort points by calligraphy aesthetic preference
        candidatePoints.sortByDescending { it.third.aestheticScore }

        // Determine how many points to extend based on level and line length
        val pointsToExtend = when (level) {
            KashidaLevel.OFF -> 0
            KashidaLevel.LIGHT -> (tokens.size / 3).coerceIn(1, 3)
            KashidaLevel.MEDIUM -> (tokens.size / 2).coerceIn(2, 5)
            KashidaLevel.HEAVY -> (tokens.size * 2 / 3).coerceIn(3, 8)
        }

        val tatweelsPerPoint = level.tatweelCount
        val tatweelInsertionMap = mutableMapOf<Pair<Int, Int>, Int>() // (wordIdx, indexInWord) -> count

        // Distribute to top scored candidates, ensuring words aren't over-extended
        val selectedWords = mutableSetOf<Int>()
        var count = 0
        for (candidate in candidatePoints) {
            val wordIdx = candidate.first
            val indexInWord = candidate.third.indexInWord

            // Avoid putting multiple kashidas in the same short word
            if (selectedWords.contains(wordIdx) && tokens[wordIdx].length <= 5) {
                continue
            }

            tatweelInsertionMap[Pair(wordIdx, indexInWord)] = tatweelsPerPoint
            selectedWords.add(wordIdx)
            count++
            if (count >= pointsToExtend) break
        }

        // Reconstruct line with kashida
        val reconstructedWords = tokens.mapIndexed { wordIdx, word ->
            val sb = StringBuilder()
            for (charIdx in word.indices) {
                sb.append(word[charIdx])
                val added = tatweelInsertionMap[Pair(wordIdx, charIdx)] ?: 0
                repeat(added) {
                    sb.append(TATWEEL)
                }
            }
            sb.toString()
        }

        return reconstructedWords.joinToString(" ")
    }

    /**
     * Applies manual kashida count at a specific point in a word.
     */
    fun applyManualKashidaToWord(cleanWord: String, connectionIndex: Int, tatweelCount: Int): String {
        if (connectionIndex < 0 || connectionIndex >= cleanWord.length) return cleanWord
        val sb = StringBuilder()
        for (i in cleanWord.indices) {
            sb.append(cleanWord[i])
            if (i == connectionIndex) {
                repeat(tatweelCount.coerceIn(0, 10)) {
                    sb.append(TATWEEL)
                }
            }
        }
        return sb.toString()
    }
}
