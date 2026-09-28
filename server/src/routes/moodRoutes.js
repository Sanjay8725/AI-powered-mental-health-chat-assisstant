const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const { supabase } = require('../config/supabase');
const { requireAuth } = require('../middleware/authMiddleware');

/**
 * 1. POST /api/moods
 * Save user daily mood entry into Supabase database with timestamp
 */
router.post('/', async (req, res) => {
  try {
    const { mood, score, note, tags = [], timestamp, userId } = req.body;

    if (!mood) {
      return res.status(400).json({
        success: false,
        error: 'Emotion/mood selection is required.'
      });
    }

    const moodEntry = {
      id: uuidv4(),
      user_id: userId || req.user?.id || 'guest_user',
      mood: mood.trim(),
      score: Number(score) || 3,
      note: note?.trim() || '',
      tags: Array.isArray(tags) ? tags : [],
      created_at: new Date(timestamp || Date.now()).toISOString(),
      timestamp: Number(timestamp) || Date.now()
    };

    // Store into Supabase moods table
    const { data, error } = await supabase
      .from('moods')
      .insert([moodEntry])
      .select();

    if (error) {
      return res.status(500).json({
        success: false,
        error: 'Supabase storage error: ' + error.message
      });
    }

    // Broadcast Realtime Event
    try {
      const channel = supabase.channel('mood-updates');
      await channel.send({
        type: 'broadcast',
        event: 'mood_logged',
        payload: moodEntry
      });
    } catch (_broadcastErr) {}

    return res.status(201).json({
      success: true,
      message: 'Mood entry successfully saved to Supabase with timestamp.',
      data: moodEntry
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: 'Mood logging handler error: ' + err.message
    });
  }
});

/**
 * 2. GET /api/moods
 * Retrieve mood history for user
 */
router.get('/', async (req, res) => {
  try {
    const userId = req.query.userId || req.user?.id || 'guest_user';
    const limit = Number(req.query.limit) || 30;

    const { data, error } = await supabase
      .from('moods')
      .select('*')
      .eq('user_id', userId)
      .order('timestamp', { ascending: false })
      .limit(limit);

    if (error) {
      return res.status(500).json({ success: false, error: error.message });
    }

    return res.json({
      success: true,
      data: data || []
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
