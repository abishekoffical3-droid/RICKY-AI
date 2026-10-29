package com.example.ui.tools

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.tools.CsvAnalysisResult
import com.example.feature.tools.CsvAnalyzer
import com.example.feature.tools.ReminderScheduler
import com.example.feature.tools.SafeCalculator
import com.example.ui.components.CodeBlock
import com.example.ui.components.MarkdownText
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🧮 Calculator", "✍️ Writing", "💻 Coding", "📊 Data CSV", "⏰ Reminder")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RICKY AI Tools Hub", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tools_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> CalculatorTool()
                    1 -> WritingTool(viewModel)
                    2 -> CodingTool(viewModel)
                    3 -> CsvTool()
                    4 -> ReminderTool()
                }
            }
        }
    }
}

@Composable
fun CalculatorTool() {
    var expr by remember { mutableStateOf("sqrt(144) + 25 * 4 - (10 / 2)") }
    var resultText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Safe Mathematical Expression Evaluator", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "Supports operators (+, -, *, /, %, ^), parentheses, and functions (sqrt, sin, cos, tan, log, ln, abs).",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = expr,
            onValueChange = { expr = it },
            label = { Text("Math Expression") },
            modifier = Modifier.fillMaxWidth().testTag("calc_input_field"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val res = SafeCalculator.evaluate(expr)
                resultText = when (res) {
                    is SafeCalculator.CalcResult.Success -> "Result = ${res.formattedResult}"
                    is SafeCalculator.CalcResult.Error -> "Error: ${res.message}"
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("calc_evaluate_button"),
            colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Calculate", fontWeight = FontWeight.Bold)
        }

        if (resultText.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant)
            ) {
                Text(
                    text = resultText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = RickyCyan,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Quick Examples:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        val examples = listOf(
            "sqrt(256) + 12^2",
            "sin(30) + cos(60)",
            "log(1000) * 5 + abs(-45)",
            "1500 * (1 - 0.15) / 12"
        )
        for (ex in examples) {
            TextButton(onClick = {
                expr = ex
                val res = SafeCalculator.evaluate(ex)
                resultText = when (res) {
                    is SafeCalculator.CalcResult.Success -> "Result = ${res.formattedResult}"
                    is SafeCalculator.CalcResult.Error -> "Error: ${res.message}"
                }
            }) {
                Text(ex, color = RickyCyan, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun WritingTool(viewModel: ChatViewModel) {
    var textInput by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf("Rewrite & Polish") }
    var outputText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    val tasks = listOf("Rewrite & Polish", "Draft Article", "Summarize Key Points", "Translate to Nepali", "Translate to Hindi", "Professional Tone")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Writing Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Draft, rewrite, proofread, and translate between Nepali, English, and Hindi.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Task:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            for (t in tasks) {
                FilterChip(
                    selected = selectedTask == t,
                    onClick = { selectedTask = t },
                    label = { Text(t, fontSize = 12.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Enter your text, notes, or prompt here...") },
            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("writing_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (textInput.isBlank()) return@Button
                isRunning = true
                viewModel.sendMessage("Writing Task: [$selectedTask]\n\n$textInput")
                outputText = "Task sent to RICKY AI conversation! Switching to chat will stream your result."
                isRunning = false
            },
            modifier = Modifier.fillMaxWidth().testTag("writing_submit_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = RickyPurple)
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Process with RICKY AI")
        }

        if (outputText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant)
            ) {
                Text(outputText, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CodingTool(viewModel: ChatViewModel) {
    var codeInput by remember { mutableStateOf("fun fibonacci(n: Int): Int {\n    if (n <= 1) return n\n    return fibonacci(n - 1) + fibonacci(n - 2)\n}") }
    var selectedAction by remember { mutableStateOf("Explain Code") }

    val actions = listOf("Explain Code", "Debug & Find Bugs", "Generate Unit Tests", "Optimize Performance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Coding & Software Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Analyze syntax, explain errors, debug code, and generate Robolectric/Unit tests.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            for (act in actions) {
                FilterChip(
                    selected = selectedAction == act,
                    onClick = { selectedAction = act },
                    label = { Text(act, fontSize = 12.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = codeInput,
            onValueChange = { codeInput = it },
            placeholder = { Text("Paste code snippet here...") },
            modifier = Modifier.fillMaxWidth().height(160.dp).testTag("code_input"),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.sendMessage("Code Assistant: [$selectedAction]\n\n```\n$codeInput\n```")
            },
            modifier = Modifier.fillMaxWidth().testTag("code_submit_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Analyze in Chat", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CsvTool() {
    var csvText by remember { mutableStateOf("Name, Age, Score, Department\nAbhishek, 22, 94.5, AI Engineering\nNirajan, 24, 88.0, Data Science\nPooja, 21, 91.2, Android\nBipin, 23, 85.0, Cloud") }
    var analysisResult by remember { mutableStateOf<CsvAnalysisResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Data Analysis & CSV Inspector", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Parse tabular data, extract statistical metrics (mean, sum, min, max), and render structured previews.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = csvText,
            onValueChange = { csvText = it },
            label = { Text("CSV Text Data") },
            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("csv_input"),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val res = CsvAnalyzer.analyze(csvText)
                if (res.isSuccess) {
                    analysisResult = res.getOrNull()
                    errorMsg = null
                } else {
                    errorMsg = res.exceptionOrNull()?.message
                    analysisResult = null
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("csv_analyze_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Analyze CSV Data", fontWeight = FontWeight.Bold)
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        val result = analysisResult
        if (result != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Summary: ${result.rowCount} rows, ${result.columnNames.size} columns", fontWeight = FontWeight.Bold, color = RickyCyan, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    for ((col, stats) in result.numericalSummaries) {
                        Text(
                            text = "• $col: Mean = ${"%.2f".format(stats.mean)}, Min = ${stats.min}, Max = ${stats.max}, Sum = ${stats.sum}",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Preview Rows:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column {
                    // Header row
                    Row(modifier = Modifier.background(Color(0xFF1E293B)).padding(6.dp)) {
                        for (col in result.columnNames) {
                            Text(col, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RickyCyan, modifier = Modifier.width(100.dp))
                        }
                    }
                    for (row in result.previewRows) {
                        Row(modifier = Modifier.padding(6.dp)) {
                            for (cell in row) {
                                Text(cell, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderTool() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Study AI Algorithms") }
    var message by remember { mutableStateOf("Review RICKY AI architecture and test local models") }
    var delayMinutes by remember { mutableIntStateOf(5) }
    var statusText by remember { mutableStateOf("") }

    val scheduler = remember { ReminderScheduler(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Device Reminders & Timers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Schedule alarms and system notifications via documented Android APIs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Reminder Title") },
            modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message / Description") },
            modifier = Modifier.fillMaxWidth().testTag("reminder_msg_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Trigger in:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1 to "1 min", 5 to "5 min", 15 to "15 min", 60 to "1 hr").forEach { (min, label) ->
                FilterChip(
                    selected = delayMinutes == min,
                    onClick = { delayMinutes = min },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val triggerAt = System.currentTimeMillis() + delayMinutes * 60 * 1000L
                val res = scheduler.scheduleReminder(title, message, triggerAt)
                statusText = if (res.isSuccess) {
                    "✅ Reminder scheduled for $delayMinutes minutes from now!"
                } else {
                    "❌ Failed to schedule: ${res.exceptionOrNull()?.message}"
                }
                Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().testTag("schedule_reminder_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Alarm, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Schedule Alarm Notification", fontWeight = FontWeight.Bold)
        }

        if (statusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusText, fontWeight = FontWeight.SemiBold, color = RickyCyan, fontSize = 13.sp)
        }
    }
}
