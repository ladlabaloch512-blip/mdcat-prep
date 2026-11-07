package com.abiz.mdcatprep

import org.json.JSONArray
import org.json.JSONObject

/** JSON parsing helpers and lightweight models mirroring the API contract. */

fun JSONArray?.toList(): List<JSONObject> {
    val out = ArrayList<JSONObject>()
    if (this == null) return out
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}

fun JSONObject.str(key: String, fallback: String = ""): String {
    val v = opt(key)
    return if (v == null || v == JSONObject.NULL) fallback else v.toString()
}

fun JSONObject.intOr(key: String, fallback: Int = 0): Int = optInt(key, fallback)

data class Banner(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val targetLink: String,
    val placement: String
) {
    companion object {
        fun from(j: JSONObject) = Banner(
            j.optInt("id"), j.str("title"), j.str("image_url"),
            j.str("target_link"), j.str("placement")
        )
    }
}

data class Subject(
    val id: Int,
    val name: String,
    val tagline: String,
    val icon: String,
    val color: String,
    val chapterCount: Int,
    val questionCount: Int,
    val attempted: Int,
    val correct: Int,
    val percent: Int
) {
    companion object {
        fun from(j: JSONObject) = Subject(
            j.optInt("id"), j.str("name"), j.str("tagline"), j.str("icon"), j.str("color"),
            j.optInt("chapter_count"), j.optInt("question_count"),
            j.optInt("attempted"), j.optInt("correct"), j.optInt("percent")
        )
    }
}

data class Chapter(
    val id: Int,
    val subjectId: Int,
    val title: String,
    val subtopics: String,
    val questionCount: Int,
    val attempted: Int,
    val correct: Int,
    val percent: Int,
    val isWeak: Boolean
) {
    companion object {
        fun from(j: JSONObject) = Chapter(
            j.optInt("id"), j.optInt("subject_id"), j.str("title"), j.str("subtopics"),
            j.optInt("question_count"), j.optInt("attempted"), j.optInt("correct"),
            j.optInt("percent"), j.optBoolean("is_weak")
        )
    }
}

data class Question(
    val id: Int,
    val chapterId: Int,
    val subtopic: String,
    val text: String,
    val options: Map<String, String>,
    var isSaved: Boolean
) {
    companion object {
        fun from(j: JSONObject): Question {
            val opts = j.optJSONObject("options") ?: JSONObject()
            return Question(
                j.optInt("id"), j.optInt("chapter_id"), j.str("subtopic"), j.str("question"),
                mapOf(
                    "A" to opts.optString("A", ""), "B" to opts.optString("B", ""),
                    "C" to opts.optString("C", ""), "D" to opts.optString("D", "")
                ),
                j.optBoolean("is_saved")
            )
        }
    }
}

/** Holds the last quiz result in memory so ResultActivity can render it
 *  without passing a large JSON payload through an Intent. */
object QuizSession {
    var lastResult: JSONObject? = null
    var lastQuizType: String = "chapter"
    var lastChapterId: Int = 0
}
