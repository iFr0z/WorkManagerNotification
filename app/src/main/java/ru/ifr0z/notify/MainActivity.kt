package ru.ifr0z.notify

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar.make
import ru.ifr0z.notify.databinding.MainActivityBinding
import ru.ifr0z.notify.work.NotifyWork
import ru.ifr0z.notify.work.NotifyWork.Companion.NOTIFICATION_ID
import ru.ifr0z.notify.work.NotifyWork.Companion.NOTIFICATION_WORK
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit.MILLISECONDS

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    private lateinit var checkNotificationPermission: ActivityResultLauncher<String>
    private var isPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkNotificationPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            isPermission = isGranted
        }

        userInterface()

        checkPermission()
    }

    private fun checkPermission() {
        if (SDK_INT >= TIRAMISU) {
            if (checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
                isPermission = true
            } else {
                isPermission = false

                checkNotificationPermission.launch(POST_NOTIFICATIONS)
            }
        } else {
            isPermission = true
        }
    }

    private fun userInterface() {
        setSupportActionBar(binding.toolbar)

        val titleNotification = getString(R.string.notification_title)
        binding.collapsingToolbarLayout.title = titleNotification

        binding.doneFab.setOnClickListener {
            if (isPermission) {
                val customCalendar = Calendar.getInstance()
                customCalendar.set(
                    binding.datePicker.year,
                    binding.datePicker.month,
                    binding.datePicker.dayOfMonth,
                    binding.timePicker.hour,
                    binding.timePicker.minute, 0
                )
                val customTime = customCalendar.timeInMillis
                val currentTime = currentTimeMillis()
                if (customTime > currentTime) {
                    val data = Data.Builder().putInt(NOTIFICATION_ID, 0).build()
                    val delay = customTime - currentTime
                    scheduleNotification(delay, data)

                    val titleNotificationSchedule = getString(R.string.notification_schedule_title)
                    val patternNotificationSchedule = getString(R.string.notification_schedule_pattern)
                    make(
                        binding.coordinatorLayout,
                        titleNotificationSchedule + SimpleDateFormat(
                            patternNotificationSchedule, getDefault()
                        ).format(customCalendar.time).toString(),
                        LENGTH_LONG
                    ).show()
                } else {
                    val errorNotificationSchedule = getString(R.string.notification_schedule_error)
                    make(binding.coordinatorLayout, errorNotificationSchedule, LENGTH_LONG).show()
                }
            } else {
                if (SDK_INT >= TIRAMISU) {
                    checkNotificationPermission.launch(POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun scheduleNotification(delay: Long, data: Data) {
        val notificationWork = OneTimeWorkRequest.Builder(NotifyWork::class.java)
            .setInitialDelay(delay, MILLISECONDS).setInputData(data).build()

        val instanceWorkManager = WorkManager.getInstance(this)
        instanceWorkManager.beginUniqueWork(NOTIFICATION_WORK, REPLACE, notificationWork).enqueue()
    }
}
