# LLM Mobile

[`llm`](https://llm.datasette.io) by Simon Willison, the command line and
library for talking to language models, as a native iOS and Android
application. The package and everything it depends on are the releases on
PyPI, unmodified; only the user interface in `src/main.py` is new.

- **Chat** with any model llm knows, streamed as it arrives, with
  conversation context kept between turns.
- **History**: the same `logs.db` the command line writes, newest first,
  with search through the full-text index llm maintains. A `logs.db` or
  `keys.json` copied from a laptop works unchanged.
- **Settings**: the model, the OpenAI key (stored in llm's `keys.json`), an
  OpenAI-compatible endpoint such as Ollama, LM Studio or a proxy, and a
  system prompt.

## Build it

```bash
cd myapp   # a Codename One application, e.g. from https://start.codenameone.com
mvn cn1:import-python-project -Dsource=/path/to/llm-mobile
mvn cn1:run   # simulator
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-device
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device
```

`requirements.txt` pins llm and every package it pulls in to the versions
llm's own test suite was run against.

## What runs, and what doesn't

llm's library runs as it does under CPython: models, conversations, options,
tools and toolboxes, schemas, fragments, attachments, logging and
embeddings (similarity search included). Its command line, built on
`click`, isn't part of the application, so neither are the commands that
install plugins with pip at run time: plugins are requirements, built in.
`pip`, `setuptools` and `wheel` are never installed into an application.
`readline` exists so that `llm.cli` imports, and keeps history in memory.
