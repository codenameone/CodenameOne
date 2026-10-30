// cn1:package firebase_ai
// firebase_ai (Firebase AI Logic, Gemini) over the REST API at firebasevertexai.googleapis.com,
// authenticated with the apiKey of the firebase pack's FirebaseApp. generateContentStream is
// one event: the whole generateContent reply. Function calling, toolConfig and response
// schemas are accepted and not applied.

@JavaName('com.codename1.flutter.plugins.firebaseai.FirebaseAIException')
class FirebaseAIException implements Exception {
  external String get message;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.HarmCategory')
enum HarmCategory { unknown, harassment, hateSpeech, sexuallyExplicit, dangerousContent }

@JavaName('com.codename1.flutter.plugins.firebaseai.HarmBlockThreshold')
enum HarmBlockThreshold { low, medium, high, none, off }

@JavaName('com.codename1.flutter.plugins.firebaseai.FinishReason')
enum FinishReason { unknown, stop, maxTokens, safety, recitation, other }

@JavaName('com.codename1.flutter.plugins.firebaseai.SafetySetting')
class SafetySetting {
  external SafetySetting(HarmCategory category, HarmBlockThreshold threshold, [Object? method]);
  external HarmCategory get category;
  external HarmBlockThreshold get threshold;
  external Map<String, Object?> toJson();
}

@JavaName('com.codename1.flutter.plugins.firebaseai.Tool')
class Tool {
  external static Tool functionDeclarations(List<Object> functionDeclarations);
  external static Tool googleSearch([Object? googleSearch]);
  external static Tool codeExecution();
}

@JavaName('com.codename1.flutter.plugins.firebaseai.GenerationConfig')
class GenerationConfig {
  external GenerationConfig({int? candidateCount, List<String>? stopSequences, int? maxOutputTokens,
      double? temperature, double? topP, int? topK, String? responseMimeType, Object? responseSchema});
  external Map<String, Object?> toJson();
}

@JavaName('com.codename1.flutter.plugins.firebaseai.Part')
abstract class Part {
  external Object toJson();
}

@JavaName('com.codename1.flutter.plugins.firebaseai.TextPart')
class TextPart extends Part {
  external TextPart(String text);
  external String get text;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.InlineDataPart')
class InlineDataPart extends Part {
  external InlineDataPart(String mimeType, Uint8List bytes, {bool? willContinue});
  external String get mimeType;
  external Uint8List get bytes;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.Content')
class Content {
  external Content(String? role, Iterable<Part> parts);
  external static Content text(String text);
  external static Content multi(Iterable<Part> parts);
  external static Content model(Iterable<Part> parts);
  external static Content data(String mimeType, Uint8List bytes);
  external String? get role;
  external List<Part> get parts;
  external Map<String, Object?> toJson();
}

@JavaName('com.codename1.flutter.plugins.firebaseai.UsageMetadata')
class UsageMetadata {
  external int? get promptTokenCount;
  external int? get candidatesTokenCount;
  external int? get totalTokenCount;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.CountTokensResponse')
class CountTokensResponse {
  external int get totalTokens;
  external int? get totalBillableCharacters;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.Candidate')
class Candidate {
  external Content get content;
  external FinishReason? get finishReason;
  external String? get finishMessage;
  external String? get text;
}

@JavaName('com.codename1.flutter.plugins.firebaseai.GenerateContentResponse')
class GenerateContentResponse {
  external List<Candidate> get candidates;
  external UsageMetadata? get usageMetadata;
  external String? get text;
  // Codename One only: parses a generateContent reply (an `error` object throws FirebaseAIException).
  external static GenerateContentResponse cn1FromJson(String json);
}

@JavaName('com.codename1.flutter.plugins.firebaseai.ChatSession')
class ChatSession {
  external Iterable<Content> get history;
  external Future<GenerateContentResponse> sendMessage(Content message);
  external Stream<GenerateContentResponse> sendMessageStream(Content message);
}

@JavaName('com.codename1.flutter.plugins.firebaseai.GenerativeModel')
class GenerativeModel {
  external Future<GenerateContentResponse> generateContent(Iterable<Content> prompt,
      {List<SafetySetting>? safetySettings, GenerationConfig? generationConfig, List<Tool>? tools,
      Object? toolConfig});
  external Stream<GenerateContentResponse> generateContentStream(Iterable<Content> prompt,
      {List<SafetySetting>? safetySettings, GenerationConfig? generationConfig, List<Tool>? tools,
      Object? toolConfig});
  external Future<CountTokensResponse> countTokens(Iterable<Content> contents);
  external ChatSession startChat({List<Content>? history, List<SafetySetting>? safetySettings,
      GenerationConfig? generationConfig});
}

@JavaName('com.codename1.flutter.plugins.firebaseai.FirebaseAI')
class FirebaseAI {
  external static FirebaseAI googleAI({FirebaseApp? app, FirebaseAppCheck? appCheck, FirebaseAuth? auth});
  external static FirebaseAI vertexAI({FirebaseApp? app, FirebaseAppCheck? appCheck, FirebaseAuth? auth,
      String? location});
  external FirebaseApp get app;
  external String? get location;
  external GenerativeModel generativeModel({required String model, GenerationConfig? generationConfig,
      List<SafetySetting>? safetySettings, List<Tool>? tools, Object? toolConfig, Content? systemInstruction});
}
