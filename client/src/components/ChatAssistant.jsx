import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  Sparkles,
  ShieldAlert,
  PhoneCall,
  MessageSquare,
  Bot,
  User,
  Heart,
  BookOpen,
  Info,
  RefreshCw,
  PlusCircle,
  Menu,
  X
} from 'lucide-react';
import { retrieveRagContext } from '../services/supabaseClient';
import { generateMindCareResponse } from '../services/geminiService';

export default function ChatAssistant() {
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'assistant',
      content:
        "Hello, I am MindCare AI, your supportive mental health and wellness companion. I use evidence-based psychoeducation grounded in our Supabase knowledge base to help you navigate stress, anxiety, or overwhelmed feelings. How are you feeling today?",
      emotion: 'calm',
      safetyRisk: 'safe',
      citations: ['MindCare Knowledge Base'],
      timestamp: Date.now()
    }
  ]);
  const [inputText, setInputText] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [showCrisisModal, setShowCrisisModal] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [apiKey, setApiKey] = useState(import.meta.env?.VITE_GEMINI_API_KEY || '');
  const [retrievedCount, setRetrievedCount] = useState(0);

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isGenerating]);

  const quickPrompts = [
    'Help me calm down right now',
    "I'm overwhelmed with deadlines and stress",
    "Can't sleep due to racing thoughts",
    'Guide me through 5-4-3-2-1 grounding',
    'I feel lonely and disconnected'
  ];

  const handleSendMessage = async (textToSend) => {
    const query = (textToSend || inputText).trim();
    if (!query || isGenerating) return;

    setInputText('');
    const userMessageObj = {
      id: 'user_' + Date.now(),
      role: 'user',
      content: query,
      timestamp: Date.now()
    };

    setMessages((prev) => [...prev, userMessageObj]);
    setIsGenerating(true);

    try {
      // 1. RAG Retrieval from Supabase
      const ragPassages = await retrieveRagContext(query);
      setRetrievedCount(ragPassages.length);

      // 2. AI Response generation with Gemini API
      const aiResponse = await generateMindCareResponse({
        userMessage: query,
        conversationHistory: messages,
        ragPassages,
        apiKey
      });

      const assistantMessageObj = {
        id: 'assistant_' + Date.now(),
        role: 'assistant',
        content: aiResponse.text,
        emotion: aiResponse.emotion,
        safetyRisk: aiResponse.safetyRisk,
        citations: aiResponse.citations,
        isSafetyEscalation: aiResponse.isSafetyEscalation,
        timestamp: Date.now()
      };

      setMessages((prev) => [...prev, assistantMessageObj]);

      if (aiResponse.safetyRisk === 'crisis') {
        setShowCrisisModal(true);
      }
    } catch (err) {
      console.error('Chat error:', err);
      setMessages((prev) => [
        ...prev,
        {
          id: 'error_' + Date.now(),
          role: 'assistant',
          content:
            "I encountered a temporary connection issue, but I'm here. Take a gentle breath with me: inhale for 4 seconds, and exhale slowly for 6 seconds.",
          emotion: 'calm',
          timestamp: Date.now()
        }
      ]);
    } finally {
      setIsGenerating(false);
    }
  };

  const getEmotionBadge = (emotion) => {
    const map = {
      joy: { label: 'Joyful Focus', bg: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
      calm: { label: 'Calming Support', bg: 'bg-teal-50 text-teal-700 border-teal-200' },
      anxiety: { label: 'Anxiety Grounding', bg: 'bg-amber-50 text-amber-700 border-amber-200' },
      stress: { label: 'Stress Relief', bg: 'bg-rose-50 text-rose-700 border-rose-200' },
      sadness: { label: 'Gentle Comfort', bg: 'bg-blue-50 text-blue-700 border-blue-200' },
      anger: { label: 'De-escalation', bg: 'bg-purple-50 text-purple-700 border-purple-200' }
    };
    return map[emotion] || { label: 'Supportive', bg: 'bg-gray-50 text-gray-700 border-gray-200' };
  };

  return (
    <div className="flex h-screen bg-slate-50 font-sans text-slate-800 antialiased overflow-hidden">
      {/* Sidebar for conversations & RAG stats */}
      <div
        className={`fixed inset-y-0 left-0 z-30 w-72 bg-white border-r border-slate-200 transform transition-transform duration-300 ease-in-out md:relative md:translate-x-0 ${
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex flex-col h-full p-4">
          <div className="flex items-center justify-between pb-4 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <div className="flex items-center justify-center w-9 h-9 rounded-xl bg-teal-600 text-white shadow-sm">
                <Heart className="w-5 h-5 fill-white" />
              </div>
              <div>
                <h1 className="text-base font-bold text-slate-900 leading-tight">MindCare AI</h1>
                <span className="text-xs text-teal-600 font-medium">Supabase RAG + Gemini</span>
              </div>
            </div>
            <button
              onClick={() => setIsSidebarOpen(false)}
              className="p-1 md:hidden text-slate-400 hover:text-slate-600"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <button
            onClick={() => {
              setMessages([
                {
                  id: 'welcome_new',
                  role: 'assistant',
                  content:
                    'Starting a fresh reflection. Take a comfortable breath. What is currently weighing on your mind?',
                  emotion: 'calm',
                  timestamp: Date.now()
                }
              ]);
              if (window.innerWidth < 768) setIsSidebarOpen(false);
            }}
            className="flex items-center justify-center gap-2 w-full mt-4 py-2.5 px-4 bg-teal-50 hover:bg-teal-100 text-teal-700 rounded-xl font-medium text-sm transition-colors border border-teal-200"
          >
            <PlusCircle className="w-4 h-4" />
            New Reflection
          </button>

          {/* RAG Knowledge Status */}
          <div className="mt-6 p-3.5 bg-slate-50 rounded-xl border border-slate-200/80">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 mb-2">
              <BookOpen className="w-4 h-4 text-teal-600" />
              <span>Grounded Knowledge Engine</span>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              Every message queries psychoeducation documents stored in Supabase before prompting Gemini 3.5 Flash.
            </p>
            <div className="mt-2.5 pt-2 border-t border-slate-200 flex items-center justify-between text-[11px] text-slate-500">
              <span>Passages Retrieved:</span>
              <span className="font-semibold text-teal-700 bg-teal-100/60 px-2 py-0.5 rounded-full">
                {retrievedCount > 0 ? `${retrievedCount} verified` : 'Active'}
              </span>
            </div>
          </div>

          {/* Scope notice */}
          <div className="mt-auto p-3 bg-amber-50/80 border border-amber-200 rounded-xl text-[11px] text-amber-800 leading-normal">
            <strong>Scope Notice:</strong> Supportive wellness companion. Non-diagnostic. If in imminent crisis, call 988 immediately.
          </div>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex flex-col flex-1 h-full min-w-0">
        {/* Top Header */}
        <header className="flex items-center justify-between px-4 py-3 bg-white border-b border-slate-200">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsSidebarOpen(true)}
              className="p-1.5 md:hidden rounded-lg hover:bg-slate-100 text-slate-600"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div>
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
                Mental Wellness Chat
                <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-100 text-emerald-800 rounded-full">
                  RAG Active
                </span>
              </h2>
              <p className="text-xs text-slate-500">Empathetic, context-aware, non-diagnostic guidance</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowCrisisModal(true)}
              className="flex items-center gap-1 px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-xs font-semibold transition-colors"
            >
              <ShieldAlert className="w-4 h-4 text-rose-600" />
              <span>SOS Hotline (988)</span>
            </button>
          </div>
        </header>

        {/* Messages Stream */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-4">
          {messages.map((msg) => {
            const isUser = msg.role === 'user';
            const badge = msg.emotion ? getEmotionBadge(msg.emotion) : null;

            return (
              <div
                key={msg.id}
                className={`flex gap-3 max-w-3xl ${isUser ? 'ml-auto flex-row-reverse' : 'mr-auto'}`}
              >
                {/* Avatar */}
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 text-white ${
                    isUser ? 'bg-slate-800' : 'bg-teal-600'
                  }`}
                >
                  {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                </div>

                {/* Bubble Container */}
                <div className="space-y-1.5">
                  {/* Emotion badge for assistant */}
                  {!isUser && badge && (
                    <div className="flex items-center gap-2">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-semibold border ${badge.bg}`}
                      >
                        {badge.label}
                      </span>
                    </div>
                  )}

                  {/* Message Bubble */}
                  <div
                    className={`p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm whitespace-pre-wrap ${
                      isUser
                        ? 'bg-teal-600 text-white rounded-tr-none'
                        : 'bg-white border border-slate-200 text-slate-800 rounded-tl-none'
                    }`}
                  >
                    {msg.content}
                  </div>

                  {/* RAG Citations */}
                  {!isUser && msg.citations && msg.citations.length > 0 && (
                    <div className="flex flex-wrap items-center gap-1.5 text-[11px] text-slate-400 pl-1">
                      <BookOpen className="w-3 h-3 text-slate-400" />
                      <span>Grounded in:</span>
                      {msg.citations.map((cite, idx) => (
                        <span key={idx} className="font-medium text-teal-700 bg-teal-50 px-1.5 py-0.5 rounded border border-teal-100">
                          {cite}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {/* Typing Indicator */}
          {isGenerating && (
            <div className="flex gap-3 max-w-md mr-auto">
              <div className="w-8 h-8 rounded-full bg-teal-600 text-white flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4" />
              </div>
              <div className="bg-white border border-slate-200 px-4 py-3 rounded-2xl rounded-tl-none shadow-sm flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-teal-500 animate-pulse"></div>
                <div className="w-2 h-2 rounded-full bg-teal-500 animate-pulse delay-150"></div>
                <div className="w-2 h-2 rounded-full bg-teal-500 animate-pulse delay-300"></div>
                <span className="text-xs text-slate-400 ml-2">Grounding response with Supabase...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Suggestion Chips */}
        <div className="px-4 py-2 border-t border-slate-100 bg-slate-50/70 overflow-x-auto flex gap-2 no-scrollbar">
          {quickPrompts.map((prompt, idx) => (
            <button
              key={idx}
              onClick={() => handleSendMessage(prompt)}
              className="shrink-0 px-3 py-1.5 bg-white hover:bg-teal-50 hover:border-teal-300 text-slate-700 border border-slate-200 rounded-full text-xs transition-colors shadow-2xs"
            >
              {prompt}
            </button>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-3 bg-white border-t border-slate-200">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex items-center gap-2 max-w-4xl mx-auto"
          >
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="Describe what's on your mind... (e.g. feeling anxious about exams)"
              disabled={isGenerating}
              className="flex-1 px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 transition-all placeholder:text-slate-400 disabled:opacity-60"
            />
            <button
              type="submit"
              disabled={!inputText.trim() || isGenerating}
              className="flex items-center justify-center w-11 h-11 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-200 text-white rounded-xl shadow-sm transition-colors shrink-0"
            >
              <Send className="w-5 h-5" />
            </button>
          </form>
        </div>
      </div>

      {/* Crisis Safety Modal */}
      {showCrisisModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-rose-200 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-3 border-b border-rose-100">
              <div className="flex items-center gap-2 text-rose-600">
                <ShieldAlert className="w-6 h-6" />
                <h3 className="text-lg font-bold">Immediate Crisis Support</h3>
              </div>
              <button
                onClick={() => setShowCrisisModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 p-3 bg-rose-50 text-rose-900 text-xs rounded-xl border border-rose-200">
              Your safety matters. MindCare AI is a wellness assistant and cannot replace professional crisis care.
              Trained human counselors are standing by 24/7 to listen and support you safely.
            </div>

            <div className="mt-4 space-y-3">
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold text-slate-900">988 Suicide & Crisis Lifeline</h4>
                  <p className="text-xs text-slate-500">Free, confidential 24/7 support in US & Canada</p>
                </div>
                <a
                  href="tel:988"
                  className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5"
                >
                  <PhoneCall className="w-3.5 h-3.5" /> Call 988
                </a>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold text-slate-900">Crisis Text Line</h4>
                  <p className="text-xs text-slate-500">Text HOME to 741741 to connect with a crisis counselor</p>
                </div>
                <a
                  href="sms:741741?body=HOME"
                  className="px-3.5 py-1.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5"
                >
                  <MessageSquare className="w-3.5 h-3.5" /> Text 741741
                </a>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                onClick={() => setShowCrisisModal(false)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold"
              >
                Close & Return to Chat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
