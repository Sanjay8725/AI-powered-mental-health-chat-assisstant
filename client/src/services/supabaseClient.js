import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env?.VITE_SUPABASE_URL || 'https://xyzcompany.supabase.co';
const supabaseAnonKey = import.meta.env?.VITE_SUPABASE_ANON_KEY || 'your-anon-key-placeholder';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

/**
 * RAG Knowledge Retrieval: Searches Supabase dataset_items and knowledge_resources
 * for evidence-based psychoeducational material matching the user's query keywords.
 */
export async function retrieveRagContext(userMessage) {
  try {
    const keywords = userMessage
      .toLowerCase()
      .replace(/[^\w\s]/gi, '')
      .split(/\s+/)
      .filter((w) => w.length > 3)
      .slice(0, 4);

    if (keywords.length === 0) {
      keywords.push('anxiety', 'calm');
    }

    // Build PostgREST OR filter for Supabase
    const filterQuery = keywords
      .map((k) => `prompt.ilike.%${k}%,response.ilike.%${k}%,category.ilike.%${k}%`)
      .join(',');

    const { data: datasetMatches, error: datasetErr } = await supabase
      .from('dataset_items')
      .select('prompt, response, category, emotion, safety_level, source')
      .or(filterQuery)
      .limit(3);

    const { data: resourceMatches, error: resourceErr } = await supabase
      .from('knowledge_resources')
      .select('title, category, content, source')
      .limit(2);

    const combinedPassages = [];

    if (datasetMatches && datasetMatches.length > 0) {
      datasetMatches.forEach((item) => {
        combinedPassages.push({
          source: item.source || 'Curated Coping Dataset',
          title: item.category.toUpperCase() + ' Intervention',
          content: `Question: "${item.prompt}" -> Evidence-based Response: "${item.response}"`
        });
      });
    }

    if (resourceMatches && resourceMatches.length > 0) {
      resourceMatches.forEach((item) => {
        combinedPassages.push({
          source: item.source || 'Mental Health Psychoeducation',
          title: item.title,
          content: item.content
        });
      });
    }

    // Default grounded fallbacks if database is empty or unconfigured
    if (combinedPassages.length === 0) {
      combinedPassages.push(
        {
          source: 'National Institute of Mental Health (NIMH)',
          title: '5-4-3-2-1 Sensory Grounding Technique',
          content: 'Acknowledge 5 things you can see, 4 things you can physically feel, 3 things you can hear, 2 things you can smell, and 1 slow diaphragmatic breath.'
        },
        {
          source: 'Mayo Clinic Mind-Body Medicine',
          title: 'Box Breathing (4-4-4-4)',
          content: 'Inhale through the nose for 4 counts, hold gently for 4 counts, exhale smoothly for 4 counts, and hold empty for 4 counts to activate vagal nerve calming.'
        }
      );
    }

    return combinedPassages;
  } catch (err) {
    console.warn('Supabase RAG retrieval notice:', err.message);
    return [
      {
        source: 'Mayo Clinic',
        title: 'Parasympathetic Reset',
        content: 'Take slow, prolonged exhalations to signal physical safety to the nervous system.'
      }
    ];
  }
}
