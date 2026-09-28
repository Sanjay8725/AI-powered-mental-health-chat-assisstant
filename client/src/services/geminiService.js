/**
 * Gemini API Integration for MindCare AI (React Client)
 * Adheres strictly to IEEE Section 10 Prompt Engineering:
 * SYSTEM POLICY + SAFETY POLICY + ROLE / TONE + RAG CONTEXT + CONVERSATION HISTORY + USER MESSAGE
 */

const GEMINI_MODEL = 'gemini-3.5-flash';
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

const CRISIS_KEYWORDS = [
  'kill myself', 'want to die', 'commit suicide', 'end my life',
  'hang myself', 'suicidal', 'better off dead', 'cutting myself',
  'self harm', 'overdose', 'slit my', "can't go on living",
  'no reason to live', 'take my own life'
];

export function checkSafetyRisk(text) {
  const lower = (text || '').toLowerCase();
  for (const phrase of CRISIS_KEYWORDS) {
    if (lower.includes(phrase)) {
      return 'crisis';
    }
  }
  const moderate = ['hopeless', 'worthless', "can't take it anymore", 'panic attack', 'giving up'];
  for (const phrase of moderate) {
    if (lower.includes(phrase)) {
      return 'moderate_risk';
    }
  }
  return 'safe';
}

export function detectEmotion(text) {
  const lower = (text || '').toLowerCase();
  if (['anxious', 'panic', 'worry', 'scared', 'shaking', 'nervous', 'dread'].some(w => lower.includes(w))) return 'anxiety';
  if (['sad', 'crying', 'depressed', 'lonely', 'empty', 'heartbroken'].some(w => lower.includes(w))) return 'sadness';
  if (['stressed', 'burned out', 'pressure', 'exhausted', 'deadline'].some(w => lower.includes(w))) return 'stress';
  if (['angry', 'furious', 'mad', 'frustrated', 'irritated'].some(w => lower.includes(w))) return 'anger';
  if (['happy', 'grateful', 'peaceful', 'good', 'joy', 'relieved'].some(w => lower.includes(w))) return 'joy';
  return 'calm';
}

export async function generateMindCareResponse({
  userMessage,
  conversationHistory = [],
  ragPassages = [],
  apiKey = import.meta.env?.VITE_GEMINI_API_KEY || ''
}) {
  const safetyRisk = checkSafetyRisk(userMessage);
  const detectedEmotion = detectEmotion(userMessage);

  // Immediate Crisis Escrow
  if (safetyRisk === 'crisis') {
    return {
      text: "I hear how much pain you're in, and your life and safety matter deeply. Because I am an AI supportive wellness companion and not a licensed doctor or crisis counselor, I cannot provide emergency intervention. Please connect right away with professionals who can support you through this moment safely:\n\n• National Suicide & Crisis Lifeline: Call or text 988 (Available 24/7, free & confidential)\n• Crisis Text Line: Text HOME to 741741\n• Emergency Services: Call 911 (or your local emergency number)\n• International Helplines: https://findahelpline.com\n\nYou do not have to carry this alone. Please reach out to someone who cares about you right now.",
      emotion: 'stress',
      safetyRisk: 'crisis',
      isSafetyEscalation: true,
      citations: ['988 Suicide & Crisis Lifeline']
    };
  }

  // Fallback if API key is not supplied
  if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
    const citationTitles = ragPassages.map(p => p.title).slice(0, 2);
    const topKnowledge = ragPassages[0]?.content || '';
    return {
      text: `Thank you for sharing this with me. Taking a moment to pause and reflect is an act of self-compassion. \n\nEvidence-based practice (${ragPassages[0]?.title || 'Grounding'}): ${topKnowledge}\n\nTake a slow, gentle breath in through your nose for 4 counts, and out through your mouth for 6 counts. What part of your body feels the most tension right now?`,
      emotion: detectedEmotion,
      safetyRisk: safetyRisk,
      isSafetyEscalation: false,
      citations: citationTitles
    };
  }

  try {
    const ragContextFormatted = ragPassages
      .map(p => `[Source: ${p.source} - ${p.title}]\n${p.content}`)
      .join('\n\n');

    const systemInstruction = `
You are MindCare AI, a supportive, empathetic, non-diagnostic mental wellness conversational assistant.

STRICT SAFETY & ETHICAL BOUNDARIES:
1. You are NOT a doctor, psychologist, or licensed therapist. Never claim to diagnose medical or psychological disorders.
2. Never prescribe medication, dosage, or medical treatments.
3. If the user mentions imminent danger or crisis, prioritize life safety immediately.
4. Ground your responses in the retrieved knowledge base provided below.
5. Maintain a compassionate, warm, non-judgmental, and validating tone.

RETRIEVED KNOWLEDGE BASE (SUPABASE RAG CONTEXT):
${ragContextFormatted}

RESPONSE FORMAT:
Provide an empathetic response. At the end of your response, append:
<!--META: {"emotion": "anxiety|sadness|joy|stress|anger|calm", "safety": "safe|moderate_risk"}-->
`.trim();

    const contents = [
      {
        role: 'user',
        parts: [{ text: systemInstruction }]
      },
      {
        role: 'model',
        parts: [{ text: 'Understood. I will act as MindCare AI, providing grounded, non-diagnostic wellness support grounded in the provided RAG knowledge.' }]
      }
    ];

    // Add recent turns (last 6)
    conversationHistory.slice(-6).forEach(msg => {
      contents.push({
        role: msg.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: msg.content }]
      });
    });

    // Add current user prompt
    contents.push({
      role: 'user',
      parts: [{ text: userMessage }]
    });

    const response = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents,
        generationConfig: {
          temperature: 0.7,
          topP: 0.9,
          maxOutputTokens: 800
        }
      })
    });

    if (!response.ok) {
      const errData = await response.text();
      console.warn('Gemini API call returned non-200:', errData);
      throw new Error(`Gemini API error (${response.status})`);
    }

    const json = await response.json();
    const candidateText = json.candidates?.[0]?.content?.parts?.[0]?.text || '';

    // Parse metadata
    let cleanText = candidateText;
    let finalEmotion = detectedEmotion;
    let finalSafety = safetyRisk;

    const metaMatch = candidateText.match(/<!--META:\s*(\{.*?\})\s*-->/s);
    if (metaMatch) {
      cleanText = candidateText.replace(metaMatch[0], '').trim();
      try {
        const metaObj = JSON.parse(metaMatch[1]);
        if (metaObj.emotion) finalEmotion = metaObj.emotion;
        if (metaObj.safety === 'crisis') finalSafety = 'crisis';
      } catch (_e) {}
    }

    return {
      text: cleanText || "I'm listening and right here with you. How can I support you right now?",
      emotion: finalEmotion,
      safetyRisk: finalSafety,
      isSafetyEscalation: finalSafety === 'crisis',
      citations: ragPassages.map(p => p.title).slice(0, 2)
    };
  } catch (err) {
    console.error('Error generating AI response:', err);
    return {
      text: `I'm right here with you. Take a slow, grounding breath. Try noticing three physical sensations in your hands or feet to help your nervous system gently settle.\n\nTechnique from ${ragPassages[0]?.title || 'Grounding'}: ${ragPassages[0]?.content || 'Focus on your natural breath.'}`,
      emotion: detectedEmotion,
      safetyRisk: safetyRisk,
      isSafetyEscalation: false,
      citations: ragPassages.map(p => p.title).slice(0, 2)
    };
  }
}
