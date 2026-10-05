package com.example.data.parser

import android.util.Log
import com.example.data.model.ClassSlot
import com.example.data.model.DaySchedule
import com.example.data.model.ScheduleClass
import com.example.data.model.Section
import com.example.data.model.SlotStatus
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object XlsxParser {
    private const val TAG = "XlsxParser"

    data class ParsedSheet(
        val name: String,
        val rows: List<List<String>>
    )

    /**
     * Parses an XLSX input stream into a Map of sheetName to 2D list of cells.
     */
    fun parseXlsx(inputStream: InputStream): Map<String, List<List<String>>> {
        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val normalizedName = entry.name.replace('\\', '/').trimStart('/')
                    zipEntries[normalizedName] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 1. Parse Shared Strings
        val sharedStrings = parseSharedStrings(zipEntries["xl/sharedStrings.xml"])

        // 2. Parse Workbook to find Sheet names and their rId
        val sheetList = parseWorkbookSheets(zipEntries["xl/workbook.xml"])

        // 3. Parse Workbook Rels to map rId -> target sheet xml
        val relsMap = parseWorkbookRels(zipEntries["xl/_rels/workbook.xml.rels"])

        val resultSheets = mutableMapOf<String, List<List<String>>>()

        // 4. For each sheet, parse worksheet XML
        for ((sheetName, rId, index) in sheetList) {
            val targetXml = relsMap[rId] ?: "worksheets/sheet$index.xml"
            val fullPath = if (targetXml.startsWith("xl/")) targetXml else "xl/$targetXml"
            val sheetBytes = zipEntries[fullPath] ?: zipEntries["xl/worksheets/sheet$index.xml"]

            if (sheetBytes != null) {
                val rows = parseWorksheet(sheetBytes, sharedStrings)
                resultSheets[sheetName] = rows
            } else {
                Log.w(TAG, "Could not find worksheet xml for sheet: $sheetName (rId: $rId)")
            }
        }

        return resultSheets
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

            var eventType = parser.eventType
            var inSi = false
            var currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "si" -> {
                                inSi = true
                                currentText = StringBuilder()
                            }
                            "t" -> {
                                if (inSi) {
                                    currentText.append(parser.nextText())
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "si") {
                            list.add(currentText.toString())
                            inSi = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shared strings", e)
        }
        return list
    }

    data class SheetDef(val name: String, val rId: String, val index: Int)

    private fun parseWorkbookSheets(bytes: ByteArray?): List<SheetDef> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<SheetDef>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

            var eventType = parser.eventType
            var index = 1

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet$index"
                    var rId = parser.getAttributeValue(
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                        "id"
                    )
                    if (rId == null) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i).endsWith("id")) {
                                rId = parser.getAttributeValue(i)
                                break
                            }
                        }
                    }
                    list.add(SheetDef(name, rId ?: "rId$index", index))
                    index++
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing workbook sheets", e)
        }
        return list
    }

    private fun parseWorkbookRels(bytes: ByteArray?): Map<String, String> {
        if (bytes == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
                        map[id] = target
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing workbook rels", e)
        }
        return map
    }

    private fun parseWorksheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rowsMap = mutableMapOf<Int, MutableMap<Int, String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

            var eventType = parser.eventType
            var currentRow = 0
            var currentCol = 0
            var currentCellType = ""
            var currentCellValue = StringBuilder()
            var inV = false
            var inInlineStr = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> {
                                val rStr = parser.getAttributeValue(null, "r")
                                currentRow = (rStr?.toIntOrNull() ?: (currentRow + 1)) - 1
                                if (currentRow < 0) currentRow = 0
                            }
                            "c" -> {
                                val rRef = parser.getAttributeValue(null, "r")
                                currentCol = if (rRef != null) colRefToIndex(rRef) else currentCol + 1
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                currentCellValue = StringBuilder()
                            }
                            "v" -> inV = true
                            "is" -> inInlineStr = true
                            "t" -> {
                                if (inInlineStr) {
                                    currentCellValue.append(parser.nextText())
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV) {
                            currentCellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v" -> inV = false
                            "is" -> inInlineStr = false
                            "c" -> {
                                val textVal = when (currentCellType) {
                                    "s" -> {
                                        val idx = currentCellValue.toString().trim().toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) {
                                            sharedStrings[idx]
                                        } else {
                                            ""
                                        }
                                    }
                                    else -> currentCellValue.toString().trim()
                                }
                                val rowCells = rowsMap.getOrPut(currentRow) { mutableMapOf() }
                                rowCells[currentCol] = textVal
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing worksheet", e)
        }

        // Convert rowsMap to dense 2D list
        if (rowsMap.isEmpty()) return emptyList()
        val maxRow = rowsMap.keys.maxOrNull() ?: 0
        val denseRows = mutableListOf<List<String>>()

        for (r in 0..maxRow) {
            val cols = rowsMap[r]
            if (cols == null) {
                denseRows.add(emptyList())
            } else {
                val maxCol = cols.keys.maxOrNull() ?: 0
                val rowList = mutableListOf<String>()
                for (c in 0..maxCol) {
                    rowList.add(cols[c] ?: "")
                }
                denseRows.add(rowList)
            }
        }
        return denseRows
    }

    private fun colRefToIndex(ref: String): Int {
        var letters = ""
        for (ch in ref) {
            if (ch.isLetter()) {
                letters += ch.uppercaseChar()
            } else {
                break
            }
        }
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }

    private fun isHolidayOrSpecialEvent(text: String): Boolean {
        if (text.isBlank()) return false
        val upper = text.uppercase()
        return upper.contains("HOLIDAY") ||
            upper.contains("DUSSEHRA") ||
            upper.contains("DIWALI") ||
            upper.contains("CHATTH") ||
            upper.contains("GURUNANAK") ||
            upper.contains("ID-E-MILAD") ||
            upper.contains("GYANODAYA") ||
            upper.contains("NETRITVA") ||
            upper.contains("TEDX") ||
            upper.contains("ASSESSMENTS") ||
            upper.contains("MID TERM") ||
            upper.contains("END TERM")
    }

    /**
     * Converts the parsed paired-row spreadsheet into DaySchedule objects
     * Row 1: Date, Day, Slot1(Course-Session), Slot2, Slot3, Slot4, Slot5
     * Row 2: ,, Slot1(Faculty), Slot2, Slot3, Slot4, Slot5
     */
    fun convertRowsToDaySchedules(rows: List<List<String>>): List<DaySchedule> {
        val result = mutableListOf<DaySchedule>()
        if (rows.isEmpty()) return result

        // Header is row 0
        // Find column indexes
        var dateCol = 0
        var dayCol = 1

        val slotColIndices = mutableListOf<Pair<ClassSlot, Int>>()
        val header = rows.firstOrNull() ?: emptyList()
        for ((idx, col) in header.withIndex()) {
            val clean = col.trim().replace(" ", "")
            if (clean.equals("date", ignoreCase = true)) dateCol = idx
            if (clean.equals("day", ignoreCase = true)) dayCol = idx
            for (slot in ClassSlot.ALL_SLOTS) {
                if (clean.contains(slot.timeRange.replace(" ", "")) || clean.contains(slot.formattedStartTime)) {
                    slotColIndices.add(slot to idx)
                }
            }
        }

        // Default to standard indexes if header didn't match
        if (slotColIndices.isEmpty()) {
            ClassSlot.ALL_SLOTS.forEachIndexed { i, slot ->
                slotColIndices.add(slot to (i + 2))
            }
        }

        var r = 1
        while (r < rows.size) {
            val row1 = rows[r]
            val row2 = if (r + 1 < rows.size) rows[r + 1] else emptyList()

            val dateVal = row1.getOrNull(dateCol)?.trim() ?: ""
            val dayVal = row1.getOrNull(dayCol)?.trim() ?: ""

            if (dateVal.isNotBlank() || dayVal.isNotBlank()) {
                // Check if the whole day is a holiday/event (e.g. ID-E-MILAD, GYANODAYA, END TERM EXAMINATIONS)
                var isHoliday = false
                var holidayTitle = ""

                // Check all cells for known holiday/special event strings
                for (slotIdx in 2 until row1.size) {
                    val c = row1.getOrNull(slotIdx)?.trim() ?: ""
                    if (isHolidayOrSpecialEvent(c)) {
                        isHoliday = true
                        holidayTitle = c
                        break
                    }
                }

                val classes = mutableListOf<ScheduleClass>()
                for ((slot, colIdx) in slotColIndices) {
                    val rawCourse = row1.getOrNull(colIdx)?.trim() ?: ""
                    val rawFaculty = row2.getOrNull(colIdx)?.trim() ?: ""

                    val isSlotHoliday = isHoliday || isHolidayOrSpecialEvent(rawCourse)

                    val slotHolidayTitle = if (isSlotHoliday) {
                        if (rawCourse.isNotBlank() && isHolidayOrSpecialEvent(rawCourse)) rawCourse
                        else if (holidayTitle.isNotBlank()) holidayTitle
                        else "Holiday / Event"
                    } else ""

                    // Split course name and session number (e.g. "Marketing Management I - 15")
                    var courseName = rawCourse
                    var sessionNo = ""
                    if (!isSlotHoliday && rawCourse.contains(" - ")) {
                        val parts = rawCourse.split(" - ")
                        courseName = parts[0].trim()
                        sessionNo = parts.getOrNull(1)?.trim() ?: ""
                    }

                    classes.add(
                        ScheduleClass(
                            dateStr = dateVal,
                            dayOfWeek = dayVal,
                            slot = slot,
                            courseName = if (isSlotHoliday) "" else courseName,
                            sessionNumber = if (isSlotHoliday) "" else sessionNo,
                            facultyName = if (isSlotHoliday) "" else rawFaculty,
                            isHoliday = isSlotHoliday,
                            holidayName = slotHolidayTitle,
                            status = if (isSlotHoliday) SlotStatus.HOLIDAY else SlotStatus.UPCOMING
                        )
                    )
                }

                result.add(
                    DaySchedule(
                        dateStr = dateVal,
                        dayName = dayVal,
                        isHoliday = isHoliday,
                        holidayTitle = holidayTitle,
                        classes = classes
                    )
                )
            }
            r += 2
        }

        return result
    }
}
