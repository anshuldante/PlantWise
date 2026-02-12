package com.leafiq.app.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class NetworkUtilsTest {

    @Test
    public void classifyException_socketTimeout_returnsTimeoutMessage() {
        String result = NetworkUtils.classifyException(
            new SocketTimeoutException("timeout"), 0);
        assertThat(result).contains("timed out");
    }

    @Test
    public void classifyException_unknownHost_returnsNoInternetMessage() {
        String result = NetworkUtils.classifyException(
            new UnknownHostException("host"), 0);
        assertThat(result).contains("No internet");
    }

    @Test
    public void classifyException_connectException_returnsNoInternetMessage() {
        String result = NetworkUtils.classifyException(
            new ConnectException("refused"), 0);
        assertThat(result).contains("No internet");
    }

    @Test
    public void classifyException_http401_returnsInvalidApiKeyMessage() {
        String result = NetworkUtils.classifyException(
            new AIProviderException("err", null, 401), 401);
        assertThat(result).contains("Invalid API key");
    }

    @Test
    public void classifyException_http403_returnsInvalidApiKeyMessage() {
        String result = NetworkUtils.classifyException(
            new AIProviderException("err", null, 403), 403);
        assertThat(result).contains("Invalid API key");
    }

    @Test
    public void classifyException_http500_returnsServiceUnavailableMessage() {
        String result = NetworkUtils.classifyException(
            new AIProviderException("err", null, 500), 500);
        assertThat(result).contains("temporarily unavailable");
    }

    @Test
    public void classifyException_http503_returnsServiceUnavailableMessage() {
        String result = NetworkUtils.classifyException(
            new AIProviderException("err", null, 503), 503);
        assertThat(result).contains("temporarily unavailable");
    }

    @Test
    public void classifyException_http429_returnsTooManyRequestsMessage() {
        String result = NetworkUtils.classifyException(
            new AIProviderException("err", null, 429), 429);
        assertThat(result).contains("Too many requests");
    }

    @Test
    public void classifyException_genericIOException_returnsAnalysisFailedMessage() {
        String result = NetworkUtils.classifyException(
            new IOException("generic"), 0);
        assertThat(result).contains("Analysis failed");
    }
}
