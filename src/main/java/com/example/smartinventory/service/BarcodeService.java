package com.example.smartinventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Renders scannable barcode and QR symbols as PNG images. */
@Service
public class BarcodeService {

    /** Image format written for every generated symbol. */
    static final String IMAGE_FORMAT = "PNG";

    /** Default edge length, in pixels, of a generated QR symbol. */
    static final int DEFAULT_QR_SIZE = 300;

    /** Default width, in pixels, of a generated Code 128 symbol. */
    static final int DEFAULT_BARCODE_WIDTH = 400;

    /** Default height, in pixels, of a generated Code 128 symbol. */
    static final int DEFAULT_BARCODE_HEIGHT = 120;

    /** Quiet-zone margin, in modules, left around a generated symbol. */
    private static final int MARGIN = 2;

    /**
     * Renders {@code content} as a Code 128 linear barcode.
     *
     * @param content the text to encode; must not be blank
     * @return the PNG bytes of the barcode image
     */
    public byte[] generateBarcode(String content) {
        BitMatrix matrix = encode(new Code128Writer(), require(content), BarcodeFormat.CODE_128,
                DEFAULT_BARCODE_WIDTH, DEFAULT_BARCODE_HEIGHT, Map.of(EncodeHintType.MARGIN, MARGIN));
        return write(matrix);
    }

    /**
     * Renders {@code content} as a QR code.
     *
     * @param content the text to encode; must not be blank
     * @return the PNG bytes of the QR image
     */
    public byte[] generateQrCode(String content) {
        BitMatrix matrix = encode(new QRCodeWriter(), require(content), BarcodeFormat.QR_CODE,
                DEFAULT_QR_SIZE, DEFAULT_QR_SIZE,
                Map.of(EncodeHintType.MARGIN, MARGIN, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
        return write(matrix);
    }

    private static BitMatrix encode(Writer writer, String content, BarcodeFormat format,
            int width, int height, Map<EncodeHintType, ?> hints) {
        try {
            return writer.encode(content, format, width, height, hints);
        } catch (WriterException ex) {
            throw new IllegalStateException("Failed to encode " + format + " symbol", ex);
        }
    }

    private static String require(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Barcode content must not be blank");
        }
        return content;
    }

    private static byte[] write(BitMatrix matrix) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(matrix, IMAGE_FORMAT, out);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write barcode image", ex);
        }
        return out.toByteArray();
    }

}
