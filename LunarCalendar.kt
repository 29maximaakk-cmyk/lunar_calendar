// LunarCalendar.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter

val ZODIAC_SIGNS = listOf("Овен","Телец","Близнецы","Рак","Лев","Дева",
                          "Весы","Скорпион","Стрелец","Козерог","Водолей","Рыбы")

val RECOMMENDATIONS = mapOf(
    "новолуние" to mapOf("посадка" to "неблагоприятно", "полив" to "допустимо", "обрезка" to "неблагоприятно", "урожай" to "неблагоприятно", "вредители" to "благоприятно"),
    "первая четверть" to mapOf("посадка" to "благоприятно", "полив" to "благоприятно", "обрезка" to "неблагоприятно", "урожай" to "неблагоприятно", "вредители" to "неблагоприятно"),
    "полнолуние" to mapOf("посадка" to "неблагоприятно", "полив" to "допустимо", "обрезка" to "благоприятно", "урожай" to "благоприятно", "вредители" to "благоприятно"),
    "последняя четверть" to mapOf("посадка" to "неблагоприятно", "полив" to "неблагоприятно", "обрезка" to "благоприятно", "урожай" to "благоприятно", "вредители" to "благоприятно")
)

data class LunarData(
    val date: String,
    val lunarDay: Int,
    val phase: String,
    val zodiacSign: String,
    val recommendations: Map<String, String>
)

fun calculate(date: LocalDate): LunarData {
    val base = Instant.parse("2000-01-06T18:14:00Z")
    val target = date.atStartOfDay(ZoneOffset.UTC).toInstant()
    val diff = Duration.between(base, target).toSeconds() / 86400.0
    val lunarAge = diff % 29.53058867
    var lunarDay = lunarAge.toInt() + 1
    if (lunarDay > 30) lunarDay = 30
    val phase = when {
        lunarDay <= 1 || lunarDay > 29 -> "новолуние"
        lunarDay <= 7 -> "первая четверть"
        lunarDay <= 14 -> "полнолуние"
        lunarDay <= 22 -> "последняя четверть"
        else -> "новолуние"
    }
    val start = Instant.parse("2000-01-01T00:00:00Z")
    val totalDays = Duration.between(start, target).toSeconds() / 86400.0
    val longitude = (180 + totalDays * 13.176) % 360
    val signIndex = (longitude / 30).toInt() % 12
    val sign = ZODIAC_SIGNS[signIndex]
    val recs = RECOMMENDATIONS[phase] ?: emptyMap()
    return LunarData(
        date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
        lunarDay = lunarDay,
        phase = phase,
        zodiacSign = sign,
        recommendations = recs
    )
}

fun printData(data: LunarData, verbose: Boolean, color: Boolean) {
    if (color) {
        println("\u001B[36m🌙 Лунный календарь на ${data.date}\u001B[0m")
        println("\u001B[33mЛунный день: ${data.lunarDay}\u001B[0m")
        println("\u001B[35mФаза: ${data.phase}\u001B[0m")
        println("\u001B[32mЗнак зодиака Луны: ${data.zodiacSign}\u001B[0m")
        if (verbose && data.recommendations.isNotEmpty()) {
            println("\u001B[37mРекомендации:\u001B[0m")
            data.recommendations.forEach { (k, v) ->
                val col = when (v) {
                    "благоприятно" -> "\u001B[32m"
                    "неблагоприятно" -> "\u001B[31m"
                    else -> "\u001B[33m"
                }
                println("  - $k: $col$v\u001B[0m")
            }
        }
    } else {
        println("🌙 Лунный календарь на ${data.date}")
        println("Лунный день: ${data.lunarDay}")
        println("Фаза: ${data.phase}")
        println("Знак зодиака Луны: ${data.zodiacSign}")
        if (verbose && data.recommendations.isNotEmpty()) {
            println("Рекомендации:")
            data.recommendations.forEach { (k, v) -> println("  - $k: $v") }
        }
    }
}

fun main(args: Array<String>) {
    val jc = JCommander.newBuilder().addObject(object {
        @Parameter(names = ["--date"])
        var dateStr: String? = null
        @Parameter(names = ["--json"])
        var jsonFile: String? = null
        @Parameter(names = ["--csv"])
        var csvFile: String? = null
        @Parameter(names = ["--verbose"])
        var verbose: Boolean = false
        @Parameter(names = ["--no-color"])
        var noColor: Boolean = false
    }).build()
    jc.parse(*args)

    val date = if (jc.getObject<Any>().dateStr != null) {
        LocalDate.parse(jc.getObject<Any>().dateStr)
    } else {
        LocalDate.now(ZoneOffset.UTC)
    }
    val data = calculate(date)
    val color = !(jc.getObject<Any>().noColor) && System.console() != null
    printData(data, jc.getObject<Any>().verbose, color)

    jc.getObject<Any>().jsonFile?.let { file ->
        val gson = GsonBuilder().setPrettyPrinting().create()
        File(file).writeText(gson.toJson(data))
        println("Результат сохранён в $file")
    }

    jc.getObject<Any>().csvFile?.let { file ->
        val recStr = data.recommendations.entries.joinToString("; ") { "${it.key}:${it.value}" }
        File(file).printWriter().use { pw ->
            pw.println("date,lunar_day,phase,zodiac_sign,recommendations")
            pw.println("${data.date},${data.lunarDay},${data.phase},${data.zodiacSign},\"$recStr\"")
        }
        println("Результат сохранён в $file")
    }
}
