#!/bin/bash
# Builds a Unity 2D project, as it is, into Java classes that run on the
# Unity compatibility runtime (maven/unity-compat):
#
#   build-unity-project.sh <unity project dir> <out dir> [extra java source dir]...
#
# Nothing is written into the project. The out directory receives
#
#   cs/         a generated .csproj that compiles the project's Assets/**/*.cs
#               in place, against the reference UnityEngine assembly
#   app/        the scripts and the value types, translated to class files
#   gen/        UnityAppImpl.java: the scenes and prefabs as Java source
#   resources/  the images the scenes draw, by file name
#   classes/    everything compiled: app/ plus gen/ plus any extra sources
#
# The scenes compiled are those of ProjectSettings/EditorBuildSettings.asset,
# in that order. What the translator and the scene compiler left out or approximated is printed,
# "warning:" for what changes behaviour and "note:" for what does not.
#
# Needs the .NET SDK (DOTNET names the CLI, else DOTNET_ROOT, else the PATH),
# a JDK on the PATH and the unity-compat profile built:
#
#   cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests \
#       -pl core,cil-translator,unity-compat
set -e
HERE="$( cd "$(dirname "$0")" ; pwd -P )"
ROOT="$( cd "$HERE/../.." ; pwd -P )"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

[ $# -ge 2 ] || { echo "usage: $(basename "$0") <unity project dir> <out dir> [extra java source dir]..." >&2; exit 2; }
[ -d "$1/Assets" ] || fail "$1 has no Assets directory; it is not a Unity project"
PROJECT="$( cd "$1" ; pwd -P )"
mkdir -p "$2"
OUT="$( cd "$2" ; pwd -P )"
shift 2

if [ -n "${DOTNET:-}" ]; then
  [ -x "$DOTNET" ] || fail "DOTNET names $DOTNET, which is not an executable"
elif [ -n "${DOTNET_ROOT:-}" ] && [ -x "$DOTNET_ROOT/dotnet" ]; then
  DOTNET="$DOTNET_ROOT/dotnet"
else
  DOTNET="$(command -v dotnet || true)"
fi
[ -n "$DOTNET" ] || fail "the .NET SDK was not found: set DOTNET to the dotnet executable, or DOTNET_ROOT, or put dotnet on the PATH"
export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1

UNITY_BUILD="(cd maven && mvn install -Plocal-dev-javase -Dunity-compat -DskipTests -pl core,cil-translator,unity-compat)"
CIL_CLASSES="$ROOT/maven/cil-translator/target/classes"
CIL_DEPS="$ROOT/maven/cil-translator/target/dependency"
RUNTIME="$ROOT/maven/unity-compat/target/classes"
CORE="$ROOT/maven/core/target/classes"
CSHARP="$ROOT/maven/unity-compat/src/main/csharp"
for f in "$CIL_CLASSES/com/codename1/cil/translate/Translator.class" \
         "$RUNTIME/com/codename1/unitycompat/unityengine/UnityRuntime.class" \
         "$CORE/com/codename1/gaming/physics/box2d/dynamics/World.class"; do
  [ -e "$f" ] || fail "$f is missing. Build it first: $UNITY_BUILD"
done
ls "$CIL_DEPS"/asm-*.jar > /dev/null 2>&1 || fail "ASM is not in $CIL_DEPS. Build it first: $UNITY_BUILD"
CIL="$CIL_CLASSES:$(ls "$CIL_DEPS"/*.jar | tr '\n' ':')"

# run_logged <log> <what> <command...>: quiet unless it fails.
run_logged() {
  local log="$1" what="$2"
  shift 2
  "$@" > "$log" 2>&1 || { tail -40 "$log" >&2; fail "$what (log: $log)"; }
}

# The scripts are compiled where they are: a link, so that nothing of the
# project is copied and nothing is written beside it. Editor scripts are not
# part of a player build.
rm -rf "$OUT/cs" "$OUT/app" "$OUT/gen" "$OUT/resources" "$OUT/classes"
mkdir -p "$OUT/cs"
cat > "$OUT/cs/Assembly-CSharp.csproj" <<CSPROJ
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>Library</OutputType>
    <TargetFramework>netstandard2.1</TargetFramework>
    <LangVersion>9.0</LangVersion>
    <Nullable>disable</Nullable>
    <ImplicitUsings>disable</ImplicitUsings>
    <AssemblyName>Assembly-CSharp</AssemblyName>
    <Deterministic>true</Deterministic>
    <DebugType>portable</DebugType>
    <Optimize>true</Optimize>
    <GenerateAssemblyInfo>false</GenerateAssemblyInfo>
    <EnableDefaultCompileItems>false</EnableDefaultCompileItems>
    <NoWarn>\$(NoWarn);CS0649;CS0414;CS0169;CS0108;CS0114;CS0618</NoWarn>
  </PropertyGroup>
  <ItemGroup>
    <Compile Include="$PROJECT/Assets/**/*.cs" Exclude="$PROJECT/Assets/**/Editor/**/*.cs" />
    <ProjectReference Include="$CSHARP/UnityEngine/UnityEngine.csproj" />
    <ProjectReference Include="$CSHARP/UnityEngine.Values/UnityEngine.Values.csproj" />
  </ItemGroup>
</Project>
CSPROJ

echo "== C#"
run_logged "$OUT/dotnet.log" "dotnet build of the project's scripts" \
  "$DOTNET" build -c Release --nologo "$OUT/cs/Assembly-CSharp.csproj"
VALUES="$CSHARP/UnityEngine.Values/bin/Release/netstandard2.1/Codename1.UnityValues.dll"
NETSTANDARD="$CSHARP/UnityEngine.Values/bin/Release/netstandard2.1/netstandard-ref/netstandard.dll"
ENGINE="$CSHARP/UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll"
SCRIPTS="$OUT/cs/bin/Release/netstandard2.1/Assembly-CSharp.dll"
for f in "$VALUES" "$NETSTANDARD" "$ENGINE" "$SCRIPTS"; do
  [ -f "$f" ] || fail "the C# build produced no $f"
done

echo "== translation"
run_logged "$OUT/translate.log" "translation of the scripts" \
  java -cp "$CIL" com.codename1.cil.translate.Translator --out "$OUT/app" --runtime "$RUNTIME" \
    --ref "$ENGINE" --ref "$NETSTANDARD" "$VALUES" "$SCRIPTS"

echo "== scenes"
run_logged "$OUT/scene-compiler.log" "the scene compiler" \
  java -Djava.awt.headless=true -cp "$CIL" com.codename1.unity.scenecompiler.SceneCompiler --project "$PROJECT" \
    --out "$OUT/gen" --resources "$OUT/resources" --ref "$ENGINE" --ref "$NETSTANDARD" "$SCRIPTS" "$VALUES"
[ -f "$OUT/gen/com/codename1/generated/unity/UnityAppImpl.java" ] || fail "the scene compiler wrote no UnityAppImpl.java"
# The translator warns too -- a Unity message nothing sends, for one.
grep -hE '^(warning|note):' "$OUT/translate.log" "$OUT/scene-compiler.log" || true
echo "   $(cat "$OUT/translate.log" "$OUT/scene-compiler.log" | grep -c '^warning:' || true) warnings, $(grep -c '^note:' "$OUT/scene-compiler.log" || true) notes"

echo "== javac"
mkdir -p "$OUT/classes" "$OUT/resources"
cp -R "$OUT/app/." "$OUT/classes/"
find "$OUT/gen" "$@" -name '*.java' > "$OUT/java.sources"
run_logged "$OUT/javac.log" "javac of the generated scenes" \
  javac -nowarn -source 8 -target 8 -Xlint:-options -d "$OUT/classes" -cp "$RUNTIME:$OUT/app:$CORE" "@$OUT/java.sources"
echo "built $OUT/classes ($(find "$OUT/classes" -name '*.class' | wc -l | tr -d ' ') classes) and $OUT/resources ($(ls "$OUT/resources" | wc -l | tr -d ' ') images)"
echo "run with: -cp $OUT/classes:$OUT/resources:$RUNTIME:$CORE"
