package nl.ibs.gym8020.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.MainActivity
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.DoneTraining
import nl.ibs.gym8020.core.logic.PlanSlot
import nl.ibs.gym8020.core.logic.Reminder
import nl.ibs.gym8020.core.logic.ReminderPlanner
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.toLocalDate
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Local reminders (D-65): no server, the phone schedules them itself. Times are approximate
 * (within about 15 minutes), which needs no special alarm permission.
 */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val DAILY = "daily-reminder"
    private const val OPEN = "open-session"
    /** A training still open after this long probably was not finished. */
    val OPEN_AFTER: Duration = Duration.ofHours(3)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.rem_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)plans the daily check at the chosen time; cancels it when all daily reminders are off. */
    fun reschedule(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (!settings.reminders.any) {
            wm.cancelUniqueWork(DAILY)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.ofSecondOfDay(settings.reminderMinutes * 60L))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    /** After starting a training: check later whether it was finished. */
    fun onTrainingStarted(context: Context, sessionId: Long, settings: AppSettings) {
        if (!settings.reminderOpenSession) return
        val request = OneTimeWorkRequestBuilder<OpenSessionWorker>()
            .setInitialDelay(OPEN_AFTER.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("session" to sessionId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(OPEN, ExistingWorkPolicy.REPLACE, request)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_gym)
            .setColor(0xFFF9C852.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn in the meantime.
        }
    }
}

/** The daily check: training day, behind on the week target, body weight. */
class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as GymApp).container
        val settings = c.settings.current()
        val today = LocalDate.now()
        val plan = c.db.weekPlans().observeActive().first()
        val slots = plan?.let { p -> c.db.weekPlans().allSlots().filter { it.weekPlanId == p.id } }.orEmpty()
        val sessions = c.db.sessions().all()
        // A training in progress counts as today's training.
        if (sessions.any { it.endedAt == null && it.startedAt.toLocalDate() == today }) return Result.success()
        val history = sessions.filter { it.endedAt != null }.map { DoneTraining(it.startedAt.toLocalDate(), it.schemaId) }
        val lastBodyweight = c.db.bodyweights().latestOnOrBefore(today.toEpochDay())?.epochDay?.let(LocalDate::ofEpochDay)
        val recent = sessions.filter { it.startedAt.toLocalDate().isAfter(today.minusDays(30)) }.map { it.id }.toSet()
        val bodyweightIds = c.db.exercises().all().filter { it.loadType == LoadType.BODYWEIGHT }.map { it.id }.toSet()
        val usesBodyweight = c.db.sessions().allExercises().any { it.sessionId in recent && it.exerciseId in bodyweightIds }

        val reminders = ReminderPlanner.daily(
            today, slots.map { PlanSlot(it.schemaId, it.day?.let(DayOfWeek::of)) }, plan?.targetPerWeek,
            history, lastBodyweight, usesBodyweight, settings.reminders,
        )
        val ctx = applicationContext
        reminders.forEach { r ->
            when (r) {
                is Reminder.TrainingDay -> {
                    val name = c.db.schemas().get(r.schemaId)?.name ?: ""
                    Reminders.show(ctx, 1, ctx.getString(R.string.rem_day_title), ctx.getString(R.string.rem_day_text, name))
                }
                is Reminder.Behind -> Reminders.show(
                    ctx, 2, ctx.getString(R.string.rem_behind_title),
                    ctx.resources.getQuantityString(R.plurals.rem_behind_text, r.remaining, r.remaining, r.daysLeft),
                )
                Reminder.Bodyweight -> Reminders.show(ctx, 3, ctx.getString(R.string.rem_bw_title), ctx.getString(R.string.rem_bw_text))
            }
        }
        return Result.success()
    }
}

/** A few hours after starting: is the training still open? */
class OpenSessionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as GymApp).container
        val id = inputData.getLong("session", -1)
        val session = c.db.sessions().get(id)
        if (session != null && session.endedAt == null) {
            Reminders.show(applicationContext, 4, applicationContext.getString(R.string.rem_open_title), applicationContext.getString(R.string.rem_open_text))
        }
        return Result.success()
    }
}
