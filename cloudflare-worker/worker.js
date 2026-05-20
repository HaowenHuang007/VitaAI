/**
 * VitaAI Cloudflare Worker — Gemini API proxy
 *
 * Deployed on the Cloudflare Workers free tier (100k req/day).
 * The Gemini API key lives in Worker Secrets, not in the APK.
 *
 * Three endpoints:
 *   POST /chat                  -> single-turn JSON answer
 *   POST /chat-with-history     -> multi-turn chat
 *   POST /analyze-image         -> food image analysis (base64 in, JSON out)
 *
 * Auth: every request must carry `Authorization: Bearer <firebase-id-token>`.
 * We do a basic JWT shape check + audience match — this stops casual scrapers
 * but is not a cryptographic verification. Good enough for a personal project;
 * upgrade later by verifying signatures against Google's public keys.
 */

const GEMINI_MODEL = "gemini-2.0-flash";
const GEMINI_URL = (model) =>
  `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "86400",
};

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...CORS_HEADERS },
  });
}

function err(status, message) {
  return json({ error: message }, status);
}

// Minimal Firebase ID-token sanity check. Decodes the JWT payload and verifies
// audience matches the Firebase project ID. Does NOT verify the signature.
function checkAuth(request, expectedProjectId) {
  const auth = request.headers.get("Authorization") || "";
  if (!auth.startsWith("Bearer ")) return false;
  const token = auth.slice(7);
  const parts = token.split(".");
  if (parts.length !== 3) return false;
  try {
    const payload = JSON.parse(
      atob(parts[1].replace(/-/g, "+").replace(/_/g, "/"))
    );
    if (payload.aud !== expectedProjectId) return false;
    if (typeof payload.exp === "number" && payload.exp * 1000 < Date.now()) return false;
    return true;
  } catch {
    return false;
  }
}

async function callGemini(env, contents, systemInstruction, generationConfig) {
  const body = {
    contents,
    generationConfig: generationConfig || { temperature: 0.7, maxOutputTokens: 1500 },
  };
  if (systemInstruction) {
    body.systemInstruction = { parts: [{ text: systemInstruction }] };
  }

  const res = await fetch(`${GEMINI_URL(GEMINI_MODEL)}?key=${env.GEMINI_API_KEY}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Gemini ${res.status}: ${text.slice(0, 300)}`);
  }
  const data = await res.json();
  return data.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
}

async function handleChat(request, env) {
  const { prompt } = await request.json();
  if (!prompt || typeof prompt !== "string") {
    return err(400, "prompt is required");
  }
  if (prompt.length > 4000) return err(400, "prompt too long");

  const content = await callGemini(
    env,
    [{ role: "user", parts: [{ text: prompt }] }],
    "You are an expert nutritionist. Always respond in valid JSON. Values must be plain text, never nested JSON.",
    { temperature: 0.7, maxOutputTokens: 1500, responseMimeType: "application/json" }
  );
  return json({ content });
}

async function handleChatWithHistory(request, env) {
  const { systemPrompt, history } = await request.json();
  if (!systemPrompt || typeof systemPrompt !== "string") {
    return err(400, "systemPrompt is required");
  }
  if (!Array.isArray(history)) return err(400, "history must be an array");
  if (history.length > 30) return err(400, "history too long");

  // Gemini uses role "user" / "model" (not "assistant").
  const contents = [];
  for (const msg of history) {
    if (!msg || typeof msg !== "object") continue;
    const role = msg.role === "assistant" ? "model" : "user";
    const text = String(msg.content || "").slice(0, 4000);
    if (text) contents.push({ role, parts: [{ text }] });
  }
  if (contents.length === 0) return err(400, "history is empty");

  const content = await callGemini(env, contents, systemPrompt, {
    temperature: 0.7,
    maxOutputTokens: 500,
  });
  return json({ content });
}

async function handleAnalyzeImage(request, env) {
  const { base64Image, language } = await request.json();
  if (!base64Image || typeof base64Image !== "string") {
    return err(400, "base64Image is required");
  }
  if (base64Image.length > 7_000_000) return err(400, "image too large");

  const lang = language || "Spanish";
  const userPrompt =
    'Analyze the food in this image. Return strict JSON with these keys:\n' +
    '{\n' +
    '  "name": "food name",\n' +
    '  "calories": integer (kcal for the visible portion),\n' +
    '  "protein": integer (grams),\n' +
    '  "carbs": integer (grams),\n' +
    '  "fat": integer (grams),\n' +
    '  "rating": one of "healthy"|"moderate"|"avoid",\n' +
    '  "tip": "one brief nutritional tip"\n' +
    '}\n' +
    'If unsure, give your best estimate. No text outside the JSON.';

  const contents = [
    {
      role: "user",
      parts: [
        { text: userPrompt },
        { inlineData: { mimeType: "image/jpeg", data: base64Image } },
      ],
    },
  ];

  const content = await callGemini(
    env,
    contents,
    `You are an expert nutritionist. Always respond in valid JSON only, no markdown fences. All textual fields must be in ${lang}.`,
    { temperature: 0.4, maxOutputTokens: 400, responseMimeType: "application/json" }
  );
  return json({ content });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    if (request.method !== "POST") {
      return err(405, "Method not allowed");
    }
    if (!env.GEMINI_API_KEY) {
      return err(500, "Server misconfigured: GEMINI_API_KEY missing");
    }
    if (!env.FIREBASE_PROJECT_ID) {
      return err(500, "Server misconfigured: FIREBASE_PROJECT_ID missing");
    }
    if (!checkAuth(request, env.FIREBASE_PROJECT_ID)) {
      return err(401, "Unauthorized");
    }

    const url = new URL(request.url);
    try {
      switch (url.pathname) {
        case "/chat":
          return await handleChat(request, env);
        case "/chat-with-history":
          return await handleChatWithHistory(request, env);
        case "/analyze-image":
          return await handleAnalyzeImage(request, env);
        default:
          return err(404, "Not found");
      }
    } catch (e) {
      return err(500, e.message || "Internal error");
    }
  },
};
