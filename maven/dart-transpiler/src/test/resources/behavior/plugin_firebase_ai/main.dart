// firebase_ai, headless: models, contents and configs are built and serialised as the REST
// API wants them, a canned reply is parsed, and a request fails at once with the plugin's
// own FirebaseAIException instead of hanging.
import 'dart:convert';
import 'dart:typed_data';
import 'package:firebase_ai/firebase_ai.dart';
import 'package:firebase_core/firebase_core.dart';

Future<void> main() async {
  await Firebase.initializeApp(
      options: FirebaseOptions(apiKey: 'k', appId: 'a', messagingSenderId: 'm', projectId: 'proj'));
  final ai = FirebaseAI.googleAI();
  final vertex = FirebaseAI.vertexAI(location: 'europe-west4');
  print('${ai.app.name} ${ai.location} ${vertex.location}');

  final config = GenerationConfig(temperature: 0.5, maxOutputTokens: 256, topP: 0.9, topK: 40,
      responseMimeType: 'text/plain', candidateCount: 1, stopSequences: ['END']);
  print(jsonEncode(config.toJson()));
  print(jsonEncode(SafetySetting(HarmCategory.hateSpeech, HarmBlockThreshold.medium).toJson()));

  print(jsonEncode(Content.text('hi').toJson()));
  print(jsonEncode(Content.model([TextPart('yo')]).toJson()));
  print(jsonEncode(Content('user', [TextPart('a'), InlineDataPart('image/png', Uint8List.fromList([1, 2, 3]))]).toJson()));

  final model = ai.generativeModel(
      model: 'gemini-2.0-flash',
      generationConfig: config,
      safetySettings: [SafetySetting(HarmCategory.harassment, HarmBlockThreshold.low)],
      systemInstruction: Content.text('be brief'));

  final reply = GenerateContentResponse.cn1FromJson('{"candidates":[{"content":{"role":"model",'
      '"parts":[{"text":"Hello "},{"text":"world"}]},"finishReason":"STOP"},'
      '{"content":{"role":"model","parts":[{"text":"x"}]},"finishReason":"MAX_TOKENS"}],'
      '"usageMetadata":{"promptTokenCount":3,"candidatesTokenCount":5,"totalTokenCount":8}}');
  print('${reply.text} ${reply.candidates.length}');
  print('${reply.candidates[0].finishReason == FinishReason.stop} ${reply.candidates[1].finishReason == FinishReason.maxTokens}');
  print('${reply.candidates[0].content.role} ${reply.candidates[0].content.parts.length}');
  final u = reply.usageMetadata!;
  print('${u.promptTokenCount} ${u.candidatesTokenCount} ${u.totalTokenCount}');

  try {
    GenerateContentResponse.cn1FromJson('{"error":{"code":403,"message":"API key not valid"}}');
  } on FirebaseAIException catch (e) {
    print('error ${e.message}');
  }
  try {
    GenerateContentResponse.cn1FromJson('{"promptFeedback":{"blockReason":"SAFETY"}}').text;
  } on FirebaseAIException catch (e) {
    print('blocked ${e.message}');
  }

  try {
    await model.generateContent([Content.text('hello')]);
  } on FirebaseAIException catch (e) {
    print('generate failed');
  }
  try {
    await model.generateContentStream([Content.text('hello')]).toList();
  } on FirebaseAIException catch (e) {
    print('stream failed');
  }
  try {
    await model.countTokens([Content.text('hello')]);
  } on FirebaseAIException catch (e) {
    print('count failed');
  }
  final chat = model.startChat(history: [Content.text('earlier')]);
  try {
    await chat.sendMessage(Content.text('next'));
  } on FirebaseAIException catch (e) {
    print('chat failed ${chat.history.length}');
  }
}
