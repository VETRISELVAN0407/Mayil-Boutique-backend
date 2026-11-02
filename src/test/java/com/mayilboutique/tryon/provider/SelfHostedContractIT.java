package com.mayilboutique.tryon.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;

/**
 * Manual check of the Java client against a RUNNING tryon-service (skipped unless TRYON_CONTRACT_URL is set):
 * {@code TRYON_CONTRACT_URL=http://127.0.0.1:8090 TRYON_CONTRACT_KEY=... mvn test -Dtest=SelfHostedContractIT}.
 * With a stand-in model it proves the two sides agree on the wire format; it says nothing about image quality.
 */
@EnabledIfEnvironmentVariable(named = "TRYON_CONTRACT_URL", matches = ".+")
class SelfHostedContractIT {

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void fullRoundTripAgainstTheRealService() throws Exception {
        TryOnProperties props = new TryOnProperties();
        props.setServiceUrl(System.getenv("TRYON_CONTRACT_URL"));
        props.setServiceApiKey(System.getenv("TRYON_CONTRACT_KEY"));
        SelfHostedTryOnProvider provider = new SelfHostedTryOnProvider(props);

        String id = provider.start(new TryOnRequest(GarmentType.DRESS, png(), List.of(png())));
        assertNotNull(id);

        ProviderStatus.State state = ProviderStatus.State.QUEUED;
        for (int i = 0; i < 100 && state != ProviderStatus.State.DONE && state != ProviderStatus.State.FAILED; i++) {
            Thread.sleep(50);
            state = provider.status(id).state();
        }
        assertEquals(ProviderStatus.State.DONE, state);
        assertTrue(provider.result(id).length > 0);

        provider.delete(id);
        ProviderException gone = assertThrows(ProviderException.class, () -> provider.status(id));
        assertEquals(false, gone.isRetryable());
    }
}
