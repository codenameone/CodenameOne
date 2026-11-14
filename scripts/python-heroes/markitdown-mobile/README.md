# MarkItDown Mobile

Microsoft's [MarkItDown](https://github.com/microsoft/markitdown), which
turns documents into Markdown for language models, as a native iOS and
Android application. The package and its converters' dependencies are the
releases on PyPI, unmodified; only the user interface in `src/main.py` is
new.

Open a PDF, Word, PowerPoint, Excel, HTML, EPUB, CSV, notebook or ZIP file,
or enter a URL, and read, copy or share the Markdown. Conversion runs in the
application: the document never leaves the device.

## Build it

```bash
cd myapp   # a Codename One application, e.g. from https://start.codenameone.com
mvn cn1:import-python-project -Dsource=/path/to/markitdown-mobile
mvn cn1:run
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-device
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device
```

## What runs, and what doesn't

| Input | Status |
|---|---|
| PDF (pdfminer.six, pdfplumber) | Converted on the device |
| DOCX (mammoth), PPTX (python-pptx), XLSX (openpyxl) | Converted on the device |
| HTML, RSS/Atom, Wikipedia, YouTube pages, EPUB, CSV, JSON, XML, notebooks, ZIP | Converted on the device |
| Legacy XLS, Outlook MSG | Not supported: `xlrd` and `olefile` aren't bundled |
| Audio transcription, image description | Not supported: they call a speech or vision service |

File types are detected by the runtime's `magika`, which reads file
signatures where Google's runs a neural model through onnxruntime. Text
encodings are UTF-8, UTF-16, ASCII and Latin-1; a file in another legacy
encoding (Shift-JIS, GB18030, Windows-1252) fails to decode rather than
being guessed.
