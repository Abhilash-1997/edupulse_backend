package com.school.management.service;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    /**
     * Generates a PDF from a Thymeleaf template.
     *
     * @param templateName the name of the Thymeleaf template (without .html
     *                     extension)
     * @param variables    the variables to pass to the template
     * @return byte array containing the PDF
     */
    public byte[] generatePdf(String templateName, Map<String, Object> variables) {
        // Create Thymeleaf context with variables
        Context context = new Context();
        context.setVariables(variables);

        // Process the template to HTML
        String htmlContent = templateEngine.process(templateName, context);

        // Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF from template: {}", templateName, e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
