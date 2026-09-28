const CRISIS_KEYWORDS = [
  'kill myself', 'want to die', 'commit suicide', 'end my life',
  'hang myself', 'suicidal', 'better off dead', 'cutting myself',
  'self harm', 'overdose', 'slit my', "can't go on living",
  'no reason to live', 'take my own life'
];

const MODERATE_KEYWORDS = [
  'hopeless', 'worthless', "can't take it anymore", 'overwhelmed',
  'panic attack', 'nobody cares', 'deep depression', 'giving up'
];

function evaluateSafetyRisk(text) {
  const lower = (text || '').toLowerCase();
  for (const phrase of CRISIS_KEYWORDS) {
    if (lower.includes(phrase)) {
      return {
        level: 'crisis',
        isEmergency: true,
        action: 'TRIGGER_SAFETY_HOTLINE_ESC',
        resources: [
          { name: '988 Suicide & Crisis Lifeline', contact: 'Call or text 988', available: '24/7' },
          { name: 'Crisis Text Line', contact: 'Text HOME to 741741', available: '24/7' },
          { name: 'Emergency Services', contact: 'Call 911 / 112', available: 'Immediate' }
        ]
      };
    }
  }

  for (const phrase of MODERATE_KEYWORDS) {
    if (lower.includes(phrase)) {
      return {
        level: 'moderate_risk',
        isEmergency: false,
        action: 'PROVIDE_GROUNDING_AND_COPING'
      };
    }
  }

  return {
    level: 'safe',
    isEmergency: false,
    action: 'NORMAL_AI_CONVERSATION'
  };
}

module.exports = { evaluateSafetyRisk };
