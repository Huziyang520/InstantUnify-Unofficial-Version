package com.huziyang520.instantunify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Rewrites the list values of our own configuration file so that every entry has its own line.
 * <p>
 * Forge writes lists inline and offers no way to change that (its writer cannot be replaced through any public
 * API), so the file is reflowed afterwards. This is a pure text transformation: entries are copied verbatim, so
 * values, comments and the order of the keys can not change. Anything not understood is left exactly as it is.
 * <p>
 * Forge saves the same file from its own threads, so the new content is written to a temporary file and moved
 * into place in one step, and every failure is swallowed: a formatting problem may never break the game, and
 * it may never leave a half written configuration behind.
 */
public final class ConfigFileFormatter {

    private static final Set<String> LIST_KEYS = Set.of(
            "blacklist", "whitelist", "preferredMods", "blacklistMods",
            "blacklistItems", "alternatives", "namespaceAliases");

    private static final int ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 100L;

    private ConfigFileFormatter() {
    }

    public static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(InstantUnify.MOD_ID + "-common.toml");
    }

    public static synchronized void format() {
        Path file = configFile();
        Path temp = file.resolveSibling(file.getFileName() + ".reflow.tmp");
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            try {
                if (!Files.isRegularFile(file)) {
                    return;
                }
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                String reflowed = reflow(text);
                if (reflowed.equals(text)) {
                    return;
                }
                Files.write(temp, reflowed.getBytes(StandardCharsets.UTF_8));
                move(temp, file);
                return;
            } catch (Exception e) {
                // Forge may be writing the very same file right now. Back off, and in the end give up quietly.
                if (attempt == ATTEMPTS - 1) {
                    InstantUnify.LOG.warn("Left {} as it is: {}", file.getFileName(), e.toString());
                } else {
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally {
                deleteQuietly(temp);
            }
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Nothing to do about it.
        }
    }

    static String reflow(String text) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        StringBuilder out = new StringBuilder(text.length() + 512);
        boolean changed = false;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            String indent = leadingWhitespace(line);
            String body = line.substring(indent.length());
            int equals = body.indexOf('=');
            String key = equals < 0 ? "" : body.substring(0, equals).trim();

            if (!LIST_KEYS.contains(key) || !isArrayStart(body, equals)) {
                appendLine(out, line, newline, i < lines.size() - 1);
                continue;
            }

            String collected = body.substring(equals + 1).trim();
            String tail = "";
            boolean closed = false;
            while (true) {
                int end = endOfArray(collected);
                if (end >= 0) {
                    tail = collected.substring(end + 1).trim();
                    collected = collected.substring(0, end + 1);
                    closed = true;
                    break;
                }
                if (i + 1 >= lines.size()) {
                    break;
                }
                String next = lines.get(++i);
                collected = collected + newline + (next.endsWith("\r") ? next.substring(0, next.length() - 1) : next);
            }

            List<String> elements = closed ? splitTopLevel(collected) : List.of();
            boolean understood = !elements.isEmpty()
                    && elements.stream().allMatch(e -> e.startsWith("\"") || e.startsWith("["));

            if (!understood) {
                appendLine(out, line, newline, i < lines.size() - 1);
                continue;
            }

            out.append(indent).append(key).append(" = [");
            for (int e = 0; e < elements.size(); e++) {
                out.append(newline).append(indent).append('\t').append(elements.get(e));
                if (e < elements.size() - 1) {
                    out.append(',');
                }
            }
            out.append(newline).append(indent).append(']');
            if (!tail.isEmpty()) {
                out.append(' ').append(tail);
            }
            appendLine(out, "", newline, i < lines.size() - 1);
            changed = true;
        }
        return changed ? out.toString() : text;
    }

    private static void appendLine(StringBuilder out, String line, String newline, boolean more) {
        out.append(line);
        if (more) {
            out.append(newline);
        }
    }

    private static String leadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }
        return s.substring(0, i);
    }

    private static boolean isArrayStart(String body, int equals) {
        return equals >= 0 && body.substring(equals + 1).trim().startsWith("[");
    }

    /** @return index of the ']' closing the first '[' in the text, or -1 when it is not complete yet */
    private static int endOfArray(String text) {
        int start = text.indexOf('[');
        if (start < 0) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitTopLevel(String array) {
        String inner = array.substring(1, array.length() - 1);
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (inString) {
                current.append(c);
                if (c == '\\') {
                    if (i + 1 < inner.length()) {
                        current.append(inner.charAt(++i));
                    }
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                current.append(c);
            } else if (c == '[') {
                depth++;
                current.append(c);
            } else if (c == ']') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().trim().isEmpty()) {
            parts.add(current.toString().trim());
        }
        parts.removeIf(String::isEmpty);
        return parts;
    }
}
