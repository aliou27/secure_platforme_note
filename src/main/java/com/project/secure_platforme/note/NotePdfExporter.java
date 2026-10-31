package com.project.secure_platforme.note;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotePdfExporter {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50f;
    private static final float HEADER_SIZE = 18f;
    private static final float TITLE_SIZE = 13f;
    private static final float BODY_SIZE = 11f;
    private static final float META_SIZE = 9f;
    private static final float LINE_SPACING = 1.35f;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] export(String username, List<Note> notes, long totalNotes, Instant exportedAt) {
        try (PDDocument document = new PDDocument()) {
            Layout layout = new Layout(document);
            try {
                layout.writeWrapped("Notes of " + username, layout.bold, HEADER_SIZE);
                layout.writeWrapped("Exported " + DATE_TIME.format(exportedAt) + " | " + summary(notes.size(), totalNotes),
                        layout.regular, META_SIZE);
                layout.space(BODY_SIZE);

                if (notes.isEmpty()) {
                    layout.writeWrapped("You have no notes yet.", layout.regular, BODY_SIZE);
                }

                for (Note note : notes) {
                    layout.writeWrapped(note.getTitle(), layout.bold, TITLE_SIZE);
                    layout.writeWrapped("Created " + format(note.getCreatedAt()) + " | Updated " + format(note.getUpdatedAt()),
                            layout.regular, META_SIZE);
                    layout.space(4f);
                    layout.writeWrapped(note.getContent(), layout.regular, BODY_SIZE);
                    layout.space(BODY_SIZE * 1.5f);
                }
            } finally {
                layout.close();
            }

            addPageNumbers(document, layout.regular);

            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("Notes export");
            info.setCreator("Secure Notes");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate PDF export", e);
        }
    }

    private static String summary(int exported, long total) {
        if (exported == total) {
            return total + (total == 1 ? " note" : " notes");
        }
        return "showing the " + exported + " most recent of " + total + " notes";
    }

    private static String format(Instant instant) {
        return instant == null ? "unknown" : DATE_TIME.format(instant);
    }

    private static void addPageNumbers(PDDocument document, PDFont font) throws IOException {
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream stream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                String label = "Page " + (i + 1) + " of " + total;
                float width = font.getStringWidth(label) / 1000f * META_SIZE;
                stream.beginText();
                stream.setFont(font, META_SIZE);
                stream.newLineAtOffset((PAGE_SIZE.getWidth() - width) / 2, MARGIN / 2);
                stream.showText(label);
                stream.endText();
            }
        }
    }

    private static final class Layout implements AutoCloseable {

        private final PDDocument document;
        private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final Map<Integer, Boolean> encodable = new HashMap<>();
        private final float maxWidth = PAGE_SIZE.getWidth() - 2 * MARGIN;
        private PDPageContentStream stream;
        private float y;

        private Layout(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void writeWrapped(String text, PDFont font, float size) throws IOException {
            for (String line : wrap(sanitize(text), font, size)) {
                writeLine(line, font, size);
            }
        }

        private void space(float height) {
            y -= height;
        }

        private void writeLine(String line, PDFont font, float size) throws IOException {
            float leading = size * LINE_SPACING;
            if (y - leading < MARGIN) {
                newPage();
            }
            y -= leading;
            if (line.isEmpty()) {
                return;
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(line);
            stream.endText();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_SIZE.getHeight() - MARGIN;
        }

        private String sanitize(String text) {
            if (text == null) {
                return "";
            }
            StringBuilder result = new StringBuilder(text.length());
            text.codePoints().forEach(codePoint -> {
                if (codePoint == '\n') {
                    result.append('\n');
                } else if (codePoint == '\t') {
                    result.append("    ");
                } else if (Character.isISOControl(codePoint)) {
                    return;
                } else if (canEncode(codePoint)) {
                    result.appendCodePoint(codePoint);
                } else {
                    result.append('?');
                }
            });
            return result.toString();
        }

        private boolean canEncode(int codePoint) {
            return encodable.computeIfAbsent(codePoint, c -> {
                try {
                    regular.encode(new String(Character.toChars(c)));
                    return true;
                } catch (IOException | IllegalArgumentException e) {
                    return false;
                }
            });
        }

        private List<String> wrap(String text, PDFont font, float size) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String paragraph : text.split("\n", -1)) {
                if (paragraph.isBlank()) {
                    lines.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split(" ")) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (width(candidate, font, size) <= maxWidth) {
                        line.setLength(0);
                        line.append(candidate);
                        continue;
                    }
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    String remaining = word;
                    while (width(remaining, font, size) > maxWidth) {
                        int cut = fittingLength(remaining, font, size);
                        lines.add(remaining.substring(0, cut));
                        remaining = remaining.substring(cut);
                    }
                    line.append(remaining);
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
            }
            return lines;
        }

        private int fittingLength(String word, PDFont font, float size) throws IOException {
            float total = 0;
            int length = 0;
            while (length < word.length()) {
                float charWidth = width(String.valueOf(word.charAt(length)), font, size);
                if (total + charWidth > maxWidth && length > 0) {
                    break;
                }
                total += charWidth;
                length++;
            }
            return length;
        }

        private float width(String text, PDFont font, float size) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
