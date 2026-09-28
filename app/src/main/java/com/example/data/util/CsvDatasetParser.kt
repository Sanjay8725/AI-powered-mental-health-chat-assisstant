package com.example.data.util

import com.example.data.local.entity.DatasetItemEntity
import com.example.data.local.entity.KnowledgeResourceEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class CsvParseResult(
    val items: List<DatasetItemEntity>,
    val totalRows: Int,
    val validRows: Int,
    val skippedRows: Int,
    val errors: List<String>
)

object CsvDatasetParser {
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB limit

    fun parseCsv(inputStream: InputStream, sourceName: String = "user_upload.csv"): CsvParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val items = mutableListOf<DatasetItemEntity>()
        val errors = mutableListOf<String>()
        var totalRows = 0
        var validRows = 0
        var skippedRows = 0

        var headerMap = mapOf<String, Int>()
        var isFirstLine = true

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                totalRows++

                val tokens = parseCsvLine(trimmed)

                if (isFirstLine) {
                    isFirstLine = false
                    // Detect headers
                    headerMap = tokens.mapIndexed { index, col ->
                        col.lowercase().replace("\"", "").replace(" ", "_") to index
                    }.toMap()

                    // If first line wasn't headers, parse as first row
                    val hasKnownHeader = headerMap.keys.any {
                        it.contains("prompt") || it.contains("question") || it.contains("input") || it.contains("query")
                    }
                    if (hasKnownHeader) {
                        continue
                    }
                }

                // Map row columns
                try {
                    val promptIdx = headerMap["prompt"] ?: headerMap["question"] ?: headerMap["input"] ?: 0
                    val responseIdx = headerMap["response"] ?: headerMap["answer"] ?: headerMap["target"] ?: 1
                    val categoryIdx = headerMap["category"] ?: headerMap["intent"] ?: headerMap["topic"] ?: 2
                    val emotionIdx = headerMap["emotion"] ?: headerMap["sentiment"] ?: 3
                    val safetyIdx = headerMap["safety_level"] ?: headerMap["safety"] ?: headerMap["risk"] ?: 4

                    val prompt = tokens.getOrNull(promptIdx)?.cleanCsv() ?: ""
                    val response = tokens.getOrNull(responseIdx)?.cleanCsv() ?: ""

                    if (prompt.isBlank() || response.isBlank()) {
                        skippedRows++
                        if (errors.size < 5) {
                            errors.add("Row $totalRows: Prompt or response was empty.")
                        }
                        continue
                    }

                    val category = tokens.getOrNull(categoryIdx)?.cleanCsv()?.ifBlank { "wellness" } ?: "wellness"
                    val emotion = tokens.getOrNull(emotionIdx)?.cleanCsv()?.ifBlank { "neutral" } ?: "neutral"
                    val safety = tokens.getOrNull(safetyIdx)?.cleanCsv()?.ifBlank { "safe" } ?: "safe"

                    val item = DatasetItemEntity(
                        id = UUID.randomUUID().toString(),
                        prompt = prompt,
                        response = response,
                        category = category.lowercase(),
                        emotion = emotion.lowercase(),
                        safetyLevel = safety.lowercase(),
                        source = sourceName,
                        importedAt = System.currentTimeMillis()
                    )
                    items.add(item)
                    validRows++
                } catch (e: Exception) {
                    skippedRows++
                    if (errors.size < 5) {
                        errors.add("Row $totalRows parsing error: ${e.message}")
                    }
                }
            }
        }

        return CsvParseResult(
            items = items,
            totalRows = totalRows,
            validRows = validRows,
            skippedRows = skippedRows,
            errors = errors
        )
    }

    fun parseCsvText(csvText: String, sourceName: String = "pasted_dataset.csv"): CsvParseResult {
        return parseCsv(csvText.byteInputStream(Charsets.UTF_8), sourceName)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> {
                    inQuotes = !inQuotes
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> {
                    current.append(ch)
                }
            }
        }
        result.add(current.toString())
        return result
    }

    private fun String.cleanCsv(): String {
        return this.trim().removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
    }

    fun exportDatasetToCsv(items: List<DatasetItemEntity>): String {
        val sb = StringBuilder()
        sb.append("prompt,response,category,emotion,safety_level,source\n")
        items.forEach { item ->
            val p = "\"${item.prompt.replace("\"", "\"\"")}\""
            val r = "\"${item.response.replace("\"", "\"\"")}\""
            val c = "\"${item.category}\""
            val e = "\"${item.emotion}\""
            val s = "\"${item.safetyLevel}\""
            val src = "\"${item.source}\""
            sb.append("$p,$r,$c,$e,$s,$src\n")
        }
        return sb.toString()
    }

    // Pre-loaded IEEE-ready mental health psychoeducation knowledge base & dataset seeds
    fun getInitialKnowledgeResources(): List<KnowledgeResourceEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            KnowledgeResourceEntity(
                id = "kr_1",
                title = "5-4-3-2-1 Sensory Grounding Technique",
                category = "Grounding",
                content = "The 5-4-3-2-1 technique helps anchor your nervous system when experiencing acute panic or dissociation. Look around and name: 5 things you can see, 4 things you can physically touch (texture of clothing, cold desk), 3 things you can hear (clock ticking, distant breeze), 2 things you can smell, and 1 slow breath or taste. This redirects brain activity from the amygdala to the prefrontal cortex.",
                source = "National Institute of Mental Health (NIMH)",
                url = "https://www.nimh.nih.gov",
                tags = "anxiety,panic,grounding,mindfulness",
                updatedAt = now
            ),
            KnowledgeResourceEntity(
                id = "kr_2",
                title = "Box Breathing & Parasympathetic Regulation",
                category = "Breathing",
                content = "Box breathing activates the vagus nerve and downregulates physical stress responses. Inhale through the nose for 4 counts, hold your breath gently for 4 counts, exhale smoothly through the mouth for 4 counts, and hold empty for 4 counts. Repeating this cycle 4 times lowers cortisol and heart rate variability.",
                source = "Mayo Clinic Mind-Body Medicine",
                url = "https://www.mayoclinic.org",
                tags = "stress,breathing,panic,vagus_nerve",
                updatedAt = now
            ),
            KnowledgeResourceEntity(
                id = "kr_3",
                title = "Cognitive Reframing & Thought Challenging (CBT)",
                category = "Coping",
                content = "Cognitive reframing identifies distorted automatic thoughts like catastrophizing ('Everything is going wrong') or all-or-nothing thinking. Ask yourself: 1) What is factual evidence for this thought? 2) What is evidence against it? 3) What would I say to a dear friend in this situation? Replace it with a balanced, objective thought.",
                source = "Beck Institute for Cognitive Behavior Therapy",
                url = "https://beckinstitute.org",
                tags = "cbt,reframing,depression,overthinking",
                updatedAt = now
            ),
            KnowledgeResourceEntity(
                id = "kr_4",
                title = "Emergency Safety & Crisis Lifelines",
                category = "Crisis",
                content = "In situations of immediate emotional crisis, severe distress, or thoughts of self-harm: Call or text 988 (USA/Canada 24/7 Lifeline, free and confidential). Text HOME to 741741 (Crisis Text Line). International directory available at findahelpline.com or call emergency services (911 / 112). Connecting with trained human crisis counselors saves lives.",
                source = "988 Suicide & Crisis Lifeline / SAMHSA",
                url = "https://988lifeline.org",
                tags = "crisis,safety,emergency,hotline",
                updatedAt = now
            ),
            KnowledgeResourceEntity(
                id = "kr_5",
                title = "Sleep Hygiene and Restorative Wind-Down",
                category = "Sleep",
                content = "Consistent sleep schedules anchor circadian rhythm. Avoid blue-light screens 45 minutes before sleep. Maintain a cool bedroom environment (65°F / 18°C). If unable to fall asleep after 20 minutes, get out of bed and engage in a low-stimulation activity like dim reading rather than tossing and turning.",
                source = "Sleep Foundation Research",
                url = "https://www.sleepfoundation.org",
                tags = "sleep,insomnia,fatigue,wellness",
                updatedAt = now
            ),
            KnowledgeResourceEntity(
                id = "kr_6",
                title = "Compassionate Boundary Setting for Burnout",
                category = "Burnout",
                content = "Burnout stems from chronic nervous system depletion. Setting clear boundaries is protective, not selfish. Practice clear verbal scripts: 'I do not have capacity to take this on right now, but thank you for thinking of me.' Prioritize passive rest without digital overstimulation.",
                source = "American Psychological Association (APA)",
                url = "https://www.apa.org",
                tags = "burnout,workplace,boundaries,stress",
                updatedAt = now
            )
        )
    }

    fun getInitialDatasetItems(): List<DatasetItemEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            DatasetItemEntity(
                id = "ds_1",
                prompt = "I feel like I'm having a panic attack, my heart is pounding and I can't breathe",
                response = "I am right here with you. Panic attacks are terrifying, but they are a temporary surge of adrenaline that will pass safely. Place both feet flat on the floor. Take a slow breath in for 4 seconds, hold for 4, and let it out for 6 seconds. You are safe in this physical room right now.",
                category = "anxiety",
                emotion = "anxiety",
                safetyLevel = "moderate_risk",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            ),
            DatasetItemEntity(
                id = "ds_2",
                prompt = "I'm overwhelmed by work deadlines and feel like a total failure",
                response = "It makes complete sense that you feel overwhelmed when deadlines pile up, but please remember: your productivity does not define your worth as a human being. Let's break this down together. What is the single smallest task you can focus on for just 10 minutes?",
                category = "burnout",
                emotion = "stress",
                safetyLevel = "safe",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            ),
            DatasetItemEntity(
                id = "ds_3",
                prompt = "I feel so lonely and disconnected from everyone around me",
                response = "Loneliness is one of the heaviest emotions to carry, and feeling disconnected even among people is deeply painful. Acknowledging this takes courage. Would you consider sending a low-pressure text to someone you care about, like sharing a photo or saying hello, or would you like to explore gentle ways to be comforting company to yourself today?",
                category = "coping",
                emotion = "sadness",
                safetyLevel = "safe",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            ),
            DatasetItemEntity(
                id = "ds_4",
                prompt = "I want to end my life, I can't do this anymore",
                response = "I am so deeply sorry you are in this intense pain, but please hear me: your life matters. Because I am an AI, I cannot provide crisis care, but compassionate people are ready to support you right now. Please call or text 988 or reach out to someone you trust immediately.",
                category = "crisis",
                emotion = "stress",
                safetyLevel = "crisis",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            ),
            DatasetItemEntity(
                id = "ds_5",
                prompt = "I can't stop replaying negative thoughts in my head before bed",
                response = "Bedtime rumination happens when external distractions fade and our tired brain tries to resolve every pending concern. Try doing a 'brain dump': write every worry down on paper so your mind knows they are stored, then practice a gentle body scan starting from your toes up to your forehead.",
                category = "sleep",
                emotion = "anxiety",
                safetyLevel = "safe",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            ),
            DatasetItemEntity(
                id = "ds_6",
                prompt = "How can I calm down quickly when I feel angry or irritated?",
                response = "When anger rises, your body prepares to fight. Give yourself a 60-second physiological pause. Unclench your jaw, drop your shoulders, and splash cold water on your face—this triggers the mammalian dive reflex, naturally slowing your heart rate.",
                category = "coping",
                emotion = "anger",
                safetyLevel = "safe",
                source = "ieee_mental_health_benchmark",
                importedAt = now
            )
        )
    }
}
