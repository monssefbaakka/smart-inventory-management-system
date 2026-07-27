package com.example.smartinventory.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class BarcodeServiceTest {

    private final BarcodeService barcodeService = new BarcodeService();

    @Test
    void generateBarcodeProducesScannableCode128() throws Exception {
        byte[] png = barcodeService.generateBarcode("SKU-1");

        Result result = decode(png, BarcodeFormat.CODE_128);
        assertThat(result.getText()).isEqualTo("SKU-1");
        assertThat(result.getBarcodeFormat()).isEqualTo(BarcodeFormat.CODE_128);
    }

    @Test
    void generateQrCodeProducesScannableQr() throws Exception {
        byte[] png = barcodeService.generateQrCode("5901234123457");

        Result result = decode(png, BarcodeFormat.QR_CODE);
        assertThat(result.getText()).isEqualTo("5901234123457");
        assertThat(result.getBarcodeFormat()).isEqualTo(BarcodeFormat.QR_CODE);
    }

    @Test
    void generateBarcodeUsesConfiguredImageSize() throws Exception {
        BufferedImage image = read(barcodeService.generateBarcode("SKU-1"));

        assertThat(image.getWidth()).isEqualTo(BarcodeService.DEFAULT_BARCODE_WIDTH);
        assertThat(image.getHeight()).isEqualTo(BarcodeService.DEFAULT_BARCODE_HEIGHT);
    }

    @Test
    void generateQrCodeUsesConfiguredImageSize() throws Exception {
        BufferedImage image = read(barcodeService.generateQrCode("SKU-1"));

        assertThat(image.getWidth()).isEqualTo(BarcodeService.DEFAULT_QR_SIZE);
        assertThat(image.getHeight()).isEqualTo(BarcodeService.DEFAULT_QR_SIZE);
    }

    @Test
    void generateBarcodeRejectsBlankContent() {
        assertThatThrownBy(() -> barcodeService.generateBarcode("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void generateQrCodeRejectsNullContent() {
        assertThatThrownBy(() -> barcodeService.generateQrCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    private static BufferedImage read(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private static Result decode(byte[] png, BarcodeFormat format) throws IOException, NotFoundException {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(read(png))));
        Map<DecodeHintType, Object> hints = Map.of(DecodeHintType.POSSIBLE_FORMATS, List.of(format));
        return new MultiFormatReader().decode(bitmap, hints);
    }

}
