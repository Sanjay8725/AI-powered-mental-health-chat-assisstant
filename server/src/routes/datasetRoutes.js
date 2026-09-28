const express = require('express');
const router = express.Router();
const multer = require('multer');
const csv = require('csv-parser');
const stream = require('stream');
const { v4: uuidv4 } = require('uuid');
const { supabase } = require('../config/supabase');
const { requireAuth } = require('../middleware/authMiddleware');

// Configure Multer storage in-memory with strict 10MB limit and CSV filter
const upload = multer({
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB maximum
  fileFilter: (req, file, cb) => {
    const isCsvMime = file.mimetype === 'text/csv' ||
                      file.mimetype === 'application/vnd.ms-excel' ||
                      file.mimetype === 'text/plain';
    const isCsvExt = file.originalname.toLowerCase().endsWith('.csv');
    if (isCsvMime || isCsvExt) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type. Only CSV (.csv) files are allowed.'));
    }
  }
});

/**
 * 1. POST /api/dataset/upload-csv
 * Uploads, validates, parses, stores into Supabase, and triggers real-time broadcast
 */
router.post('/upload-csv', upload.single('dataset'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'No CSV file attached. Use multipart/form-data with key "dataset".'
      });
    }

    const itemsToInsert = [];
    const parseErrors = [];
    let rowCount = 0;

    const bufferStream = new stream.PassThrough();
    bufferStream.end(req.file.buffer);

    bufferStream
      .pipe(csv())
      .on('data', (row) => {
        rowCount++;
        // Normalize headers
        const normalizedRow = {};
        for (const key of Object.keys(row)) {
          normalizedRow[key.trim().toLowerCase().replace(/[\s-]/g, '_')] = row[key];
        }

        const prompt = normalizedRow['prompt'] || normalizedRow['question'] || normalizedRow['input'] || Object.values(row)[0];
        const response = normalizedRow['response'] || normalizedRow['answer'] || normalizedRow['target'] || Object.values(row)[1];
        const category = normalizedRow['category'] || normalizedRow['intent'] || normalizedRow['topic'] || 'wellness';
        const emotion = normalizedRow['emotion'] || normalizedRow['sentiment'] || 'neutral';
        const safetyLevel = normalizedRow['safety_level'] || normalizedRow['safety'] || normalizedRow['risk'] || 'safe';

        if (!prompt || !response || !prompt.trim() || !response.trim()) {
          if (parseErrors.length < 5) {
            parseErrors.push(`Row ${rowCount}: missing required prompt or response.`);
          }
          return;
        }

        itemsToInsert.push({
          id: uuidv4(),
          prompt: prompt.trim(),
          response: response.trim(),
          category: category.trim().toLowerCase(),
          emotion: emotion.trim().toLowerCase(),
          safety_level: safetyLevel.trim().toLowerCase(),
          source: req.file.originalname,
          imported_at: Date.now()
        });
      })
      .on('end', async () => {
        if (itemsToInsert.length === 0) {
          return res.status(400).json({
            success: false,
            error: 'No valid rows found in CSV. Expected headers: prompt, response, category, emotion, safety_level',
            details: parseErrors
          });
        }

        // Store into Supabase dataset_items table
        const { data, error } = await supabase
          .from('dataset_items')
          .upsert(itemsToInsert, { onConflict: 'id' });

        if (error) {
          return res.status(500).json({
            success: false,
            error: 'Database persistence error: ' + error.message
          });
        }

        // Send a Realtime Broadcast to notify connected frontend listeners
        try {
          const channel = supabase.channel('dataset-updates');
          await channel.send({
            type: 'broadcast',
            event: 'dataset_modified',
            payload: {
              action: 'bulk_upload',
              rowCount: itemsToInsert.length,
              source: req.file.originalname,
              timestamp: Date.now()
            }
          });
        } catch (_broadcastErr) {
          // Non-blocking broadcast
        }

        return res.status(200).json({
          success: true,
          message: `Successfully ingested ${itemsToInsert.length} dataset rows into Supabase.`,
          totalRowsProcessed: rowCount,
          validRowsInserted: itemsToInsert.length,
          skippedRows: rowCount - itemsToInsert.length,
          warnings: parseErrors
        });
      })
      .on('error', (err) => {
        return res.status(400).json({
          success: false,
          error: 'CSV Parsing Error: ' + err.message
        });
      });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: 'Upload handler error: ' + err.message
    });
  }
});

/**
 * 2. GET /api/dataset
 * Lists paginated dataset items with optional category/search query
 */
router.get('/', async (req, res) => {
  try {
    const { category, search, limit = 50, offset = 0 } = req.query;

    let query = supabase
      .from('dataset_items')
      .select('*', { count: 'exact' })
      .order('imported_at', { ascending: false })
      .range(Number(offset), Number(offset) + Number(limit) - 1);

    if (category && category !== 'ALL') {
      query = query.eq('category', category.toLowerCase());
    }

    if (search) {
      query = query.or(`prompt.ilike.%${search}%,response.ilike.%${search}%,category.ilike.%${search}%`);
    }

    const { data, count, error } = await query;

    if (error) {
      return res.status(500).json({ success: false, error: error.message });
    }

    return res.json({
      success: true,
      data,
      totalCount: count,
      limit: Number(limit),
      offset: Number(offset)
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * 3. DELETE /api/dataset
 * Wipes or resets the dataset
 */
router.delete('/', async (req, res) => {
  try {
    const { error } = await supabase
      .from('dataset_items')
      .delete()
      .neq('id', '00000000-0000-0000-0000-000000000000'); // delete all

    if (error) {
      return res.status(500).json({ success: false, error: error.message });
    }

    // Broadcast reset event
    const channel = supabase.channel('dataset-updates');
    await channel.send({
      type: 'broadcast',
      event: 'dataset_modified',
      payload: { action: 'clear', timestamp: Date.now() }
    });

    return res.json({ success: true, message: 'Dataset cleared successfully.' });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
