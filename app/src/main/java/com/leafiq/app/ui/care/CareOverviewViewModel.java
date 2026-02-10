package com.leafiq.app.ui.care;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.leafiq.app.LeafIQApplication;
import com.leafiq.app.care.CareScheduleManager;
import com.leafiq.app.data.entity.CareSchedule;
import com.leafiq.app.data.entity.Plant;
import com.leafiq.app.data.model.CareCompletionWithPlantInfo;
import com.leafiq.app.data.repository.PlantRepository;
import com.leafiq.app.util.DateFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * ViewModel for Care Overview screen.
 * Provides LiveData for today's tasks, upcoming 7-day tasks, and recent completions.
 * Delegates mark complete and snooze operations to CareScheduleManager.
 */
public class CareOverviewViewModel extends AndroidViewModel {

    private final PlantRepository repository;
    private final CareScheduleManager careScheduleManager;
    private final Executor ioExecutor;

    private final MediatorLiveData<List<CareTaskItem>> todayTasks;
    private final MediatorLiveData<List<CareTaskItem>> upcomingTasks;
    private final MediatorLiveData<List<CareCompletionItem>> recentCompletions;

    /**
     * POJO to pair a care schedule with its plant.
     */
    public static class CareTaskItem {
        public final CareSchedule schedule;
        public final Plant plant;

        public CareTaskItem(CareSchedule schedule, Plant plant) {
            this.schedule = schedule;
            this.plant = plant;
        }
    }

    /**
     * POJO for care completion with plant info.
     */
    public static class CareCompletionItem {
        public final String scheduleId;
        public final String careType;
        public final String displayText;     // "Watered Monstera"
        public final String relativeTime;    // "2 days ago"
        public final String plantId;         // For navigation
        public final long completedAt;

        public CareCompletionItem(String scheduleId, String careType,
                                   String displayText, String relativeTime,
                                   String plantId, long completedAt) {
            this.scheduleId = scheduleId;
            this.careType = careType;
            this.displayText = displayText;
            this.relativeTime = relativeTime;
            this.plantId = plantId;
            this.completedAt = completedAt;
        }
    }

    public CareOverviewViewModel(@NonNull Application application) {
        super(application);

        LeafIQApplication app = (LeafIQApplication) application;
        this.repository = app.getPlantRepository();
        this.careScheduleManager = app.getCareScheduleManager();
        this.ioExecutor = app.getAppExecutors().io();

        this.todayTasks = new MediatorLiveData<>();
        this.upcomingTasks = new MediatorLiveData<>();
        this.recentCompletions = new MediatorLiveData<>();

        initializeLiveData();
    }

    /**
     * Gets today's due tasks (schedules where nextDue <= end of today).
     */
    public LiveData<List<CareTaskItem>> getTodayTasks() {
        return todayTasks;
    }

    /**
     * Gets upcoming 7-day tasks (schedules where nextDue > end of today AND nextDue <= 7 days from now).
     */
    public LiveData<List<CareTaskItem>> getUpcomingTasks() {
        return upcomingTasks;
    }

    /**
     * Gets recent completions (last 10).
     */
    public LiveData<List<CareCompletionItem>> getRecentCompletions() {
        return recentCompletions;
    }

    /**
     * Marks a care task as complete.
     * Delegates to CareScheduleManager.
     *
     * @param scheduleId Schedule ID to mark complete
     * @param callback Callback for success/error
     */
    public void markComplete(String scheduleId, PlantRepository.RepositoryCallback<Void> callback) {
        repository.markCareComplete(scheduleId, "in_app", callback);
    }

    /**
     * Snoozes a care task.
     * Delegates to CareScheduleManager.
     *
     * @param scheduleId Schedule ID to snooze
     * @param snoozeOption Snooze option (0 = 6 hours, 1 = 1 day, 2 = next due window)
     * @param callback Callback for success/error
     */
    public void snooze(String scheduleId, int snoozeOption, PlantRepository.RepositoryCallback<Void> callback) {
        ioExecutor.execute(() -> {
            try {
                CareSchedule schedule = repository.getScheduleByIdSync(scheduleId);
                if (schedule == null) {
                    callback.onError(new Exception("Schedule not found"));
                    return;
                }

                long now = System.currentTimeMillis();
                long newNextDue;

                switch (snoozeOption) {
                    case 0: // 6 hours
                        newNextDue = now + (6 * 60 * 60 * 1000L);
                        break;
                    case 1: // Tomorrow (24 hours)
                        newNextDue = now + (24 * 60 * 60 * 1000L);
                        break;
                    case 2: // Next due window (add full frequency)
                        newNextDue = schedule.nextDue + (schedule.frequencyDays * 24L * 60 * 60 * 1000);
                        break;
                    default:
                        callback.onError(new Exception("Invalid snooze option"));
                        return;
                }

                schedule.nextDue = newNextDue;
                schedule.snoozeCount++;

                repository.updateSchedule(schedule, callback);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Initializes LiveData by observing all plants and filtering schedules by date ranges.
     */
    private void initializeLiveData() {
        // Observe all plants (when plants change, schedules might have changed)
        LiveData<List<Plant>> allPlantsSource = repository.getAllPlants();

        // Add source to mediator - when plants change, refresh tasks
        todayTasks.addSource(allPlantsSource, plants -> refreshTasks());
        upcomingTasks.addSource(allPlantsSource, plants -> refreshTasks());
        recentCompletions.addSource(allPlantsSource, plants -> refreshRecentCompletions());
    }

    /**
     * Refreshes task lists by querying schedules and loading plants on background thread.
     */
    private void refreshTasks() {
        ioExecutor.execute(() -> {
            try {
                // Calculate time ranges
                Calendar now = Calendar.getInstance();
                Calendar endOfToday = Calendar.getInstance();
                endOfToday.set(Calendar.HOUR_OF_DAY, 23);
                endOfToday.set(Calendar.MINUTE, 59);
                endOfToday.set(Calendar.SECOND, 59);
                endOfToday.set(Calendar.MILLISECOND, 999);

                Calendar sevenDaysFromNow = Calendar.getInstance();
                sevenDaysFromNow.add(Calendar.DAY_OF_MONTH, 7);
                sevenDaysFromNow.set(Calendar.HOUR_OF_DAY, 23);
                sevenDaysFromNow.set(Calendar.MINUTE, 59);
                sevenDaysFromNow.set(Calendar.SECOND, 59);
                sevenDaysFromNow.set(Calendar.MILLISECOND, 999);

                long endOfTodayTimestamp = endOfToday.getTimeInMillis();
                long sevenDaysTimestamp = sevenDaysFromNow.getTimeInMillis();

                // Get all enabled schedules
                List<CareSchedule> allSchedules = repository.getAllEnabledSchedulesSync();

                // Filter into today and upcoming
                List<CareTaskItem> todayList = new ArrayList<>();
                List<CareTaskItem> upcomingList = new ArrayList<>();

                for (CareSchedule schedule : allSchedules) {
                    Plant plant = repository.getPlantByIdSync(schedule.plantId);
                    if (plant == null) continue;

                    CareTaskItem item = new CareTaskItem(schedule, plant);

                    if (schedule.nextDue <= endOfTodayTimestamp) {
                        todayList.add(item);
                    } else if (schedule.nextDue <= sevenDaysTimestamp) {
                        upcomingList.add(item);
                    }
                }

                // Post results to main thread
                todayTasks.postValue(todayList);
                upcomingTasks.postValue(upcomingList);

            } catch (Exception e) {
                android.util.Log.e("CareOverviewViewModel", "Error refreshing tasks", e);
            }
        });
    }

    /**
     * Refreshes recent completions list by querying database on background thread.
     * Loads completions from last 14 days, max 7 entries, sorted most recent first.
     */
    private void refreshRecentCompletions() {
        ioExecutor.execute(() -> {
            try {
                // Calculate 14 days ago
                long fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000);

                // Query database for recent completions (max 7 entries)
                List<CareCompletionWithPlantInfo> completions =
                        repository.getRecentCompletionsSync(fourteenDaysAgo, 7);

                // Transform to CareCompletionItem
                List<CareCompletionItem> items = new ArrayList<>();
                for (CareCompletionWithPlantInfo completion : completions) {
                    // Plant display name: nickname if non-null/non-empty, else commonName, else "Your plant"
                    String plantName;
                    if (completion.nickname != null && !completion.nickname.isEmpty()) {
                        plantName = completion.nickname;
                    } else if (completion.commonName != null && !completion.commonName.isEmpty()) {
                        plantName = completion.commonName;
                    } else {
                        plantName = "Your plant";
                    }

                    // Past tense verb
                    String verb = getPastTenseVerb(completion.careType);

                    // Display text: "Watered Monstera"
                    String displayText = verb + " " + plantName;

                    // Relative time: "2 days ago"
                    String relativeTime = DateFormatter.getRelativeTime(getApplication(), completion.completedAt);

                    items.add(new CareCompletionItem(
                            completion.scheduleId,
                            completion.careType,
                            displayText,
                            relativeTime,
                            completion.plantId,
                            completion.completedAt
                    ));
                }

                // Post results to main thread
                recentCompletions.postValue(items);

            } catch (Exception e) {
                Log.e("CareSystem", "Error refreshing recent completions", e);
            }
        });
    }

    /**
     * Gets past tense verb for care type.
     */
    private String getPastTenseVerb(String careType) {
        switch (careType) {
            case "water":
                return "Watered";
            case "fertilize":
                return "Fertilized";
            case "repot":
                return "Repotted";
            case "prune":
                return "Pruned";
            default:
                return "Cared for";
        }
    }
}
