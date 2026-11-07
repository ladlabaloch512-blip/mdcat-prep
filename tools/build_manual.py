#!/usr/bin/env python3
"""Manual Android build driver (used because the Gradle daemon's NIO socket
layer does not work in this VM: daemon receives nothing, client reads EOF).
Reproduces what AGP does: extract AARs, merge manifests, aapt2 compile/link,
generate library R classes, kotlinc, d8, zipalign, apksigner, bundletool.
"""
import copy, os, re, shutil, subprocess, sys, zipfile
import xml.etree.ElementTree as ET

HOME = os.path.expanduser("~")
PROJ = f"{HOME}/workspace/nawab-trader/build/android"
WORK = f"{HOME}/workspace/nawab-trader/build/manual"
REPO = f"{HOME}/tools/local-maven"
SDK = f"{HOME}/android-sdk"
BT = f"{SDK}/build-tools/34.0.0"
ANDROID_JAR = f"{SDK}/platforms/android-34/android.jar"
JAVA = f"{HOME}/tools/jdk17/bin/java"
JAVAC = f"{HOME}/tools/jdk17/bin/javac"
KOTLINC = f"{HOME}/tools/kotlinc/bin/kotlinc"
AAPT2 = f"{BT}/aapt2"
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)

def java_env():
    env = dict(os.environ)
    env["JAVA_HOME"] = f"{HOME}/tools/jdk17"
    env["PATH"] = f"{HOME}/tools/jdk17/bin:" + env.get("PATH", "")
    return env

def run(cmd, **kw):
    print("+", " ".join(cmd[:8]), "..." if len(cmd) > 8 else "")
    kw.setdefault("env", java_env())
    r = subprocess.run(cmd, capture_output=True, text=True, **kw)
    if r.returncode != 0:
        print("FAILED:", " ".join(cmd))
        print(r.stdout[-4000:]); print(r.stderr[-4000:])
        sys.exit(1)
    return r

def aname(el):
    return el.get(f"{{{ANDROID_NS}}}name")

# ---------------------------------------------------------------- phase A
def extract_aars():
    aar_root = f"{WORK}/aars"
    libs = []
    for base in ("androidx", "com/google"):
        for dirpath, _dirs, files in os.walk(f"{REPO}/{base}"):
            for f in files:
                if f.endswith(".aar"):
                    libs.append(os.path.join(dirpath, f))
    libs.sort()
    for aar in libs:
        name = os.path.basename(aar)[:-4]
        dest = os.path.join(aar_root, name)
        if os.path.exists(dest):
            continue
        os.makedirs(dest, exist_ok=True)
        with zipfile.ZipFile(aar) as z:
            z.extractall(dest)
    print(f"AARs extracted: {len(libs)}")
    return sorted(os.listdir(aar_root))

# ---------------------------------------------------------------- phase B
MERGE_APP_TAGS = ("activity", "activity-alias", "service", "receiver",
                  "provider", "meta-data", "uses-library")

def merge_manifests(lib_names):
    app_manifest = f"{PROJ}/app/src/main/AndroidManifest.xml"
    tree = ET.parse(app_manifest)
    root = tree.getroot()
    if not root.get("package"):
        # The Gradle build takes the package from the namespace in
        # app/build.gradle; aapt2 needs it in the manifest itself.
        root.set("package", "com.abiz.mdcatprep")
    app_el = root.find("application")

    def key_of(el):
        return (el.tag, aname(el))

    seen = {key_of(c) for c in app_el}
    existing = {key_of(c): c for c in app_el}
    perm_seen = set()
    for p in root.findall("uses-permission"):
        perm_seen.add(aname(p))

    for name in lib_names:
        mpath = f"{WORK}/aars/{name}/AndroidManifest.xml"
        if not os.path.exists(mpath):
            continue
        lroot = ET.parse(mpath).getroot()
        for p in lroot.findall("uses-permission"):
            if aname(p) not in perm_seen:
                perm_seen.add(aname(p))
                root.append(p)
        for q in lroot.findall("queries"):
            root.append(q)
        lapp = lroot.find("application")
        if lapp is None:
            continue
        for child in lapp:
            if child.tag not in MERGE_APP_TAGS:
                continue
            if key_of(child) not in seen:
                seen.add(key_of(child))
                existing[key_of(child)] = child
                app_el.append(child)
            else:
                # Same component declared by several AARs (the classic
                # case: androidx.startup.InitializationProvider appears
                # in emoji2, lifecycle-process, profileinstaller and
                # startup-runtime, each contributing DIFFERENT meta-data
                # children). AGP unions the meta-data; taking only the
                # first provider dropped the ProcessLifecycleInitializer
                # entry, and EmojiCompatInitializer depends on it, so
                # the app crashed before the first activity with
                # "ProcessLifecycleInitializer cannot be initialized
                # lazily" (device log, 2026-11-07). Union the children.
                target = existing[key_of(child)]
                have = {aname(md) for md in target.findall("meta-data")}
                for md in child.findall("meta-data"):
                    if aname(md) not in have:
                        have.add(aname(md))
                        target.append(copy.deepcopy(md))
    # AGP substitutes the ${applicationId} placeholder in authorities.
    pkg = root.get("package")
    for prov in app_el.findall("provider"):
        auth = prov.get(f"{{{ANDROID_NS}}}authorities")
        if auth and "${applicationId}" in auth:
            prov.set(f"{{{ANDROID_NS}}}authorities",
                     auth.replace("${applicationId}", pkg))
    out = f"{WORK}/merged-manifest.xml"
    tree.write(out, xml_declaration=True, encoding="utf-8")
    print("manifest merged ->", out)
    return out

# ---------------------------------------------------------------- phase C
def aapt2_build(manifest):
    comp = f"{WORK}/compiled"
    os.makedirs(comp, exist_ok=True)
    inputs = []
    def compile_res(resdir, tag):
        if not os.path.isdir(resdir):
            return
        out = f"{comp}/{tag}.zip"
        run([AAPT2, "compile", "--dir", resdir, "-o", out])
        inputs.append(out)
    compile_res(f"{PROJ}/app/src/main/res", "app")
    lib_names = sorted(os.listdir(f"{WORK}/aars"))
    for name in lib_names:
        compile_res(f"{WORK}/aars/{name}/res", f"lib-{name}")
    gen = f"{WORK}/gen"
    os.makedirs(gen, exist_ok=True)
    base = [AAPT2, "link", "-I", ANDROID_JAR, "--manifest", manifest,
            "--java", gen, "--output-text-symbols", f"{WORK}/symbols.txt",
            "--min-sdk-version", "24", "--target-sdk-version", "34",
            "--version-code", "1", "--version-name", "1.0"]
    run(base + ["-o", f"{WORK}/linked.apk"] + inputs)
    run(base + ["--proto-format", "-o", f"{WORK}/linked-proto.apk"] + inputs)
    print("aapt2 link done")
    return inputs

# ---------------------------------------------------------------- phase D
RTYPE_RE = re.compile(r"^int (\w+) (\S+) (0x[0-9a-fA-F]+)$")
RTYPE_ARR_RE = re.compile(r"^int\[\] styleable (\S+) \{ ([^}]*) \}$")
RSTYLE_IDX_RE = re.compile(r"^int styleable (\S+) (\d+)$")

def parse_rtxt(path):
    """Return dict[(type, name)] = raw value string, plus styleable arrays."""
    entries, arrays = {}, {}
    if not os.path.exists(path):
        return entries, arrays
    text = open(path).read()
    for line in text.splitlines():
        line = line.strip()
        m = RTYPE_ARR_RE.match(line)
        if m:
            arrays[m.group(1)] = [v.strip() for v in m.group(2).split(",") if v.strip()]
            continue
        m = RTYPE_RE.match(line)
        if m:
            entries[(m.group(1), m.group(2))] = m.group(3)
            continue
        m = RSTYLE_IDX_RE.match(line)
        if m:
            entries[("styleable_idx", m.group(1))] = m.group(2)
    return entries, arrays

def gen_library_r(lib_names):
    symbols, sym_arrays = parse_rtxt(f"{WORK}/symbols.txt")
    genroot = f"{WORK}/rjava"
    count = 0
    for name in lib_names:
        adir = f"{WORK}/aars/{name}"
        rtxt = f"{adir}/R.txt"
        if not os.path.exists(rtxt):
            continue
        man = ET.parse(f"{adir}/AndroidManifest.xml").getroot()
        pkg = man.get("package")
        if not pkg:
            continue
        entries, arrays = parse_rtxt(rtxt)
        if not entries and not arrays:
            continue
        types = {}
        for (t, n) in entries:
            types.setdefault(t, []).append(n)
        lines = [f"package {pkg};", "public final class R {"]
        for t in sorted(types):
            if t == "styleable_idx":
                continue
            lines.append(f"  public static final class {t} {{")
            for n in sorted(types[t]):
                val = symbols.get((t, n), "0")
                lines.append(f"    public static int {n} = {val};")
            lines.append("  }")
        if arrays:
            lines.append("  public static final class styleable {")
            # Assign each index field to the LONGEST styleable name it
            # prefixes, so every field is emitted exactly once.
            idx_names = [n for (t, n) in entries if t == "styleable_idx"]
            owner = {}
            for n in idx_names:
                cands = [a for a in arrays if n.startswith(a + "_")]
                if cands:
                    owner[n] = max(cands, key=len)
            emitted = set()
            for aname_ in sorted(arrays):
                vals = sym_arrays.get(aname_, arrays[aname_])
                lines.append(f"    public static int[] {aname_} = {{ {', '.join(vals)} }};")
                for n in sorted(idx_names):
                    if owner.get(n) == aname_ and n not in emitted:
                        emitted.add(n)
                        # Index values MUST come from the final link
                        # symbols: aapt2 sorts styleable attrs at link
                        # time, so the AAR R.txt indices are placeholders
                        # (mostly 0). Using them made every TypedArray
                        # read in appcompat read position 0 (fixed
                        # 2026-11-07, launch crash round 2).
                        idx_val = symbols.get(("styleable_idx", n),
                                              entries[("styleable_idx", n)])
                        lines.append(f"    public static int {n} = {idx_val};")
            lines.append("  }")
        lines.append("}")
        dest = os.path.join(genroot, *pkg.split("."), "R.java")
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "w") as fh:
            fh.write("\n".join(lines))
        count += 1
    print(f"library R.java generated for {count} libraries")

def find_jar(relpath):
    p = os.path.join(REPO, relpath)
    return p if os.path.exists(p) else None

def plain_jars():
    cand = [
        "androidx/annotation/annotation/1.6.0/annotation-1.6.0.jar",
        "androidx/arch/core/core-common/2.2.0/core-common-2.2.0.jar",
        "androidx/collection/collection/1.1.0/collection-1.1.0.jar",
        "androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.jar",
        "androidx/constraintlayout/constraintlayout-core/1.0.4/constraintlayout-core-1.0.4.jar",
        "androidx/lifecycle/lifecycle-common/2.6.1/lifecycle-common-2.6.1.jar",
        # Transitive runtime deps that MUST be dexed (fix 2026-11-07):
        # lifecycle/fragment/activity reference kotlinx-coroutines, and
        # emoji2 references flatbuffers. Missing them crashed the app at
        # startup with NoClassDefFoundError.
        "org/jetbrains/kotlinx/kotlinx-coroutines-android/1.6.4/kotlinx-coroutines-android-1.6.4.jar",
        "org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.6.4/kotlinx-coroutines-core-jvm-1.6.4.jar",
        "com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0.jar",
        "org/jetbrains/kotlin/kotlin-stdlib/1.9.24/kotlin-stdlib-1.9.24.jar",
        "com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar",
        "com/google/errorprone/error_prone_annotations/2.18.0/error_prone_annotations-2.18.0.jar",
        "com/google/j2objc/j2objc-annotations/2.8/j2objc-annotations-2.8.jar",
        "org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.jar",
        "org/jetbrains/annotations/23.0.0/annotations-23.0.0.jar",
    ]
    out = [p for p in (find_jar(c) for c in cand) if p]
    out.append(f"{WORK}/libs/listenablefuture-1.0.jar")
    return out

def aar_classes_jars():
    out = []
    for name in sorted(os.listdir(f"{WORK}/aars")):
        cj = f"{WORK}/aars/{name}/classes.jar"
        if os.path.exists(cj):
            out.append(cj)
        # AAR-bundled jars (e.g. emoji2's libs/repackaged.jar, which holds
        # the flatbuffer MetadataList/MetadataItem classes that the
        # EmojiCompatInitializer needs at app startup). Missing these was
        # the 2026-11-07 launch crash.
        libs = f"{WORK}/aars/{name}/libs"
        if os.path.isdir(libs):
            for f in sorted(os.listdir(libs)):
                if f.endswith(".jar"):
                    out.append(os.path.join(libs, f))
    return out

def compile_r():
    sources = []
    for base in (f"{WORK}/rjava", f"{WORK}/gen"):
        for dirpath, _d, files in os.walk(base):
            for f in files:
                if f.endswith(".java"):
                    sources.append(os.path.join(dirpath, f))
    out = f"{WORK}/rclasses"
    os.makedirs(out, exist_ok=True)
    run([JAVAC, "--release", "8", "-nowarn", "-cp", ANDROID_JAR, "-d", out] + sources)
    print(f"R classes compiled: {len(sources)} files")

def compile_kotlin():
    cp = ":".join([ANDROID_JAR] + aar_classes_jars() + plain_jars() + [f"{WORK}/rclasses"])
    srcs = []
    for dirpath, _d, files in os.walk(f"{PROJ}/app/src/main/java"):
        for f in files:
            if f.endswith(".kt"):
                srcs.append(os.path.join(dirpath, f))
    out = f"{WORK}/appclasses"
    os.makedirs(out, exist_ok=True)
    run([KOTLINC, "-cp", cp, "-jvm-target", "1.8", "-no-stdlib", "-d", out] + srcs)
    print(f"Kotlin compiled: {len(srcs)} files")

def dex():
    out = f"{WORK}/dex"
    os.makedirs(out, exist_ok=True)
    # d8 in build-tools 34 rejects bare class directories here; feed jars.
    for name in ("appclasses", "rclasses"):
        jar = f"{WORK}/{name}.jar"
        if os.path.exists(jar):
            os.remove(jar)
        run([f"{HOME}/tools/jdk17/bin/jar", "cf", jar, "-C", f"{WORK}/{name}", "."])
    inputs = [f"{WORK}/appclasses.jar", f"{WORK}/rclasses.jar"] + aar_classes_jars() + plain_jars()
    # Supplementary gms R classes (base R$color/$drawable/$string, common
    # R$string) generated by gen_rfix.py from the final symbol table; the
    # per-AAR R.txt files only declare styleable for those packages.
    if os.path.exists(f"{WORK}/rfix.jar"):
        inputs.append(f"{WORK}/rfix.jar")
    env = dict(os.environ)
    env["JAVA_HOME"] = f"{HOME}/tools/jdk17"
    env["PATH"] = f"{HOME}/tools/jdk17/bin:" + env.get("PATH", "")
    # build-tools 34's bundled R8 (8.2.2-dev) NPEs on appcompat 1.7.0's
    # ActionBarDrawerToggle$1.class; standalone R8 8.7.18 handles it.
    r = subprocess.run([JAVA, "-cp", f"{HOME}/tools/r8-8.7.18.jar",
                        "com.android.tools.r8.D8",
                        "--lib", ANDROID_JAR, "--min-api", "24",
                        "--output", out] + inputs,
                       capture_output=True, text=True, env=env)
    print(r.stdout[-1500:]); print(r.stderr[-3000:])
    if r.returncode != 0:
        print("D8 FAILED"); sys.exit(1)
    print("dex done:", os.listdir(out))

def jar_resources(dest_dir, jar_root=""):
    """Copy non-class resources of all dependency jars into dest_dir.
    For a bundle module, jar resources belong under root/ (jar_root)."""
    skip_prefixes = ("META-INF/",)
    skip_suffixes = (".class",)
    count = 0
    for jar in aar_classes_jars() + plain_jars():
        with zipfile.ZipFile(jar) as z:
            for info in z.infolist():
                n = info.filename
                if n.endswith("/") or n.endswith(skip_suffixes):
                    continue
                if n.startswith(skip_prefixes) and not n.startswith("META-INF/services/"):
                    continue
                target = os.path.join(dest_dir, jar_root, n)
                os.makedirs(os.path.dirname(target), exist_ok=True)
                if not os.path.exists(target):
                    with open(target, "wb") as fh:
                        fh.write(z.read(n))
                    count += 1
    # AAR assets
    for name in sorted(os.listdir(f"{WORK}/aars")):
        adir = f"{WORK}/aars/{name}/assets"
        if os.path.isdir(adir):
            for dirpath, _d, files in os.walk(adir):
                for f in files:
                    src = os.path.join(dirpath, f)
                    rel = os.path.relpath(src, f"{WORK}/aars/{name}")
                    target = os.path.join(dest_dir, rel)
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    shutil.copyfile(src, target)
                    count += 1
    print(f"jar/asset resources staged: {count}")
    return count

def package_apk():
    stage = f"{WORK}/apk-stage"
    if os.path.exists(stage):
        shutil.rmtree(stage)
    os.makedirs(stage)
    with zipfile.ZipFile(f"{WORK}/linked.apk") as z:
        z.extractall(stage)
    shutil.copyfile(f"{WORK}/dex/classes.dex", f"{stage}/classes.dex")
    jar_resources(stage)
    unsigned = f"{WORK}/unsigned.apk"
    if os.path.exists(unsigned):
        os.remove(unsigned)
    # resources.arsc and PNGs MUST be stored uncompressed: Android's
    # installer rejects an APK whose resources.arsc is deflated
    # ("problem parsing the package", found 2026-11-07).
    with zipfile.ZipFile(unsigned, "w", zipfile.ZIP_DEFLATED) as z:
        for dirpath, _d, files in os.walk(stage):
            for f in files:
                full = os.path.join(dirpath, f)
                arc = os.path.relpath(full, stage)
                if f == "resources.arsc" or f.lower().endswith(".png"):
                    z.write(full, arc, compress_type=zipfile.ZIP_STORED)
                else:
                    z.write(full, arc)
    aligned = f"{WORK}/aligned.apk"
    run([f"{BT}/zipalign", "-f", "-p", "4", unsigned, aligned])
    props = {}
    for line in open(f"{PROJ}/keystore.properties"):
        if "=" in line:
            k, v = line.strip().split("=", 1)
            props[k] = v
    final = f"{WORK}/MDCAT-Prep.apk"
    run([f"{BT}/apksigner", "sign", "--ks", f"{PROJ}/release.keystore",
         "--ks-pass", f"pass:{props['storePassword']}",
         "--key-pass", f"pass:{props['keyPassword']}",
         "--ks-key-alias", props["keyAlias"],
         "--out", final, aligned])
    run([f"{BT}/apksigner", "verify", "--print-certs", final])
    r = run([f"{BT}/aapt", "dump", "badging", final])
    print(r.stdout)
    print("APK built:", final, os.path.getsize(final))

def package_aab():
    stage = f"{WORK}/module-stage"
    if os.path.exists(stage):
        shutil.rmtree(stage)
    os.makedirs(f"{stage}/dex")
    with zipfile.ZipFile(f"{WORK}/linked-proto.apk") as z:
        z.extractall(stage)
    # Bundle modules keep the manifest under manifest/, not at the root.
    os.makedirs(f"{stage}/manifest", exist_ok=True)
    shutil.move(f"{stage}/AndroidManifest.xml", f"{stage}/manifest/AndroidManifest.xml")
    shutil.copyfile(f"{WORK}/dex/classes.dex", f"{stage}/dex/classes.dex")
    jar_resources(stage, jar_root="root")
    base_zip = f"{WORK}/base.zip"
    if os.path.exists(base_zip):
        os.remove(base_zip)
    with zipfile.ZipFile(base_zip, "w", zipfile.ZIP_DEFLATED) as z:
        for dirpath, _d, files in os.walk(stage):
            for f in files:
                full = os.path.join(dirpath, f)
                z.write(full, os.path.relpath(full, stage))
    bundletool = f"{HOME}/tools/bundletool-1.16.0-all.jar"
    aab = f"{WORK}/MDCAT-Prep.aab"
    run([JAVA, "-jar", bundletool, "build-bundle", "--modules", base_zip,
         "--output", aab, "--overwrite"])
    props = {}
    for line in open(f"{PROJ}/keystore.properties"):
        if "=" in line:
            k, v = line.strip().split("=", 1)
            props[k] = v
    run([f"{HOME}/tools/jdk17/bin/jarsigner", "-keystore", f"{PROJ}/release.keystore",
         "-storepass", props["storePassword"], "-keypass", props["keyPassword"],
         aab, props["keyAlias"]])
    run([JAVA, "-jar", bundletool, "validate", "--bundle", aab])
    apks = f"{WORK}/from-bundle.apks"
    run([JAVA, "-jar", bundletool, "build-apks", "--bundle", aab, "--output", apks,
         "--mode", "universal", "--overwrite",
         "--ks", f"{PROJ}/release.keystore", "--ks-pass", f"pass:{props['storePassword']}",
         "--ks-key-alias", props["keyAlias"], "--key-pass", f"pass:{props['keyPassword']}"])
    with zipfile.ZipFile(apks) as z:
        z.extractall(f"{WORK}/from-bundle")
    r = run([f"{BT}/aapt", "dump", "badging", f"{WORK}/from-bundle/universal.apk"])
    print(r.stdout)
    print("AAB built:", aab, os.path.getsize(aab))

if __name__ == "__main__":
    phase = sys.argv[1] if len(sys.argv) > 1 else "all"
    lib_names = extract_aars()
    if phase in ("all", "merge"):
        manifest = merge_manifests(lib_names)
    if phase in ("all", "link"):
        manifest = f"{WORK}/merged-manifest.xml"
        aapt2_build(manifest)
    if phase in ("all", "rgen"):
        gen_library_r(lib_names)
    if phase in ("all2", "rcompile"):
        compile_r()
    if phase in ("all2", "kcompile"):
        compile_kotlin()
    if phase in ("all2", "dex"):
        dex()
    if phase in ("all2", "apk"):
        package_apk()
    if phase in ("all2", "aab"):
        package_aab()
    print("phase done:", phase)
