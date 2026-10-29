package com.example.feature.tools

data class CsvAnalysisResult(
    val rowCount: Int,
    val columnNames: List<String>,
    val previewRows: List<List<String>>,
    val numericalSummaries: Map<String, ColumnStats>
)

data class ColumnStats(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double
)

object CsvAnalyzer {

    fun analyze(csvContent: String): Result<CsvAnalysisResult> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Result.failure(IllegalArgumentException("CSV content is empty"))
        }

        val headers = parseLine(lines[0])
        val rows = lines.drop(1).map { parseLine(it) }

        // Compute numerical statistics for numeric columns
        val statsMap = mutableMapOf<String, ColumnStats>()
        for (colIndex in headers.indices) {
            val colName = headers[colIndex]
            val numbers = mutableListOf<Double>()
            for (row in rows) {
                if (colIndex < row.size) {
                    val valueStr = row[colIndex].trim().replace("$", "").replace(",", "")
                    val num = valueStr.toDoubleOrNull()
                    if (num != null) {
                        numbers.add(num)
                    }
                }
            }
            if (numbers.isNotEmpty() && numbers.size >= rows.size / 2) {
                val sum = numbers.sum()
                val mean = sum / numbers.size
                val min = numbers.minOrNull() ?: 0.0
                val max = numbers.maxOrNull() ?: 0.0
                statsMap[colName] = ColumnStats(
                    count = numbers.size,
                    sum = sum,
                    mean = mean,
                    min = min,
                    max = max
                )
            }
        }

        return Result.success(
            CsvAnalysisResult(
                rowCount = rows.size,
                columnNames = headers,
                previewRows = rows.take(10),
                numericalSummaries = statsMap
            )
        )
    }

    private fun parseLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    values.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        values.add(current.toString().trim())
        return values
    }
}
