package com.leafiq.app.util;

import android.util.Log;

import com.leafiq.app.data.db.AnalysisDao;
import com.leafiq.app.data.entity.Analysis;

import java.util.List;

/**
 * Helper for background parse scanning of analyses.
 * Incrementally scans existing analyses on app launch to populate parse_status.
 * <p>
 * Per CONTEXT.md: "Lightweight, incremental scan (5-10 items per launch or when idle).
 * Cache result permanently."
 */
public class ParseScanHelper {

    private static final String TAG = "AnalysisParser";
    private static final int SCAN_BATCH_SIZE = 5;

    /**
     * Scans up to SCAN_BATCH_SIZE analyses that still have parse_status='OK'
     * (default from migration) and re-evaluates their rawResponse.
     * Analyses that actually parse fine stay OK. Those that fail get PARTIAL/FAILED/EMPTY.
     * <p>
     * Must be called on a background thread.
     *
     * @param analysisDao DAO for analysis operations
     */
    public static void scanOnLaunch(AnalysisDao analysisDao) {
        List<Analysis> toScan = analysisDao.getAnalysesNeedingScan(SCAN_BATCH_SIZE);
        int updated = 0;

        for (Analysis analysis : toScan) {
            RobustJsonParser.ParseResult result = RobustJsonParser.parse(analysis.rawResponse);

            // Only update if status changed from OK
            if (!"OK".equals(result.parseStatus)) {
                analysisDao.updateParseStatus(analysis.id, result.parseStatus);
                updated++;
                Log.i(TAG, "parse_scan_updated: id=" + analysis.id +
                    " status=" + result.parseStatus + " hash=" + result.contentHash);
            }
        }

        Log.i(TAG, "parse_scan_complete: scanned=" + toScan.size() + " updated=" + updated);
    }
}
