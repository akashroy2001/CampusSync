package com.example.data.parser

import android.util.Log
import com.example.data.model.ClassSlot
import com.example.data.model.DaySchedule
import com.example.data.model.ScheduleClass
import com.example.data.model.SlotStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    private const val TAG = "CsvParser"

    fun parseCsv(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val tokens = parseCsvLine(line)
                    rows.add(tokens)
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV", e)
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Converts paired-row CSV data into a list of DaySchedule
     */
    fun convertCsvRowsToDaySchedules(rows: List<List<String>>): List<DaySchedule> {
        return XlsxParser.convertRowsToDaySchedules(rows)
    }
}
