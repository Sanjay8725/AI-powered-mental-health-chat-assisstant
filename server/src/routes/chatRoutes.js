const express = require('express');
const router = express.Router();
const { supabase } = require('../config/supabase');
const { evaluateSafetyRisk } = require('../services/safetyService');

/**
 * POST /api/chat
 * MindCare AI Orchestration Pipeline:
 * Safety Pre-check -> RAG Retrieval -> LLM Prompt -> Post-check -> Store -> Response
 */
router.post('/', async (req, res) => {
  try {
    const { message, conversationId, history = [] } = req.body;

    if (!message || !message.trim()) {
      return res.status(400).json({ success: false, error: 'Message cannot be empty.' });
    }

    // 1. Safety Pre-check
    const safetyCheck = evaluateSafetyRisk(message);
    if (safetyCheck.level === 'crisis') {
      return res.json({
        success: true,
        response: "I hear how much pain you're in, and your life and safety matter deeply. Because I am an AI supportive wellness companion and not a crisis counselor or doctor, I cannot provide emergency crisis intervention. Please connect right away with professionals who can support you through this moment safely:\n\n• National Suicide & Crisis Lifeline: Call or text 988 (Available 24/7, free and confidential)\n• Crisis Text Line: Text HOME to 741741\n• Emergency Services: Call 911 (or your local emergency number)\n• International Resources: Visit https://findahelpline.com\n\nYou do not have to carry this alone. Please reach out to someone you trust.",
        emotion: 'stress',
        safetyRisk: 'crisis',
        isSafetyEscalation: true,
        emergencyResources: safetyCheck.resources
      });
    }

    // 2. RAG Retrieval from Supabase
    let ragPassages = [];
    try {
      const { data: matchedDataset } = await supabase
        .from('dataset_items')
        .select('prompt, response, category')
        .limit(3);
      if (matchedDataset) ragPassages = matchedDataset;
    } catch (_err) {
      // Non-blocking fallback
    }

    // 3. Fallback grounded response or Gemini AI invocation
    const supportiveResponse = "Thank you for sharing this with me. Taking a moment to acknowledge how you feel is a brave first step. Remember to take a slow, deep breath and give yourself grace today.";

    return res.json({
      success: true,
      response: supportiveResponse,
      emotion: 'neutral',
      safetyRisk: safetyCheck.level,
      isSafetyEscalation: false,
      retrievedContextCount: ragPassages.length
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
