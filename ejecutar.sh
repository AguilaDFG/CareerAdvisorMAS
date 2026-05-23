#!/usr/bin/env bash
# ================================================================
#  CareerAdvisor-MAS  —  Lanzador macOS / Linux
#  Doble clic desde Finder/gestor de archivos, o: ./ejecutar.sh
# ================================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
BIN="$ROOT/bin"
LIB="$ROOT/lib/jade.jar"
MAIN="es.upm.careeradvisor.MainLauncher"

error() {
    echo ""
    echo "  [ERROR] $1"
    echo ""
    # Diálogo gráfico si hay entorno de escritorio
    command -v osascript &>/dev/null && \
        osascript -e "display alert \"CareerAdvisor-MAS\" message \"$1\"" 2>/dev/null
    command -v zenity   &>/dev/null && zenity  --error --text="$1" 2>/dev/null
    command -v kdialog  &>/dev/null && kdialog --error "$1"         2>/dev/null
    read -rp "  Pulsa INTRO para cerrar..." _
    exit 1
}

# ── 1. Comprobar Java ──────────────────────────────────────────
command -v java  &>/dev/null || error "No se encontró 'java'.\nInstala el JDK desde https://adoptium.net/"
command -v javac &>/dev/null || error "No se encontró 'javac'.\nAsegúrate de instalar el JDK (no solo el JRE)."

# ── 2. Comprobar jade.jar ──────────────────────────────────────
[ -f "$LIB" ] || error "No se encontró lib/jade.jar\nDescarga JADE desde https://jade.tilab.com/ y cópialo en lib/"

# ── 3. Compilar si bin no existe o está vacío ──────────────────
NEEDS_COMPILE=0
[ ! -d "$BIN" ] && NEEDS_COMPILE=1
[ "$NEEDS_COMPILE" -eq 0 ] && \
    [ -z "$(find "$BIN" -name '*.class' 2>/dev/null)" ] && NEEDS_COMPILE=1

if [ "$NEEDS_COMPILE" -eq 1 ]; then
    echo ""
    echo "  Compilando el proyecto..."
    mkdir -p "$BIN"
    find "$SRC" -name "*.java" > /tmp/careeradvisor_sources.txt
    javac -encoding UTF-8 -cp "$LIB" -d "$BIN" @/tmp/careeradvisor_sources.txt
    [ $? -eq 0 ] || error "La compilación ha fallado.\nRevisa la terminal para ver los errores."
    echo "  Compilación correcta."
fi

# ── 4. Ejecutar ────────────────────────────────────────────────
echo ""
echo "  Iniciando CareerAdvisor-MAS..."
echo ""
java -cp "$BIN:$LIB" "$MAIN"
