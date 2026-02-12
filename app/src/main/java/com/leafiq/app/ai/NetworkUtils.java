package com.leafiq.app.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Utility class for network connectivity checks and exception classification.
 * <p>
 * Provides:
 * - Pre-check for internet connectivity before making network requests
 * - Classification of network exceptions into user-friendly error messages
 */
public class NetworkUtils {

    // Private constructor prevents instantiation
    private NetworkUtils() {
        throw new AssertionError("NetworkUtils is a utility class and should not be instantiated");
    }

    /**
     * Checks if the device has an active, validated internet connection.
     * <p>
     * Uses NET_CAPABILITY_INTERNET to check for basic connectivity and
     * NET_CAPABILITY_VALIDATED to ensure actual internet access (not just
     * Wi-Fi connected to a router with no internet).
     *
     * @param context Application or Activity context
     * @return true if device has validated internet connection, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) {
            return false;
        }

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Classifies an exception into a user-friendly error message.
     * <p>
     * Classification order (specific before general):
     * 1. SocketTimeoutException -> "Analysis timed out"
     * 2. UnknownHostException/ConnectException -> "No internet connection"
     * 3. HTTP 401/403 -> "Invalid API key" with Settings hint
     * 4. HTTP 500+ -> "Service temporarily unavailable"
     * 5. HTTP 429 -> "Too many requests"
     * 6. HTTP 4xx -> "Request error (code)"
     * 7. Default -> "Analysis failed: [exception message]"
     *
     * @param e Exception caught during analysis
     * @param httpStatusCode HTTP status code (0 if no HTTP response received)
     * @return User-friendly error message
     */
    public static String classifyException(Exception e, int httpStatusCode) {
        // Check exception type first (most specific)
        if (e instanceof SocketTimeoutException) {
            return "Analysis timed out. Please try again.";
        }

        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return "No internet connection. Please check your network.";
        }

        // Check HTTP status codes
        if (httpStatusCode == 401 || httpStatusCode == 403) {
            return "Invalid API key. Please check your API key in Settings.";
        }

        if (httpStatusCode >= 500) {
            return "Service temporarily unavailable. Please try again later.";
        }

        if (httpStatusCode == 429) {
            return "Too many requests. Please wait a moment and try again.";
        }

        if (httpStatusCode >= 400) {
            return "Request error (" + httpStatusCode + "). Please try again.";
        }

        // Default fallback
        return "Analysis failed: " + e.getMessage();
    }
}
