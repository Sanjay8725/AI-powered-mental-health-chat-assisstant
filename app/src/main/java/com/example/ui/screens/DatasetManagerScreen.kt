package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.DatasetItemEntity
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.EmotionAnxiety
import com.example.ui.theme.EmotionCalm
import com.example.ui.theme.EmotionJoy
import com.example.ui.theme.EmotionSadness
import com.example.ui.theme.EmotionStress
import com.example.ui.theme.MindCareTeal
import com.example.ui.viewmodel.DatasetUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatasetManagerScreen(
    datasetItems: List<DatasetItemEntity>,
    datasetCount: Int,
    datasetUiState: DatasetUiState,
    isSupabaseConfigured: Boolean,
    onUploadCsvFile: (Uri) -> Unit,
    onUploadCsvContent: (String) -> Unit,
    onClearDataset: () -> Unit,
    onExportDataset: () -> Unit,
    onTriggerRealtimeSync: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit
) {
    var showPasteDialog by remember { mutableStateOf(false) }
    var showFormatInfoDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadCsvFile(uri)
        }
    }

    val categories = listOf("ALL", "anxiety", "coping", "burnout", "sleep", "crisis")

    val filteredItems = remember(datasetItems, datasetUiState.searchQuery, datasetUiState.filterCategory) {
        datasetItems.filter { item ->
            val matchesCategory = datasetUiState.filterCategory == "ALL" || item.category.equals(datasetUiState.filterCategory, ignoreCase = true)
            val matchesQuery = datasetUiState.searchQuery.isEmpty() ||
                    item.prompt.contains(datasetUiState.searchQuery, ignoreCase = true) ||
                    item.response.contains(datasetUiState.searchQuery, ignoreCase = true) ||
                    item.category.contains(datasetUiState.searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MindCareTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dataset & Knowledge Hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("$datasetCount benchmark entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onTriggerRealtimeSync,
                        modifier = Modifier.testTag("sync_supabase_realtime_button")
                    ) {
                        if (datasetUiState.isRealtimeSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync Supabase Realtime", tint = MindCareTeal)
                        }
                    }
                    IconButton(
                        onClick = { showFormatInfoDialog = true },
                        modifier = Modifier.testTag("dataset_format_info_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Format Info")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Upload Callout & Question Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("dataset_upload_hero_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Upload CSV Dataset",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    if (isSupabaseConfigured) "Connected to Supabase Realtime" else "Local & Supabase Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Have your own mental health, intent, or emotion dataset in CSV format? Upload it to expand MindCare AI's grounded RAG responses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { filePicker.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).testTag("select_csv_file_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload File")
                            }

                            OutlinedButton(
                                onClick = { showPasteDialog = true },
                                modifier = Modifier.weight(1f).testTag("paste_csv_text_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste CSV")
                            }
                        }
                    }
                }
            }

            // 2. Parse Stats or Error banner
            if (datasetUiState.uploadError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrisisRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrisisRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(datasetUiState.uploadError, style = MaterialTheme.typography.bodySmall, color = CrisisRed)
                        }
                    }
                }
            }

            if (datasetUiState.lastImportStats != null) {
                item {
                    val stats = datasetUiState.lastImportStats
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MindCareTeal.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("import_stats_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MindCareTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CSV Parsing Completed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MindCareTeal)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Total rows: ${stats.totalRows} • Valid: ${stats.validRows} • Skipped: ${stats.skippedRows}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 3. Search and Category Filter
            item {
                Column {
                    OutlinedTextField(
                        value = datasetUiState.searchQuery,
                        onValueChange = onSearchChange,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search dataset by intent, response, keyword...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dataset_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MindCareTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = datasetUiState.filterCategory.equals(cat, ignoreCase = true),
                                onClick = { onCategoryFilterChange(cat) },
                                label = { Text(cat.uppercase(), fontSize = 11.sp) },
                                modifier = Modifier.testTag("dataset_category_chip_$cat")
                            )
                        }
                    }
                }
            }

            // 4. Dataset Actions: Clear & Export
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredItems.size} items displayed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onExportDataset,
                            modifier = Modifier.testTag("export_dataset_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }

                        TextButton(
                            onClick = onClearDataset,
                            colors = ButtonDefaults.textButtonColors(contentColor = CrisisRed),
                            modifier = Modifier.testTag("clear_dataset_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }

            // 5. Items list
            items(filteredItems, key = { it.id }) { item ->
                DatasetItemCard(item = item)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Paste CSV Text Dialog
    if (showPasteDialog) {
        var pasteText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showPasteDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("paste_csv_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Paste CSV Dataset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Expected columns: prompt,response,category,emotion,safety_level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("paste_csv_text_field"),
                        placeholder = { Text("prompt,response,category,emotion,safety_level\n\"How to stop panic?\",\"Breathe in for 4s...\",anxiety,anxiety,safe") },
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                pasteText = """
prompt,response,category,emotion,safety_level
"I feel anxious about social events","Practice gentle exposure. Set an intention to stay 15 minutes before deciding.",coping,anxiety,safe
"I am feeling burnt out at work","Rest is productive. Set digital boundaries after hours.",burnout,stress,safe
"I have severe chest tightness and panic","This will pass. Sit down, place hands on your belly, and breathe slow.",anxiety,anxiety,moderate_risk
                                """.trimIndent()
                            }
                        ) {
                            Text("Insert Sample")
                        }

                        Row {
                            TextButton(onClick = { showPasteDialog = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (pasteText.isNotBlank()) {
                                        onUploadCsvContent(pasteText)
                                        showPasteDialog = false
                                    }
                                },
                                enabled = pasteText.isNotBlank()
                            ) {
                                Text("Import")
                            }
                        }
                    }
                }
            }
        }
    }

    // Format Info Dialog
    if (showFormatInfoDialog) {
        Dialog(onDismissRequest = { showFormatInfoDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("CSV Dataset Schema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "MindCare AI accepts standard CSV format up to 10MB. The recommended header row is:\n\n" +
                                "• prompt: User input or question\n" +
                                "• response: Supportive, non-diagnostic answer\n" +
                                "• category: anxiety, coping, burnout, sleep, crisis\n" +
                                "• emotion: joy, sadness, anxiety, stress, anger, neutral\n" +
                                "• safety_level: safe, moderate_risk, crisis\n\n" +
                                "If headers differ, columns are automatically mapped.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showFormatInfoDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Understood")
                    }
                }
            }
        }
    }
}

@Composable
fun DatasetItemCard(item: DatasetItemEntity) {
    val emotionColor = when (item.emotion.lowercase()) {
        "joy" -> EmotionJoy
        "calm" -> EmotionCalm
        "anxiety" -> EmotionAnxiety
        "stress" -> EmotionStress
        "sadness" -> EmotionSadness
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isCrisis = item.safetyLevel == "crisis"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCrisis) CrisisRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(emotionColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            item.emotion,
                            style = MaterialTheme.typography.labelSmall,
                            color = emotionColor
                        )
                    }
                }

                if (isCrisis) {
                    Text(
                        "CRISIS PROTOCOL",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrisisRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Q: ${item.prompt}",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.response,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Source: ${item.source}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
